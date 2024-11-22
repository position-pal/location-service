package io.github.positionpal.location.messages.groups

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import cats.effect.kernel.Resource
import eu.monniot.scala3mock.cats.withExpectations
import eu.monniot.scala3mock.scalatest.MockFactory
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.messages.{MessageBrokerConnectionFactory, RabbitMQ}
import io.github.positionpal.{AddedMemberToGroup, AvroSerializer, MessageType, RemovedMemberToGroup, User}
import lepus.client.{Connection, Message}
import lepus.protocol.classes.basic.Properties
import lepus.protocol.domains.{ExchangeType, FieldData, FieldTable, ShortString}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RabbitMQGroupsEventConsumerTest extends AnyWordSpec with Matchers with MockFactory with RabbitMQ.Utils:

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
            _ <- IO.sleep(10.seconds)
          yield ()
      }
      result.unsafeRunSync()

  object Utils:

    private val configuration: RabbitMQ.Configuration =
      RabbitMQ.Configuration(host = "localhost", port = 5672, username = "guest", password = "admin", virtualHost = "/")
    val connection: Resource[IO, Connection[IO]] = MessageBrokerConnectionFactory.ofRabbitMQ[IO](configuration)
    private val user = User.create("uid-test", "name-test", "surname-test", "email-test", "role-test")
    private val serializer = AvroSerializer()
    val addedEvent: AddedMemberToGroup = AddedMemberToGroup.create("guid-test-1", user)
    val removedEvent: RemovedMemberToGroup = RemovedMemberToGroup.create("guid-test-1", user)
    private val messageEvents = (addedEvent :: removedEvent :: Nil).map:
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

    private def typeHeader(mt: MessageType) =
      FieldTable(Map[ShortString, FieldData](messageTypeKey -> mt.name().asShortOrEmpty))

    def testProducer(conn: Connection[IO]): IO[Unit] = conn.channel.use: ch =>
      for
        _ <- IO.println(conn.capabilities.toFieldTable)
        _ <- ch.exchange.declare(groupsEventsExchange, ExchangeType.Headers)
        publish = fs2.Stream(messageEvents*).evalMap(ch.messaging.publish(groupsEventsExchange, ShortString.empty, _))
        _ <- publish.compile.drain
      yield ()
