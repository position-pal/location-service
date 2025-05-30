package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.*
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import io.github.positionpal.location.domain.UserState.*
import cats.effect.Async
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.domain.*
import io.github.positionpal.entities.NotificationMessage

/** A [[TrackingEventReaction]] performing a preliminary check on the event, possibly emitting a
  * notification if it is a noteworthy event.
  */
object PreCheckNotifier:

  def apply[F[_]: Async](using NotificationService[F], UserGroupsService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      createFrom(event, session) match
        case Some(n) => sendNotification(session.scope, n).as(Left(()))
        case None => Right(Continue).pure[F]

  private def createFrom(event: ClientDrivingEvent, session: Session): Option[NotificationMessage] =
    val notificationMessage = event match
      case RoutingStarted(_, _, _, _, mode, destination, eta) =>
        Some(" started a journey", s" is on their way to ${destination.name} ($mode). ETA: ${eta.format}.")
      case _: SOSAlertTriggered =>
        Some(" triggered an SOS alert!", " has triggered an SOS help request. Check their real-time location now!")
      case _: WentOffline if session.userState == SOS || session.userState == Routing =>
        Some(" went offline!", " went offline. Please check on their real-time location and status!")
      case _: RoutingStopped => Some("'s journey ended", "'s journey has completed successfully.")
      case _: SOSAlertStopped => Some(" SOS alarm stopped!", " has stopped the SOS alarm.")
      case _ => None
    notificationMessage.map(notification)
