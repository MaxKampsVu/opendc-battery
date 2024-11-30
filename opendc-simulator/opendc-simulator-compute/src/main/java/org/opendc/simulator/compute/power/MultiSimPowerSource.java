package org.opendc.simulator.compute.power;

import org.opendc.simulator.compute.power.battery.SimBattery;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;

import java.util.List;

public class MultiSimPowerSource extends SimPowerSource {
    private FlowEdge batteryEdge;
    private long lastBatteryUpdate;

    private SimBattery battery;

    private double batteryPowerDemand = 0.0f;
    private double batteryPowerSupplied = 0.0f;
    private double batteryEnergyUsage = 0.0f;

    public MultiSimPowerSource(FlowGraph graph, double max_capacity, List<CarbonFragment> carbonFragments, long startTime, SimBattery battery) {
        super(graph, max_capacity, carbonFragments, startTime);
        lastBatteryUpdate = this.clock.millis();
        this.battery = battery;
    }


    @Override
    public double getPowerDemand() {
        return super.getPowerDemand() + batteryPowerDemand;
    }

    /**
     * Return the instantaneous power usage of the machine (in W) measured at the InPort of the power supply.
     */
    @Override
    public double getPowerDraw() {
        return super.getPowerDraw() + batteryPowerSupplied;
    }

    /**
     * Return the cumulated energy usage of the machine (in J) measured at the InPort of the powers supply.
     */
    @Override
    public double getEnergyUsage() {
        return super.getEnergyUsage() + batteryEnergyUsage;
    }

    public double getAdapterEnergyUsage() {
        return super.getEnergyUsage();
    }

    public double getBatteryEnergyUsage() {
        return this.batteryEnergyUsage;
    }


    @Override
    public long onUpdate(long now) {
        updateCounters(now);

        super.supplyMuxEdge();
        supplyBatteryEdge();

        return Long.MAX_VALUE;
    }

    private void supplyBatteryEdge() {
        double batteryPowerSupply = this.batteryPowerDemand;
        if (!battery.isCharging()) {
            this.batteryPowerDemand = 0; //make sure battery is not being supplied with power if it is not chagring
        }

        if (batteryPowerSupply != this.batteryPowerSupplied) {
            this.pushSupply(batteryEdge, batteryPowerSupply);
        }
    }

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

    @Override
    public void handleDemand(FlowEdge consumerEdge, double newPowerDemand) {
        if (consumerEdge.equals(batteryEdge)) {
            this.batteryPowerDemand = newPowerDemand;
            this.invalidate();
        } else {
            super.handleDemand(consumerEdge, newPowerDemand);
        }
    }

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
