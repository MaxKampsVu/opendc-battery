package org.opendc.simulator.compute.power.battery.greenenergy;

public class SimpleCarbonPolicy implements CarbonPolicy {
    private final double carbonIntensityThreshold = 200;

    @Override
    public boolean greenEnergyAvailable(double carbonIntensity, long now) {
        return carbonIntensity < carbonIntensityThreshold;
    }
}
