package io.github.positionpal.location.messages.groups

import io.github.positionpal.location.commons.ScopeFunctions.withContext
import io.github.positionpal.events.EventType
import lepus.protocol.domains.{ExchangeType, FieldTable, ShortString}
import org.slf4j.LoggerFactory
import io.github.positionpal.AvroSerializer
import cats.implicits.*
import lepus.client.{Connection, ConsumeMode, Message}
import io.github.positionpal.location.messages.RabbitMQ
import cats.effect.Async
import io.github.positionpal.location.application.groups.UserGroupsService

/** A consumer of groups-related events from RabbitMQ broker. */
object RabbitMQGroupsEventConsumer:

  import cats.implicits.catsSyntaxApplicativeId

  def apply[F[_]: Async](userGroupsService: UserGroupsService[F]): F[RabbitMQGroupsEventConsumerImpl[F]] =
    RabbitMQGroupsEventConsumerImpl(userGroupsService).pure[F]

  class RabbitMQGroupsEventConsumerImpl[F[_]: Async](userGroupsService: UserGroupsService[F]) extends RabbitMQ.Utils:

    private val logger = LoggerFactory.getLogger(getClass)

    def start(connection: Connection[F]): F[Unit] = connection.channel.use: ch =>
      for
        _ <- ch.exchange.declare(groupsEventsExchange, ExchangeType.Headers, durable = true, autoDelete = false)
        q <- ch.queue.declare(groupsEventsQueue, durable = true)
        q <- Async[F].fromOption(q, QueueDeclarationFailed)
        _ <- ch.queue.bind(q.queue, groupsEventsExchange, ShortString.empty)
        consumer = ch.messaging
          .consume[Array[Byte]](q.queue, ConsumeMode.RaiseOnError(true))
          .evalMap: e =>
            handleGroupEvent(e.message).attempt
              .flatMap:
                case Right(_) => ch.messaging.ack(e.deliveryTag)
                case Left(err) => ch.messaging.nack(e.deliveryTag) *> Async[F].delay(logger.error(err.getMessage))
        _ <- consumer.compile.drain
      yield ()

    private def handleGroupEvent(event: Message[Array[Byte]]): F[Unit] = withContext(AvroSerializer()): s =>
      event.properties.headers match
        case Some(headers) if Header.memberAdded in headers =>
          userGroupsService.addedMember(s.deserializeAddedMemberToGroup(event.payload))
        case Some(headers) if Header.memberRemoved in headers =>
          userGroupsService.removeMember(s.deserializeRemovedMemberToGroup(event.payload))
        case _ => Async[F].unit

    private object Header:
      val memberAdded: ShortString = EventType.MEMBER_ADDED.name().asShortOrEmpty
      val memberRemoved: ShortString = EventType.MEMBER_REMOVED.name().asShortOrEmpty
