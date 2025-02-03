package io.github.positionpal.location.presentation

import java.time.Instant

import scala.Option.empty

import cats.implicits.catsSyntaxTuple4Semigroupal
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec, deriveEncoder}
import io.bullet.borer.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.{GroupId, UserId}

/** Provides codecs for the domain model and application services. */
trait ModelCodecs:

  given instantsCodec: Codec[Instant] = Codec.bimap[String, Instant](_.toString, Instant.parse)

  given userIdCodec: Codec[UserId] = Codec.bimap[String, UserId](_.value(), UserId.create)

  given groupIdCodec: Codec[GroupId] = Codec.bimap[String, GroupId](_.value(), GroupId.create)

  given scopeCodec: Codec[Scope] = deriveCodec[Scope]

  given userStateCodec: Codec[UserState] = deriveCodec[UserState]

  given gpsLocationCodec: Codec[GPSLocation] = deriveCodec[GPSLocation]

  given routingModeCodec: Codec[RoutingMode] = deriveCodec[RoutingMode]

  given routingStartedCodec: Codec[RoutingStarted] = deriveCodec[RoutingStarted]

  given routingStoppedCodec: Codec[RoutingStopped] = deriveCodec[RoutingStopped]

  given sampledLocationCodec: Codec[SampledLocation] = deriveCodec[SampledLocation]

  given internalEventCodec: Codec[InternalEvent] = deriveAllCodecs[InternalEvent]

  given drivingEventCodec: Codec[DrivingEvent] = deriveAllCodecs[DrivingEvent]

  given drivenEventCodec: Codec[DrivenEvent] = deriveAllCodecs[DrivenEvent]

  given trackingCodec: Codec[Tracking] = Codec[Tracking](
    Encoder[Tracking]: (writer, tracking) =>
      writer.writeMapOpen(1).writeString("route").write(tracking.route).writeMapClose(),
    Decoder[Tracking]: reader =>
      val unbounded = reader.readMapOpen(1)
      val tracking = reader.readString() match
        case "route" => Tracking(reader.read[Route]())
        case _ => reader.unexpectedDataItem(expected = "`route`")
      reader.readMapClose(unbounded, tracking),
  )

  given monitorableTrackingCodec: Codec[MonitorableTracking] = Codec[MonitorableTracking](
    Encoder[MonitorableTracking]: (writer, tracking) =>
      writer
        .writeMapOpen(4)
        .writeString("route")
        .write(tracking.route)
        .writeString("mode")
        .write(tracking.mode)
        .writeString("destination")
        .write(tracking.destination)
        .writeString("expectedArrival")
        .write(tracking.expectedArrival)
        .writeMapClose(),
    Decoder[MonitorableTracking]: reader =>
      val unbounded = reader.readMapOpen(4)
      val fields = (0 until 4).foldLeft((empty[RoutingMode], empty[GPSLocation], empty[Instant], empty[Route])):
        (d, _) =>
          reader.readString() match
            case "mode" => d.copy(_1 = Some(reader.read[RoutingMode]()))
            case "destination" => d.copy(_2 = Some(reader.read[GPSLocation]()))
            case "expectedArrival" => d.copy(_3 = Some(reader.read[Instant]()))
            case "route" => d.copy(_4 = Some(reader.read[Route]()))
            case _ => reader.unexpectedDataItem(expected = "`route`, `mode`, `destination` or `expectedArrival`")
      val res = fields.mapN(Tracking.withMonitoring).getOrElse(reader.validationFailure("Missing required fields"))
      reader.readMapClose(unbounded, res),
  )

  given sessionCodec: Codec[Session] = Codec[Session](
    Encoder[Session]: (writer, session) =>
      writer
        .writeMapOpen(4)
        .writeString("scope")
        .write(session.scope)
        .writeString("state")
        .write(session.userState)
        .writeString("lastSampledLocation")
        .write(session.lastSampledLocation)
      if session.tracking.exists(_.isMonitorable) then
        writer.writeString("monitorableTracking").write(session.tracking.flatMap(_.asMonitorable))
      else writer.writeString("tracking").write(session.tracking)
      writer.writeMapClose()
    ,
    Decoder[Session]: reader =>
      val unbounded = reader.readMapOpen(4)
      val fields = (0 until 4).foldLeft(
        (
          empty[Scope],
          empty[UserState],
          empty[Option[SampledLocation]],
          empty[Option[Tracking | MonitorableTracking]],
        ),
      ): (d, _) =>
        reader.readString() match
          case "scope" => d.copy(_1 = Some(reader.read[Scope]()))
          case "state" => d.copy(_2 = Some(reader.read[UserState]()))
          case "lastSampledLocation" => d.copy(_3 = Some(reader.read[Option[SampledLocation]]()))
          case "monitorableTracking" => d.copy(_4 = Some(reader.read[Option[MonitorableTracking]]()))
          case "tracking" => d.copy(_4 = Some(reader.read[Option[Tracking]]()))
          case _ => reader.unexpectedDataItem(expected = "`userId`, `state`, `lastSampledLocation` or `tracking`")
      val res = fields.mapN(Session.from).getOrElse(reader.validationFailure("Missing required fields"))
      reader.readMapClose(unbounded, res),
  )
