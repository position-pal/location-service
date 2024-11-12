dependencies {
    api(project(":application"))
    implementation(libs.akka.cluster.typed)
    implementation(libs.akka.persistence.cassandra)
    implementation(libs.akka.persistence.typed)
    implementation(libs.akka.persistence.query)
    implementation(libs.log4cats.slf4j)
    implementation(libs.logback.classic)
    testImplementation(libs.akka.actor.testkit)
}

dockerCompose.isRequiredBy(tasks.test)
