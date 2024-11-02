package io.github.positionpal.location.domain

trait Session:
  def userState: UserState
  def lastSampledLocation: Option[SampledLocation]
  def tracking: Option[Tracking | MonitorableTracking]
  def updateWith(event: DrivingEvent): Session

object Session:

  import io.github.positionpal.location.domain.UserState.*
  import io.github.positionpal.location.domain.EventConversions.*
  import io.github.positionpal.location.domain.EventConversions.given

  def apply(): Session = SessionImpl(Inactive, None, None)

  private case class SessionImpl(
      override val userState: UserState,
      override val lastSampledLocation: Option[SampledLocation],
      override val tracking: Option[Tracking | MonitorableTracking],
  ) extends Session:
    override def updateWith(event: DrivingEvent): Session = event match
      case e: SampledLocation =>
        userState match
          case Routing | SOS => copy(tracking = tracking.map(_ + e), lastSampledLocation = Some(e))
          case _ => copy(userState = Active, tracking = tracking.map(_ + e), lastSampledLocation = Some(e))
      case e: RoutingStarted => copy(userState = Routing, tracking = Some(e.toMonitorableTracking))
      case e: SOSAlertTriggered => copy(userState = SOS, tracking = Some(e.toTracking), lastSampledLocation = Some(e))
      case _: (SOSAlertStopped | RoutingStopped) => copy(userState = Active, tracking = None)
      case _: WentOffline => copy(userState = Inactive)
