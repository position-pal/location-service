package io.github.positionpal.location.application.groups

import io.github.positionpal.events.{AddedMemberToGroup, RemovedMemberToGroup}
import io.github.positionpal.entities.{GroupId, UserId}

/** A service for managing user groups.
  * @tparam F the effect type
  */
trait UserGroupsService[F[_]]:

  /** Add a member to a group described by the given [[event]]. */
  def addedMember(event: AddedMemberToGroup): F[Unit]

  /** Remove a user from a group described by the given [[event]]. */
  def removeMember(event: RemovedMemberToGroup): F[Unit]

  /** @return the groups of the given [[userId]]. */
  def groupsOf(userId: UserId): F[Set[GroupId]]

  /** @return the members of the given [[groupId]]. */
  def membersOf(groupId: GroupId): F[Set[UserId]]
