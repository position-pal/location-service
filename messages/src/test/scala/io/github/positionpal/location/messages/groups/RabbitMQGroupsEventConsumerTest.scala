package io.github.positionpal.location.messages.groups

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import eu.monniot.scala3mock.cats.withExpectations
import eu.monniot.scala3mock.scalatest.MockFactory
import io.github.positionpal.location.application.groups.UserGroupsService
import io.github.positionpal.location.messages.RabbitMQ
import io.github.positionpal.location.messages.RabbitMQTestUtils.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RabbitMQGroupsEventConsumerTest extends AnyWordSpec with Matchers with MockFactory with RabbitMQ.Utils:

  import Utils.*
  import cats.effect.unsafe.implicits.global

  private val userGroupsService = mock[UserGroupsService[IO]]
  private val consumer = RabbitMQGroupsEventConsumer[IO](userGroupsService)

  "RabbitMQ groups related events consumer" should:
    "receive them correctly and forward appropriately to the `UserGroupsService`" in:
      withExpectations() {
        when(userGroupsService.addedMember).expects(addedEvent).returns(IO.unit).once
        when(userGroupsService.removeMember).expects(removedEvent).returns(IO.unit).once
        connection.use: conn =>
          for
            _ <- consumer.start(conn).start
            _ <- IO.sleep(5.seconds)
            _ <- testProducer(conn)
            _ <- IO.sleep(10.seconds)
          yield ()
      }.unsafeRunSync()

  private object Utils:
    import io.github.positionpal.{AvroSerializer, User, AddedMemberToGroup, RemovedMemberToGroup, MessageType}
    import io.github.positionpal.MessageType.*
    import lepus.client.{Connection, Message}
    import lepus.protocol.domains.{ExchangeType, ShortString}

    private val user = User.create("uid-test", "name-test", "surname-test", "email-test", "role-test")
    private val serializer = AvroSerializer()
    val addedEvent: AddedMemberToGroup = AddedMemberToGroup.create("guid-test-1", user)
    val removedEvent: RemovedMemberToGroup = RemovedMemberToGroup.create("guid-test-1", user)
    private val messageEvents = (addedEvent :: removedEvent :: Nil).map:
      case e: AddedMemberToGroup =>
        serializer.serializeAddedMemberToGroup(e).toMessage(Map(msgTypeKey -> MEMBER_ADDED.name().asShortOrEmpty))
      case e: RemovedMemberToGroup =>
        serializer.serializeRemovedMemberToGroup(e).toMessage(Map(msgTypeKey -> MEMBER_REMOVED.name().asShortOrEmpty))
    def testProducer(conn: Connection[IO]): IO[Unit] = conn.channel.use: ch =>
      for
        _ <- ch.exchange.declare(groupsEventsExchange, ExchangeType.Headers)
        publish = fs2.Stream(messageEvents*).evalMap(ch.messaging.publish(groupsEventsExchange, ShortString.empty, _))
        _ <- publish.compile.drain
      yield ()
  end Utils
end RabbitMQGroupsEventConsumerTest
