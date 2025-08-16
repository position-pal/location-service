plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.30"
    id("com.gradle.develocity") version "4.1"
}

rootProject.name = "location-service"

include(
    "commons",
    "domain",
    "application",
    "presentation",
    "tracking-actors",
    "storage",
    "ws",
    "messages",
    "grpc",
    "entrypoint",
)

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        uploadInBackground = !System.getenv("CI").toBoolean()
        publishing.onlyIf { it.buildResult.failures.isNotEmpty() }
    }
}

gitHooks {
    commitMsg { conventionalCommits() }
    preCommit {
        tasks("checkScalafmtAll", "checkScalafix")
    }
    createHooks(overwriteExisting = true)
}
