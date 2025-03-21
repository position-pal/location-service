package io.github.positionpal.location.presentation

/** A set of given instances for converting domain entities to and from their protocol buffer representations. */
object ProtoConversions:
  import io.github.positionpal.entities.{GroupId, UserId}
  import io.github.positionpal.location.domain.*
  import scalapb.TimestampConverters.fromJavaInstant

  given uidToProto: Conversion[UserId, proto.UserId] = uid => proto.UserId(uid.value())

  given protoToUid: Conversion[proto.UserId, UserId] = uid => UserId.create(uid.value)

  given protoToGid: Conversion[GroupId, proto.GroupId] = gid => proto.GroupId(gid.value)

  given gidToProto: Conversion[proto.GroupId, GroupId] = gid => GroupId.create(gid.value)

  given scopeToProto: Conversion[Scope, proto.Scope] = s => proto.Scope(Some(s.userId), Some(s.groupId))

  given protoToScope: Conversion[proto.Scope, Scope] = s => Scope(s.getUser, s.getGroup)

  given stateToProto: Conversion[UserState, proto.UserState] = {
    case UserState.Active => proto.UserState.ACTIVE
    case UserState.Inactive => proto.UserState.INACTIVE
    case UserState.SOS => proto.UserState.SOS
    case UserState.Routing => proto.UserState.ROUTING
    case UserState.Warning => proto.UserState.WARNING
  }

  given locationToProto: Conversion[GPSLocation, proto.GPSLocation] = l => proto.GPSLocation(l.latitude, l.longitude)

  given protoToLocation: Conversion[proto.GPSLocation, GPSLocation] = l => GPSLocation(l.latitude, l.longitude)

  given sampledLocationToProto: Conversion[SampledLocation, proto.SampledLocation] = l =>
    proto.SampledLocation(Some(l.user), Some(l.timestamp), Some(l.position))

  given routeToProto: Conversion[Route, proto.Route] = r => proto.Route(r.map(identity))

  given addressToProto: Conversion[Address, proto.Address] = a => proto.Address(a.name, Some(a.position))

  given trackingToProto: Conversion[Tracking, proto.Tracking] = {
    case t: MonitorableTracking => proto.Tracking(Some(t.route), Some(t.destination), Some(t.expectedArrival))
    case t: Tracking => proto.Tracking(Some(t.route))
  }

  given sessionToProto: Conversion[Session, proto.Session] = s =>
    proto.Session(
      Some(s.scope),
      s.userState,
      s.lastSampledLocation.map(sampledLocationToProto(_)),
      s.tracking.map(trackingToProto(_)),
    )
end ProtoConversions

/** A set of utility methods for working with protocol buffer messages. */
object ProtoUtils:
  import io.github.positionpal.location.presentation.proto.Status

  /** @return a successful response status. */
  def okResponse: Some[Status] = Some(proto.Status(proto.StatusCode.OK))

  /** @return a not found response status with the provided message. */
  def notFoundResponse(message: String): Some[Status] = Some(proto.Status(proto.StatusCode.NOT_FOUND, message))

  /** @return a generic error response with the provided message. */
  def errorResponse(message: String): Some[Status] = Some(proto.Status(proto.StatusCode.GENERIC_ERROR, message))
end ProtoUtils
