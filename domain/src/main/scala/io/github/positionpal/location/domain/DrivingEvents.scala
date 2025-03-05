package io.github.positionpal.location.domain

import java.time.Instant

import io.github.positionpal.entities.{GroupId, UserId}

/** An event driving an application use case. */
sealed trait DrivingEvent extends DomainEvent

/** An event driving a use case triggered by the system itself. */
sealed trait InternalEvent extends DrivingEvent

/** An event triggered when a user is stuck in the same position for a suspicious amount of time. */
case class StuckAlertTriggered(timestamp: Instant, user: UserId, group: GroupId) extends InternalEvent

object StuckAlertTriggered:
  def apply(timestamp: Instant, scope: Scope): StuckAlertTriggered = this(timestamp, scope.userId, scope.groupId)

/** An event triggered when a user stops being stuck. */
case class StuckAlertStopped(timestamp: Instant, user: UserId, group: GroupId) extends InternalEvent

object StuckAlertStopped:
  def apply(timestamp: Instant, scope: Scope): StuckAlertStopped = this(timestamp, scope.userId, scope.groupId)

/** An event triggered when a user is late to reach a destination. */
case class TimeoutAlertTriggered(timestamp: Instant, user: UserId, group: GroupId) extends InternalEvent

object TimeoutAlertTriggered:
  def apply(timestamp: Instant, scope: Scope): TimeoutAlertTriggered = this(timestamp, scope.userId, scope.groupId)

/** An event driving an application use case triggered by a client. */
sealed trait ClientDrivingEvent extends DrivingEvent

/** An event triggered regularly on behalf of a user, tracking its position. */
case class SampledLocation(timestamp: Instant, user: UserId, group: GroupId, position: GPSLocation)
    extends ClientDrivingEvent

object SampledLocation:
  def apply(timestamp: Instant, scope: Scope, position: GPSLocation): SampledLocation =
    this(timestamp, scope.userId, scope.groupId, position)

/** An event triggered when a user starts routing to a destination. */
case class RoutingStarted(
    timestamp: Instant,
    user: UserId,
    group: GroupId,
    position: GPSLocation,
    mode: RoutingMode,
    destination: Address,
    expectedArrival: Instant,
) extends ClientDrivingEvent

object RoutingStarted:
  def apply(
      timestamp: Instant,
      scope: Scope,
      position: GPSLocation,
      mode: RoutingMode,
      destination: Address,
      expectedArrival: Instant,
  ): RoutingStarted = this(timestamp, scope.userId, scope.groupId, position, mode, destination, expectedArrival)

/** An event triggered to stop an active route. */
case class RoutingStopped(timestamp: Instant, user: UserId, group: GroupId) extends ClientDrivingEvent

object RoutingStopped:
  def apply(timestamp: Instant, scope: Scope): RoutingStopped = this(timestamp, scope.userId, scope.groupId)

/** An event triggered by a user when needing help. */
case class SOSAlertTriggered(
    timestamp: Instant,
    user: UserId,
    group: GroupId,
    position: GPSLocation,
) extends ClientDrivingEvent

object SOSAlertTriggered:
  def apply(timestamp: Instant, scope: Scope, position: GPSLocation): SOSAlertTriggered =
    this(timestamp, scope.userId, scope.groupId, position)

/** An event triggered by a user when stopping the SOS alert. */
case class SOSAlertStopped(timestamp: Instant, user: UserId, group: GroupId) extends ClientDrivingEvent

object SOSAlertStopped:
  def apply(timestamp: Instant, scope: Scope): SOSAlertStopped = this(timestamp, scope.userId, scope.groupId)

/** An event triggered when a user goes offline. */
case class WentOffline(timestamp: Instant, user: UserId, group: GroupId) extends ClientDrivingEvent

object WentOffline:
  def apply(timestamp: Instant, scope: Scope): WentOffline = this(timestamp, scope.userId, scope.groupId)
