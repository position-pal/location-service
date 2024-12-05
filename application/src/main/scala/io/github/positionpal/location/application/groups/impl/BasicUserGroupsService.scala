package io.github.positionpal.location.application.groups.impl

import io.github.positionpal.{AddedMemberToGroup, RemovedMemberToGroup}
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.application.groups.{UserGroupsService, UserGroupsStore}

class BasicUserGroupsService[F[_]](userGroupsStore: UserGroupsStore[F, Unit]) extends UserGroupsService[F]:

  override def addedMember(event: AddedMemberToGroup): F[Unit] =
    userGroupsStore.addMember(GroupId.create(event.groupId()), UserId.create(event.addedMember().id()))

  override def removeMember(event: RemovedMemberToGroup): F[Unit] =
    userGroupsStore.removeMember(GroupId.create(event.groupId()), UserId.create(event.removedMember().id()))

  override def groupsOf(userId: UserId): F[Set[GroupId]] = userGroupsStore.groupsOf(userId)

  override def membersOf(groupId: GroupId): F[Set[UserId]] = userGroupsStore.membersOf(groupId)
