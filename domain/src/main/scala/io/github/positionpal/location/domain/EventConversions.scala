package io.github.positionpal.location.domain

object EventConversions:

  given Conversion[SOSAlertTriggered, SampledLocation] = ev => SampledLocation(ev.timestamp, ev.user, ev.position)

  extension (ev: RoutingStarted)
    def toMonitorableTracking: MonitorableTracking =
      Tracking.withMonitoring(ev.user, ev.mode, ev.destination, ev.expectedArrival)

  extension (ev: SOSAlertTriggered)
    def toTracking: Tracking = Tracking(ev.user)
    def toSampledLocation: SampledLocation = SampledLocation(ev.timestamp, ev.user, ev.position)
