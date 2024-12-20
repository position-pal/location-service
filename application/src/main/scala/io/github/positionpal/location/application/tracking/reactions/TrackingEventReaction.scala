package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.domain.{BinaryShortCircuitReaction, DrivingEvent, FilterableOps, Session}

/** A binary short circuit reaction to [[DrivingEvent]]s producing as an [[Outcome]] a [[Notification]]
  * (either a [[Notification.Alert]] or a [[Notification.Success]]).
  */
object TrackingEventReaction extends BinaryShortCircuitReaction with FilterableOps:
  case object Continue
  override type Environment = Session
  override type Event = DrivingEvent
  override type LeftOutcome = Unit | DrivingEvent
  override type RightOutcome = Continue.type
