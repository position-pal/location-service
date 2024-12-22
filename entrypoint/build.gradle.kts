plugins {
    application
}

dependencies {
    implementation(project(":tracking-actors"))
    implementation(project(":ws"))
    implementation(project(":grpc"))
    implementation(project(":storage"))
    implementation(project(":messages"))
    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":ws")))
}

application {
    mainClass.set("$group.location.entrypoint.Launcher")
}
