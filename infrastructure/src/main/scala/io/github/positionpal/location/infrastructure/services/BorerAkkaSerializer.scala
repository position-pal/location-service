package io.github.positionpal.location.infrastructure.services

import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodec
import io.github.positionpal.location.domain.DomainEvent
import io.github.positionpal.location.infrastructure.ws.Protocol
import io.github.positionpal.location.infrastructure.ws.Protocol.Msg
import io.github.positionpal.location.presentation.{BorerCborAkkaSerializer, ModelCodecs}

/** Custom Akka serializer for the RealTimeUserTracker actor. */
class BorerAkkaSerializer extends BorerCborAkkaSerializer with ModelCodecs:
  override def identifier: Int = 19923

  given stateCodec: Codec[RealTimeUserTracker.State] = deriveCodec[RealTimeUserTracker.State]
  given ignoreCoded: Codec[RealTimeUserTracker.Ignore.type] = deriveCodec[RealTimeUserTracker.Ignore.type]
  given alignCheckCodec: Codec[RealTimeUserTracker.AliveCheck.type] = deriveCodec[RealTimeUserTracker.AliveCheck.type]

  // for akka ws tests
  given msgCodec: Codec[Protocol.Msg] = deriveCodec[Protocol.Msg]
  given streamCompletedSuccessfullyCodec: Codec[Protocol.StreamCompletedSuccessfully.type] =
    deriveCodec[Protocol.StreamCompletedSuccessfully.type]
  given wsMsgCodec: Codec[Protocol.WsMsg] = deriveCodec[Protocol.WsMsg]
  given completedCodec: Codec[Protocol.Completed.type] = deriveCodec[Protocol.Completed.type]

  println("Registering codecs")

  register[RealTimeUserTracker.AliveCheck.type]()
  register[RealTimeUserTracker.Ignore.type]()
  register[DomainEvent]()
  register[RealTimeUserTracker.State]()
  // for akka ws tests
  register[Protocol.Msg]()
  register[Protocol.StreamCompletedSuccessfully.type]()
  register[Protocol.WsMsg]()
  register[Protocol.Completed.type]()
