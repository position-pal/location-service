package io.github.positionpal.location.messages.notifications

import scala.concurrent.duration.DurationInt

import cats.effect.IO
import cats.effect.kernel.Resource
import eu.monniot.scala3mock.scalatest.MockFactory
import io.github.positionpal.*
import io.github.positionpal.commands.GroupWisePushNotification
import io.github.positionpal.entities.{GroupId, NotificationMessage, UserId}
import io.github.positionpal.location.messages.{MessageBrokerConnectionFactory, RabbitMQ}
import lepus.client.{Connection, ConsumeMode}
import lepus.protocol.domains.{QueueName, ShortString}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RabbitMQNotificationsPublisherTest extends AnyWordSpec with Matchers with MockFactory with RabbitMQ.Utils:

  import Utils.*
  import cats.effect.unsafe.implicits.global

  "RabbitMQ notifications publisher" should:
    "work" in:
      val app = for
        p <- RabbitMQNotificationsPublisher[IO]()
        _ <- p.send(GroupWisePushNotification.of(group, user, notification))
      yield p
      connection.use: conn =>
        for
          a <- app
          _ <- a.start(conn).start
          _ <- Utils.consumer(conn)
        yield ()
      .unsafeRunSync()

  object Utils:

    private val configuration: RabbitMQ.Configuration =
      RabbitMQ.Configuration(host = "localhost", port = 5672, username = "guest", password = "admin", virtualHost = "/")
    val connection: Resource[IO, Connection[IO]] = MessageBrokerConnectionFactory.ofRabbitMQ[IO](configuration)
    val user: UserId = UserId.create("uid-test")
    val group: GroupId = GroupId.create("gid-test")
    val notification: NotificationMessage = NotificationMessage
      .create("A entitled test message", "Some useful information")

    def consumer(conn: Connection[IO]): IO[Unit] = conn.channel.use: ch =>
      for
        q <- ch.queue.declare(QueueName("hello-world"), autoDelete = false)
        q <- IO.fromOption(q)(new Exception())
        _ <- ch.queue.bind(q.queue, notificationsCommandExchange, ShortString.empty)
        print = ch.messaging.consume[String](q.queue, mode = ConsumeMode.RaiseOnError(true)).printlns
        _ <- print.interruptAfter(15.seconds).compile.drain
      yield ()
