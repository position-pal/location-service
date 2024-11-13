plugins {
    application
}

dependencies {
    implementation(project(":infrastructure"))
}

application {
    mainClass.set("io.github.positionpal.location.entrypoint.main")
}
