package io.github.positionpal.location.messages.groups

import lepus.protocol.domains.{ExchangeType, FieldTable, ShortString}
import io.github.positionpal.{AvroSerializer, MessageType}
import cats.implicits.{toFlatMapOps, toFunctorOps}
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

    private val serializer = AvroSerializer()

    def start(connection: Connection[F]): F[Unit] = connection.channel.use: ch =>
      for
        _ <- ch.exchange.declare(groupsEventsExchange, ExchangeType.Headers)
        q <- ch.queue.declare(groupsEventsQueue, autoDelete = false, durable = true, exclusive = false)
        q <- Async[F].fromOption(q, QueueDeclarationFailed)
        _ <- ch.queue.bind(q.queue, groupsEventsExchange, ShortString.empty)
        consumer = ch.messaging
          .consume[Array[Byte]](q.queue, mode = ConsumeMode.RaiseOnError(true))
          .evalMap(e => handleGroupEvent(e.message))
        _ <- consumer.compile.drain
      yield ()

    private def handleGroupEvent(event: Message[Array[Byte]]): F[Unit] = event.properties.headers match
      case Some(headers) if Header.memberAdded in headers =>
        userGroupsService.addedMember(serializer.deserializeAddedMemberToGroup(event.payload))
      case Some(headers) if Header.memberRemoved in headers =>
        userGroupsService.removeMember(serializer.deserializeRemovedMemberToGroup(event.payload))
      case _ => Async[F].unit

    private object Header:
      val memberAdded: ShortString = MessageType.MEMBER_ADDED.name().asShortOrEmpty
      val memberRemoved: ShortString = MessageType.MEMBER_REMOVED.name().asShortOrEmpty
