package org.opendc.simulator.compute.power.battery;

import org.opendc.simulator.compute.cpu.SimCpu;
import org.opendc.simulator.engine.FlowConsumer;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;
import org.opendc.simulator.engine.FlowNode;
import org.opendc.simulator.engine.FlowSupplier;

public class SimBattery extends FlowNode implements FlowSupplier, FlowConsumer {
    private long lastUpdate;

    private double powerDemand = 0.0f;
    private double powerSupplied = 0.0f;
    private double totalEnergyUsage = 0.0f;
    private long currentDuration = 0;

    private double capacity = 1000.0f;
    private double chargePerInterval = 30.0f;
    private int intervalSize = 30000000;
    private double chargeLevel = 0.0f;
    private double totalEnergyCharged = 0.0f;

    enum STATE {
        CHARGING,
        IDLE,
        DEPLETING
    }

    STATE state = STATE.DEPLETING;


    private FlowEdge consumerEdge;
    private FlowEdge supplierEdge;

    /**
     *
     * @return
     */
    public double getChargePerInterval() {return this.chargePerInterval; }

    /**
     * Determine whether the InPort is connected to a {@link SimCpu}.
     *
     * @return <code>true</code> if the InPort is connected to an OutPort, <code>false</code> otherwise.
     */
    public boolean isConnected() {
        return consumerEdge != null;
    }

    /**
     * Return the power demand of the machine (in W) measured in the PSU.
     * <p>
     * This method provides access to the power consumption of the machine before PSU losses are applied.
     */
    public double getPowerDemand() {
        return this.powerDemand;
    }

    public double getChargeSupply() {
        return this.getPowerDemand();
    }

    /**
     * Return the instantaneous power usage of the machine (in W) measured at the InPort of the power supply.
     */
    public double getPowerDraw() {
        return this.powerSupplied;
    }

    /**
     * Return the cumulated energy usage of the machine (in J) measured at the InPort of the powers supply.
     */
    public double getEnergyUsage() {
        return this.totalEnergyUsage;
    }

    public double getEnergyCharged() {
        return this.totalEnergyCharged;
    }

    @Override
    public double getCapacity() {
        return this.capacity;
    }

    public double getChargeLevel() {
        return this.chargeLevel;
    }

    public String getStateString() {
        return this.state.toString();
    }

    public void setCharging() {
        this.state = STATE.CHARGING;
    }

    public void setDepleting() {
        this.state = STATE.DEPLETING;
    }

    public void setIdle() {
        this.state = STATE.IDLE;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimBattery(FlowGraph graph, double max_capacity, long startTime) {
        super(graph);

        this.capacity = max_capacity;

        lastUpdate = this.clock.millis();
    }

    public void close() {
        this.closeNode();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowNode related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {
        updateCounters();

        if (state == STATE.CHARGING) {
            if (chargeLevel < capacity * 0.99) {
                double newChargeDemand = Math.min((capacity - chargeLevel), chargePerInterval) * currentDuration / intervalSize;
                this.pushDemand(this.supplierEdge, newChargeDemand);
            } else {
                state = STATE.IDLE;
            }
        }
        else if (state == STATE.DEPLETING) {
            double powerSupply = this.powerDemand;

            if (powerSupply != this.powerSupplied) {
                this.pushSupply(this.consumerEdge, powerSupply);
            }
        }
        else if (state == STATE.IDLE) {
            //do nothing
        }

        return Long.MAX_VALUE;
    }

    public void updateCounters() {
        updateCounters(clock.millis());
    }

    /**
     * Calculate the energy usage up until <code>now</code>.
     */
    public void updateCounters(long now) {
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;


        long duration = now - lastUpdate;
        this.currentDuration = duration;
        if (duration > 0) {
            double energyUsage = (this.powerSupplied * duration * 0.001);

            // Compute the energy usage of the machine
            this.totalEnergyUsage += energyUsage;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Battery as power source functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void handleDemand(FlowEdge consumerEdge, double newPowerDemand) {
        this.powerDemand = newPowerDemand;
        this.invalidate();
    }

    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {

        this.powerSupplied = newSupply;
        consumerEdge.pushSupply(newSupply);
    }

    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.consumerEdge = consumerEdge;
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        this.consumerEdge = null;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Charging functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void handleSupply(FlowEdge supplierEdge, double newSupply) {
        this.chargeLevel += newSupply;
    }

    @Override
    public void pushDemand(FlowEdge supplierEdge, double newDemand) {
        this.supplierEdge.pushDemand(newDemand);
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.supplierEdge = supplierEdge;
    }

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        this.supplierEdge = null;
    }
}
