import DotenvUtils.dotenv
import DotenvUtils.injectInto
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import kotlin.reflect.KClass

plugins {
    `java-library`
    id("scala")
    alias(libs.plugins.scala.extras)
}

allprojects {
    group = "io.github.positionpal"

    with(rootProject.libs.plugins) {
        apply(plugin = "java-library")
        apply(plugin = "scala")
        apply(plugin = scala.extras.get().pluginId)
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.akka.io/maven") }
    }

    with(rootProject.libs) {
        dependencies {
            implementation(scala.library)
            implementation(bundles.cats)
            testImplementation(bundles.scala.testing)
        }
    }

    tasks.test {
        useJUnitPlatform {
            includeEngines("scalatest")
        }
        testLogging {
            showCauses = true
            showStackTraces = true
            events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.STARTED, TestLogEvent.STANDARD_OUT)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    injectInto(JavaExec::class, Test::class) environmentsFrom rootProject.dotenv
}
