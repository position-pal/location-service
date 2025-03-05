package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.domain.Distance.*
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import cats.implicits.{catsSyntaxApplicativeId, toFlatMapOps, toFunctorOps}
import io.github.positionpal.location.application.tracking.MapsService
import io.github.positionpal.location.domain.*
import cats.effect.Async
import io.github.positionpal.location.application.groups.UserGroupsService

/** A [[TrackingEventReaction]] checking if the position curried by the event is near the arrival position. */
object ArrivalCheck:

  def apply[F[_]: Async](using MapsService[F], NotificationService[F], UserGroupsService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      event match
        case e: SampledLocation if session.tracking.exists(_.isMonitorable) =>
          for
            config <- ReactionsConfiguration.get
            tracking <- session.tracking.asMonitorable.get.pure[F]
            distance <- summon[MapsService[F]].distance(tracking.mode)(e.position, tracking.destination.position)
            isWithinProximity = distance.toMeters.value <= config.proximityToleranceMeters.meters.value
            _ <- if isWithinProximity then sendNotification(session.scope, successMessage) else Async[F].unit
          yield if isWithinProximity then Left(RoutingStopped(e.timestamp, e.user, e.group)) else Right(Continue)
        case _ => Right(Continue).pure[F]

  private val successMessage = notification(" arrived!", " has reached their destination on time.")
