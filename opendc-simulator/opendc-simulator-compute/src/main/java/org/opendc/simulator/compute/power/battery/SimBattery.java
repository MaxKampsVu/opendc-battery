package org.opendc.simulator.compute.power.battery;

import org.opendc.simulator.compute.cpu.SimCpu;
import org.opendc.simulator.engine.FlowConsumer;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;
import org.opendc.simulator.engine.FlowNode;
import org.opendc.simulator.engine.FlowSupplier;

public class SimBattery extends FlowNode implements FlowSupplier, FlowConsumer {
    private long lastUpdate;

    private double powerDemand = 0.0f; // the demand from the adapter
    private double powerSupplied = 0.0f; // the demand supplied to the adapter
    private double totalEnergyUsage = 0.0f; // the total energy supplied

    private final double capacity; // the capacity of the battery (in J = Watt * second)
    private final double chargeCurrent; // the charge current (in W)
    private double chargeLevel = 0.0f; // the charge level (in J = Watt * second)

    enum STATE {
        CHARGING,
        IDLE,
        DEPLETING
    }

    private long chargeDuration = 0; //

    STATE state = STATE.DEPLETING;

    private FlowEdge consumerEdge; // power from battery to adapter -> depletes the battery
    private FlowEdge supplierEdge; // power from power supply to battery -> charges the battery

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

    public SimBattery(FlowGraph graph, double max_capacity, double charge_current) {
        super(graph);

        this.capacity = max_capacity;
        this.chargeCurrent = charge_current;
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
            powerSupplied = 0.0; // no power should be supplied to the adapter
            if (chargeLevel < capacity * 0.9999) {
                // newMaxDemand = current (in W) * duration (in s)
                double newMaxDemand = chargeCurrent * (chargeDuration / 60);
                double newChargeDemand = Math.min((capacity - chargeLevel), newMaxDemand);
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
            powerSupplied = 0.0; // no power should be supplied to the adapter
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
        this.chargeDuration = duration;
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
        this.chargeLevel -= newSupply;
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
