import Utils.inCI
import Utils.normally
import Utils.onMac
import Utils.onWindows

dependencies {
    api(project(":application"))
    with(libs) {
        implementation(akka.cluster.typed)
        api(akka.persistence.cassandra)
        implementation(akka.persistence.typed)
        implementation(akka.persistence.query)
        implementation(logback.classic)
        testImplementation(akka.actor.testkit)
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
