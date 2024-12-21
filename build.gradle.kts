import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("scala")
    alias(libs.plugins.scala.extras)
    alias(libs.plugins.git.sensitive.semantic.versioning)
    alias(libs.plugins.shadowJar)
}

allprojects {
    group = "io.github.positionpal"

    with(rootProject.libs.plugins) {
        apply(plugin = "scala")
        apply(plugin = scala.extras.get().pluginId)
        apply(plugin = shadowJar.get().pluginId)
    }

    repositories {
        mavenCentral()
    }

    with(rootProject.libs) {
        dependencies {
            implementation(scala.library)
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
            events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.STARTED)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}

/* Set the project version based on the git history. */
gitSemVer {
    assignGitSemanticVersion()
}
