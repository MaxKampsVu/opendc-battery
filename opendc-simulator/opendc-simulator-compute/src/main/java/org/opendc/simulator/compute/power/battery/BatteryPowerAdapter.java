package org.opendc.simulator.compute.power.battery;

import org.opendc.simulator.compute.power.CarbonFragment;
import org.opendc.simulator.compute.power.MultiSimPowerSource;
import org.opendc.simulator.compute.power.battery.greenenergy.CarbonPolicy;
import org.opendc.simulator.engine.FlowConsumer;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;

import java.util.List;

public final class BatteryPowerAdapter extends PowerAdapter implements FlowConsumer {
    SimBattery battery;
    private FlowEdge muxEdge;
    private FlowEdge batterySupplierEdge;
    private FlowEdge powerSourceSupplierEdge;

    private double combinedPowerDemand = 0.0f;
    private double combinedPowerSupplied = 0.0f;
    private double combinedEnergyUsage = 0.0f;

    private double batteryEnergyUsage = 0.0f;
    private double batteryPowerSupplied = 0.0f;

    private double powerSourceEnergyUsage = 0.0f;
    private double powerSourcePowerSupplied = 0.0f;

    private CarbonPolicy carbonPolicy;
    private boolean greenEnergyAvailable = false;

    private long lastUpdate;

    public BatteryPowerAdapter(FlowGraph graph, double max_capacity, List<CarbonFragment> carbonFragments, long startTime, CarbonPolicy carbonPolicy, SimBattery battery) {
        //initialize MultiSimPowerSource in super class
        super(graph, new MultiSimPowerSource(graph, max_capacity, carbonFragments, startTime, battery));

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

    @Override
    public double getPowerDemand() {
        return this.combinedPowerDemand;
    }

    @Override
    public double getPowerDraw() {
        return this.combinedPowerSupplied;
    }

    @Override
    public double getEnergyUsage() {
        return this.combinedEnergyUsage;
    }

    public double getPowerSourceEnergyUsage() {
        return this.powerSourceEnergyUsage;
    }

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
        //Trigger supply push in powerSource and battery
        powerSource.onUpdate(now);
        battery.onUpdate(now);
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
        this.combinedPowerDemand = newPowerDemand;

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
        if (greenEnergyAvailable) {
            powerSource.handleDemand(powerSourceSupplierEdge, newDemand);
            battery.setCharging();
        } else {
            if (battery.isEmpty()) {
                battery.setIdle();
                powerSource.handleDemand(powerSourceSupplierEdge, newDemand);
            } else {
                battery.setDepleting();
                battery.handleDemand(batterySupplierEdge, newDemand);
            }
        }
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        //TODO: Figure out how to handle
    }

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        //TODO: Figure out how to handle
    }

}
