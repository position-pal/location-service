package io.github.positionpal.location.ws.handlers.v1

import scala.collection.concurrent.TrieMap

import akka.actor.typed.ActorRef
import io.github.positionpal.location.domain.DrivenEvent
import io.github.positionpal.entities.{GroupId, UserId}

/** A thread-safe web sockets connections manager that is responsible for
  * keeping track of the active connections for each group.
  */
object ConnectionsManager:

  /** A tuple of the user id and the reference to the actor in charge of managing the web socket connection. */
  type Connection = (UserId, ActorRef[DrivenEvent])

  private val activeConnections = TrieMap[GroupId, Set[Connection]]()

  /** @return the active connections for the given [[groupId]]. */
  def activeConnectionsFor(groupId: GroupId): Set[Connection] =
    synchronized:
      activeConnections.getOrElse(groupId, Set.empty)

  /** Adds a new connection to the givne [[groupId]] performed by the given [[userId]] (managed by the [[actorRef]]).
    * @return the old active connections for the given [[groupId]].
    */
  def addConnection(groupId: GroupId, userId: UserId, actorRef: ActorRef[DrivenEvent]): Set[Connection] =
    synchronized:
      val connections = activeConnectionsFor(groupId)
      activeConnections.update(groupId, connections + ((userId, actorRef)))
      connections

  /** Removes the connection for the given [[userId]] in the given [[groupId]].
    * @return the old active connections for the given [[groupId]].
    */
  def removeConnection(groupId: GroupId, userId: UserId): Set[Connection] =
    synchronized:
      val connections = activeConnectionsFor(groupId)
      activeConnections.updateWith(groupId)(_.map(_.filterNot(_._1 == userId)))
      connections
