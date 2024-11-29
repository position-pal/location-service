package io.github.positionpal.location.commons

/**
 * A sealed trait representing the possible configuration errors.
 * @param message the error message.
 */
enum ConfigurationError(val message: String):
  case NotSet(name: String) extends ConfigurationError(s"`$name` is not set!")
  case Invalid(reason: String) extends ConfigurationError(s"Invalid configuration: $reason")

/** A set of extension methods to validate configuration parameters. */
object ConfigurationError:
  export ConfigurationError.*
  import cats.data.ValidatedNec
  import cats.implicits.catsSyntaxValidatedIdBinCompat0

  extension (s: Option[String])
    def validStr(name: String): ValidatedNec[ConfigurationError, String] =
      if s.nonEmpty then s.get.nonEmpty else NotSet(name).invalidNec

  extension (s: String)
    def nonEmpty: ValidatedNec[ConfigurationError, String] =
      if s.trim.nonEmpty then s.validNec else Invalid("Empty string not allowed!").invalidNec

  extension (n: Option[Int])
    def validateNum(name: String): ValidatedNec[ConfigurationError, Int] =
      if n.nonEmpty then n.get.positive else NotSet(name).invalidNec

  extension (n: Int)
    def positive: ValidatedNec[ConfigurationError, Int] =
      if n > 0 then n.validNec else Invalid("Parameter must be positive").invalidNec
