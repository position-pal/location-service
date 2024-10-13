package io.github.positionpal.location.application.services

import io.github.positionpal.location
import io.github.positionpal.location.application.services
import io.github.positionpal.location.domain.DomainEvent

/** The real-time tracking service in charge of handling the [[DomainEvent]]. */
trait RealTimeTrackingService[M[_]]:

  /** Handle the [[event]]. */
  def handle(event: DomainEvent): M[Unit]
