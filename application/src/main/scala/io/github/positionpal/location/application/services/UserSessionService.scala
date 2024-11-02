package io.github.positionpal.location.application.services

import io.github.positionpal.location.domain.{UserId, Session}

/** A service to query and retrieve the tracking session information of a user. */
trait UserSessionService[F[_]]:

  /** @return the current [[Session]] of the given [[userId]]. */
  def sessionOf(userId: UserId): F[Session]
