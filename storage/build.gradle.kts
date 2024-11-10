dependencies {
    api(project(":application"))
    implementation("com.typesafe.akka:akka-actor-typed_3:2.8.8")
    implementation("com.lightbend.akka:akka-stream-alpakka-cassandra_3:6.0.2")
    implementation("com.lightbend.akka:akka-projection-cassandra_3:1.5.0-M4")
    implementation(libs.akka.persistence.cassandra)
    implementation(libs.log4cats.slf4j)
    implementation(libs.logback.classic)

    implementation(libs.akka.cluster.typed)
    implementation(libs.akka.cluster.sharding.typed)
    implementation(libs.akka.persistence.typed)
    implementation(libs.akka.persistence.query)
    implementation(libs.logback.classic)
    implementation(libs.akka.stream)
    implementation(libs.akka.http)
}

dockerCompose.isRequiredBy(tasks.test)
