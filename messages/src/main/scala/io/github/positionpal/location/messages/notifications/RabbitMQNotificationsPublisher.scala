package io.github.positionpal.location.messages.notifications

import cats.effect.Async
import cats.effect.std.Queue
import cats.implicits.{toFlatMapOps, toFunctorOps}
import io.github.positionpal.AvroSerializer
import io.github.positionpal.commands.{CoMembersPushNotification, GroupWisePushNotification, PushNotificationCommand}
import io.github.positionpal.location.application.notifications.impl.NotificationServiceProxy
import io.github.positionpal.location.messages.RabbitMQ
import lepus.client.{Connection, Message}
import lepus.protocol.classes.basic.Properties
import lepus.protocol.domains.{ExchangeType, FieldData, FieldTable, ShortString}

/** A [[NotificationServiceProxy]] implementing the [[send()]] method for publishing notifications
  * command to a RabbitMQ broker, expecting some downstream consumer service to handle the actual sending.
  *
  * @see <a href="https://github.com/position-pal/notification-service">the notification microservice</a>
  */
object RabbitMQNotificationsPublisher:

  def apply[F[_]: Async](): F[RabbitMQNotificationsPublisherImpl[F]] =
    Queue.unbounded[F, PushNotificationCommand].map(RabbitMQNotificationsPublisherImpl(_))

  class RabbitMQNotificationsPublisherImpl[F[_]: Async](
      queue: Queue[F, PushNotificationCommand],
  ) extends NotificationServiceProxy[F]
      with RabbitMQ.Utils:

    private val serializer = AvroSerializer()

    def start(connection: Connection[F]): F[Unit] = connection.channel.use: ch =>
      for
        _ <- ch.exchange.declare(notificationsCommandExchange, ExchangeType.Headers)
        publish = fs2.Stream.fromQueueUnterminated(queue).map {
          case c: GroupWisePushNotification =>
            Message(
              serializer.serializeGroupWiseNotification(c),
              Properties(headers =
                Some(FieldTable(Map[ShortString, FieldData](messageTypeKey -> c.`type`().name().asShortOrEmpty))),
              ),
            )
          case c: CoMembersPushNotification =>
            Message(
              serializer.serializeCoMembersNotification(c),
              Properties(headers =
                Some(FieldTable(Map[ShortString, FieldData](messageTypeKey -> c.`type`().name().asShortOrEmpty))),
              ),
            )
        }.evalMap(ch.messaging.publish(notificationsCommandExchange, ShortString.empty, _))
        _ <- publish.compile.drain
      yield ()

    override def send(command: PushNotificationCommand): F[Unit] = queue.offer(command)
