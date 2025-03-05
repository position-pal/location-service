package io.github.positionpal.location.application.sessions.impl

import fs2.Stream
import io.github.positionpal.location.application.sessions.{UserSessionsReader, UserSessionsService}
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.domain.{Scope, Session}
import io.github.positionpal.entities.GroupId

/** A basic implementation of the [[UserSessionsService]] that retrieves user sessions
  * based on their group membership.
  *
  * @param userGroupsService the service for managing user groups
  * @param userSessionStore the store for user sessions
  */
class BasicUserSessionsService[F[_]](
    userGroupsService: UserGroupsService[F],
    userSessionStore: UserSessionsReader[F],
) extends UserSessionsService[F]:

  override def ofGroup(groupId: GroupId): Stream[F, Session] = Stream
    .eval(userGroupsService.membersOf(groupId))
    .flatMap(members => Stream.emits(members.toSeq))
    .evalMap(user => userSessionStore.sessionOf(Scope(user.id(), groupId)))
    .collect { case Some(s) => s }

  override def ofScope(scope: Scope): F[Option[Session]] =
    userSessionStore.sessionOf(scope)
