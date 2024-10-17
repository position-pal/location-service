package io.github.positionpal.location.domain

import java.util.Date

/** A list of ordered [[SampledLocation]]s that can be interpolated
  * forming a path between two geographical positions.
  */
type Route = List[SampledLocation]

/** The tracking of a user. */
trait Tracking:

  /** @return the route followed by the user ordered by the most recent location. */
  def route: Route

  /** @return the user being tracked. */
  def user: UserId

  /** @return a new tracking with the added sample at the beginning of the route. */
  def addSample(sample: SampledLocation): Tracking

  /** An alias for [[addSample]], allowing to use the `+` operator to add a sample. */
  def +(sample: SampledLocation): Tracking = addSample(sample)

/** A [[Tracking]] with additional information to monitor the user's route. */
trait MonitorableTracking extends Tracking:

  /** @return the mode of routing to the destination. */
  def mode: RoutingMode

  /** @return the destination of the route. */
  def destination: GPSLocation

  /** @return the expected arrival time at the destination. */
  def expectedArrival: Date

  /** @return a new [[MonitorableTracking]] with the added sample at the beginning of the route. */
  override def addSample(sample: SampledLocation): MonitorableTracking

  /** An alias for [[addSample]], allowing to use the `+` operator to add a sample. */
  override def +(sample: SampledLocation): MonitorableTracking = addSample(sample)

/** The mode of routing to a destination. */
enum RoutingMode:
  case Driving, Walking, Cycling

object Tracking:

  /** Creates a new [[Tracking]]. */
  def apply(userId: UserId, route: Route = List()): Tracking = TrackingImpl(route, userId)

  /** Creates a new [[MonitorableTracking]]. */
  def withMonitoring(
      userId: UserId,
      routingMode: RoutingMode,
      arrivalLocation: GPSLocation,
      estimatedArrival: Date,
      route: Route = List(),
  ): MonitorableTracking = MonitorableTrackingImpl(route, userId, routingMode, arrivalLocation, estimatedArrival)

  private case class TrackingImpl(override val route: Route, override val user: UserId) extends Tracking:
    override def addSample(sample: SampledLocation): Tracking = TrackingImpl(sample :: route, user)

  private case class MonitorableTrackingImpl(
      override val route: Route,
      override val user: UserId,
      override val mode: RoutingMode,
      override val destination: GPSLocation,
      override val expectedArrival: Date,
  ) extends MonitorableTracking:
    override def addSample(sample: SampledLocation): MonitorableTracking =
      MonitorableTrackingImpl(sample :: route, user, mode, destination, expectedArrival)
