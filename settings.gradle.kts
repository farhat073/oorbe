pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "oorbitt"

include(":app")
include(":core-model")
include(":core-data")
include(":core-ui")
include(":core-platform")
include(":core-security")
include(":feature-home")
include(":feature-drawer")
include(":feature-search")
include(":feature-iconpack")
include(":feature-theme")
include(":feature-settings")
include(":feature-backup")
include(":feature-gesture")
include(":feature-shizuku")
include(":feature-premium")
include(":feature-applock")
include(":feature-profiles")
include(":feature-vault")
include(":feature-wellness")
include(":feature-orbspace")
include(":feature-onboarding")
include(":feature-stylehub")


