package io.github.positionpal.location.entrypoint

import org.scalatest.flatspec.AnyFlatSpec
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.library.Architectures.onionArchitecture

class ArchitecturalTest extends AnyFlatSpec:

  "Project-wise architecture" should "adhere to ports and adapters, a.k.a onion architecture" in:
    val locationGroup = "io.github.positionpal.location"
    val code = ClassFileImporter().importPackages(locationGroup)
    onionArchitecture()
      .domainModels(s"$locationGroup.commons..", s"$locationGroup.domain..")
      .applicationServices(s"$locationGroup.application..", s"$locationGroup.presentation..")
      .adapter("real time tracker component", s"$locationGroup.tracking..")
      .adapter("storage", s"$locationGroup.storage..")
      .adapter("message broker", s"$locationGroup.messages..")
      .adapter("gRPC API", s"$locationGroup.grpc..")
      .adapter("web sockets and http web API", s"$locationGroup.ws..")
      .ignoreDependency(havingEntrypointAsOrigin, andAnyTarget)
      .because("`Entrypoint` submodule contains the main method wiring all the adapters together.")
      .ensureAllClassesAreContainedInArchitectureIgnoring(havingEntrypointAsOrigin)
      .withOptionalLayers(true)
      .check(code)

  private def havingEntrypointAsOrigin =
    DescribedPredicate.describe[JavaClass]("in `entrypoint` package", _.getPackage.getName.contains("entrypoint"))

  private def andAnyTarget = DescribedPredicate.alwaysTrue()
