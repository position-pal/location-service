package io.github.positionpal.location.domain

/** A bunch of conversion utilities for domain events. */
object EventConversions:

  given Conversion[SOSAlertTriggered, SampledLocation] = ev => SampledLocation(ev.timestamp, ev.user, ev.position)

  extension (ev: RoutingStarted)
    /** Creates a [[MonitorableTracking]] from the given [[RoutingStarted]] event. */
    def toMonitorableTracking: MonitorableTracking =
      Tracking.withMonitoring(ev.mode, ev.destination, ev.expectedArrival)
