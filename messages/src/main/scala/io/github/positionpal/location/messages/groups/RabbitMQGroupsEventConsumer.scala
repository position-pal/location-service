package io.github.positionpal.location.messages.groups

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.messages.RabbitMQ
import io.github.positionpal.{AvroSerializer, MessageType}
import lepus.client.{Connection, Message}
import lepus.protocol.domains.{FieldTable, ShortString}

/** A consumer of groups-related events from RabbitMQ broker.
  * @param userGroupsService the service to handle the events
  * @tparam F the effect type
  */
class RabbitMQGroupsEventConsumer[F[_]: Async](userGroupsService: UserGroupsService[F]) extends RabbitMQ.Utils:

  private val serializer = AvroSerializer()

  def start(connection: Connection[F]): F[Unit] = connection.channel.use: ch =>
    for
      q <- ch.queue.declare(groupsEventsQueue, autoDelete = false, durable = true, exclusive = false)
      q <- Async[F].fromOption(q, QueueDeclarationFailed)
      _ <- ch.queue.bind(q.queue, groupsEventsExchange, ShortString.empty)
      consumer = ch.messaging.consume[Array[Byte]](q.queue).evalMap(e => handleGroupEvent(e.message))
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
