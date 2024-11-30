package org.opendc.simulator.compute.power.battery;
import org.opendc.simulator.compute.power.MultiSimPowerSource;
import org.opendc.simulator.compute.power.SimPowerSource;
import org.opendc.simulator.engine.FlowEngine;
import org.opendc.simulator.engine.FlowGraph;

public class BatteryPowerAdapterLogger extends MultiSimPowerSource {
    private final BatteryPowerAdapter batteryPowerAdapter;

    public BatteryPowerAdapterLogger(BatteryPowerAdapter batteryPowerAdapter) {
        super(batteryPowerAdapter.getGraph(), 0, null, 0, null);

        this.batteryPowerAdapter = batteryPowerAdapter;
    }

    @Override
    public double getPowerDemand() {
        return batteryPowerAdapter.getPowerDemand();
    }

    @Override
    public double getPowerDraw() {
        return batteryPowerAdapter.getPowerDraw();
    }

    @Override
    public double getCarbonIntensity() {
        return batteryPowerAdapter.getCarbonIntensity();
    }

    @Override
    public double getEnergyUsage() {
        return batteryPowerAdapter.getEnergyUsage();
    }

    @Override
    public double getCarbonEmission() {
        return batteryPowerAdapter.getCarbonEmission();
    }

    public double getAdapterEnergyUsage() {
        return batteryPowerAdapter.getPSUPowerSupplied();
    }

    public double getBatteryEnergyUsage() {
        return batteryPowerAdapter.getBatteryPowerSupplied();
    }


    @Override
    public void close() {
        batteryPowerAdapter.close();
    }
}