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
    implementation("com.typesafe.akka:akka-serialization-jackson_3:2.10.0")
    testImplementation(libs.akka.actor.testkit)
    testImplementation(libs.akka.persistence.testkit)
}

scalaExtras {
    qa {
        allWarningsAsErrors = false
    }
}
