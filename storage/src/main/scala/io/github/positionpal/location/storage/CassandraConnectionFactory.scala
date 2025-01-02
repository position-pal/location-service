package io.github.positionpal.location.storage

import io.github.positionpal.location.commons.ConnectionFactory

/** A factory for creating connections to a Cassandra database. */
object CassandraConnectionFactory:
  import akka.actor.typed.ActorSystem
  import akka.stream.alpakka.cassandra.scaladsl.{CassandraSession, CassandraSessionRegistry}
  import cats.effect.kernel.Async

  /** Creates a new instance of a connection factory for Cassandra leveraging akka persistence plugin.
    * @param actorSystem the actor system to use for the Cassandra session
    * @tparam F the [[Async]] effect type
    * @return a new instance of [[ConnectionFactory]] for Cassandra
    */
  def apply[F[_]: Async](actorSystem: ActorSystem[?]): ConnectionFactory[F, CassandraSession] =
    new ConnectionFactory[F, CassandraSession]:
      def get: F[CassandraSession] =
        Async[F].delay(CassandraSessionRegistry(actorSystem).sessionFor("akka.persistence.cassandra"))
