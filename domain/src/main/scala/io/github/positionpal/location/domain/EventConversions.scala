package io.github.positionpal.location.domain

/** A bunch of conversion utilities for domain events. */
object EventConversions:

  given Conversion[SOSAlertTriggered, SampledLocation] = ev => SampledLocation(ev.timestamp, ev.user, ev.position)
  given Conversion[RoutingStarted, SampledLocation] = ev => SampledLocation(ev.timestamp, ev.user, ev.position)

  extension (ev: RoutingStarted)
    /** Creates a [[MonitorableTracking]] from the given [[RoutingStarted]] event. */
    def toMonitorableTracking: MonitorableTracking =
      Tracking.withMonitoring(ev.mode, ev.destination, ev.expectedArrival)

  /** @return a [[UserUpdate]] built from the given [[DrivingEvent]] and [[Session]]. */
  def userUpdateFrom(event: DrivingEvent, session: Session): UserUpdate =
    UserUpdate(event.timestamp, event.user, session.lastSampledLocation.map(_.position), session.userState)
