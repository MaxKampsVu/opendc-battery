package org.opendc.simulator.compute.power;

import org.opendc.simulator.compute.power.battery.SimBattery;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;

import java.util.List;

/**
 * Behaves like a SimPowerSource but also supplies power to a Battery
 */
public class MultiSimPowerSource extends SimPowerSource {
    private FlowEdge batteryEdge;
    private long lastBatteryUpdate;

    private double batteryPowerDemand = 0.0f;
    private double batteryPowerSupplied = 0.0f;
    private double batteryEnergyUsage = 0.0f;

    public MultiSimPowerSource(FlowGraph graph, double max_capacity, List<CarbonFragment> carbonFragments, long startTime) {
        super(graph, max_capacity, carbonFragments, startTime);
        lastBatteryUpdate = this.clock.millis();
    }

    public double getAdapterPowerDemand() {
        return super.getPowerDemand();
    }

    /**
     * @return the power demand of the adapter and the battery (in W)
     */
    @Override
    public double getPowerDemand() {
        return super.getPowerDemand() + batteryPowerDemand;
    }

    /**
     * @return the power draw to the adapter and the battery (in W)
     */
    @Override
    public double getPowerDraw() {
        return super.getPowerDraw() + batteryPowerSupplied;
    }

    /**
     * @return the cumulated energy usage of the adapter and battery (in J)
     */
    @Override
    public double getEnergyUsage() {
        return super.getEnergyUsage() + batteryEnergyUsage;
    }

    /**
     * @return the cumulated energy usage of the adapter
     */
    public double getAdapterEnergyUsage() {
        return super.getEnergyUsage();
    }

    /**
     * @return the cumulated energy usage of the battery
     */
    public double getBatteryEnergyUsage() {
        return this.batteryEnergyUsage;
    }

    /**
     * Supply power to the adapter and the battery
     * @param now The virtual timestamp in milliseconds after epoch at which the update is occurring.
     * @return
     */
    @Override
    public long onUpdate(long now) {
        updateCounters(now);

        super.supplyMuxEdge();
        supplyBatteryEdge();

        return Long.MAX_VALUE;
    }

    /**
     * Supply power to the battery
     */
    private void supplyBatteryEdge() {
        double batteryPowerSupply = this.batteryPowerDemand;

        if (batteryPowerSupply != this.batteryPowerSupplied) {
            this.pushSupply(batteryEdge, batteryPowerSupply);
        }
    }

    /**
     * Energy usage and carbon emissions for the adapter is computed in the super class
     * Add the energy usage and carbon emissions for the battery
     * @param now
     */
    @Override
    public void updateCounters(long now) {
        super.updateCounters(now);

        long lastUpdate = this.lastBatteryUpdate;
        this.lastBatteryUpdate = now;

        long duration = now - lastUpdate;
        if (duration > 0) {
            double batteryEnergyUsage = (this.batteryPowerSupplied * duration * 0.001);
            this.batteryEnergyUsage += batteryEnergyUsage;
            this.totalCarbonEmission += this.carbonIntensity * (batteryEnergyUsage / 3600000.0);
        }
    }

    /**
     * Determine if demand is from the battery or the adapter and handle it accordingly
     * @param consumerEdge
     * @param newPowerDemand
     */
    @Override
    public void handleDemand(FlowEdge consumerEdge, double newPowerDemand) {
        if (consumerEdge.equals(batteryEdge)) {
            this.batteryPowerDemand = newPowerDemand;
            this.invalidate();
        } else {
            super.handleDemand(consumerEdge, newPowerDemand);
        }
    }

    /**
     * Determine if supply is for the battery or the adapter and supply accordingly
     * @param consumerEdge
     * @param newSupply
     */
    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {
        if (consumerEdge.equals(batteryEdge)) {
            this.batteryPowerSupplied = newSupply;
            batteryEdge.pushSupply(newSupply);
        } else {
            super.pushSupply(consumerEdge, newSupply);
        }
    }

    public void addBatteryEdge(FlowEdge batteryEdge) {
        this.batteryEdge = batteryEdge;
    }
}
