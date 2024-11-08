package io.github.positionpal.location.infrastructure.services.actors

import org.scalatest.concurrent.Eventually.PatienceConfig
import org.scalatest.matchers.should.Matchers

trait SystemVerifier[S, E, X](val ins: List[S], val events: List[E]):
  infix def -->(outs: List[S])(using ctx: Context[S, X]): Verification[E, X]

trait Verification[E, X]:
  infix def verifying(verifyLast: (E, X) => Unit)(using patience: PatienceConfig): Unit

trait Context[S, X]:
  def initialStates(ins: List[S]): List[X]

import akka.actor.testkit.typed.scaladsl.ActorTestKitBase

trait RealTimeUserTrackerVerifierDSL:
  context: ActorTestKitBase & Matchers =>

  import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
  import io.github.positionpal.location.domain.{DrivingEvent, UserState}
  import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker.*
  import org.scalatest.concurrent.Eventually.eventually

  given Conversion[UserState, List[UserState]] = _ :: Nil
  given Conversion[DrivingEvent, List[DrivingEvent]] = _ :: Nil
  extension (u: UserState) infix def |(other: UserState): List[UserState] = u :: other :: Nil
  extension (us: List[UserState]) infix def |(other: UserState): List[UserState] = us :+ other

  extension (xs: List[UserState])
    infix def --(events: List[DrivingEvent]): SystemVerifier[UserState, DrivingEvent, ObservableSession] =
      RealTimeUserTrackerVerifier(xs, events)

  private class RealTimeUserTrackerVerifier(ins: List[UserState], events: List[DrivingEvent])
      extends SystemVerifier[UserState, DrivingEvent, ObservableSession](ins, events):

    override infix def -->(
        outs: List[UserState],
    )(using ctx: Context[UserState, ObservableSession]): Verification[DrivingEvent, ObservableSession] =
      new Verification[DrivingEvent, ObservableSession]:
        override infix def verifying(verifyLast: (DrivingEvent, ObservableSession) => Unit)(using
            patience: PatienceConfig,
        ): Unit =
          val testKit =
            EventSourcedBehaviorTestKit[Command, Event, ObservableSession](system, RealTimeUserTracker("userTest"))
          println(s"Initializing with ${ctx.initialStates(ins)}")
          ctx.initialStates(ins).zipWithIndex.foreach: (state, idx) =>
            testKit.initialize(state)
            events.foreach(ev => testKit.runCommand(ev).events should contain only ev)
            eventually:
              val currentState = testKit.getState()
              currentState.session.userState shouldBe outs(if outs.size == 1 then 0 else idx)
              verifyLast(events.last, currentState)
