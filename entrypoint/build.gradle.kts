plugins {
    application
}

dependencies {
    implementation(project(":infrastructure"))
    implementation(project(":grpc"))
    implementation(project(":storage"))
    implementation(project(":messages"))
}

application {
    mainClass.set("$group.location.entrypoint.main")
}
