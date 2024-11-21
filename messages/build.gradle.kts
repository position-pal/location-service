dependencies {
    api(project(":presentation"))
    implementation("dev.hnaderi:named-codec-circe_3:0.2.1")
    implementation(libs.lepus.client)
    implementation(libs.lepus.std)
    implementation(libs.lepus.circe)
    testImplementation(libs.scalamock.cats)
}

dockerCompose {
    startedServices = listOf("rabbitmq-broker")
}

dockerCompose.isRequiredBy(tasks.test)
