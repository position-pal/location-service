package io.github.positionpal.location.application.reactions

import io.github.positionpal.entities.NotificationMessage
import io.github.positionpal.location.domain.{MonitorableTracking, SampledLocation}

/** A reaction to [[SampledLocation]] events. */
object TrackingEventReaction extends BinaryShortCircuitReaction with FilterableOps:
  case object Continue
  enum Notification(val message: NotificationMessage):
    case Alert(override val message: NotificationMessage) extends Notification(message)
    case Success(override val message: NotificationMessage) extends Notification(message)

  override type Environment = MonitorableTracking
  override type Event = SampledLocation
  override type LeftOutcome = Notification
  override type RightOutcome = Continue.type

import cats.Monad
import cats.effect.Sync
import cats.implicits.{toFlatMapOps, toFunctorOps}
import TrackingEventReaction.*
import Notification.*

/** A [[TrackingEventReaction]] checking if the position curried by the event is near the arrival position. */
object ArrivalCheck:
  import io.github.positionpal.location.domain.Distance.*
  import io.github.positionpal.location.application.tracking.MapsService

  def apply[M[_]: Sync](mapsService: MapsService[M]): EventReaction[M] =
    on[M]: (monitoredTracking, event) =>
      for
        config <- ReactionsConfiguration.get
        distance <- mapsService.distance(monitoredTracking.mode)(event.position, monitoredTracking.destination)
        outcome =
          if distance.toMeters.value <= config.proximityToleranceMeters.meters.value
          then Left(Success(successMessage(event.user.username())))
          else Right(Continue)
      yield outcome

  private def successMessage(username: String) = NotificationMessage
    .create(s"$username arrived!", s"$username has reached their destination on time.")

/** A [[TrackingEventReaction]] checking if the position curried by the event is continually in the same location. */
object StationaryCheck:

  def apply[M[_]: Sync](): EventReaction[M] =
    on[M]: (monitoredTracking, event) =>
      for
        config <- ReactionsConfiguration.get
        samples = monitoredTracking.route.take(config.stationarySamples)
        result <-
          if samples.size >= config.stationarySamples && samples.forall(_.position == event.position)
          then Monad[M].pure(Left(Alert(alertMessage(event.user.username()))))
          else Monad[M].pure(Right(Continue))
      yield result

  private def alertMessage(username: String) = NotificationMessage
    .create(s"$username() stationary alert!", s"$username has been stuck in the same position for a while.")

/** A [[TrackingEventReaction]] checking if the expected arrival time has expired. */
object ArrivalTimeoutCheck:

  def apply[M[_]: Monad](): EventReaction[M] = on[M]: (monitoredTracking, event) =>
    if event.timestamp.isAfter(monitoredTracking.expectedArrival)
    then Monad[M].pure(Left(Alert(alertMessage(event.user.username()))))
    else Monad[M].pure(Right(Continue))

  private def alertMessage(username: String) = NotificationMessage
    .create(s"$username delay alert!", s"$username has not reached their destination as expected, yet.")
