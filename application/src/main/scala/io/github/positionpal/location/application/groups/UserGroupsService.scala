package io.github.positionpal.location.application.groups

import cats.effect.kernel.Async
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.{AddedMemberToGroup, RemovedMemberToGroup}

/**
 * A service for managing user groups.
 * @tparam F the effect type
 */
trait UserGroupsService[F[_]: Async]:

  /** Add a member to a group described by the given [[event]]. */
  def addedMember(event: AddedMemberToGroup): F[Unit]

  /** Remove a user from a group described by the given [[event]]. */
  def removeMember(event: RemovedMemberToGroup): F[Unit]

  /** @return the groups of the given [[userId]]. */
  def groupsOf(userId: UserId): F[Set[GroupId]]

  /** @return the members of the given [[groupId]]. */
  def membersOf(groupId: GroupId): F[Set[UserId]]
