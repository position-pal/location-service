package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.application.tracking.reactions.TrackingEventReaction.{on, Continue, EventReaction}
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps}
import io.github.positionpal.location.domain.{notification, SampledLocation, TimeoutAlertTriggered}
import cats.effect.Async
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.location.domain.Alert.Late

/** A [[TrackingEventReaction]] that verifies if the expected arrival time for a [[MonitorableTracking]]
  * has expired, possibly indicating a suspicious situation.
  * In this case, it triggers a notification, returning as an [[Outcome]] a [[TimeoutAlertTriggered]].
  * Its behavior is optimized to not send multiple notifications if the same alert was already triggered.
  */
object ArrivalTimeoutCheck:

  def apply[F[_]: Async](using NotificationService[F], UserGroupsService[F]): EventReaction[F] =
    on[F]: (session, event) =>
      event match
        case e: SampledLocation if session.tracking.exists(_.isMonitorable) =>
          val tracking = session.tracking.asMonitorable.get
          if e.timestamp.isAfter(tracking.expectedArrival) && !(tracking has Late) then
            sendNotification(session.scope, alertMessage).map(_ => Left(TimeoutAlertTriggered(e.timestamp, e.scope)))
          else Right(Continue).pure[F]
        case _ => Right(Continue).pure[F]

  private val alertMessage = notification(" delay alert!", " has not reached their destination as expected, yet.")
