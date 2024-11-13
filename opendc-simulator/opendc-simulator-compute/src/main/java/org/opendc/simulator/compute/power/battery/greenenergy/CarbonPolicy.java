package org.opendc.simulator.compute.power.battery.greenenergy;

public interface CarbonPolicy {
    public boolean greenEnergyAvailable(double carbonIntensity, long now);
}
