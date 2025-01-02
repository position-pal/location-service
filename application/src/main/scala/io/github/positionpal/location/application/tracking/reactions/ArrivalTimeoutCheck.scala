package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.{on, Continue, EventReaction}
import cats.implicits.toFunctorOps
import cats.Monad
import cats.effect.Async
import io.github.positionpal.location.domain.SampledLocation
import io.github.positionpal.entities.{GroupId, NotificationMessage, UserId}

/** A [[TrackingEventReaction]] checking if the expected arrival time has expired. */
object ArrivalTimeoutCheck:

  def apply[F[_]: Async](using notifier: NotificationService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      event match
        case e: SampledLocation if session.tracking.exists(_.isMonitorable) =>
          val tracking = session.tracking.flatMap(_.asMonitorable).get
          if e.timestamp.isAfter(tracking.expectedArrival) then
            sendNotification(session.scope.group, e.user).map(_ => Left(()))
          else Monad[F].pure(Right(Continue))
        case _ => Monad[F].pure(Right(Continue))

  private def sendNotification[F[_]: Async](group: GroupId, user: UserId)(using notifier: NotificationService[F]) =
    Async[F].start(notifier.sendToGroup(group, user, alertMessage(user.username()))).void

  private def alertMessage(username: String) = NotificationMessage
    .create(s"$username delay alert!", s"$username has not reached their destination as expected, yet.")
