package io.github.positionpal.location.domain

import java.util.Date

/** An event driving an application use case. */
sealed trait DomainEvent:
  /** The timestamp when the event occurred. */
  def timestamp: Date

  /** The user who triggered the event. */
  def user: UserId

/** An event triggered regularly on behalf of a user, tracking its position. */
case class SampledLocation(timestamp: Date, user: UserId, position: GPSLocation) extends DomainEvent

/** An event triggered when a user starts routing to a destination. */
case class RoutingStarted(
    timestamp: Date,
    user: UserId,
    mode: RoutingMode,
    destination: GPSLocation,
    expectedArrival: Date,
) extends DomainEvent

/** An event triggered to stop an active route. */
case class RoutingStopped(timestamp: Date, user: UserId) extends DomainEvent

/** An event triggered by a user when needing help. */
case class SOSAlertTriggered(timestamp: Date, user: UserId, position: GPSLocation) extends DomainEvent

/** An event triggered by a user when stopping the SOS alert. */
case class SOSAlertStopped(timestamp: Date, user: UserId) extends DomainEvent

/** An event triggered when a user goes offline. */
case class WentOffline(timestamp: Date, user: UserId) extends DomainEvent
