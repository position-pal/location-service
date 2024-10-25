package io.github.positionpal.location.infrastructure.services

import io.github.positionpal.location.domain.DrivenEvent
import io.github.positionpal.location.infrastructure.services.actors.AkkaSerializable

object Protocol:
  sealed trait WebSocketEvent extends AkkaSerializable
  case class MessageToClient(message: DrivenEvent) extends WebSocketEvent
  case object Complete extends WebSocketEvent
  case class Failure(ex: Throwable) extends WebSocketEvent
