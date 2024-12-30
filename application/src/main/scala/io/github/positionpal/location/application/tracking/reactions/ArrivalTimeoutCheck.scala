package io.github.positionpal.location.application.tracking.reactions

import cats.Monad
import cats.effect.Async
import cats.implicits.toFunctorOps
import io.github.positionpal.entities.NotificationMessage
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.{Continue, EventReaction, on}
import io.github.positionpal.location.domain.Alert.Late
import io.github.positionpal.location.domain.{MonitorableTracking, SampledLocation, Scope, TimeoutAlertTriggered}

/** A [[TrackingEventReaction]] that verifies if the expected arrival time for a [[MonitorableTracking]]
  * has expired, possibly indicating a suspicious situation.
  * In this case, it triggers a notification, returning as an [[Outcome]] a [[TimeoutAlertTriggered]].
  * Its behavior is optimized to not send multiple notifications if the same alert was already triggered.
  */
object ArrivalTimeoutCheck:

  def apply[F[_]: Async](using notifier: NotificationService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      event match
        case e: SampledLocation if session.tracking.isDefined && session.tracking.get.isMonitorable =>
          val tracking = session.tracking.get.asInstanceOf[MonitorableTracking]
          if e.timestamp.isAfter(tracking.expectedArrival) && !(tracking has Late) then
            sendNotification(session.scope).map(_ => Left(TimeoutAlertTriggered(e.timestamp, e.scope)))
          else Monad[F].pure(Right(Continue))
        case _ => Monad[F].pure(Right(Continue))

  private def sendNotification[F[_]: Async](scope: Scope)(using notifier: NotificationService[F]) =
    Async[F].start(notifier.sendToGroup(scope.group, scope.user, alertMessage(scope.user.username()))).void

  private def alertMessage(username: String) = NotificationMessage
    .create(s"$username delay alert!", s"$username has not reached their destination as expected, yet.")
