package io.github.positionpal.location.infrastructure

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.Http
import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory
import io.github.positionpal.location.infrastructure.services.ActorBasedRealTimeTracking
import io.github.positionpal.location.infrastructure.services.actors.WebSocketsManagers.WebSocketsManager
import io.github.positionpal.location.infrastructure.services.actors.{GroupManager, WebSocketsManagers}
import io.github.positionpal.location.infrastructure.utils.AkkaUtils
import io.github.positionpal.location.infrastructure.ws.WebSockets

@main def main(): Unit =
  import cats.effect.unsafe.implicits.global
  val config = ConfigFactory.load("akka.conf")
  val result = for
    actorSystem <- AkkaUtils.startup[IO, WebSocketsManager.Command](config)(WebSocketsManager())
    given ActorSystem[WebSocketsManager.Command] = actorSystem
    wsManagerRef = actorSystem.systemActorOf(WebSocketsManagers.WebSocketsManager(), "ws-manager")
    sharding <- Resource.eval(IO(ClusterSharding(actorSystem)))
    _ <- Resource.eval(IO(sharding.init(GroupManager())))
    realTimeTrackingService <- Resource.eval(IO(ActorBasedRealTimeTracking.Service[IO](actorSystem)))
    wsService <- Resource.eval(IO(configureHttpServer(8080)(realTimeTrackingService, wsManagerRef)))
  yield wsService
  result.use(_ => IO.never).unsafeRunSync()

def configureHttpServer(port: Int)(
    service: ActorBasedRealTimeTracking.Service[IO],
    wsManagerRef: ActorRef[WebSocketsManagers.WebSocketsManager.Command],
)(using actorSystem: ActorSystem[?]) =
  Http(actorSystem.classicSystem).newServerAt("localhost", port)
    .bind(WebSockets.Routes.groupRoute(service, wsManagerRef))
