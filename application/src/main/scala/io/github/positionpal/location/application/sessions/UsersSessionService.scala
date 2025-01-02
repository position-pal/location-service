package io.github.positionpal.location.application.sessions

import fs2.Stream
import io.github.positionpal.location.domain.{Scope, Session}
import io.github.positionpal.entities.GroupId

/** A service to query and retrieve the tracking session information of users.
  * @tparam F the effect type
  */
trait UsersSessionService[F[_]]:

  /** @return the current [[Session]] of the given [[scope.user]] in the given [[scope.group]], if it exists. */
  def ofScope(scope: Scope): F[Option[Session]]

  /** @return the current [[Session]]s of all members of the given [[groupId]]. */
  def ofGroup(groupId: GroupId): Stream[F, Session]
