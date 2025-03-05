package io.github.positionpal.location.messages.notifications

import lepus.protocol.domains.{ExchangeType, ShortString}
import io.github.positionpal.location.application.notifications.NotificationServiceProxy
import io.github.positionpal.commands.{CoMembersPushNotification, GroupWisePushNotification, PushNotificationCommand}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import lepus.client.{Connection, Message}
import io.github.positionpal.location.messages.RabbitMQ
import cats.effect.Async
import io.github.positionpal.AvroSerializer
import cats.effect.std.Queue

/** A [[NotificationServiceProxy]] implementing the [[send()]] method for publishing notifications
  * command to a RabbitMQ broker, expecting some downstream consumer service to handle the actual sending.
  *
  * @see <a href="https://github.com/position-pal/notification-service">the notification microservice</a>
  */
object RabbitMQNotificationsPublisher:

  def apply[F[_]: Async](): F[RabbitMQNotificationsPublisherImpl[F]] =
    Queue.unbounded[F, PushNotificationCommand].map(RabbitMQNotificationsPublisherImpl(_))

  class RabbitMQNotificationsPublisherImpl[F[_]: Async](queue: Queue[F, PushNotificationCommand])
      extends NotificationServiceProxy[F]
      with RabbitMQ.Utils:

    private val serializer = AvroSerializer()

    override def send(command: PushNotificationCommand): F[Unit] = queue.offer(command)

    def start(connection: Connection[F]): F[Unit] = connection.channel.use: ch =>
      for
        _ <- ch.exchange.declare(notificationsCommandExchange, ExchangeType.Headers, durable = true, autoDelete = false)
        publish =
          fs2.Stream
            .fromQueueUnterminated(queue)
            .map:
              case c: GroupWisePushNotification =>
                serializer.serializeGroupWiseNotification(c).toMessage(Map(msgTypeKey -> c.toShortString))
              case c: CoMembersPushNotification =>
                serializer.serializeCoMembersNotification(c).toMessage(Map(msgTypeKey -> c.toShortString))
            .evalMap(ch.messaging.publish(notificationsCommandExchange, ShortString.empty, _))
        _ <- publish.compile.drain
      yield ()

    extension (c: PushNotificationCommand) private def toShortString = c.`type`().name().asShortOrEmpty
