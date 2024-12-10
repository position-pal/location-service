package io.github.positionpal.location.application.tracking

import scala.concurrent.duration.FiniteDuration

import io.github.positionpal.location.domain.{Distance, GPSLocation, RoutingMode}

/** A service to interact with maps and geolocation services.
  * @tparam F the effect constructor type.
  */
trait MapsService[F[_]]:

  /**
   * Estimates the duration of a route between the [[origin]] and the [[destination]] using the given [[RoutingMode]].
   * @param mode the [[RoutingMode]] being used to move from the [[origin]] to the [[destination]].
   * @param origin the origin position of the route.
   * @param destination the destination position of the route.
   * @return the estimated duration of the route.
   */
  def duration(mode: RoutingMode)(origin: GPSLocation, destination: GPSLocation): F[FiniteDuration]

  /**
   * Estimates the distance between the [[origin]] and the [[destination]] using the given [[RoutingMode]].
   * @param mode the [[RoutingMode]] being used to move from the [[origin]] to the [[destination]].
   * @param origin the origin position of the route.
   * @param destination the destination position of the route.
   * @return the estimated distance.
   */
  def distance(mode: RoutingMode)(origin: GPSLocation, destination: GPSLocation): F[Distance]
