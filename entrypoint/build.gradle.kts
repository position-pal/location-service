import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer

plugins {
    application
}

with(libs) {
    dependencies {
        implementation(project(":tracking-actors"))
        implementation(project(":ws"))
        implementation(project(":grpc"))
        implementation(project(":storage"))
        implementation(project(":messages"))
        implementation(bundles.clusterman)
        testImplementation(testFixtures(project(":domain")))
        testImplementation(testFixtures(project(":ws")))
    }

}

application {
    mainClass.set("$group.location.entrypoint.Launcher")
}

tasks.withType<ShadowJar> {
    val newTransformer = AppendingTransformer()
    newTransformer.resource = "reference.conf"
    transformers.add(newTransformer)
}