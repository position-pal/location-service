package io.github.positionpal.location.grpc

import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import fs2.Stream
import io.github.positionpal.location.application.sessions.UsersSessionService
import io.github.positionpal.location.domain.Session
import io.github.positionpal.location.presentation.ProtoConversions.given
import io.github.positionpal.location.presentation.ProtoUtils.*
import io.github.positionpal.location.presentation.{ProtoUtils, proto}
import io.grpc.Metadata

/** A gRPC service adapter for the [[UsersSessionService]] to expose the session information of users.
  * @param usersSessionService the service to which delegate the session queries logic
  * @tparam F the effect type
  */
class GrpcUserSessionService[F[_]: Async](
    usersSessionService: UsersSessionService[F],
) extends proto.UserSessionServiceFs2Grpc[F, Metadata]:

  override def getCurrentLocation(request: proto.UserId, ctx: Metadata): F[proto.LocationResponse] =
    ofUser(request):
      _.lastSampledLocation match
        case Some(location) => (okResponse, Some(location.position))
        case None => (notFoundResponse("User haven't shared their location, yet."), None)
    .map((s, r) => proto.LocationResponse(s, r.map(locationToProto(_))))

  override def getCurrentState(request: proto.UserId, ctx: Metadata): F[proto.UserStateResponse] =
    ofUser(request)(s => (okResponse, Some(s.state)))
      .map((s, r) => proto.UserStateResponse(s, r.getOrElse(proto.UserState.INACTIVE)))

  override def getCurrentTracking(request: proto.UserId, ctx: Metadata): F[proto.TrackingResponse] =
    ofUser(request):
      _.tracking match
        case Some(tracking) => (okResponse, Some(tracking))
        case None => (notFoundResponse("User has no active tracking."), None)
    .map((s, r) => proto.TrackingResponse(s, r.map(trackingToProto(_))))

  private def ofUser[T](uid: proto.UserId)(onSuccess: Session => (Some[proto.Status], Option[T])) =
    usersSessionService.ofUser(uid).map:
      case Some(s) => onSuccess(s)
      case None => (notFoundResponse(s"No session found for user ${uid.username}"), None)
    .handleError(e => (errorResponse(s"Error while fetching session related data: ${e.getMessage}"), None))

  override def getCurrentSession(request: proto.GroupId, ctx: Metadata): Stream[F, proto.SessionResponse] =
    usersSessionService.ofGroup(request).map(s => proto.SessionResponse(okResponse, Some(s)))
      .handleError(e => proto.SessionResponse(errorResponse(s"Error while fetching group sessions: ${e.getMessage}")))
