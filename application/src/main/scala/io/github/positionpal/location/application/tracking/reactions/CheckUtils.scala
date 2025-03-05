package io.github.positionpal.location.application.tracking.reactions

import io.github.positionpal.location.application.notifications.NotificationService
import cats.implicits.{toFlatMapOps, toFunctorOps}
import cats.effect.Async
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.domain.{prepended, Scope}
import io.github.positionpal.entities.{NotificationMessage, User}

def sendNotification[F[_]: Async](
    scope: Scope,
    msg: NotificationMessage,
)(using notifier: NotificationService[F], groups: UserGroupsService[F]): F[Unit] =
  Async[F]
    .start:
      groups
        .of(scope)
        .flatMap:
          case Some(user) => notifier.sendToOwnGroup(scope, msg.prepended(s"${user.name()} ${user.surname()}"))
          case None => Async[F].unit
    .void
