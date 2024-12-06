package io.github.positionpal.location

import io.cucumber.scala.{EN, ScalaDsl}
import io.github.positionpal.location.commons.ScopeFunctions.*
import io.github.positionpal.location.entrypoint.Launcher

object CommonSteps extends ScalaDsl with EN with AcceptanceTests:

  import cats.effect.unsafe.implicits.global

  BeforeAll:
    Launcher.run.unsafeRunAndForget()
    Thread.sleep(5_000) // wait for the server to start

  Given("I am a logged-in user") {}

  And("I'm part of one or more groups") {
    import io.github.positionpal.{AddedMemberToGroup, MessageType}
    messageBrokerConnection.use: conn =>
      conn.channel.use: ch =>
        (impersonatingUser :: roby :: Nil)
          .map(u => serializer.serializeAddedMemberToGroup(AddedMemberToGroup.create(astroGroup.value(), u)))
          .map(e => e.toMessage(Map(msgTypeKey -> MessageType.MEMBER_ADDED.name().asShortOrEmpty)))
          .let(msg => fs2.Stream.emits(msg).evalMap(ch.messaging.publish(groupsEventsExchange, "".asShortOrEmpty, _)))
          .let(_.compile.drain)
    .unsafeRunSync()
  }
