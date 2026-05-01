pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // JetBrains Compose dev previews — restricted to avoid resolving other groups from this repo
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") {
            content {
                includeGroup("org.jetbrains.compose")
                includeGroupByRegex("org\\.jetbrains\\.compose\\..*")
            }
        }
    }
}

rootProject.name = "afilaxy-kmm"
include(":androidApp")
include(":shared")
