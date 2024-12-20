package io.github.positionpal.location.application.tracking.reactions

/** [[TrackingEventReaction]]s' configuration.
  * @param proximityToleranceMeters the distance, expressed in meters, to consider two
  *   positions approximately in the same location
  * @param stationarySamples the number of samples to consider a user stuck in the same location
  * @param offlineThresholdSeconds the number of seconds after which, without any location update,
  *   a user is considered offline.
  */
private case class ReactionsConfiguration(
    proximityToleranceMeters: Double,
    stationarySamples: Int,
    offlineThresholdSeconds: Int,
)

object ReactionsConfiguration:
  import cats.effect.Sync
  import pureconfig.ConfigReader
  import io.github.positionpal.location.commons.ConfigProvider

  private val namespace = "reactions"

  implicit val reader: ConfigReader[ReactionsConfiguration] =
    ConfigReader.forProduct3(
      "proximityToleranceMeters",
      "stationarySamples",
      "offlineThresholdSeconds",
    )(ReactionsConfiguration.apply)

  def get[M[_]: Sync]: M[ReactionsConfiguration] =
    ConfigProvider[M, ReactionsConfiguration](namespace = namespace).configuration
