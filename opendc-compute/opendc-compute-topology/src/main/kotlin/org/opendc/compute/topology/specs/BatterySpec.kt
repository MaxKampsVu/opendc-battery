package org.opendc.compute.topology.specs

import java.util.UUID

/**
 * Description of a battery that can be used to support a compute cluster's power needs.
 *
 * @param uid Unique identifier of the battery.
 * @param capacity The maximum capacity of the battery in watt-hours (J).
 * @param chargeSpeed The maximum charging speed of the battery in watts (W).
 * @param carbonThreshold The carbon intensity threshold
 * @param meta Additional metadata about the battery.
 */
public data class BatterySpec(
    val uid: UUID,
    val capacity: Double,
    val chargeSpeed: Double,
    val carbonThreshold: Double,
)

