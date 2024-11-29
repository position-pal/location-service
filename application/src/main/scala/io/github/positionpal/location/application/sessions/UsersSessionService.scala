package io.github.positionpal.location.application.sessions

import fs2.Stream
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.domain.Session

/** A service to query and retrieve the tracking session information of users.
  * @tparam F the effect type
  */
trait UsersSessionService[F[_]]:

  /** @return the current [[Session]] of the given [[userId]] if it exists. */
  def ofUser(userId: UserId): F[Option[Session]]

  /** @return the current [[Session]]s of all members of the given [[groupId]]. */
  def ofGroup(groupId: GroupId): Stream[F, Session]
