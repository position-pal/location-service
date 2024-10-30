dependencies {
    api(project(":presentation"))
    implementation(libs.bundles.http4s)
    implementation(libs.log4cats.slf4j)
    implementation(libs.akka.cluster.typed)
    implementation(libs.akka.cluster.sharding.typed)
    implementation(libs.akka.persistence.typed)
    implementation(libs.akka.persistence.r2dbc)
    implementation(libs.logback.classic)
    implementation(libs.akka.stream)
    implementation(libs.akka.http)
    testImplementation(libs.akka.actor.testkit)
    testImplementation(libs.akka.persistence.testkit)
}

tasks.withType<Test> {
    dependsOn(":infrastructure:composeUp")
    finalizedBy(":infrastructure:composeDown")
}

tasks.create("composeDown") {
    compose("down")
}

tasks.create("composeUp") {
    compose("up", "-d")
}

fun Task.compose(vararg args: String) {
    doLast {
        exec {
            workingDir = project.rootDir
            commandLine("docker", "compose", *args)
        }
    }
}
