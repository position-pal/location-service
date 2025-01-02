package io.github.positionpal.location.application.notifications.impl

import io.github.positionpal.location.application.notifications.NotificationService
import io.github.positionpal.commands.{CoMembersPushNotification, GroupWisePushNotification, PushNotificationCommand}
import io.github.positionpal.entities.{GroupId, NotificationMessage, UserId}

/** A base implementation for a proxy notification service that delegates the actual logic to an external component.
  * @tparam F the effect type
  */
trait NotificationServiceProxy[F[_]] extends NotificationService[F]:

  override def sendToGroup(recipient: GroupId, sender: UserId, message: NotificationMessage): F[Unit] =
    send(GroupWisePushNotification.of(recipient, sender, message))

  override def sendToAllMembersSharingGroupWith(user: UserId, sender: UserId, message: NotificationMessage): F[Unit] =
    send(CoMembersPushNotification.of(user, sender, message))

  /** Send the given [[command]] to the external notification service, which will handle the actual sending. */
  def send(command: PushNotificationCommand): F[Unit]
