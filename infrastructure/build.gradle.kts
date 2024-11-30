import Utils.inCI
import Utils.normally
import Utils.onMac
import Utils.onWindows

dependencies {
    api(project(":presentation"))
    with(libs) {
        implementation(bundles.http4s)
        implementation(akka.cluster.typed)
        implementation(akka.cluster.sharding.typed)
        implementation(akka.persistence.typed)
        implementation(akka.persistence.cassandra)
        implementation(akka.persistence.query)
        implementation(akka.stream)
        implementation(akka.http)
        implementation(akka.projection.eventsourced)
        implementation(akka.projection.cassandra)
        testImplementation(akka.projection.testkit)
        testImplementation(akka.stream.testkit)
        testImplementation(akka.actor.testkit)
        testImplementation(akka.persistence.testkit)
    }
}

normally {
    dockerCompose {
        startedServices = listOf("cassandra-init", "cassandra-db")
        isRequiredBy(tasks.test)
    }
} except { inCI and (onMac or onWindows) } where {
    tasks.test { enabled = false }
} cause "GitHub Actions runner does not support Docker Compose"
