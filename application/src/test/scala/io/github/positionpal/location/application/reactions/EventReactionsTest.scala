package io.github.positionpal.location.application.reactions

import scala.util.Right

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class EventReactionsTest extends AnyFunSpec with Matchers:

  import TrackingEventReaction.*
  import Notification.Alert
  import cats.effect.IO
  import cats.effect.unsafe.implicits.global
  import io.github.positionpal.entities.UserId
  import io.github.positionpal.location.domain.*
  import io.github.positionpal.location.domain.RoutingMode.Driving

  import java.time.Instant.now

  private val tracking = Tracking.withMonitoring(Driving, GPSLocation(0.0, 0.0), now)
  private val event = SampledLocation(now, UserId.create("test"), GPSLocation(0.1, 0.1))

  describe("`TrackingEventReaction`s"):
    it("should be able to be composed"):
      val reaction1 = on((_, _) => IO(Right(Continue)))
      val reaction2 = on((_, _) => IO(Left(Alert("Test"))))
      val composed = reaction1 >>> reaction2
      composed(tracking, event).unsafeRunSync() shouldBe Left(Alert("Test"))

    it("should use short circuit evaluation"):
      var sentinels = List[String]()
      def updateSentinels(s: String): Unit = sentinels = sentinels :+ s
      val reaction1 = on((_, _) => IO { updateSentinels("reaction1"); Left(Alert("Test")) })
      val reaction2 = on((_, _) => IO { updateSentinels("reaction2"); Right(Continue) })
      val composed = reaction1 >>> reaction2
      composed(tracking, event).unsafeRunSync() shouldBe Left(Alert("Test"))
      sentinels shouldBe List("reaction1")

    it("should be able to be filtered"):
      val reaction1 = on((_, _) => IO(Right(Continue)))
      val reaction2 = on((_, _) => IO(Left(Alert("Test"))))
      val composed1 = reaction1 >>> reaction2.when((_, _) => false)(Right(Continue))
      composed1(tracking, event).unsafeRunSync() shouldBe Right(Continue)
      val composed2 = reaction1 >>> reaction2.when((_, _) => true)(Right(Continue))
      composed2(tracking, event).unsafeRunSync() shouldBe Left(Alert("Test"))
