package io.github.positionpal.location.messages.groups

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import eu.monniot.scala3mock.cats.withExpectations
import eu.monniot.scala3mock.scalatest.MockFactory
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.messages.{MessageBrokerConnectionFactory, RabbitMQ}
import io.github.positionpal.{AddedMemberToGroup, AvroSerializer, MessageType, RemovedMemberToGroup, User}
import lepus.client.{Connection, Message}
import lepus.protocol.classes.basic.Properties
import lepus.protocol.domains.{ExchangeName, ExchangeType, FieldData, FieldTable, ShortString}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RabbitMQGroupsEventConsumerTest extends AnyWordSpec with Matchers with MockFactory with RabbitMQ.Protocol:

  import Utils.*

  private val userGroupsService = mock[UserGroupsService[IO]]
  private val consumer = RabbitMQGroupsEventConsumer[IO](userGroupsService)

  import cats.effect.unsafe.implicits.global

  "RabbitMQ groups related events consumer" should:
    "receive them correctly and forward appropriately to the `UserGroupsService`" in:
      when(userGroupsService.addedMember).expects(addedEvent).returns(IO.unit).once
      when(userGroupsService.removeMember).expects(removedEvent).returns(IO.unit).once
      val result = withExpectations() {
        connection.use: conn =>
          for
            _ <- consumer.start(conn).start
            _ <- testProducer(conn)
            _ <- IO.sleep(5.seconds)
          yield ()
      }
      result.unsafeRunSync()

  object Utils:

    val configuration = RabbitMQ.Configuration(
      host = "localhost",
      port = 5672,
      username = "guest",
      password = "admin",
      virtualHost = "/",
    )
    val connection = MessageBrokerConnectionFactory.ofRabbitMQ[IO](configuration)
    val user = User.create("uid-test", "name-test", "surname-test", "email-test", "role-test")
    val serializer = AvroSerializer()
    val addedEvent = AddedMemberToGroup.create("guid-test-1", user)
    val removedEvent = RemovedMemberToGroup.create("guid-test-1", user)
    val events = addedEvent :: removedEvent :: Nil
    val messageEvents = events.map:
      case e: AddedMemberToGroup =>
        Message(
          serializer.serializeAddedMemberToGroup(e),
          Properties(headers = Some(typeHeader(MessageType.MEMBER_ADDED))),
        )
      case e: RemovedMemberToGroup =>
        Message(
          serializer.serializeRemovedMemberToGroup(e),
          Properties(headers = Some(typeHeader(MessageType.MEMBER_REMOVED))),
        )

    private def typeHeader(mt: MessageType) = FieldTable(
      Map[ShortString, FieldData](
        ShortString.from(messageTypeHeaderKey).getOrElse(ShortString.empty) -> ShortString.from(mt.name())
          .getOrElse(ShortString.empty),
      ),
    )

    def testProducer(conn: Connection[IO]) = conn.channel.use: ch =>
      for
        _ <- IO.println(conn.capabilities.toFieldTable)
        e <- IO.fromOption(ExchangeName.from(groupsEventsExchangeName).toOption)(
          new RuntimeException("Exchange not declared"),
        )
        _ <- ch.exchange.declare(e, ExchangeType.Fanout)
        publish = fs2.Stream(messageEvents*).evalMap(ch.messaging.publish(e, ShortString.empty, _))
        _ <- publish.compile.drain
      yield ()
