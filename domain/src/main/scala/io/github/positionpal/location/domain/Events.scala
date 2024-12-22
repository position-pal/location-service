package io.github.positionpal.location.domain

import java.time.Instant

import io.github.positionpal.entities.{GroupId, UserId}

/** A generic domain event, representing a valuable change in the system. */
trait DomainEvent:
  /** The timestamp when the event occurred. */
  def timestamp: Instant

  /** The user who triggered the event. */
  def user: UserId

  /** The group the user belongs to and the event is related to. */
  def group: GroupId

  /** The [[Scope]] of the event, i.e., an aggregation of the user and group this event is related to. */
  def scope: Scope = Scope(user, group)

/** An event driving an application use case. */
sealed trait DrivingEvent extends DomainEvent

/** An event triggered regularly on behalf of a user, tracking its position. */
case class SampledLocation(timestamp: Instant, user: UserId, group: GroupId, position: GPSLocation) extends DrivingEvent

object SampledLocation:
  def apply(timestamp: Instant, scope: Scope, position: GPSLocation): SampledLocation =
    SampledLocation(timestamp, scope.user, scope.group, position)

/** An event triggered when a user starts routing to a destination. */
case class RoutingStarted(
    timestamp: Instant,
    user: UserId,
    group: GroupId,
    position: GPSLocation,
    mode: RoutingMode,
    destination: GPSLocation,
    expectedArrival: Instant,
) extends DrivingEvent

object RoutingStarted:
  def apply(
      timestamp: Instant,
      scope: Scope,
      position: GPSLocation,
      mode: RoutingMode,
      destination: GPSLocation,
      expectedArrival: Instant,
  ): RoutingStarted = this(timestamp, scope.user, scope.group, position, mode, destination, expectedArrival)

/** An event triggered to stop an active route. */
case class RoutingStopped(timestamp: Instant, user: UserId, group: GroupId) extends DrivingEvent

object RoutingStopped:
  def apply(timestamp: Instant, scope: Scope): RoutingStopped = RoutingStopped(timestamp, scope.user, scope.group)

/** An event triggered by a user when needing help. */
case class SOSAlertTriggered(
    timestamp: Instant,
    user: UserId,
    group: GroupId,
    position: GPSLocation,
) extends DrivingEvent

object SOSAlertTriggered:
  def apply(timestamp: Instant, scope: Scope, position: GPSLocation): SOSAlertTriggered =
    SOSAlertTriggered(timestamp, scope.user, scope.group, position)

/** An event triggered by a user when stopping the SOS alert. */
case class SOSAlertStopped(timestamp: Instant, user: UserId, group: GroupId) extends DrivingEvent

object SOSAlertStopped:
  def apply(timestamp: Instant, scope: Scope): SOSAlertStopped = this(timestamp, scope.user, scope.group)

/** An event triggered when a user goes offline. */
case class WentOffline(timestamp: Instant, user: UserId, group: GroupId) extends DrivingEvent

object WentOffline:
  def apply(timestamp: Instant, scope: Scope): WentOffline = this(timestamp, scope.user, scope.group)

/** An event triggered by the system as a result of some system action. */
sealed trait DrivenEvent extends DomainEvent

/** A user update event. */
case class UserUpdate(
    timestamp: Instant,
    user: UserId,
    group: GroupId,
    position: Option[GPSLocation],
    status: UserState,
) extends DrivenEvent

object UserUpdate:
  def apply(timestamp: Instant, scope: Scope, position: Option[GPSLocation], status: UserState): UserUpdate =
    UserUpdate(timestamp, scope.user, scope.group, position, status)
