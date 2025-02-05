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
  infix def +(sample: SampledLocation): Tracking = addSample(sample)

/** The possible alerts that can be triggered during a [[MonitorableTracking]] session
  * while monitoring the user's route to a destination.
  */
enum Alert:
  /** Indicates that the user appears to be stuck on their current route,
    * possibly indicating a dangerous or unexpected situation.
    */
  case Stuck

  /** Indicates that the user is late and didn't reach the destination at
    * the expected time, possibly indicating a delay or a dangerous situation.
    */
  case Late

  /** Indicates that the user went offline on their current route, possibly
    * indicating a dangerous or unexpected situation.
    */
  case Offline

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
  infix override def +(sample: SampledLocation): MonitorableTracking = addSample(sample)

  /** @return a new [[MonitorableTracking]] with the added [[alert]]. */
  def addAlert(alert: Alert): MonitorableTracking

  /** @return a new [[MonitorableTracking]] without the removed [[alert]]. */
  def removeAlert(alert: Alert): MonitorableTracking

  /** @return `true` if the tracking has the given [[alert]], `false` otherwise. */
  infix def has(alert: Alert): Boolean

/** The mode of routing to a destination. */
enum RoutingMode:
  case Driving, Walking, Cycling

object Tracking:

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
      private val alerts: Set[Alert] = Set(),
  ) extends MonitorableTracking:
    override def addSample(sample: SampledLocation): MonitorableTracking = copy(route = sample :: route)
    override def removeAlert(alert: Alert): MonitorableTracking = copy(alerts = alerts - alert)
    override def addAlert(alert: Alert): MonitorableTracking = copy(alerts = alerts + alert)
    override def has(alert: Alert): Boolean = alerts.contains(alert)

  extension (t: Tracking)
    /** @return `true` if the tracking is monitorable (i.e., is an instance
      *         of [[MonitorableTracking]]), `false` otherwise.
      */
    def isMonitorable: Boolean = t match
      case _: MonitorableTracking => true
      case _ => false

    /** @return a [[Some]] with the [[MonitorableTracking]] instance if the tracking is
      *         monitorable (i.e., is an instance of [[MonitorableTracking]]), [[None]] otherwise.
      */
    def asMonitorable: Option[MonitorableTracking] = t match
      case m: MonitorableTracking => Some(m)
      case _ => None

  extension (t: Option[Tracking])
    /** @return a [[Some]] with the [[MonitorableTracking]] instance if the `Option` is non-empty and
      *         the tracking is monitorable (i.e., is an instance of [[MonitorableTracking]]), [[None]] otherwise.
      */
    def asMonitorable: Option[MonitorableTracking] = t.flatMap(_.asMonitorable)
