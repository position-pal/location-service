package io.github.positionpal.location.infrastructure.geo

import cats.effect.IO
import io.github.positionpal.location.commons.{ConfigurationProvider, EnvVariablesProvider}
import io.github.positionpal.location.infrastructure.utils.HTTPUtils

class MapboxConfigurationProvider(fileName: String) extends ConfigurationProvider[IO, MapboxService.Configuration]:
  override def configuration: IO[MapboxService.Configuration] =
    for
      envs <- EnvVariablesProvider[IO].configuration
      clientRes = HTTPUtils.clientRes
      config <- clientRes.use(client => IO.pure(MapboxService.Configuration(client, envs(fileName))))
    yield config
