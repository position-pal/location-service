dependencies {
    api(project(":presentation"))
    implementation("io.grpc:grpc-netty-shaded:1.68.1")
    testImplementation(libs.scalamock.cats)
}
