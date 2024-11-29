/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.compute.simulator.telemetry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.opendc.common.Dispatcher
import org.opendc.common.asCoroutineDispatcher
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.compute.simulator.telemetry.table.BatteryTableReader
import org.opendc.compute.simulator.telemetry.table.BatteryTableReaderImpl
import org.opendc.compute.simulator.telemetry.table.HostTableReaderImpl
import org.opendc.compute.simulator.telemetry.table.PowerSourceTableReader
import org.opendc.compute.simulator.telemetry.table.PowerSourceTableReaderImpl
import org.opendc.compute.simulator.telemetry.table.ServiceTableReaderImpl
import org.opendc.compute.simulator.telemetry.table.TaskTableReaderImpl
import org.opendc.simulator.compute.power.SimPowerSource
import org.opendc.simulator.compute.power.battery.BatteryPowerAdapter
import org.opendc.simulator.compute.power.battery.BatteryPowerAdapterLogger
import org.opendc.simulator.compute.power.battery.PowerAdapter
import org.opendc.simulator.compute.power.battery.SimBattery
import org.opendc.simulator.compute.power.battery.StubPowerAdapter
import org.opendc.simulator.compute.power.battery.greenenergy.CarbonPolicy
import java.time.Duration

/**
 * A helper class to collect metrics from a [ComputeService] instance and automatically export the metrics every
 * export interval.
 *
 * @param dispatcher A [Dispatcher] for scheduling the future events.
 * @param service The [ComputeService] to monitor.
 * @param monitor The monitor to export the metrics to.
 * @param exportInterval The export interval.
 */
public class ComputeMetricReader(
    dispatcher: Dispatcher,
    private val service: ComputeService,
    private val monitor: ComputeMonitor,
    private val exportInterval: Duration = Duration.ofMinutes(5),
    private val startTime: Duration = Duration.ofMillis(0),
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private val scope = CoroutineScope(dispatcher.asCoroutineDispatcher())
    private val clock = dispatcher.timeSource

    /**
     * Aggregator for service metrics.
     */
    private val serviceTableReader =
        ServiceTableReaderImpl(
            service,
            startTime,
        )

    private var loggCounter = 0

    /**
     * Mapping from [SimHost] instances to [HostTableReaderImpl]
     */
    private val hostTableReaders = mutableMapOf<SimHost, HostTableReaderImpl>()

    /**
     * Mapping from [ServiceTask] instances to [TaskTableReaderImpl]
     */
    private val taskTableReaders = mutableMapOf<ServiceTask, TaskTableReaderImpl>()

    /**
     * Mapping from [PowerAdapter] instances to [PowerSourceTableReaderImpl]
     */

    private val powerSourceTableReaders = mutableMapOf<SimPowerSource, PowerSourceTableReader>()

    private val batteryTableReaders = mutableMapOf<SimBattery, BatteryTableReader>()

    private val carbonPolicyTableReaders = mutableMapOf<CarbonPolicy, PowerSourceTableReader>()

    /**
     * The background job that is responsible for collecting the metrics every cycle.
     */
    private val job =
        scope.launch {
            val intervalMs = exportInterval.toMillis()
            try {
                while (isActive) {
                    delay(intervalMs)

                    loggState()
                }
            } finally {
                if (monitor is AutoCloseable) {
                    monitor.close()
                }
            }
        }


    public fun loggState() {
        loggCounter++
        try {
            val now = this.clock.instant()

            for (host in this.service.hosts) {
                val reader =
                    this.hostTableReaders.computeIfAbsent(host) {
                        HostTableReaderImpl(
                            it,
                            startTime,
                        )
                    }
                reader.record(now)
                this.monitor.record(reader.copy())
                reader.reset()
            }

            for (task in this.service.tasks) {
                val reader =
                    this.taskTableReaders.computeIfAbsent(task) {
                        TaskTableReaderImpl(
                            service,
                            it,
                            startTime,
                        )
                    }
                reader.record(now)
                this.monitor.record(reader.copy())
                reader.reset()
            }

            for (task in this.service.tasksToRemove) {
                task.delete()
            }
            this.service.clearTasksToRemove()

            for (powerAdapter in this.service.powerSources) {

                val powerSourceReader = this.powerSourceTableReaders.computeIfAbsent(powerAdapter.simPowerSource) {
                    PowerSourceTableReaderImpl(
                        it,
                        startTime,
                    )
                }

                powerSourceReader.record(now)
                this.monitor.record(powerSourceReader.copy())
                powerSourceReader.reset()

                //If the powerAdapter is of type BatteryPowerAdapter additionally add logging for battery
                if (powerAdapter is BatteryPowerAdapter) {
                    val batteryReader = this.batteryTableReaders.computeIfAbsent(powerAdapter.simBattery) {
                        BatteryTableReaderImpl(
                            it,
                            startTime,
                        )
                    }
                    batteryReader.record(now)
                    this.monitor.record(batteryReader.copy())
                    batteryReader.reset()
                } else {
                    val powerSourceReader = this.powerSourceTableReaders.computeIfAbsent(powerAdapter.simPowerSource) {
                        PowerSourceTableReaderImpl(
                            it,
                            startTime,
                        )
                    }


                    powerSourceReader.record(now)
                    this.monitor.record(powerSourceReader.copy())
                    powerSourceReader.reset()
                }

            }

            this.serviceTableReader.record(now)
            monitor.record(this.serviceTableReader.copy())

            if (loggCounter >= 100) {
                var loggString = "\n\t\t\t\t\tMetrics after ${now.toEpochMilli() / 1000 / 60 / 60} hours:\n"
                loggString += "\t\t\t\t\t\tTasks Total: ${this.serviceTableReader.tasksTotal}\n"
                loggString += "\t\t\t\t\t\tTasks Active: ${this.serviceTableReader.tasksActive}\n"
                loggString += "\t\t\t\t\t\tTasks Pending: ${this.serviceTableReader.tasksPending}\n"
                loggString += "\t\t\t\t\t\tTasks Completed: ${this.serviceTableReader.tasksCompleted}\n"
                loggString += "\t\t\t\t\t\tTasks Terminated: ${this.serviceTableReader.tasksTerminated}\n"

                this.logger.warn { loggString }
                loggCounter = 0
            }
        } catch (cause: Throwable) {
            this.logger.warn(cause) { "Exporter threw an Exception" }
        }
    }

    override fun close() {
        job.cancel()
    }
}
