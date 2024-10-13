//package io.github.positionpal.location.infrastructure
//
//import akka.actor.typed.ActorSystem
//import akka.cluster.sharding.typed.scaladsl.ClusterSharding
//import cats.effect.{IO, Resource}
//import io.github.positionpal.location.domain.{DomainEvent, GPSLocation, SampledLocation, UserId}
//import io.github.positionpal.location.infrastructure.services.{RealTimeTrackingService, RealTimeUserTracker}
//
//@main def testRealTimeTrackingService(): Unit =
//  import com.typesafe.config.ConfigFactory
//  import cats.effect.unsafe.implicits.global
//  import scala.concurrent.duration.DurationInt
//  val service = RealTimeTrackingService[IO]()
//  val cluster: Resource[IO, ActorSystem[DomainEvent]] = service.cluster.run(ConfigFactory.load("akka.conf"))
//  cluster.use: system =>
//    IO.println(s"System: $system")
//      *> IO.println(s"Cluster Sharding: ${ClusterSharding(system)}")
//      *> IO(
//      ClusterSharding(system).entityRefFor(RealTimeUserTracker.typeKey, "1") !
//        SampledLocation(java.util.Date(), UserId("1"), GPSLocation(0.0, 0.0))
//    )
//      *> IO.sleep(10.seconds)
//      *> IO(system.terminate())
//  .unsafeRunSync()
