dependencies {
    api(project(":presentation"))
    with(libs) {
        api(grpc.netty.shaded)
        testImplementation(scalamock.cats)
    }
}
