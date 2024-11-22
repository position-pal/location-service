dependencies {
    api(project(":presentation"))
    implementation(libs.circe.named.codec)
    implementation(libs.lepus.client)
    implementation(libs.lepus.std)
    implementation(libs.lepus.circe)
    testImplementation(libs.scalamock.cats)
}

dockerCompose {
    val rabbitMqService = "rabbitmq-broker"
    startedServices = listOf(rabbitMqService)
}

dockerCompose.isRequiredBy(tasks.test)
