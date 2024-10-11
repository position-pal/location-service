package io.github.positionpal.location.infrastructure.services

import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodec
import io.github.positionpal.location.domain.DomainEvent
import io.github.positionpal.location.presentation.{BorerCborAkkaSerializer, ModelCodecs}

/** Custom Akka serializer for the RealTimeUserTracker actor. */
class BorerAkkaSerializer extends BorerCborAkkaSerializer with ModelCodecs:
  override def identifier: Int = 19923

  given stateCodec: Codec[RealTimeUserTracker.State] = deriveCodec[RealTimeUserTracker.State]
  given ignoreCoded: Codec[RealTimeUserTracker.Ignore.type] = deriveCodec[RealTimeUserTracker.Ignore.type]
  given alignCheckCodec: Codec[RealTimeUserTracker.AliveCheck.type] = deriveCodec[RealTimeUserTracker.AliveCheck.type]

  register[RealTimeUserTracker.AliveCheck.type]()
  register[RealTimeUserTracker.Ignore.type]()
  register[DomainEvent]()
  register[RealTimeUserTracker.State]()
