package io.github.positionpal.location.application.sessions

import fs2.Stream
import io.github.positionpal.location.domain.{Scope, Session}
import io.github.positionpal.entities.GroupId

/** A service to query and retrieve the tracking session information of users.
  * @tparam F the effect type
  */
trait UserSessionsService[F[_]]:

  /** @return the current [[Session]] of the given [[scope.user]] in the given [[scope.group]], if it exists. */
  def ofScope(scope: Scope): F[Option[Session]]

  /** @return the current [[Session]]s of all members of the given [[groupId]]. */
  def ofGroup(groupId: GroupId): Stream[F, Session]

object UserSessionsService:
  import io.github.positionpal.location.application.groups.UserGroupsService

  /** @return a new [[UserSessionsService]] instance retrieving user sessions based on their group membership. */
  def apply[F[_]](groupsService: UserGroupsService[F], sessionStore: UserSessionsReader[F]): UserSessionsService[F] =
    UserSessionsServiceImpl[F](groupsService, sessionStore)

  private class UserSessionsServiceImpl[F[_]](
      groupsService: UserGroupsService[F],
      sessionStore: UserSessionsReader[F],
  ) extends UserSessionsService[F]:

    override def ofGroup(groupId: GroupId): Stream[F, Session] =
      Stream
        .eval(groupsService.membersOf(groupId))
        .flatMap(members => Stream.emits(members.toSeq))
        .evalMap(user => sessionStore.sessionOf(Scope(user.id(), groupId)))
        .collect { case Some(s) => s }

    override def ofScope(scope: Scope): F[Option[Session]] = sessionStore.sessionOf(scope)
