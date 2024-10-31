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
        writer.writeMapOpen(2).writeString("user").write(tracking.user).writeString("route").write(tracking.route)
          .writeMapClose(),
      Decoder[Tracking]: reader =>
        val unbounded = reader.readMapOpen(2)
        val fields = (0 until 2).foldLeft(Map.empty[String, Any]): (data, _) =>
          reader.readString() match
            case "user" => data + ("user" -> reader.read[UserId]())
            case "route" => data + ("route" -> reader.read[Route]())
            case _ => reader.unexpectedDataItem(expected = "`user` or `route`")
        reader.readMapClose(unbounded, Tracking(fields.at[UserId]("user"), fields.at[Route]("route"))),
    )

  given monitorableTrackingCodec: Codec[MonitorableTracking] =
    Codec[MonitorableTracking](
      Encoder[MonitorableTracking]: (writer, tracking) =>
        writer.writeMapOpen(5).writeString("user").write(tracking.user).writeString("route").write(tracking.route)
          .writeString("mode").write(tracking.mode).writeString("destination").write(tracking.destination)
          .writeString("expectedArrival").write(tracking.expectedArrival).writeMapClose(),
      Decoder[MonitorableTracking]: reader =>
        val unbounded = reader.readMapOpen(5)
        val fields = (0 until 5).foldLeft(Map.empty[String, Any]): (data, _) =>
          reader.readString() match
            case s @ "user" => data + (s -> reader.read[UserId]())
            case s @ "route" => data + (s -> reader.read[Route]())
            case s @ "mode" => data + (s -> reader.read[RoutingMode]())
            case s @ "destination" => data + (s -> reader.read[GPSLocation]())
            case s @ "expectedArrival" => data + (s -> reader.read[Instant]())
            case _ => reader.unexpectedDataItem(expected = "`user`, `route`, `mode`, `destination`, `expectedArrival`")
        reader.readMapClose(
          unbounded,
          Tracking.withMonitoring(
            fields.at[UserId]("user"),
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
