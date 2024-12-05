package org.opendc.simulator.compute.power.battery;

import org.opendc.simulator.compute.power.CarbonFragment;
import org.opendc.simulator.compute.power.MultiSimPowerSource;
import org.opendc.simulator.compute.power.battery.greenenergy.CarbonPolicy;
import org.opendc.simulator.engine.FlowConsumer;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;

import java.util.List;

/**
 * An Adapter between the Multiplexer, a SimBattery and a SimPowerSource
 * Provides power to the multiplexer from the SimBattery or SimPowerSource based on if green energy is available
 */
public final class BatteryPowerAdapter extends PowerAdapter implements FlowConsumer {
    SimBattery battery;
    private FlowEdge muxEdge;
    private FlowEdge batterySupplierEdge;
    private final FlowEdge powerSourceSupplierEdge;

    private double powerDemand = 0.0f;
    private double combinedPowerSupplied = 0.0f;
    private double combinedEnergyUsage = 0.0f;

    private double batteryEnergyUsage = 0.0f;
    private double batteryPowerSupplied = 0.0f;

    private double powerSourceEnergyUsage = 0.0f;
    private double powerSourcePowerSupplied = 0.0f;

    private final CarbonPolicy carbonPolicy;
    private boolean greenEnergyAvailable = false;


    private long lastUpdate;

    /**
     * Create a new BatteryPowerAdapter
     * @param graph
     * @param max_capacity
     * @param carbonFragments for the SimPowerSource
     * @param startTime
     * @param carbonPolicy to determine if green energy is available ore not
     * @param battery
     */
    public BatteryPowerAdapter(FlowGraph graph, double max_capacity, List<CarbonFragment> carbonFragments, long startTime, CarbonPolicy carbonPolicy, SimBattery battery) {
        //initialize MultiSimPowerSource in super class
        super(graph, new MultiSimPowerSource(graph, max_capacity, carbonFragments, startTime));
        //connect battery and powerSource to each other
        this.battery = battery;
        batterySupplierEdge = new FlowEdge(battery, powerSource);
        battery.addSupplierEdge(batterySupplierEdge);
        ((MultiSimPowerSource)powerSource).addBatteryEdge(batterySupplierEdge);

        //connect battery and powerSupply to the adapter
        powerSourceSupplierEdge = new FlowEdge(this, powerSource);
        batterySupplierEdge = new FlowEdge(this, battery);

        //add the carbon policy
        this.carbonPolicy = carbonPolicy;

        lastUpdate = this.clock.millis();
    }

    /**
     * @return the power demand of the multiplexer
     */
    @Override
    public double getPowerDemand() {
        return this.powerDemand;
    }

    /**
     * @return the combined power draw from the power supply and the battery (in W)
     */
    @Override
    public double getPowerDraw() {
        return this.combinedPowerSupplied;
    }

    /**
     * @return the combined energy usage from the power supply and the battery (in J)
     */
    @Override
    public double getEnergyUsage() {
        return this.combinedEnergyUsage;
    }

    /**
     * @return the energy usage of the power source (in J)
     */
    public double getPowerSourceEnergyUsage() {
        return this.powerSourceEnergyUsage;
    }

    /**
     * @return the energy usage of the battery (in J)
     */
    public double getBatteryEnergyUsage() {
        return this.batteryEnergyUsage;
    }

    public SimBattery getSimBattery() {
        return this.battery;
    }

    @Override
    public void close() {
        powerSource.close();
        battery.close();
        this.closeNode();
    }

    @Override
    public long onUpdate(long now) {
        //Compute if green energy is available
        double carbonIntensity = powerSource.getCarbonIntensity();
        greenEnergyAvailable = carbonPolicy.greenEnergyAvailable(carbonIntensity, now);

        if (greenEnergyAvailable) {
            battery.setCharging();
        }

        //Trigger supply push in powerSource and battery
        battery.onUpdate(now);
        powerSource.onUpdate(now);
        //Update the energy supplied
        updateCounters(now);

        return Long.MAX_VALUE;
    }

    @Override
    public void updateCounters(long now) {
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;

        long duration = now - lastUpdate;
        if (duration > 0) {
            double newEnergyUsage = (this.combinedPowerSupplied * duration * 0.001);
            this.combinedEnergyUsage += newEnergyUsage;

            double newBatteryEnergyUsage  = (this.batteryPowerSupplied * duration * 0.001);
            this.batteryEnergyUsage += newBatteryEnergyUsage;

            double newPowerSourceEnergyUsage = (this.powerSourcePowerSupplied * duration * 0.001);
            this.powerSourceEnergyUsage += newPowerSourceEnergyUsage;
        }
    }

    @Override
    public void updateCounters() {
        updateCounters(clock.millis());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality from Adapter to Multiplexer
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Delegate the demand from the multiplexer to the powerSupply or battery
     * @param consumerEdge always powerSourceSupplierEdge
     * @param newPowerDemand from the multiplexer
     */
    @Override
    public void handleDemand(FlowEdge consumerEdge, double newPowerDemand) {
        this.powerDemand = newPowerDemand;

        this.pushDemand(consumerEdge, newPowerDemand);
        this.invalidate();
    }

    /**
     * Push the newSupply to the multiplexer
     * @param consumerEdge always muxEdge
     * @param newSupply from the PowerSupply or Battery
     */
    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {
        muxEdge.pushSupply(newSupply);
    }

    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.muxEdge = consumerEdge;
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        this.muxEdge = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality from Adapter to Battery and PowerSource
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Handle the supply from the powerSource or battery by pushing it to the multiplexer
     * @param supplierEdge always the mux edge
     * @param newSupply from the powerSource or battery
     */
    @Override
    public void handleSupply(FlowEdge supplierEdge, double newSupply) {
        this.combinedPowerSupplied = newSupply;

        if (supplierEdge.equals(batterySupplierEdge)) {
            powerSourcePowerSupplied = 0;
            batteryPowerSupplied = newSupply;
        }
        else {
            batteryPowerSupplied = 0;
            powerSourcePowerSupplied = newSupply;
        }

        this.pushSupply(muxEdge, newSupply);
        this.invalidate();
    }

    /**
     * Push new demand (from the multiplexer) to the powerSource or battery
     * @param supplierEdge either powerSourceSupplierEdge or batterySupplierEdge
     * @param newDemand (from the multiplexer)
     */
    @Override
    public void pushDemand(FlowEdge supplierEdge, double newDemand) {
        if (greenEnergyAvailable) { // use the power source and charge the battery
            powerSource.handleDemand(powerSourceSupplierEdge, newDemand);
        } else {
            if (battery.isEmpty()) { // when no green energy is available and the battery is empty, use the power source
                battery.setIdle();
                powerSource.handleDemand(powerSourceSupplierEdge, newDemand);
            } else { // deplete the battery when no green energy is available
                battery.setDepleting();
                battery.handleDemand(batterySupplierEdge, newDemand);
                powerSource.handleDemand(powerSourceSupplierEdge, 0); // make sure power supply is not used
            }
        }
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        //
    }

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        //
    }

}
