package io.github.positionpal.location.infrastructure.services

import java.util.Date

import scala.concurrent.duration.DurationInt

import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory
import io.github.positionpal.location.domain.{GPSLocation, SampledLocation, UserId}
import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker
import io.github.positionpal.location.infrastructure.utils.AkkaUtils
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AkkaInfrastructure extends AnyFlatSpec with Matchers:

  import cats.effect.unsafe.implicits.global

  "Akka infrastructure" should "be able to get started with no problems" in:
    val actorSystemRes = for
      system <- AkkaUtils.startup[IO, Any](ConfigFactory.load("akka.conf"))(Behaviors.empty)
      cluster <- Resource.eval(IO(ClusterSharding(system)))
      _ <- Resource.eval(IO(cluster.init(RealTimeUserTracker())))
      _ <- Resource.eval:
        IO:
          cluster.entityRefFor(RealTimeUserTracker.key, "test") ! SampledLocation(
            Date(),
            UserId("test"),
            GPSLocation(0.0, 0.0),
          )
    yield system
    noException should be thrownBy:
      actorSystemRes.use(actorSystem => IO.sleep(5.seconds) *> IO(actorSystem.terminate())).unsafeRunSync()
