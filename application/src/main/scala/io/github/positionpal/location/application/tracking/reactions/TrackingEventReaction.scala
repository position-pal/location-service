package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.domain.*

/** A binary short circuit reaction to [[ClientDrivingEvent]]s producing as an [[Outcome]] a [[Notification]]
  * (either a [[Notification.Alert]] or a [[Notification.Success]]).
  */
object TrackingEventReaction extends BinaryShortCircuitReaction with FilterableOps:
  case object Continue
  override type Environment = Session
  override type Event = ClientDrivingEvent
  override type LeftOutcome = Unit | DrivingEvent
  override type RightOutcome = Continue.type
