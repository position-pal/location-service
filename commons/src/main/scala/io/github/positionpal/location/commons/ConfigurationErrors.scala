package io.github.positionpal.location.commons

enum ConfigurationError(val message: String):
  case Invalid(reason: String) extends ConfigurationError(s"`Invalid configuration: $reason")
  case NotSet(name: String) extends ConfigurationError(s"`$name` is not set!")

object ConfigurationError:
  export ConfigurationError.*
  import cats.data.ValidatedNec
  import cats.implicits.catsSyntaxValidatedIdBinCompat0

  extension (s: Option[String])
    def validate(name: String): ValidatedNec[ConfigurationError, String] =
      if s.nonEmpty then s.get.validate else NotSet(name).invalidNec

  extension (s: String)
    def validate: ValidatedNec[ConfigurationError, String] =
      if s.trim.nonEmpty then s.validNec else Invalid("Empty string not allowed!").invalidNec

  extension (n: Int)
    def validate: ValidatedNec[ConfigurationError, Int] =
      if n > 0 then n.validNec else Invalid("$n must be positive").invalidNec
