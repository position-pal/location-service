dependencies {
    api(project(":domain"))
    with(libs) {
        api(fs2.core)
        api(logback.classic)
        api(log4cats.slf4j)
    }
}
