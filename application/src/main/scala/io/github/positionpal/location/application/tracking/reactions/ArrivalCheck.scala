package io.github.positionpal.location.application.tracking.reactions

import cats.Monad
import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import io.github.positionpal.entities.{GroupId, NotificationMessage, UserId}
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.tracking.MapsService
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.Distance.*

/** A [[TrackingEventReaction]] checking if the position curried by the event is near the arrival position. */
object ArrivalCheck:

  def apply[F[_]: Async](using maps: MapsService[F], notifier: NotificationService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      event match
        case e: SampledLocation if session.tracking.isDefined && session.tracking.get.isMonitorable =>
          for
            config <- ReactionsConfiguration.get
            tracking = session.tracking.get.asInstanceOf[MonitorableTracking]
            distance <- maps.distance(tracking.mode)(e.position, tracking.destination)
            isWithinProximity = distance.toMeters.value <= config.proximityToleranceMeters.meters.value
            _ <- if isWithinProximity then sendNotification(session.scope.group, e.user) else Async[F].unit
          yield if isWithinProximity then Left(RoutingStopped(e.timestamp, e.user, e.group)) else Right(Continue)
        case _ => Monad[F].pure(Right(Continue))

  private def sendNotification[F[_]: Async](group: GroupId, user: UserId)(using notifier: NotificationService[F]) =
    Async[F].start(notifier.sendToGroup(group, user, successMessage(user.username()))).void

  private def successMessage(username: String) = NotificationMessage
    .create(s"$username arrived!", s"$username has reached their destination on time.")
