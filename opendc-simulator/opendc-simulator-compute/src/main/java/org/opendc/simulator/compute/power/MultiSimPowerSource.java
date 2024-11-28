package org.opendc.simulator.compute.power;

import org.opendc.simulator.engine.FlowConsumer;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;
import org.opendc.simulator.engine.FlowSupplier;

import java.util.List;

public class MultiSimPowerSource extends SimPowerSource {
    private FlowEdge batteryEdge;

    private double batteryPowerDemand = 0.0f;
    private double batteryPowerSupplied = 0.0f;
    private double newBatteryDemand = 0.0f;

    public MultiSimPowerSource(FlowGraph graph, double max_capacity, List<CarbonFragment> carbonFragments, long startTime) {
        super(graph, max_capacity, carbonFragments, startTime);
    }

    @Override
    public long onUpdate(long now) {
        updateCounters(now);

        super.supplyMuxEdge();
        supplyBatteryEdge();

        return Long.MAX_VALUE;
    }

    private void supplyBatteryEdge() {
        if (newBatteryDemand != 0) {
            this.pushSupply(this.batteryEdge, newBatteryDemand);
            newBatteryDemand = 0;
        }
    }

    @Override
    public void updateCounters(long now) {
        long duration = now - lastUpdate;
        if (duration > 0) {
            super.updateCounters(now);
            double batteryEnergyUsage = (this.batteryPowerSupplied * duration * 0.001);

            // Compute the energy usage of the machine
            this.totalEnergyUsage += batteryEnergyUsage;
        }
    }

    @Override
    public void handleDemand(FlowEdge consumerEdge, double newPowerDemand) {
        if (consumerEdge.equals(batteryEdge)) {
            this.newBatteryDemand = newPowerDemand;
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
