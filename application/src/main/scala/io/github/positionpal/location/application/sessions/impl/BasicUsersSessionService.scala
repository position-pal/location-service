package io.github.positionpal.location.application.sessions.impl

import fs2.Stream
import io.github.positionpal.entities.GroupId
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.application.sessions.{UserSessionReader, UsersSessionService}
import io.github.positionpal.location.domain.{Scope, Session}

/** A basic implementation of the [[UsersSessionService]] that retrieves user sessions
  * based on their group membership.
  * @param userGroupsService the service for managing user groups
  * @param userSessionStore the store for user sessions
  */
class BasicUsersSessionService[F[_]](
    userGroupsService: UserGroupsService[F],
    userSessionStore: UserSessionReader[F],
) extends UsersSessionService[F]:

  override def ofGroup(groupId: GroupId): Stream[F, Session] =
    Stream
      .eval(userGroupsService.membersOf(groupId))
      .flatMap(members => Stream.emits(members.toSeq))
      .evalMap(userId => userSessionStore.sessionOf(Scope(userId, groupId)))
      .collect { case Some(s) => s }

  override def ofScope(scope: Scope): F[Option[Session]] =
    userSessionStore.sessionOf(scope)
