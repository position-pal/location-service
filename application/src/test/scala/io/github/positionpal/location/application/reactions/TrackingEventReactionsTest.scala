package io.github.positionpal.location.application.reactions

import io.github.positionpal.location.domain.RoutingMode
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TrackingEventReactionsTest extends AnyFunSpec with Matchers:

  import TrackingEventReaction.*
  import cats.effect.IO
  import cats.effect.unsafe.implicits.global
  import io.github.positionpal.location.domain.DrivingEvents.TrackingEvent
  import io.github.positionpal.location.domain.{DrivingEvents, GPSLocation, Route, UserId}

  import java.util.Date

  private val route = Route(
    DrivingEvents.StartRoutingEvent(Date(), UserId("test"), RoutingMode.Driving, GPSLocation(0.0, 0.0)),
  )
  private val event: TrackingEvent = TrackingEvent(Date(), UserId("test"), GPSLocation(0.1, 0.1))

  describe("`TrackingEventReaction`s"):
    it("should be able to be composed"):
      val reaction1 = on((_, _) => IO(Right(Continue)))
      val reaction2 = on((_, _) => IO(Left(TriggerNotification("Test"))))
      val composed = reaction1 >>> reaction2
      composed(route, event).unsafeRunSync() shouldBe Left(TriggerNotification("Test"))

    it("should use short circuit evaluation"):
      var sentinels = List[String]()
      def updateSentinels(s: String): Unit = sentinels = sentinels :+ s
      val reaction1 = on((_, _) => IO { updateSentinels("reaction1"); Left(TriggerNotification("Test")) })
      val reaction2 = on((_, _) => IO { updateSentinels("reaction2"); Right(Continue) })
      val composed = reaction1 >>> reaction2
      composed(route, event).unsafeRunSync() shouldBe Left(TriggerNotification("Test"))
      sentinels shouldBe List("reaction1")
