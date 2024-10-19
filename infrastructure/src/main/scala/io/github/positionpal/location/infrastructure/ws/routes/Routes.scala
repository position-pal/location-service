package io.github.positionpal.location.infrastructure.ws.routes

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import io.github.positionpal.location.infrastructure.ws.Protocol
import io.github.positionpal.location.infrastructure.ws.routes.WebSocketHandlers.websocketHandler

/** Object that contains the routes definition for the websocket server */
object Routes:

  def webSocketFlowRoute(groupRef: ActorRef[ShardingEnvelope[Protocol.IncomingEvent]]): Route =
    path("group" / Segment): groupId =>
      println(s"Opened a new connection for group id: $groupId")
      handleWebSocketMessages:
        println(s"[Route] Creating a new websocket handler for the group $groupId")
        websocketHandler(groupId, groupRef)

  /** Default route for the server
    * @return The route
    */
  def defaultRoute: Route =
    path("hello"):
      get:
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
