package io.github.positionpal.location.grpc

import cats.data.{Validated, ValidatedNec}
import cats.implicits.toFoldableOps
import org.scalatest.matchers.should.Matchers
import io.github.positionpal.location.commons.ConfigurationError
import org.scalatest.wordspec.AnyWordSpec
import cats.effect.IO

class GrpcConfigurationTest extends AnyWordSpec with Matchers:

  import cats.effect.unsafe.implicits.global

  "Grpc server configuration" when:
    "created with legit options" should:
      "be valid" in:
        val configuration = GrpcServer.Configuration[IO](50051)
        configuration.unsafeRunSync().isValid shouldBe true

    "created with illegal options" should:
      "be invalid" in:
        val configuration = GrpcServer.Configuration[IO](-1)
        val result: ValidatedNec[ConfigurationError, GrpcServer.Configuration] = configuration.unsafeRunSync()
        result.isInvalid shouldBe true
        result match
          case Validated.Invalid(e) => e.toList.collect { case e: ConfigurationError.Invalid => e }.size shouldBe 1
          case _ => fail("Expected invalid configuration")

    "created by non-existing environment variables" should:
      "be invalid" in:
        val configuration = GrpcServer.Configuration.fromEnv[IO]
        val result: ValidatedNec[ConfigurationError, GrpcServer.Configuration] = configuration.unsafeRunSync()
        result.isInvalid shouldBe true
        result match
          case Validated.Invalid(e) =>
            e.toList.collect { case e: ConfigurationError.NotSet => e }.size shouldBe 1
          case _ => fail("Expected invalid configuration")
