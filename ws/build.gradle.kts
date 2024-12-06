import Utils.inCI
import Utils.normally
import Utils.onMac
import Utils.onWindows

dependencies {
    api(project(":infrastructure"))
    with(libs) {
        implementation(akka.actor.typed)
        setOf(akka.http, akka.stream.typed).forEach {
            api(it)
            testFixturesImplementation(it)
        }
    }
    testImplementation(testFixtures(project(":domain")))
}

normally {
    dockerCompose {
        startedServices = listOf("cassandra-init", "cassandra-db")
        isRequiredBy(tasks.test)
    }
} except { inCI and (onMac or onWindows) } where {
    tasks.test { enabled = false }
} cause "GitHub Actions runner does not support Docker Compose"
