package io.github.positionpal.location.application.notifications

import io.github.positionpal.location.domain.Scope
import io.github.positionpal.entities.{GroupId, NotificationMessage, UserId}

/** An outbound port for sending notifications to users.
  * @tparam F the effect type
  */
trait NotificationService[F[_]]:

  /** Send the given [[message]] to all members of the group of the
    * given [[scope]] on behalf of the [[scope]] user.
    */
  def sendToOwnGroup(scope: Scope, message: NotificationMessage): F[Unit] =
    sendToGroup(scope.groupId, scope.userId, message)

  /** Send the given [[message]] to all members of the given [[recipient]] group
    * on behalf of the [[sender]] user.
    */
  def sendToGroup(recipient: GroupId, sender: UserId, message: NotificationMessage): F[Unit]

  /** Send the given [[message]] to all users sharing a group with the given [[user]]
    * on behalf of the [[sender]] user.
    */
  def sendToAllMembersSharingGroupWith(user: UserId, sender: UserId, message: NotificationMessage): F[Unit]
