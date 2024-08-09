package io.github.positionpal.location.commons

import cats.effect.IO

trait ConfigurationProvider[C]:
  def configuration: IO[C]

object EnvVariablesConfigurationProvider extends ConfigurationProvider[Map[String, String]]:
  def configuration: IO[Map[String, String]] = ???
