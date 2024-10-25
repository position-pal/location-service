package io.github.positionpal.location.application.services

import io.github.positionpal.location
import io.github.positionpal.location.application.services
import io.github.positionpal.location.domain.{DrivingEvent, GroupId}

trait RealTimeTracking:

  /** The type of the outcome. */
  type Outcome

  /** An observer that listens to the outcome of a process following the Observer pattern. */
  type OutcomeObserver

  /** The real-time tracking service in charge of handling the [[DrivingEvent]]. */
  trait Service[F[_]]:

    /** Handle the [[event]]. */
    def handle(event: DrivingEvent): F[Unit]

    /** Add an observer for the given [[groupId]]. */
    def addObserverFor(groupId: GroupId)(observer: OutcomeObserver): F[Unit]

    /** Remove the observer for the given [[groupId]]. */
    def removeObserverFor(groupId: GroupId)(observer: OutcomeObserver): F[Unit]
