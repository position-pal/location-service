dependencies {
    api(project(":domain"))
    with(libs) {
        api(fs2.core)
        api(logback.classic)
    }
}
