dependencies {
    api(project(":application"))
    api(libs.bundles.circe)
    api(libs.bundles.borer)
    implementation(libs.akka.actor)
}
