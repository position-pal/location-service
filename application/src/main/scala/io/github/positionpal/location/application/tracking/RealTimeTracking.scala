package io.github.positionpal.location.application.tracking

import io.github.positionpal.location.domain.DrivingEvent

trait RealTimeTracking:

  /** A type representing the outcome the [[Service]] can produce. */
  type Outcome

  /** An observer that listens for [[Outcome]]s. */
  type OutcomeObserver

  /** The real-time tracking service in charge of handling the [[DrivingEvent]]. */
  trait Service[F[_], G]:

    /** Handle the [[event]]. */
    def handle(resource: G)(event: DrivingEvent): F[Unit]

    /** Add an observer for the given [[resource]]. */
    def addObserverFor(resource: G)(observer: OutcomeObserver): F[Unit]

    /** Remove the observer for the given [[resource]]. */
    def removeObserverFor(resource: G)(observer: OutcomeObserver): F[Unit]
