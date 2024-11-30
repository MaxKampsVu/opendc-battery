/*
 * Copyright (c) 2024 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.compute.simulator.telemetry.table

import org.opendc.simulator.compute.power.MultiSimPowerSource
import org.opendc.simulator.compute.power.SimPowerSource
import org.opendc.simulator.compute.power.battery.BatteryPowerAdapter
import java.time.Duration
import java.time.Instant

/**
 * An aggregator for task metrics before they are reported.
 */
public class BatteryAdapterTableReaderImpl(
    batteryAdapter: BatteryPowerAdapter,
    private val startTime: Duration = Duration.ofMillis(0),
) : BatteryAdapterTableReader {
    override fun copy(): BatteryAdapterTableReader {
        val newBatteryAdapterTable =
            BatteryAdapterTableReaderImpl(
                batteryAdapter,
                startTime
            )
        newBatteryAdapterTable.setValues(this)

        return newBatteryAdapterTable
    }

    override fun setValues(table: BatteryAdapterTableReader) {
        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute

        _hostsConnected = table.hostsConnected
        _powerDraw = table.powerDraw
        _energyUsage = table.energyUsage
        _energyUsageBattery = table.energyUsageBattery
        _energyUsagePowerSource = table.energyUsagePowerSource
    }

    private val batteryAdapter = batteryAdapter

    private var _timestamp = Instant.MIN
    override val timestamp: Instant
        get() = _timestamp

    private var _timestampAbsolute = Instant.MIN
    override val timestampAbsolute: Instant
        get() = _timestampAbsolute

    override val hostsConnected: Int
        get() = _hostsConnected
    private var _hostsConnected: Int = 0

    override val powerDraw: Double
        get() = _powerDraw
    private var _powerDraw = 0.0

    override val energyUsage: Double
        get() = _energyUsage - previousEnergyUsage
    private var _energyUsage = 0.0
    private var previousEnergyUsage = 0.0

    override val energyUsageBattery: Double
        get() = _energyUsageBattery - previousEnergyUsageBattery
    private var _energyUsageBattery = 0.0
    private var previousEnergyUsageBattery = 0.0

    override val energyUsagePowerSource: Double
        get() = _energyUsagePowerSource - previousEnergyUsagePowerSource
    private var _energyUsagePowerSource = 0.0
    private var previousEnergyUsagePowerSource = 0.0

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        _timestamp = now
        _timestampAbsolute = now + startTime

        _hostsConnected = 0

        batteryAdapter.updateCounters()
        _powerDraw = batteryAdapter.powerDraw
        _energyUsage = batteryAdapter.energyUsage
        _energyUsageBattery = batteryAdapter.batteryEnergyUsage
        _energyUsagePowerSource = batteryAdapter.powerSourceEnergyUsage
    }

    /**
     * Finish the aggregation for this cycle.
     */
    override fun reset() {
        previousEnergyUsage = _energyUsage
        previousEnergyUsageBattery = _energyUsageBattery
        previousEnergyUsagePowerSource = _energyUsagePowerSource


        _hostsConnected = 0
        _powerDraw = 0.0
        _energyUsage = 0.0
        _energyUsageBattery = 0.0
        _energyUsagePowerSource = 0.0
    }
}
