package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.domain.Distance.*
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import cats.implicits.*
import io.github.positionpal.location.application.tracking.MapsService
import cats.effect.Async
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.domain.Alert.Stuck
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.NotificationMessage

/** A [[TrackingEventReaction]] checking whether the position curried by the event has remained
  * approximately in the same location for some time, possibly indicating a suspicious situation.
  * In this case, it triggers a notification, returning as an [[Outcome]] a [[StuckAlertTriggered]].
  * Once the user moves again, it stops the alert, returning a [[StuckAlertStopped]].
  */
object StationaryCheck:

  def apply[F[_]: Async](using maps: MapsService[F], notifier: NotificationService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      event match
        case e: SampledLocation if session.tracking.exists(_.isMonitorable) =>
          for
            config <- ReactionsConfiguration.get
            tracking <- session.tracking.flatMap(_.asMonitorable).get.pure[F]
            samples = tracking.route.take(config.stationarySamples)
            distances <- samples.traverse(s => maps.distance(tracking.mode)(s.position, e.position))
            isStationary = distances.size >= config.stationarySamples &&
              distances.forall(_.toMeters.value <= config.proximityToleranceMeters.meters.value)
            res <-
              if isStationary && !(tracking has Stuck) then
                sendNotification(session.scope).map(_ => Left(StuckAlertTriggered(e.timestamp, e.scope)))
              else if !isStationary && (tracking has Stuck) then
                Left(StuckAlertStopped(e.timestamp, e.user, e.group)).pure[F]
              else Right(Continue).pure[F]
          yield res
        case _ => Right(Continue).pure[F]

  private def sendNotification[F[_]: Async](scope: Scope)(using notifier: NotificationService[F]) =
    Async[F].start(notifier.sendToGroup(scope.groupId, scope.userId, alertMessage(scope.userId.value()))).void

  private def alertMessage(username: String) = NotificationMessage
    .create(s"$username() stationary alert!", s"$username has been stuck in the same position for a while.")
