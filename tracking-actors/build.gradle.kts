dependencies {
    api(project(":presentation"))
    with(libs) {
        implementation(bundles.http4s)
        api(akka.cluster.typed)
        api(akka.cluster.sharding.typed)
        implementation(akka.persistence.typed)
        implementation(akka.persistence.cassandra)
        implementation(akka.persistence.query)
        implementation(akka.stream.typed)
        implementation(akka.http)
        implementation(akka.projection.eventsourced)
        implementation(akka.projection.cassandra)
        testImplementation(akka.projection.testkit)
        testImplementation(akka.stream.testkit)
        testImplementation(akka.actor.testkit)
        testImplementation(akka.persistence.testkit)
        testImplementation(testFixtures(project(":domain")))
        testImplementation(libs.scalamock.cats)
    }
}
