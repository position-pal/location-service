dependencies {
    api(project(":domain"))
    with(libs) {
        api(bundles.fs2)
        api(logback.classic)
        api(log4cats.slf4j)
    }
    testImplementation(testFixtures(project(":domain")))
}
