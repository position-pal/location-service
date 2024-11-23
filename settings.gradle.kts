plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.15"
    id("com.gradle.develocity") version "3.18.2"
}

rootProject.name = "location-service"

include(
    "commons",
    "domain",
    "application",
    "presentation",
    "infrastructure",
    "storage",
    "ws",
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
        tasks("check")
    }
    createHooks(overwriteExisting = true)
}
