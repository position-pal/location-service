package io.github.positionpal.location.domain

import eu.monniot.scala3mock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class EventReactionsTest extends AnyFunSpec with Matchers with MockFactory:

  import cats.effect.IO
  import cats.effect.unsafe.implicits.global
  import scala.util.Right

  private val event = mock[ClientDrivingEvent]

  object TestableEventReaction extends BinaryShortCircuitReaction with FilterableOps:
    override type Environment = Unit
    override type Event = ClientDrivingEvent
    override type LeftOutcome = String
    override type RightOutcome = Int

  import TestableEventReaction.*

  describe("`BinaryShortCircuitReaction` with `FilterableOps`"):
    it("should be able to be composed"):
      val reaction1 = on((_, _) => IO(Right(100)))
      val reaction2 = on((_, _) => IO(Left("r2")))
      val composed = reaction1 >>> reaction2
      composed((), event).unsafeRunSync() shouldBe Left("r2")

    it("should use short circuit evaluation"):
      var sentinels = List[String]()
      def updateSentinels(s: String): Unit = sentinels = sentinels :+ s
      val reaction1 = on((_, _) => IO { updateSentinels("reaction1"); Left("r1") })
      val reaction2 = on((_, _) => IO { updateSentinels("reaction2"); Right(100) })
      val composed = reaction1 >>> reaction2
      composed((), event).unsafeRunSync() shouldBe Left("r1")
      sentinels shouldBe List("reaction1")

    it("should be able to be filtered"):
      val reaction1 = on((_, _) => IO(Right(100)))
      val reaction2 = on((_, _) => IO(Left("r2")))
      val composed1 = reaction1 >>> reaction2.when((_, _) => false)(Left("c"))
      composed1((), event).unsafeRunSync() shouldBe Left("c")
      val composed2 = reaction1 >>> reaction2.when((_, _) => true)(Right(150))
      composed2((), event).unsafeRunSync() shouldBe Left("r2")
