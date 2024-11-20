package io.github.positionpal.location.storage

/** A factory for creating connections to a storage system.
  * @tparam F the effect type
  * @tparam C the connection type
  */
trait ConnectionFactory[F[_], C]:

  /** Creates a new connection to the storage system. */
  def connect: F[C]

/** A factory for creating connections to a Cassandra database. */
object CassandraConnectionFactory:
  import akka.actor.typed.ActorSystem
  import akka.stream.alpakka.cassandra.scaladsl.{CassandraSessionRegistry, CassandraSession}
  import cats.effect.kernel.Async

  /** Creates a new instance of a connection factory for Cassandra leveraging akka persistence plugin.
    * @param actorSystem the actor system to use for the Cassandra session
    * @tparam F the [[Async]] effect type
    * @return a new instance of [[ConnectionFactory]] for Cassandra
    */
  def apply[F[_]: Async](actorSystem: ActorSystem[?]): ConnectionFactory[F, CassandraSession] =
    new ConnectionFactory[F, CassandraSession]:
      def connect: F[CassandraSession] =
        Async[F].delay(CassandraSessionRegistry(actorSystem).sessionFor("akka.persistence.cassandra"))
