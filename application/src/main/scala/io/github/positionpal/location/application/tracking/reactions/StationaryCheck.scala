package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.domain.Distance.*
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import cats.implicits.{toFlatMapOps, toFunctorOps, toTraverseOps}
import io.github.positionpal.location.application.tracking.MapsService
import cats.Monad
import cats.effect.Async
import io.github.positionpal.location.domain.SampledLocation
import io.github.positionpal.entities.{GroupId, NotificationMessage, UserId}

/** A [[TrackingEventReaction]] checking if the position curried by the event is continually in the same location. */
object StationaryCheck:

  def apply[F[_]: Async](using maps: MapsService[F], notifier: NotificationService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      event match
        case e: SampledLocation if session.tracking.exists(_.isMonitorable) =>
          for
            config <- ReactionsConfiguration.get
            tracking <- Async[F].pure(session.tracking.flatMap(_.asMonitorable).get)
            samples = tracking.route.take(config.stationarySamples)
            distances <- samples.traverse(s => maps.distance(tracking.mode)(s.position, e.position))
            isStationary = distances.size >= config.stationarySamples &&
              distances.forall(_.toMeters.value <= config.proximityToleranceMeters.meters.value)
            _ <- if isStationary then sendNotification(session.scope.group, e.user) else Async[F].unit
          yield if isStationary then Left(()) else Right(Continue)
        case _ => Monad[F].pure(Right(Continue))

  private def sendNotification[F[_]: Async](group: GroupId, user: UserId)(using notifier: NotificationService[F]) =
    Async[F].start(notifier.sendToGroup(group, user, alertMessage(user.username()))).void

  private def alertMessage(username: String) = NotificationMessage
    .create(s"$username() stationary alert!", s"$username has been stuck in the same position for a while.")
