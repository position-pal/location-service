package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.commons.ScopeFunctions.let
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import io.github.positionpal.location.domain.UserState.*
import cats.effect.Async
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.NotificationMessage

/** A [[TrackingEventReaction]] performing a preliminary check on the event, possibly emitting a
  * notification if it is a noteworthy event.
  */
object PreCheckNotifier:

  def apply[F[_]: Async](using notifier: NotificationService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      event.notify(session) match
        case Some(n) => Async[F].start(notifier.sendToOwnGroup(session.scope, n)).as(Left(()))
        case None => Right(Continue).pure[F]

  extension (event: ClientDrivingEvent)
    private def notify(s: Session) =
      val notification = event match
        case RoutingStarted(_, _, _, _, mode, destination, eta) =>
          Some("started a journey", s"is on their way to $destination ($mode). ETA: ${eta.format}.")
        case SOSAlertTriggered(_, _, _, position) =>
          Some("triggered an SOS alert!", s"has triggered an SOS help request at $position!")
        case _: WentOffline if s.userState == SOS || s.userState == Routing =>
          Some("went offline!", "went offline while on a journey.")
        case _: RoutingStopped => Some("journey ended", "journey completed successfully.")
        case _: SOSAlertStopped => Some("SOS alarm stopped!", "has stopped the SOS alarm.")
        case _ => None
      notification.map((t, b) => event.user.value().let(u => NotificationMessage.create(s"$u $t", s"$u $b")))
