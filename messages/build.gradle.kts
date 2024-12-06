import Utils.inCI
import Utils.normally
import Utils.onMac
import Utils.onWindows

dependencies {
    api(project(":presentation"))
    implementation(libs.circe.named.codec)
    api(libs.lepus.client)
    implementation(libs.lepus.std)
    implementation(libs.lepus.circe)
    testImplementation(libs.scalamock.cats)
}

normally {
    dockerCompose {
        startedServices = listOf("rabbitmq-broker")
        isRequiredBy(tasks.test)
    }
} except { inCI and (onMac or onWindows) } where {
    tasks.test { enabled = false }
} cause "GitHub Actions runner does not support Docker Compose"
