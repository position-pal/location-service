package io.github.positionpal.location.presentation

object ProtoConversions:
  import io.github.positionpal.entities.{UserId, GroupId}
  import io.github.positionpal.location.domain.*
  import scalapb.TimestampConverters.fromJavaInstant

  given uidToProto: Conversion[UserId, proto.UserId] = uid => proto.UserId(uid.username())

  given protoToUid: Conversion[proto.UserId, UserId] = uid => UserId.create(uid.username)

  given protoToGid: Conversion[GroupId, proto.GroupId] = gid => proto.GroupId(gid.value)

  given gidToProto: Conversion[proto.GroupId, GroupId] = gid => GroupId.create(gid.value)

  given stateToProto: Conversion[UserState, proto.UserState] = {
    case UserState.Active => proto.UserState.ACTIVE
    case UserState.Inactive => proto.UserState.INACTIVE
    case UserState.SOS => proto.UserState.SOS
    case UserState.Routing => proto.UserState.ROUTING
  }

  given locationToProto: Conversion[GPSLocation, proto.GPSLocation] = l => proto.GPSLocation(l.latitude, l.longitude)

  given protoToLocation: Conversion[proto.GPSLocation, GPSLocation] = l => GPSLocation(l.latitude, l.longitude)

  given sampledLocationToProto: Conversion[SampledLocation, proto.SampledLocation] = l =>
    proto.SampledLocation(Some(l.user), Some(l.timestamp), Some(l.position))

  given routeToProto: Conversion[Route, proto.Route] = r => proto.Route(r.map(identity))

  given trackingToProto: Conversion[Tracking | MonitorableTracking, proto.Tracking] = {
    case t: MonitorableTracking => proto.Tracking(Some(t.route), Some(t.destination), Some(t.expectedArrival))
    case t: Tracking => proto.Tracking(Some(t.route))
  }

  given sessionToProto: Conversion[Session, proto.Session] = s =>
    proto.Session(
      Some(s.userId),
      s.userState,
      s.lastSampledLocation.map(sampledLocationToProto(_)),
      s.tracking.map(trackingToProto(_)),
    )
end ProtoConversions

object ProtoUtils:
  import io.github.positionpal.location.presentation.proto.Status

  def okResponse: Some[Status] = Some(proto.Status(proto.StatusCode.OK))

  def notFoundResponse(message: String): Some[Status] = Some(proto.Status(proto.StatusCode.NOT_FOUND, message))

  def errorResponse(message: String): Some[Status] = Some(proto.Status(proto.StatusCode.GENERIC_ERROR, message))
end ProtoUtils
