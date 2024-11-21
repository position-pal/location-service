package io.github.positionpal.location.application.groups

import cats.effect.kernel.Async
import io.github.positionpal.location.domain.{GroupId, UserId}
import io.github.positionpal.{AddedMemberToGroup, RemovedMemberToGroup}

trait UserGroupsService[F[_]: Async]:
  def addedMember(event: AddedMemberToGroup): F[Unit]
  def removeMember(groupId: RemovedMemberToGroup): F[Unit]
  def groupsOf(userId: UserId): F[Set[GroupId]]
  def membersOf(groupId: GroupId): F[Set[UserId]]
