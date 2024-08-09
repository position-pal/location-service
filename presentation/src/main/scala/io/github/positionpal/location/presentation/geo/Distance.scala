package io.github.positionpal.location.presentation.geo

import io.circe.{Decoder, HCursor}
import io.github.positionpal.location.application.geo.Distance
import io.github.positionpal.location.application.geo.Distance.meters

object Distance:
  given distancePlainDecoder: Decoder[Distance] with
    def apply(c: HCursor): Decoder.Result[Distance] =
      Decoder.decodeDouble.map(meters)(c)
