package io.github.positionpal.location.domain

import java.util.Date

/** A generic domain event, representing a valuable change in the system. */
trait DomainEvent:
  /** The timestamp when the event occurred. */
  def timestamp: Date

  /** The user who triggered the event. */
  def user: UserId

/** An event driving an application use case. */
sealed trait DrivingEvent extends DomainEvent

/** An event triggered regularly on behalf of a user, tracking its position. */
case class SampledLocation(timestamp: Date, user: UserId, position: GPSLocation) extends DrivingEvent

/** An event triggered when a user starts routing to a destination. */
case class RoutingStarted(
    timestamp: Date,
    user: UserId,
    mode: RoutingMode,
    destination: GPSLocation,
    expectedArrival: Date,
) extends DrivingEvent

/** An event triggered to stop an active route. */
case class RoutingStopped(timestamp: Date, user: UserId) extends DrivingEvent

/** An event triggered by a user when needing help. */
case class SOSAlertTriggered(timestamp: Date, user: UserId, position: GPSLocation) extends DrivingEvent

/** An event triggered by a user when stopping the SOS alert. */
case class SOSAlertStopped(timestamp: Date, user: UserId) extends DrivingEvent

/** An event triggered when a user goes offline. */
case class WentOffline(timestamp: Date, user: UserId) extends DrivingEvent

/** An event triggered by the system as a result of some system action. */
sealed trait DrivenEvent extends DomainEvent

/** A user update event. */
case class UserUpdate(
    timestamp: Date,
    user: UserId,
    position: GPSLocation,
    status: UserState,
) extends DrivenEvent
