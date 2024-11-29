import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.remove
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    alias(libs.plugins.protobuf)
    alias(libs.plugins.scala.extras)
}

dependencies {
    api(project(":application"))
    with(libs) {
        api(bundles.circe)
        api(bundles.borer)
        api(positionpal.kernel.presentation)
        api(akka.actor)
        api(fs2.grpc.runtime)
        api(grpc.core)
        api(grpc.stub)
        api(grpc.protobuf)
        api(protoc.gen.fs2)
        api(scalapb.runtime)
        api(scalapb.runtime.grpc)
    }
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        val extension = if (Os.isFamily(Os.FAMILY_WINDOWS)) "${Os.FAMILY_WINDOWS}@bat" else "${Os.FAMILY_UNIX}@sh"
        id("scalapb") {
            artifact = "${libs.scalapb.protoc.gen.get()}:$extension"
        }
        id("fs2grpc") {
            artifact = "${libs.protoc.gen.fs2.get()}:$extension"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                remove("java")
            }
            it.plugins {
                id("scalapb") {
                    option("grpc")
                    option("flat_package")
                    option("scala3_sources")
                }
                id("fs2grpc") {
                    option("flat_package")
                }
            }
        }
    }
}

sourceSets {
    main {
        scala {
            srcDirs("${protobuf.generatedFilesBaseDir}/main/scalapb")
            srcDirs("${protobuf.generatedFilesBaseDir}/main/fs2grpc")
        }
    }
}

scalafix {
    excludes = setOf("**/proto/**")
}
