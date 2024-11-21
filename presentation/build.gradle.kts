dependencies {
    api(project(":application"))
    api(libs.bundles.circe)
    api(libs.bundles.borer)
    api(libs.positionpal.kernel.presentation)
    api("org.apache.avro:avro:1.12.0")
    implementation(libs.akka.actor)
}
