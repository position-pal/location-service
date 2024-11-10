package io.github.positionpal.location.storage

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.typed.ActorSystem
import akka.stream.alpakka.cassandra.scaladsl.{CassandraSession, CassandraSessionRegistry}
import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxMonadError, toFunctorOps, toTraverseOps}
import com.datastax.oss.driver.api.core.cql.{Row, SimpleStatement}
import io.github.positionpal.location.application.storage.UserSessionStore
import io.github.positionpal.location.commons.CanRaise
import io.github.positionpal.location.domain
import io.github.positionpal.location.domain.*
import io.github.positionpal.location.domain.Session.Snapshot
import io.github.positionpal.location.domain.UserState.*

object CassandraUserSessionStore:

  def apply[F[_]: Async: CanRaise[StoreError]](
      keyspace: String,
  )(using actorSystem: ActorSystem[?]): F[UserSessionStore[F, Unit]] =
    Async[F].delay(CassandraSessionRegistry(actorSystem).sessionFor("akka.persistence.cassandra"))
      .map(session => Impl(session, keyspace))

  sealed trait StoreError extends RuntimeException
  final case class InvalidSessionVariation(message: String) extends StoreError
  final case class DatabaseError(message: String) extends StoreError

  private class Impl[F[_]: Async: CanRaise[StoreError]](using actorSystem: ActorSystem[?])(
      session: CassandraSession,
      keyspace: String,
  ) extends UserSessionStore[F, Unit]:
    import Queries.*
    import Tables.*

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

    private def executeWithErrorHandling[T](operation: => Future[T]): F[T] =
      Async[F].fromFuture(Async[F].delay(operation)).adaptError { case e: Exception => DatabaseError(e.getMessage) }

    extension (l: GPSLocation)
      private def unlessNullIsland: Option[GPSLocation] = Option(l).filter(l => l._1 != 0.0 || l._2 != 0.0)

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

    private object Queries:
      import Tables.*

      def getUserInfoQuery(userId: UserId) = SimpleStatement.newInstance(
        s"SELECT Status, Latitude, Longitude, LastUpdated FROM $keyspace.$userInfo WHERE UserId = ?",
        userId.id,
      )

      def getTrackingQuery(userId: UserId) = SimpleStatement.newInstance(
        s"SELECT Timestamp, Latitude, Longitude FROM $keyspace.$userRoutes WHERE UserId = ? ORDER BY Timestamp",
        userId.id,
      )

      def insertUserInfoQuery(userId: UserId, state: UserState, position: GPSLocation, timestamp: Instant) =
        SimpleStatement.newInstance(
          s"INSERT INTO $keyspace.$userInfo(UserId, Status, Latitude, Longitude, LastUpdated) VALUES (?, ?, ?, ?, ?)",
          userId.id,
          state.toString,
          position.latitude,
          position.longitude,
          timestamp,
        )

      def updateUserInfoQuery(userId: UserId, state: UserState) = SimpleStatement
        .newInstance(s"UPDATE $keyspace.$userInfo SET Status = ? WHERE UserId = ?", state.toString, userId.id)

      def deleteUserRoutesQuery(userId: UserId) =
        SimpleStatement.newInstance(s"DELETE FROM $keyspace.$userRoutes WHERE UserId = ?", userId.id)

      def updateUserRoutesQuery(userId: UserId, position: GPSLocation, timestamp: Instant) =
        SimpleStatement.newInstance(
          s"INSERT INTO $keyspace.$userRoutes(UserId, Latitude, Longitude, Timestamp) VALUES (?, ?, ?, ?)",
          userId.id,
          position.latitude,
          position.longitude,
          timestamp,
        )
