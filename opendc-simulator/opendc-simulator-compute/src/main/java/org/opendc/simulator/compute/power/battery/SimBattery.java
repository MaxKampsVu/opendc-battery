package org.opendc.simulator.compute.power.battery;

import org.opendc.simulator.engine.FlowConsumer;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;
import org.opendc.simulator.engine.FlowNode;
import org.opendc.simulator.engine.FlowSupplier;

/**
 * Battery implementation
 */
public class SimBattery extends FlowNode implements FlowSupplier, FlowConsumer {
    private long lastUpdate;

    private double powerDemand = 0.0f; // the demand from the adapter (in W)
    private double powerSupplied = 0.0f; // the demand supplied to the adapter (in W)
    private double totalEnergyUsage = 0.0f; // the total energy supplied (in J)

    private final double capacity; // the capacity of the battery (in J)
    private final double chargeCurrent; // the charge current (in W)
    private double chargeLevel = 0.0f; // the charge level (in J)
    private double chargeReceived = 0.0f;
    private double totalChargeReceived = 0.0f;

    private final double chargeLowerBound = 0.05f; // minimum depletion
    private final double chargeUpperBound = 0.95f; // maximum charge level

    enum STATE {
        CHARGING, // the battery is being charged by a SimPowerSupply
        IDLE, // do nothing
        DEPLETING // the battery is providing power to the Adapter
    }

    STATE state;

    private FlowEdge consumerEdge; // power from battery to adapter -> depletes the battery
    private FlowEdge supplierEdge; // power from power supply to battery -> charges the battery

    /**
     * Determine whether the InPort is connected to a {@link BatteryPowerAdapter}.
     */
    public boolean isConnected() {
        return consumerEdge != null;
    }

    /**
     * @return the power demand of the machine (in W) measured in the Battery.
     */
    public double getPowerDemand() {
        return this.powerDemand;
    }
    /**
     * @return the instantaneous power usage of the machine (in W) measured at the InPort of the Battery.
     */
    public double getPowerDraw() {
        return this.powerSupplied;
    }

    /**
     * @return the supplied energy (in J) by the Battery.
     */
    public double getEnergyUsage() {
        return this.totalEnergyUsage;
    }

    /**
     * @return the maximum capacity of the battery (in J)
     */
    @Override
    public double getCapacity() {
        return this.capacity;
    }

    /**
     * @return the current charge level of the battery
     */
    public double getChargeLevel() {
        return this.chargeLevel;
    }

    /**
     * @return the total charge received
     */
    public double getTotalChargeReceived() {
        return this.totalChargeReceived;
    }

    /**
     * @return the current state
     */
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

    public boolean isCharging() {
        return this.state == STATE.CHARGING;
    }

    public boolean isDepleting() {
        return this.state == STATE.DEPLETING;
    }

    public boolean isIdle() {
        return this.state == STATE.IDLE;
    }

    public boolean isEmpty() {
        return chargeLevel < capacity * chargeLowerBound;
    }

    public boolean isFull() {
        return chargeLevel > capacity * chargeUpperBound;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a new Battery
     * @param graph
     * @param max_capacity the maximum capacity (in J) to which the battery can be charged
     * @param charge_current the current (in W) with which the battery is charged
     */
    public SimBattery(FlowGraph graph, double max_capacity, double charge_current) {
        super(graph);

        this.capacity = max_capacity;
        this.chargeCurrent = charge_current;

        this.state = STATE.CHARGING;
        lastUpdate = this.clock.millis();
    }

    public void close() {
        this.closeNode();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowNode related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Based on the current state charge, discharge or set the battery to idle
     * @param now The virtual timestamp in milliseconds after epoch at which the update is occurring.
     * @return
     */
    @Override
    public long onUpdate(long now) {
        updateCounters();

        if (state == STATE.CHARGING) {
            powerSupplied = 0.0; // no power should be supplied to the adapter
            if (!isFull()) {
                this.pushDemand(this.supplierEdge, chargeCurrent);
            } else {
                state = STATE.IDLE;
            }
        }
        else if (state == STATE.DEPLETING) {
            double powerSupply = this.powerDemand;
            this.pushDemand(this.supplierEdge, 0); // make sure no charge is supplied by power source
            if (!isEmpty()) {
                if (powerSupply != this.powerSupplied) {
                    this.pushSupply(this.consumerEdge, powerSupply);
                }
            } else {
                state = STATE.IDLE;
            }
        }

        if (state == STATE.IDLE) {
            this.powerDemand = 0.0; // the battery should not demand any power
            this.pushDemand(this.supplierEdge, 0); // make sure no charge is supplied by power source
            powerSupplied = 0.0; // make sure the battery does not supply any power
        }

        return Long.MAX_VALUE; //Compute time to call me again = now + (time to fully deplete/charge me)
    }

    public long computeNextUpdateDuration(long now) {
        long duration = 0;
        if(state == STATE.DEPLETING) { // compute the time at which the battery will be fully depleted
            duration = (long)((this.chargeLevel) /
                    (this.powerSupplied * 0.001));
        } else if (state == STATE.CHARGING) { // compute the time at which the battery will be fully charged
            duration = (long)((this.capacity - this.chargeLevel) /
                (this.chargeReceived * 0.001));
        } else {
            duration = 1000000000;
        }

        return duration;
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
        if (duration > 0) {
            double energyUsage = (this.powerSupplied * duration * 0.001);
            this.totalEnergyUsage += energyUsage;
            this.chargeLevel -= energyUsage;

            double energyReceived = (this.chargeReceived * duration * 0.001);
            this.totalChargeReceived += energyReceived;
            this.chargeLevel += energyReceived;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Battery as power source functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * handle a power demand from the adapter
     * @param consumerEdge to the adapter
     * @param newPowerDemand from the multiplexer
     */
    @Override
    public void handleDemand(FlowEdge consumerEdge, double newPowerDemand) {
        this.powerDemand = newPowerDemand;
        this.invalidate();
    }

    /**
     * push supply to the adapter
     * @param consumerEdge to the adapter
     * @param newSupply for the multiplexer
     */
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

    /**
     * Handle supply form the SimPowerSource
     * @param supplierEdge to SimPowerSource
     * @param newSupply charge
     */
    @Override
    public void handleSupply(FlowEdge supplierEdge, double newSupply) {
        this.chargeReceived = newSupply;
    }

    /**
     * Push new charge demand to SimPowerSource
     * @param supplierEdge to SimPowerSource
     * @param newDemand for charge
     */
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
