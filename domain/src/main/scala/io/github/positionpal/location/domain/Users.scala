package io.github.positionpal.location.domain

/** An identifier uniquely identifying a [[User]]. */
final case class UserId(id: String)

/** An identifier uniquely identifying a group of [[User]]s. */
final case class GroupId(id: String)

/** A user of the system, identified by a [[UserId]] and belonging to a set of [[GroupId]]s. */
case class User(userId: UserId, inGroups: Set[GroupId])

/** The [[io.github.positionpal.location.domain.User]] state information. */
enum UserState:
  /** The user is online and is continuously sending location updates. */
  case Active

  /** The user is not sending location updates. */
  case Inactive

  /** The user requested help. */
  case SOS

  /** The user is currently routing to a destination. */
  case Routing

trait Identifiable[A]:
  def id: A
  
trait UserIdentifiable extends Identifiable[UserId]
