package io.github.positionpal.location.application.sessions.impl

import fs2.Stream
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.application.sessions.{UserSessionReader, UsersSessionService}
import io.github.positionpal.location.domain.Session

class BasicUsersSessionService[F[_]](
    userGroupsService: UserGroupsService[F],
    userSessionStore: UserSessionReader[F],
) extends UsersSessionService[F]:

  override def ofGroup(groupId: GroupId): Stream[F, Session] =
    Stream.eval(userGroupsService.membersOf(groupId)).flatMap(members => Stream.emits(members.toSeq))
      .evalMap(userSessionStore.sessionOf).collect { case Some(s) => s }

  override def ofUser(userId: UserId): F[Option[Session]] =
    userSessionStore.sessionOf(userId)
