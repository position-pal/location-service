package io.github.positionpal.location.ws.handlers

import io.github.positionpal.entities.{GroupId, UserId}

/** A trait representing the routes handler for the web socket API. */
trait RoutesHandler[F[_]]:

  /** Handles the tracking route for the given [[userId]] and [[groupId]]. */
  def handleTrackingRoute(userId: UserId, groupId: GroupId): F[Any]
