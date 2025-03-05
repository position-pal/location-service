package io.github.positionpal.location.storage.sessions

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import io.github.positionpal.location.storage.{StorageUtils, StoreError}
import io.github.positionpal.location.domain
import akka.actor.typed.ActorSystem
import io.github.positionpal.location.domain.Session.Snapshot
import cats.implicits.{catsSyntaxApplicativeId, toFunctorOps, toTraverseOps}
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.application.sessions.UserSessionsStore
import cats.effect.kernel.Async
import io.github.positionpal.location.domain.UserState.*
import io.github.positionpal.location.commons.CanRaise
import com.datastax.oss.driver.api.core.cql.Row

/** A Cassandra-based implementation of the [[UserSessionsStore]]. */
object CassandraUserSessionStore:

  /** Creates a new instance of the Cassandra-based implementation of [[UserSessionsStore]].
    *
    * @param keyspace the keyspace where the tables are stored. Default is "locationservice".
    * @param actorSystem the actor system to use for the Cassandra session
    * @tparam F the effect type
    * @return a new instance of the Cassandra-based implementation of [[UserSessionsStore]]
    */
  def apply[F[_]: {Async, CanRaise[StoreError]}](
      session: F[CassandraSession],
      keyspace: String = "locationservice",
  )(using actorSystem: ActorSystem[?]): F[UserSessionsStore[F, Unit]] = session.map(Impl(_, keyspace))

  final case class InvalidSessionVariation(message: String) extends StoreError(message)

  private class Impl[F[_]: {Async, CanRaise[StoreError]}](using actorSystem: ActorSystem[?])(
      session: CassandraSession,
      keyspace: String,
  ) extends UserSessionsStore[F, Unit]
      with StorageUtils:
    import Queries.*

    given ExecutionContext = actorSystem.executionContext

    override def sessionOf(scope: Scope): F[Option[Session]] = executeWithErrorHandling:
      for
        userInfoRow <- session.selectOne(getUserInfoQuery(scope))
        userInfo <- userInfoRow.traverse(row => (row.userState, row.sampledLocationOf(scope)).pure[Future])
        routes <- session
          .select(getTrackingQuery(scope))
          .runFold(List.empty[(Instant, GPSLocation)])((acc, row) => (row.timestamp, row.location) :: acc)
        tracking = Option.when(routes.nonEmpty)(Tracking(routes.reverse.map(SampledLocation(_, scope, _))))
      yield userInfo.map((state, location) => Session.from(scope, state, location, tracking))

    override def update(variation: Session.Snapshot): F[Unit] = executeWithErrorHandling:
      variation match
        case Snapshot(uid, state @ (Active | Routing | SOS), Some(e)) =>
          for
            _ <- session.executeWrite(insertUserInfoQuery(uid, state, e.position, e.timestamp))
            _ <- state match
              case Active => session.executeWrite(deleteUserRoutesQuery(uid))
              case _ => session.executeWrite(updateUserRoutesQuery(uid, e.position, e.timestamp))
          yield ()
        case Snapshot(uid, state @ (Active | Inactive | Warning | SOS), None) =>
          session.executeWrite(updateUserInfoQuery(uid, state)).void
        case _ => Future.failed(InvalidSessionVariation(s"The given variation $variation is invalid"))

    private object Tables:
      val userInfo = "ScopedUserInfo"
      val userRoutes = "ScopedUserRoutes"

      extension (r: Row)
        def timestamp = r.getInstant("Timestamp")
        def userState = UserState.valueOf(r.getString("Status"))
        def location = GPSLocation(r.getDouble("Latitude"), r.getDouble("Longitude"))
        def sampledLocationOf(scope: Scope) =
          for
            timestamp <- Option(r.getInstant("LastUpdated"))
            location <- r.location.unlessNullIsland
          yield SampledLocation(timestamp, scope, location)

      extension (l: GPSLocation)
        private def unlessNullIsland: Option[GPSLocation] = Option(l).filter(l => l._1 != 0.0 || l._2 != 0.0)

    private object Queries:
      export Tables.*

      def getUserInfoQuery(scope: Scope) = cql(
        s"SELECT Status, Latitude, Longitude, LastUpdated FROM $keyspace.$userInfo WHERE GroupId = ? AND UserId = ?",
        scope.groupId.value(),
        scope.userId.value(),
      )

      def getTrackingQuery(scope: Scope) = cql(
        s"SELECT Timestamp, Latitude, Longitude FROM $keyspace.$userRoutes WHERE GroupId = ? AND UserId = ? ORDER BY Timestamp",
        scope.groupId.value(),
        scope.userId.value(),
      )

      def insertUserInfoQuery(scope: Scope, state: UserState, position: GPSLocation, timestamp: Instant) = cql(
        s"INSERT INTO $keyspace.$userInfo(GroupId, UserId, Status, Latitude, Longitude, LastUpdated) VALUES (?, ?, ?, ?, ?, ?)",
        scope.groupId.value(),
        scope.userId.value(),
        state.toString,
        position.latitude,
        position.longitude,
        timestamp,
      )

      def updateUserInfoQuery(scope: Scope, state: UserState) = cql(
        s"UPDATE $keyspace.$userInfo SET Status = ? WHERE GroupId = ? AND UserId = ?",
        state.toString,
        scope.groupId.value(),
        scope.userId.value(),
      )

      def deleteUserRoutesQuery(scope: Scope) = cql(
        s"DELETE FROM $keyspace.$userRoutes WHERE GroupId = ? AND UserId = ?",
        scope.groupId.value(),
        scope.userId.value(),
      )

      def updateUserRoutesQuery(scope: Scope, position: GPSLocation, timestamp: Instant) = cql(
        s"INSERT INTO $keyspace.$userRoutes(GroupId, UserId, Latitude, Longitude, Timestamp) VALUES (?, ?, ?, ?, ?)",
        scope.groupId.value(),
        scope.userId.value(),
        position.latitude,
        position.longitude,
        timestamp,
      )
