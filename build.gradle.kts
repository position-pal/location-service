import DotenvUtils.dotenv
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

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

    tasks.withType<JavaExec> {
        rootProject.dotenv().environmentVariables().forEach { environment(it.key, it.value) }
    }

    tasks.withType<Test> {
        rootProject.dotenv().environmentVariables().forEach { environment(it.key, it.value) }
    }
}
