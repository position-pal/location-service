package io.github.positionpal.location.application.services

import io.github.positionpal.location.domain.{GPSLocation, Route, UserId}

/** A service to query and retrieve the tracking information of a user. */
trait UserTrackingInfoService[F[_]]:

  /** @return the current [[UserState]] of the given [[userId]]. */
  def currentState(userId: UserId): F[UserState]

  /** @return the last know location of the given [[userId]], if any. */
  def lastKnownLocation(userId: UserId): F[Option[GPSLocation]]

  /** @return the [[Route]] the given [[userId]] is performing at the moment of the request, if any. */
  def currentActiveRoute(userId: UserId): F[Option[Route]]
