package org.opendc.simulator.compute.power.battery.greenenergy;

/**
 * An interface to determine if greenEnergy is available based on carbon intensity
 */
public interface CarbonPolicy {
    public boolean greenEnergyAvailable(double carbonIntensity, long now);
}
