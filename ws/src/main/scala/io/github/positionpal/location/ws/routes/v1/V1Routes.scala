package io.github.positionpal.location.ws.routes.v1

import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import io.github.positionpal.location.ws.handlers.v1.V1RoutesHandler
import io.github.positionpal.entities.{GroupId, UserId}
import io.github.positionpal.location.ws.routes.RoutesProvider

/** The routes for the v1 version of the web socket API. */
class V1Routes(routesHandler: V1RoutesHandler) extends RoutesProvider:
  override def version: String = "v1"

  override def routes: Route = groupRoute

  private val groupRoute: Route =
    path("group" / Segment / Segment): (guid, uid) =>
      handleWebSocketMessages:
        routesHandler.handleTrackingRoute(UserId.create(uid), GroupId.create(guid))
