package io.github.positionpal.location

import io.github.positionpal.location.messages.RabbitMQ
import io.github.positionpal.location.presentation.ModelCodecs
import io.github.positionpal.location.ws.WebSocketTestDSL

trait AcceptanceTests extends RabbitMQ.Utils with WebSocketTestDSL with ModelCodecs:

  export Entities.*
  export RabbitMQUtils.*
  export WebSocketsUtils.*

  object Entities:
    import io.github.positionpal.User
    import io.github.positionpal.entities.{UserId, GroupId}

    val impersonatingUser: User = User.create("luke", "Luke", "Skywalker", "luke@gmail.com", "Admin")
    val impersonatingUserId: UserId = UserId.create(impersonatingUser.id())
    val roby: User = User.create("roby", "Roby", "Perez", "robyzerep@gmail.com", "User")
    val robyId: UserId = UserId.create(roby.id())
    val alexa: User = User.create("alexa", "Alexa", "Gonzalez", "gonzalexalexa@gmail.com", "User")
    val alexaId: UserId = UserId.create(alexa.id())
    val astroGroup: GroupId = GroupId.create("astro")
    val squirrelGroup: GroupId = GroupId.create("squirrel")
  end Entities

  val serializer = io.github.positionpal.AvroSerializer()

  object RabbitMQUtils:
    import cats.data.Validated.*
    import cats.effect.{IO, Resource}
    import io.github.positionpal.location.messages.MessageBrokerConnectionFactory
    import lepus.client.Connection

    val configuration = RabbitMQ.Configuration[IO]("localhost", 5672, "guest", "admin", "/")
    val messageBrokerConnection: Resource[IO, Connection[IO]] = Resource.eval(configuration).flatMap:
      case Valid(config) => MessageBrokerConnectionFactory.ofRabbitMQ[IO](config)
      case Invalid(errors) => Resource.eval(IO.raiseError(new Exception(errors.toNonEmptyList.toList.mkString(", "))))
  end RabbitMQUtils

  object WebSocketsUtils:
    import scala.concurrent.duration.DurationInt

    val config = WebSocketTestConfig(baseEndpoint = "ws://localhost:8080/group", connectionTimeout = 5.seconds)
    val wsConfigurator = WebSocketTest(config)
  end WebSocketsUtils
end AcceptanceTests
