import Utils.inCI
import Utils.normally
import Utils.onMac
import Utils.onWindows

plugins {
    application
    alias(libs.plugins.cucumber.runner)
    alias(libs.plugins.allure)
}

dependencies {
    implementation(project(":tracking-actors"))
    implementation(project(":ws"))
    implementation(project(":grpc"))
    implementation(project(":storage"))
    implementation(project(":messages"))
    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":ws")))
}

application {
    mainClass.set("$group.location.entrypoint.Launcher")
}

cucumber {
    val featureFilesDirectory = "cucumber/features"
    featurePath = "src/test/resources/$featureFilesDirectory"
    glue = "$group.location"
    extraGlues = arrayOf(
//        "eu.delimata.stepdefinitions",
    )
    main = "io.cucumber.core.cli.Main"
    plugin = arrayOf(
        "pretty",
//        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
    )
}

tasks.test {
    finalizedBy("cucumber")
}

normally {
    dockerCompose {
        isRequiredBy(tasks.cucumber)
    }
} except { inCI and (onMac or onWindows) } where {
    tasks.test { enabled = false }
} cause "GitHub Actions runner does not support Docker Compose"
