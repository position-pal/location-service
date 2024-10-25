package io.github.positionpal.location.infrastructure.ws

import akka.actor.typed.ActorRef
import io.github.positionpal.location.infrastructure.services.actors.AkkaSerializable

object Protocol:
  sealed trait IncomingEvent extends AkkaSerializable
  case class Msg(text: String) extends IncomingEvent
  case class Connected(ws: ActorRef[OutgoingEvent]) extends IncomingEvent
  case object StreamCompletedSuccessfully extends IncomingEvent
  case class StreamCompletedWithException(ex: Throwable) extends IncomingEvent

  sealed trait OutgoingEvent extends AkkaSerializable
  case class WsMsg(groupId: String, text: String) extends OutgoingEvent
  case object Completed extends OutgoingEvent
  case class Failure(ex: Throwable) extends OutgoingEvent
  case class NewConnection(userId: String, ws: ActorRef[OutgoingEvent]) extends OutgoingEvent

  case class WebSocketHandler(outgoingMessageHandler: ActorRef[OutgoingEvent])
