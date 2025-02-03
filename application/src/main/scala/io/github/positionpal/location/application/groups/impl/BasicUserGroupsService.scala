package io.github.positionpal.location.application.groups.impl

import io.github.positionpal.events.{AddedMemberToGroup, RemovedMemberToGroup}
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.application.groups.{UserGroupsService, UserGroupsStore}

class BasicUserGroupsService[F[_]](userGroupsStore: UserGroupsStore[F, Unit]) extends UserGroupsService[F]:

  override def addedMember(event: AddedMemberToGroup): F[Unit] =
    userGroupsStore.addMember(event.groupId(), event.addedMember().id())

  override def removeMember(event: RemovedMemberToGroup): F[Unit] =
    userGroupsStore.removeMember(event.groupId(), event.removedMember().id())

  override def groupsOf(userId: UserId): F[Set[GroupId]] = userGroupsStore.groupsOf(userId)

  override def membersOf(groupId: GroupId): F[Set[UserId]] = userGroupsStore.membersOf(groupId)
