package io.github.positionpal.location.application.groups.impl

import io.github.positionpal.events.{AddedMemberToGroup, RemovedMemberToGroup}
import cats.implicits.toFunctorOps
import cats.Monad
import io.github.positionpal.location.application.groups.{UserGroupsService, UserGroupsStore}
import io.github.positionpal.location.domain.Scope
import io.github.positionpal.entities.{GroupId, User, UserId}

class BasicUserGroupsService[F[_]: Monad](userGroupsStore: UserGroupsStore[F, Unit]) extends UserGroupsService[F]:

  override def addedMember(event: AddedMemberToGroup): F[Unit] =
    userGroupsStore.addMember(event.groupId(), event.addedMember())

  override def removeMember(event: RemovedMemberToGroup): F[Unit] =
    userGroupsStore.removeMember(event.groupId(), event.removedMember().id())

  override def groupsOf(userId: UserId): F[Set[GroupId]] = userGroupsStore.groupsOf(userId)

  override def membersOf(groupId: GroupId): F[Set[User]] = userGroupsStore.membersOf(groupId)

  override def of(scope: Scope): F[Option[User]] =
    for
      members <- membersOf(scope.groupId)
      user = members.find(_.id == scope.userId)
    yield user
