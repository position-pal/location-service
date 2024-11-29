package io.github.positionpal.location.storage.sessions

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.typed.ActorSystem
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps, toTraverseOps}
import com.datastax.oss.driver.api.core.cql.Row
import io.github.positionpal.entities.UserId
import io.github.positionpal.location.application.sessions.UserSessionStore
import io.github.positionpal.location.commons.CanRaise
import io.github.positionpal.location.domain
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.Session.Snapshot
import io.github.positionpal.location.domain.UserState.*
import io.github.positionpal.location.storage.{StorageUtils, StoreError}

/** A Cassandra-based implementation of the [[UserSessionStore]]. */
object CassandraUserSessionStore:

  /** Creates a new instance of the Cassandra-based implementation of [[UserSessionStore]].
    * @param keyspace the keyspace where the tables are stored. Default is "locationservice".
    * @param actorSystem the actor system to use for the Cassandra session
    * @tparam F the effect type
    * @return a new instance of the Cassandra-based implementation of [[UserSessionStore]]
    */
  def apply[F[_]: Async: CanRaise[StoreError]](
      session: F[CassandraSession],
      keyspace: String = "locationservice",
  )(using actorSystem: ActorSystem[?]): F[UserSessionStore[F, Unit]] = session.map(Impl(_, keyspace))

  final case class InvalidSessionVariation(message: String) extends StoreError(message)

  private class Impl[F[_]: Async: CanRaise[StoreError]](using actorSystem: ActorSystem[?])(
      session: CassandraSession,
      keyspace: String,
  ) extends UserSessionStore[F, Unit]
      with StorageUtils:
    import Queries.*

    given ExecutionContext = actorSystem.executionContext

    override def sessionOf(userId: UserId): F[Option[Session]] = executeWithErrorHandling:
      for
        userInfoRow <- session.selectOne(getUserInfoQuery(userId))
        userInfo <- userInfoRow.traverse(row => (row.userState, row.sampledLocationOf(userId)).pure[Future])
        routes <- session.select(getTrackingQuery(userId))
          .runFold(List.empty[(Instant, GPSLocation)])((acc, row) => (row.timestamp, row.location) :: acc)
        tracking = Option.when(routes.nonEmpty)(Tracking(routes.reverse.map(SampledLocation(_, userId, _))))
      yield userInfo.map((state, location) => Session.from(userId, state, location, tracking))

    override def update(variation: Session.Snapshot): F[Unit] = executeWithErrorHandling:
      variation match
        case Snapshot(uid, state @ (Active | Routing | SOS), Some(e)) =>
          for
            _ <- session.executeWrite(insertUserInfoQuery(uid, state, e.position, e.timestamp))
            _ <- state match
              case Active => session.executeWrite(deleteUserRoutesQuery(uid))
              case _ => session.executeWrite(updateUserRoutesQuery(uid, e.position, e.timestamp))
          yield ()
        case Snapshot(uid, Inactive, None) => session.executeWrite(updateUserInfoQuery(uid, Inactive)).map(_ => ())
        case _ => Future.failed(InvalidSessionVariation(s"The given variation $variation is invalid"))

    private object Tables:
      val userInfo = "UserInfo"
      val userRoutes = "UserRoutes"

      extension (r: Row)
        def timestamp = r.getInstant("Timestamp")
        def userState = UserState.valueOf(r.getString("Status"))
        def location = GPSLocation(r.getDouble("Latitude"), r.getDouble("Longitude"))
        def sampledLocationOf(userId: UserId) =
          for
            timestamp <- Option(r.getInstant("LastUpdated"))
            location <- r.location.unlessNullIsland
          yield SampledLocation(timestamp, userId, location)

      extension (l: GPSLocation)
        private def unlessNullIsland: Option[GPSLocation] = Option(l).filter(l => l._1 != 0.0 || l._2 != 0.0)

    private object Queries:
      export Tables.*

      def getUserInfoQuery(userId: UserId) = cql(
        s"SELECT Status, Latitude, Longitude, LastUpdated FROM $keyspace.$userInfo WHERE UserId = ?",
        userId.username(),
      )

      def getTrackingQuery(userId: UserId) = cql(
        s"SELECT Timestamp, Latitude, Longitude FROM $keyspace.$userRoutes WHERE UserId = ? ORDER BY Timestamp",
        userId.username(),
      )

      def insertUserInfoQuery(userId: UserId, state: UserState, position: GPSLocation, timestamp: Instant) = cql(
        s"INSERT INTO $keyspace.$userInfo(UserId, Status, Latitude, Longitude, LastUpdated) VALUES (?, ?, ?, ?, ?)",
        userId.username(),
        state.toString,
        position.latitude,
        position.longitude,
        timestamp,
      )

      def updateUserInfoQuery(userId: UserId, state: UserState) =
        cql(s"UPDATE $keyspace.$userInfo SET Status = ? WHERE UserId = ?", state.toString, userId.username())

      def deleteUserRoutesQuery(userId: UserId) =
        cql(s"DELETE FROM $keyspace.$userRoutes WHERE UserId = ?", userId.username())

      def updateUserRoutesQuery(userId: UserId, position: GPSLocation, timestamp: Instant) = cql(
        s"INSERT INTO $keyspace.$userRoutes(UserId, Latitude, Longitude, Timestamp) VALUES (?, ?, ?, ?)",
        userId.username(),
        position.latitude,
        position.longitude,
        timestamp,
      )
