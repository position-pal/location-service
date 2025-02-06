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
