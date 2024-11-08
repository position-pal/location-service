package io.github.positionpal.location.domain

import java.time.Instant

/** A list of ordered [[SampledLocation]]s that can be interpolated
  * forming a path between two geographical positions.
  */
type Route = List[SampledLocation]

/** The tracking of a user. */
trait Tracking:

  /** @return the route followed by the user ordered by the most recent location. */
  def route: Route

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
  def expectedArrival: Instant

  /** @return a new [[MonitorableTracking]] with the added sample at the beginning of the route. */
  override def addSample(sample: SampledLocation): MonitorableTracking

  /** An alias for [[addSample]], allowing to use the `+` operator to add a sample. */
  override def +(sample: SampledLocation): MonitorableTracking = addSample(sample)

/** The mode of routing to a destination. */
enum RoutingMode:
  case Driving, Walking, Cycling

object Tracking:

  extension (t: Tracking | MonitorableTracking)
    def isMonitorable: Boolean = t match
      case _: MonitorableTracking => true
      case _ => false

  /** Creates a new [[Tracking]]. */
  def apply(route: Route = List()): Tracking = TrackingImpl(route)

  /** Creates a new [[MonitorableTracking]]. */
  def withMonitoring(
      routingMode: RoutingMode,
      arrivalLocation: GPSLocation,
      estimatedArrival: Instant,
      route: Route = List(),
  ): MonitorableTracking = MonitorableTrackingImpl(route, routingMode, arrivalLocation, estimatedArrival)

  private case class TrackingImpl(override val route: Route) extends Tracking:
    override def addSample(sample: SampledLocation): Tracking = TrackingImpl(sample :: route)

  private case class MonitorableTrackingImpl(
      override val route: Route,
      override val mode: RoutingMode,
      override val destination: GPSLocation,
      override val expectedArrival: Instant,
  ) extends MonitorableTracking:
    override def addSample(sample: SampledLocation): MonitorableTracking =
      MonitorableTrackingImpl(sample :: route, mode, destination, expectedArrival)
