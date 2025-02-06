package io.github.positionpal.location.domain

import java.time.Instant

import io.github.positionpal.entities.{GroupId, UserId}

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
    this(timestamp, scope.userId, scope.groupId, position, status)
