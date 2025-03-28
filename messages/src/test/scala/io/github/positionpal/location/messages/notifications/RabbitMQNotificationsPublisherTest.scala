package io.github.positionpal.location.messages.notifications

import scala.concurrent.duration.DurationInt

import eu.monniot.scala3mock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import io.github.positionpal.commands.CommandType.*
import lepus.client.DeliveredMessage
import io.github.positionpal.location.messages.RabbitMQ
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.IO
import io.github.positionpal.location.messages.RabbitMQTestUtils.*
import io.github.positionpal.commands.{CoMembersPushNotification, GroupWisePushNotification}

class RabbitMQNotificationsPublisherTest extends AnyWordSpec with Matchers with MockFactory with RabbitMQ.Utils:

  import Utils.*
  import cats.effect.unsafe.implicits.global

  "RabbitMQ notifications publisher" should:
    "successfully publish on RabbitMQ exchange" in:
      val app = for
        p <- RabbitMQNotificationsPublisher[IO]()
        _ <- IO.sleep(5.seconds)
        _ <- p.send(GroupWisePushNotification.of(group, user, notification))
        _ <- p.send(CoMembersPushNotification.of(user, user, notification))
      yield p
      val expected = List(GROUP_WISE_NOTIFICATION, CO_MEMBERS_NOTIFICATION).map(t => Some(t.name().asShortOrEmpty))
      val received = connection
        .use: conn =>
          for
            a <- app
            _ <- a.start(conn).start
            c <- Utils.consumer(conn)
          yield c
        .unsafeRunSync()
      received.map(_.message.properties.headers.get.get(msgTypeKey)).distinct should contain allElementsOf expected

  object Utils:
    import io.github.positionpal.entities.{GroupId, NotificationMessage, UserId}
    import lepus.client.{Connection, ConsumeMode}
    import lepus.protocol.domains.{QueueName, ShortString}

    val user: UserId = UserId.create("uid-test")
    val group: GroupId = GroupId.create("gid-test")
    val notification: NotificationMessage = NotificationMessage.create("A entitled message", "Some useful information")

    def consumer(conn: Connection[IO]): IO[List[DeliveredMessage[String]]] =
      conn.channel.use: ch =>
        for
          q <- ch.queue.declare(QueueName("test-notifications-queue"), autoDelete = false)
          q <- IO.fromOption(q)(new Exception())
          _ <- ch.queue.bind(q.queue, notificationsCommandExchange, ShortString.empty)
          c = ch.messaging.consume[String](q.queue, mode = ConsumeMode.RaiseOnError(true))
          l <- c.interruptAfter(15.seconds).compile.toList
        yield l
  end Utils
end RabbitMQNotificationsPublisherTest
