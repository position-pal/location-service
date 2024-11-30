dependencies {
    api(project(":presentation"))
    with(libs) {
        implementation(grpc.netty.shaded)
        testImplementation(scalamock.cats)
    }
}
