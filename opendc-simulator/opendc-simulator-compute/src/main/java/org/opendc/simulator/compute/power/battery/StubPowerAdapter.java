package org.opendc.simulator.compute.power.battery;

import org.opendc.simulator.compute.power.CarbonFragment;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;

import java.util.List;

/**
 * An implementation of PowerAdapter that keeps the functionality of the simulator unchanged
 * (only connecting a SimPowerSource to the Multiplexer)
 */
public final class StubPowerAdapter extends PowerAdapter {

    public StubPowerAdapter(FlowGraph graph, double max_capacity, List<CarbonFragment> carbonFragments, long startTime) {
        super(graph, max_capacity, carbonFragments, startTime);
    }

    @Override
    public double getPowerDraw() {
        return powerSource.getPowerDraw();
    }

    @Override
    public double getEnergyUsage() {
        return powerSource.getEnergyUsage();
    }

    @Override
    public void close() {
        powerSource.close();
    }

    @Override
    public long onUpdate(long now) {
        return powerSource.onUpdate(now);
    }

    @Override
    public void updateCounters() {
        powerSource.updateCounters();
    }

    @Override
    public void updateCounters(long now) {
        powerSource.updateCounters(now);
    }

    @Override
    public void handleDemand(FlowEdge consumerEdge, double newPowerDemand) {
        powerSource.handleDemand(consumerEdge, newPowerDemand);
    }

    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {
        powerSource.pushSupply(consumerEdge, newSupply);
    }

    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        powerSource.addConsumerEdge(consumerEdge);
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        powerSource.removeConsumerEdge(consumerEdge);
    }
}
