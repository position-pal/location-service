package io.github.positionpal.location.grpc

import io.github.positionpal.location.presentation.ProtoConversions.given
import fs2.Stream
import io.github.positionpal.location.presentation.ProtoUtils.*
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import io.grpc.Metadata
import io.github.positionpal.location.domain.Session
import cats.effect.kernel.Async
import io.github.positionpal.location.presentation.{proto, ProtoUtils}
import io.github.positionpal.location.application.sessions.UserSessionsService

/** A gRPC service adapter for the [[UserSessionsService]] to expose the session information of users.
  *
  * @param usersSessionService the service to which delegate the session queries logic
  * @tparam F the effect type
  */
class GrpcUserSessionsService[F[_]: Async](
    usersSessionService: UserSessionsService[F],
) extends proto.UserSessionsServiceFs2Grpc[F, Metadata]:

  override def getCurrentLocation(request: proto.Scope, ctx: Metadata): F[proto.LocationResponse] =
    ofUser(request):
      _.lastSampledLocation match
        case Some(location) => (okResponse, Some(location.position))
        case None => (notFoundResponse("User haven't shared their location, yet."), None)
    .map((s, r) => proto.LocationResponse(s, r.map(locationToProto(_))))

  override def getCurrentState(request: proto.Scope, ctx: Metadata): F[proto.UserStateResponse] =
    ofUser(request)(s => (okResponse, Some(s.state)))
      .map((s, r) => proto.UserStateResponse(s, r.getOrElse(proto.UserState.UNDEFINED)))

  override def getCurrentTracking(request: proto.Scope, ctx: Metadata): F[proto.TrackingResponse] =
    ofUser(request):
      _.tracking match
        case Some(tracking) => (okResponse, Some(tracking))
        case None => (notFoundResponse("User has no active tracking."), None)
    .map((s, r) => proto.TrackingResponse(s, r.map(trackingToProto(_))))

  private def ofUser[T](scope: proto.Scope)(onSuccess: Session => (Some[proto.Status], Option[T])) =
    usersSessionService
      .ofScope(scope)
      .map:
        case Some(s) => onSuccess(s)
        case None => (notFoundResponse(s"No session found for user ${scope.getUser.value}"), None)
      .handleError(e => (errorResponse(s"Error while fetching session related data: ${e.getMessage}"), None))

  override def getCurrentSession(request: proto.GroupId, ctx: Metadata): Stream[F, proto.SessionResponse] =
    usersSessionService
      .ofGroup(request)
      .map(s => proto.SessionResponse(okResponse, Some(s)))
      .handleError(e => proto.SessionResponse(errorResponse(s"Error while fetching group sessions: ${e.getMessage}")))
