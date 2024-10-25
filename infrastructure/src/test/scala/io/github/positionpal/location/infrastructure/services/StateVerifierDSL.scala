package io.github.positionpal.location.infrastructure.services

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
  import org.scalatest.concurrent.Eventually.eventually
  import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker
  import io.github.positionpal.location.infrastructure.services.actors.RealTimeUserTracker.{Command, State, Event}
  import io.github.positionpal.location.domain.{DrivingEvent, UserState}

  given Conversion[UserState, List[UserState]] = _ :: Nil
  given Conversion[DrivingEvent, List[DrivingEvent]] = _ :: Nil
  extension (u: UserState) infix def |(other: UserState): List[UserState] = u :: other :: Nil
  extension (us: List[UserState]) infix def |(other: UserState): List[UserState] = us :+ other

  extension (xs: List[UserState])
    infix def --(events: List[DrivingEvent]): SystemVerifier[UserState, DrivingEvent, State] =
      RealTimeUserTrackerVerifier(xs, events)

  private class RealTimeUserTrackerVerifier(ins: List[UserState], events: List[DrivingEvent])
      extends SystemVerifier[UserState, DrivingEvent, State](ins, events):

    override infix def -->(
        outs: List[UserState],
    )(using ctx: Context[UserState, State]): Verification[DrivingEvent, State] =
      new Verification[DrivingEvent, State]:
        override infix def verifying(verifyLast: (DrivingEvent, State) => Unit)(using patience: PatienceConfig): Unit =
          val testKit = EventSourcedBehaviorTestKit[Command, Event, State](system, RealTimeUserTracker("userTest"))
          ctx.initialStates(ins).zipWithIndex.foreach: (state, idx) =>
            testKit.initialize(state)
            events.foreach(ev => testKit.runCommand(ev).events should contain only ev)
            eventually:
              val currentState = testKit.getState()
              currentState.userState shouldBe outs(if outs.size == 1 then 0 else idx)
              verifyLast(events.last, currentState)
