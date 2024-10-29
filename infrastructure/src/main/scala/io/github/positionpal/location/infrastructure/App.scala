package io.github.positionpal.location.infrastructure

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.http.scaladsl.Http
import cats.effect.{IO, Resource}
import com.typesafe.config.{Config, ConfigFactory}
import io.github.positionpal.location.domain.UserId
import io.github.positionpal.location.infrastructure.services.ActorBasedRealTimeTracking
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker
import io.github.positionpal.location.infrastructure.utils.AkkaUtils
import io.github.positionpal.location.infrastructure.ws.WebSockets

@main def main1(): Unit =
  startup(8080)(ConfigFactory.load("akka.conf"))

@main def main2(): Unit =
  startup(8081)(
    ConfigFactory.parseString("akka.remote.artery.canonical.port = 2552").withFallback(ConfigFactory.load("akka.conf")),
  )

def startup(port: Int)(config: Config) =
  import cats.effect.unsafe.implicits.global
  val result = for
    actorSystem <- AkkaUtils.startup[IO, Any](config)(Behaviors.empty)
    given ActorSystem[Any] = actorSystem
    sharding <- Resource.eval(IO(ClusterSharding(actorSystem)))
    _ <- Resource.eval(configureSharding(sharding))
    realTimeTrackingService <- Resource.eval(IO(ActorBasedRealTimeTracking.Service[IO](actorSystem)))
    wsService <- Resource.eval(IO(configureHttpServer(port)(realTimeTrackingService)))
  yield wsService
  result.use(_ => IO.never).unsafeRunSync()

def configureSharding(sharding: ClusterSharding)(using actorSystem: ActorSystem[?]) =
  IO(sharding.init(RealTimeUserTracker()))

def configureHttpServer(port: Int)(
    service: ActorBasedRealTimeTracking.Service[IO, UserId],
)(using actorSystem: ActorSystem[?]) =
  Http(actorSystem.classicSystem).newServerAt("localhost", port).bind(WebSockets.Routes.groupRoute(service))
