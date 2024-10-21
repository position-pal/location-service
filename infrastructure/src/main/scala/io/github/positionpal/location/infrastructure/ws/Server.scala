package io.github.positionpal.location.infrastructure.ws

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.*
import com.typesafe.config.{Config, ConfigFactory}
import io.github.positionpal.location.infrastructure.ws.actors.{GroupActor, SessionActor}
import io.github.positionpal.location.infrastructure.ws.routes.Routes.{defaultRoute, webSocketFlowRoute}

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Server:

  given actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(Behaviors.empty[Any], "messaging-actor-system")
  given executionContext: ExecutionContextExecutor = actorSystem.executionContext

  def startup(config: Config, port: Int): Unit =
    val actorSystem = ActorSystem(SessionActor(), "ClusterSystem", config)
    val sessionRef = actorSystem.systemActorOf(SessionActor(), "session-actor")
    // join the current node to the cluster
//    val cluster = Cluster(actorSystem)
//    cluster.manager ! Join(cluster.selfMember.address)
    // setup the sharding
    val sharding = ClusterSharding(actorSystem)
    val groupRef = sharding.init(GroupActor())
    // start the websocket server
    val binding = Http().newServerAt("localhost", port).bind(defaultRoute ~ webSocketFlowRoute(sessionRef, groupRef))
    println("Server running...")
    // let it run until user presses return
    StdIn.readLine()
    binding.flatMap(_.unbind()).onComplete(_ => actorSystem.terminate())
    println("Server is shut down")

@main
def main(): Unit =
  import Server.startup
  startup(ConfigFactory.load("akka.conf"), 8080)

@main
def main2(): Unit =
  import Server.startup
  startup(
    ConfigFactory
      .parseString("akka.remote.artery.canonical.port = 2552")
      .withFallback(ConfigFactory.load("akka.conf")),
    8081
  )
