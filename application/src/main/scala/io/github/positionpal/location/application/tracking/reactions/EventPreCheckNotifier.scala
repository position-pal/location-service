package io.github.positionpal.location.application.tracking.reactions

import cats.effect.Async
import cats.implicits.catsSyntaxApplicativeId
import io.github.positionpal.entities.NotificationMessage
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import io.github.positionpal.location.commons.ScopeFunctions.let
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.UserState.*

/** A [[TrackingEventReaction]] performing a preliminary check on the event, possibly emitting a
  * notification if it is a noteworthy event.
  */
object EventPreCheckNotifier:

  def apply[F[_]: Async](using notifier: NotificationService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      event
        .notify(session)
        .map(n => Async[F].start(notifier.sendToGroup(session.scope.group, event.user, n)))
        .map(_ => Left(()).pure[F])
        .getOrElse(Right(Continue).pure[F])

  extension (event: DrivingEvent)
    private def notify(s: Session) =
      val notification = event match
        case RoutingStarted(_, _, _, mode, destination, eta) =>
          Some("started a journey", s"is on their way to $destination ($mode). ETA: ${eta.format}.")
        case SOSAlertTriggered(_, _, position) =>
          Some("triggered an SOS alert!", s"has triggered an SOS help request at $position!")
        case WentOffline(_, _) if s.userState == Routing || s.userState == Routing =>
          Some("went offline!", "went offline while on a journey.")
        case RoutingStopped(_, _) => Some("journey ended", "journey completed successfully.")
        case SOSAlertStopped(_, _) => Some("SOS alarm stopped!", "has stopped the SOS alarm.")
        case _ => None
      notification.map((t, b) => event.user.username().let(u => NotificationMessage.create(s"$u $t", s"$u $b")))
