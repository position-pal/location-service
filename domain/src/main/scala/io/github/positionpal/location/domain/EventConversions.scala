package io.github.positionpal.location.domain

/** A bunch of conversion utilities for domain events. */
object EventConversions:

  given Conversion[SOSAlertTriggered, SampledLocation] = e => SampledLocation(e.timestamp, e.user, e.group, e.position)
  given Conversion[RoutingStarted, SampledLocation] = e => SampledLocation(e.timestamp, e.user, e.group, e.position)

  extension (ev: SampledLocation | SOSAlertTriggered | RoutingStarted)
    def toSampledLocation: SampledLocation = ev match
      case e: SampledLocation => e
      case e: SOSAlertTriggered => e
      case e: RoutingStarted => e

  extension (ev: RoutingStarted)
    /** @return a [[MonitorableTracking]] built from the given [[RoutingStarted]] event. */
    def toMonitorableTracking: MonitorableTracking =
      Tracking.withMonitoring(ev.mode, ev.destination, ev.expectedArrival)

  extension (ev: SOSAlertTriggered)
    /** @return a [[Tracking]] built from the given [[SOSAlertTriggered]] event. */
    def toTracking: Tracking = Tracking(ev :: Nil)

  /** @return a [[UserUpdate]] built from the given [[ClientDrivingEvent]] and [[Session]]. */
  def userUpdateFrom(event: DrivingEvent, session: Session): UserUpdate =
    UserUpdate(event.timestamp, event.user, event.group, session.lastSampledLocation.map(_.position), session.userState)
