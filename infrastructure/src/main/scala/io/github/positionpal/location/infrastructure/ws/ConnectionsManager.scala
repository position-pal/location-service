package io.github.positionpal.location.infrastructure.ws

trait ConnectionsManager:
  type Key
  type Value

  def activeConnections(key: Key): Value
  def add(key: Key, value: Value): Unit
  def remove(key: Key, value: Value): Unit

object ConnectionsManager:

  def apply(): ConnectionsManager = ConnectionsManagerImpl()

  private class ConnectionsManagerImpl extends ConnectionsManager:
    import io.github.positionpal.location.domain.{GroupId, UserId}
    import akka.actor.typed.ActorRef

    override type Key = GroupId
    override type Value = Set[(UserId, ActorRef[WebSockets.Protocol])]

    private val connections = scala.collection.mutable.Map[GroupId, Set[(UserId, ActorRef[WebSockets.Protocol])]]()

    override def activeConnections(key: Key): Value = connections.getOrElse(key, Set.empty)

    override def remove(key: Key, value: Value): Unit =
      connections.updateWith(key)(_.map(_.filterNot(_ == value)))

    override def add(key: Key, value: Value): Unit =
      connections.updateWith(key)(existing => Some(existing.getOrElse(Set.empty) ++ value))
