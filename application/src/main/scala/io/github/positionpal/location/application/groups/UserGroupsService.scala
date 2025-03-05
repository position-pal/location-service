package io.github.positionpal.location.application.groups

import io.github.positionpal.events.{AddedMemberToGroup, RemovedMemberToGroup}
import io.github.positionpal.location.domain.Scope
import io.github.positionpal.entities.{GroupId, User, UserId}

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
  def membersOf(groupId: GroupId): F[Set[User]]

  /** @return the [[User]] in the given [[scope]] with all the details if it exists or an empty optional. */
  def of(scope: Scope): F[Option[User]]

object UserGroupsService:
  import cats.Monad
  import cats.implicits.toFunctorOps

  /** @return a new [[UserGroupsService]] instance. */
  def apply[F[_]: Monad](userGroupsStore: UserGroupsStore[F, Unit]): UserGroupsService[F] =
    GroupsServiceImpl[F](userGroupsStore)

  private class GroupsServiceImpl[F[_]: Monad](groupsStore: UserGroupsStore[F, Unit]) extends UserGroupsService[F]:

    override def addedMember(event: AddedMemberToGroup): F[Unit] =
      groupsStore.addMember(event.groupId(), event.addedMember())

    override def removeMember(event: RemovedMemberToGroup): F[Unit] =
      groupsStore.removeMember(event.groupId(), event.removedMember().id())

    override def groupsOf(userId: UserId): F[Set[GroupId]] = groupsStore.groupsOf(userId)

    override def membersOf(groupId: GroupId): F[Set[User]] = groupsStore.membersOf(groupId)

    override def of(scope: Scope): F[Option[User]] =
      for
        members <- membersOf(scope.groupId)
        user = members.find(_.id == scope.userId)
      yield user
