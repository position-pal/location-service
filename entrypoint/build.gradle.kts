import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer

plugins {
    application
    alias(libs.plugins.shadowJar)
}

dependencies {
    implementation(project(":tracking-actors"))
    implementation(project(":ws"))
    implementation(project(":grpc"))
    implementation(project(":storage"))
    implementation(project(":messages"))
    with(libs) {
        implementation(bundles.clusterman)
        testImplementation(archunit)
    }
    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":ws")))
}

application {
    mainClass.set("$group.location.entrypoint.Launcher")
}

tasks.withType<ShadowJar> {
    val newTransformer = AppendingTransformer()
    newTransformer.resource = "reference.conf"
    transformers.add(newTransformer)
}
