package io.github.positionpal.location.infrastructure.geo

import cats.data.EitherT
import cats.effect.IO
import io.github.positionpal.location.application.geo.{MapsService, MapsServiceError}
import io.github.positionpal.location.domain.{Address, GPSLocation, Latitude, Longitude}

type Response[E] = EitherT[IO, MapsServiceError, E]

/** A [[MapService]] adapter interacting with the Mapbox service. */
class MapboxServiceAdapter extends MapsService[Response]:
  override def addressOf(latitude: Latitude, longitude: Longitude): Response[Address] = ???
  override def locationOf(address: Address): Response[GPSLocation] = ???
