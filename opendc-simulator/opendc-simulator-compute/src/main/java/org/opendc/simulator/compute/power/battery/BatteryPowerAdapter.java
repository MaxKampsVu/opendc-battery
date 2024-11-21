package org.opendc.simulator.compute.power.battery;

import org.opendc.simulator.compute.power.CarbonFragment;
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

    private CarbonPolicy carbonPolicy;
    private boolean greenEnergyAvailable = false;

    private double powerDemand = 0.0f;
    private double powerSupplied = 0.0f;
    private double totalEnergyUsage = 0.0f;

    public BatteryPowerAdapter(FlowGraph graph, double max_capacity, List<CarbonFragment> carbonFragments, long startTime, CarbonPolicy carbonPolicy) {
        super(graph, max_capacity, carbonFragments, startTime);
        battery = new SimBattery(graph, max_capacity, startTime);

        //connect battery and powerSupply to the adapter
        powerSourceSupplierEdge = new FlowEdge(this, powerSource);
        batterySupplierEdge = new FlowEdge(this, battery);

        this.carbonPolicy = carbonPolicy;
    }

    public SimBattery getSimBattery() {
        return this.battery;
    }

    @Override
    public double getPowerDraw() {
        return this.battery.getPowerDraw() + this.powerSource.getPowerDraw();
    }

    @Override
    public double getEnergyUsage() {
        return this.battery.getEnergyUsage() + this.powerSource.getEnergyUsage();
    }

    public double getPowerSourcePowerDraw() {
        return this.powerSource.getPowerDraw();
    }

    public double getPowerSourceEnergyUsage() {
        return this.powerSource.getEnergyUsage();
    }

    public double getBatteryPowerDraw() {
        return this.battery.getPowerDraw();
    }

    public double getBatteryEnergyUsage() {
        return this.battery.getEnergyUsage();
    }



    public boolean isGreenEnergyAvailable() {
        return this.greenEnergyAvailable;
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

        //.out.println("Green energy available: " + greenEnergyAvailable);

        //Trigger supply push in powerSource and battery
        powerSource.onUpdate(now);
        battery.onUpdate(now);

        return Long.MAX_VALUE;
    }

    @Override
    public void updateCounters() {

    }

    @Override
    public void updateCounters(long now) {

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
        if (greenEnergyAvailable) {
            //.out.println("pushed " + newPowerDemand + " to psu");
            this.pushDemand(powerSourceSupplierEdge, newPowerDemand);
        } else {
            this.pushDemand(batterySupplierEdge, newPowerDemand);
            //.out.println("pushed " + newPowerDemand + " to battery");
        }
        this.invalidate();
    }

    /**
     * Push the newSupply to the multiplexer
     * @param consumerEdge always muxEdge
     * @param newSupply from the PowerSupply or Battery
     */
    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {
        //.out.println("pushed " + newSupply + " to multiplexer");
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
        //.out.println("Received " + newSupply + " from " + supplierEdge.getSupplier().getClass());
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
            powerSource.handleDemand(supplierEdge, newDemand);
        } else {
            battery.handleDemand(supplierEdge, newDemand);
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
