package io.github.positionpal.location.presentation

import java.time.Instant

import scala.reflect.ClassTag

import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import io.bullet.borer.{Codec, Decoder, Encoder, Writer}
import io.github.positionpal.location.domain.*

/** Provides codecs for the domain model and application services. */
trait ModelCodecs:

  given instantsCodec: Codec[Instant] = Codec.bimap[String, Instant](_.toString, Instant.parse)

  given userIdCodec: Codec[UserId] = deriveCodec[UserId]

  given userStateCodec: Codec[UserState] = deriveCodec[UserState]

  given gpsLocationCodec: Codec[GPSLocation] = deriveCodec[GPSLocation]

  given routingModeCodec: Codec[RoutingMode] = deriveCodec[RoutingMode]

  given routingStartedCodec: Codec[RoutingStarted] = deriveCodec[RoutingStarted]

  given routingStoppedCodec: Codec[RoutingStopped] = deriveCodec[RoutingStopped]

  given sampledLocationCodec: Codec[SampledLocation] = deriveCodec[SampledLocation]

  given drivingEventCodec: Codec[DrivingEvent] = deriveAllCodecs[DrivingEvent]

  given drivenEventCodec: Codec[DrivenEvent] = deriveAllCodecs[DrivenEvent]

  given trackingCodec: Codec[Tracking] =
    Codec[Tracking](
      Encoder[Tracking]: (writer, tracking) =>
        writer.writeMapOpen(1).writeString("route").write(tracking.route).writeMapClose(),
      Decoder[Tracking]: reader =>
        val unbounded = reader.readMapOpen(1)
        val tracking = reader.readString() match
          case "route" => Tracking(reader.read[Route]())
          case _ => reader.unexpectedDataItem(expected = "`user`")
        reader.readMapClose(unbounded, tracking),
    )

  given monitorableTrackingCodec: Codec[MonitorableTracking] =
    Codec[MonitorableTracking](
      Encoder[MonitorableTracking]: (writer, tracking) =>
        writer.writeMapOpen(4).writeString("route").write(tracking.route).writeString("mode").write(tracking.mode)
          .writeString("destination").write(tracking.destination).writeString("expectedArrival")
          .write(tracking.expectedArrival).writeMapClose(),
      Decoder[MonitorableTracking]: reader =>
        val unbounded = reader.readMapOpen(4)
        val fields = (0 until 4).foldLeft(Map.empty[String, Any]): (data, _) =>
          reader.readString() match
            case s @ "route" => data + (s -> reader.read[Route]())
            case s @ "mode" => data + (s -> reader.read[RoutingMode]())
            case s @ "destination" => data + (s -> reader.read[GPSLocation]())
            case s @ "expectedArrival" => data + (s -> reader.read[Instant]())
            case _ => reader.unexpectedDataItem(expected = "`route`, `mode`, `destination` or `expectedArrival`")
        reader.readMapClose(
          unbounded,
          Tracking.withMonitoring(
            fields.at[RoutingMode]("mode"),
            fields.at[GPSLocation]("destination"),
            fields.at[Instant]("expectedArrival"),
            fields.at[Route]("route"),
          ),
        ),
    )

  extension (m: Map[String, Any])
    private def at[T](s: String)(using ClassTag[T]): T =
      m.get(s).collect { case t: T => t }.get
