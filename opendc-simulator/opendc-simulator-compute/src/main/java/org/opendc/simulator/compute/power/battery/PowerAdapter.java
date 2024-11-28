/*
 * Copyright (c) 2024 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.simulator.compute.power.battery;

import java.util.List;

import org.opendc.simulator.compute.power.CarbonFragment;
import org.opendc.simulator.compute.power.SimPowerSource;
import org.opendc.simulator.compute.power.SimPsu;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;
import org.opendc.simulator.engine.FlowNode;
import org.opendc.simulator.engine.FlowSupplier;

/**
 * A {@link SimPsu} implementation that estimates the power consumption based on CPU usage.
 */
public abstract class PowerAdapter extends FlowNode implements FlowSupplier {
    protected SimPowerSource powerSource;

    public boolean isConnected() {
        return powerSource.isConnected();
    }

    public SimPowerSource getSimPowerSource() {
        return this.powerSource;
    }

    public double getPowerDemand() {
        return powerSource.getPowerDemand();
    }

    public abstract double getPowerDraw();

    public double getCarbonIntensity() {
        return powerSource.getCarbonIntensity();
    }

    public abstract double getEnergyUsage();

    public double getCarbonEmission() {
        return powerSource.getCarbonEmission();
    }

    public double getCapacity() {
        return powerSource.getCapacity();
    }

    public PowerAdapter(FlowGraph graph, double max_capacity, List<CarbonFragment> carbonFragments, long startTime) {
        super(graph);
        this.powerSource = new SimPowerSource(graph, max_capacity, carbonFragments, startTime);
    }

    public PowerAdapter(FlowGraph graph, SimPowerSource simPowerSource) {
        super(graph);
        this.powerSource = simPowerSource;
    }

    public abstract void close();

    @Override
    public abstract long onUpdate(long now);

    public abstract void updateCounters();

    public abstract void updateCounters(long now);

    @Override
    public abstract void handleDemand(FlowEdge consumerEdge, double newPowerDemand);

    @Override
    public abstract void pushSupply(FlowEdge consumerEdge, double newSupply);

    @Override
    public abstract void addConsumerEdge(FlowEdge consumerEdge);

    @Override
    public abstract void removeConsumerEdge(FlowEdge consumerEdge);

    public void updateCarbonIntensity(double carbonIntensity) {
        powerSource.updateCarbonIntensity(carbonIntensity);
    }
}
