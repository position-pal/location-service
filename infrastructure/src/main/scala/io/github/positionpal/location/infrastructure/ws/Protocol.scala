package io.github.positionpal.location.infrastructure.ws

import akka.actor.typed.ActorRef

object Protocol:
  sealed trait IncomingEvent
  case class Msg(text: String) extends IncomingEvent
  case class Connected(ws: ActorRef[OutgoingEvent]) extends IncomingEvent
  case object StreamCompletedSuccessfully extends IncomingEvent
  case class StreamCompletedWithException(ex: Throwable) extends IncomingEvent

  sealed trait OutgoingEvent
  case class WsMsg(text: String) extends OutgoingEvent
  case object Completed extends OutgoingEvent
  case class Failure(ex: Throwable) extends OutgoingEvent

  case class WebSocketHandler(outgoingMessageHandler: ActorRef[OutgoingEvent])
