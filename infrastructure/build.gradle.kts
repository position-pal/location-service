dependencies {
    api(project(":presentation"))
    implementation(libs.bundles.http4s)
    implementation(libs.log4cats.slf4j)
    implementation(libs.akka.cluster.typed)
    implementation(libs.akka.cluster.sharding.typed)
    implementation(libs.akka.persistence.typed)
    implementation(libs.akka.persistence.r2dbc)
    implementation(libs.logback.classic)
    testImplementation(libs.akka.actor.testkit)
    testImplementation(libs.akka.persistence.testkit)
}

fun Task.compose(vararg args: String) {
    doLast {
        exec {
            workingDir = project.rootDir
            commandLine("docker", "compose", *args)
        }
    }
}

tasks.create("composeDown") {
    compose("down")
}

tasks.create("composeUp") {
    compose("up", "-d")
}

tasks.withType<Test> {
    dependsOn(":infrastructure:composeUp")
    finalizedBy(":infrastructure:composeDown")
}
