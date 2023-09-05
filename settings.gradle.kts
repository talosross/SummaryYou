pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://chaquo.com/maven-test") }
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

rootProject.name = "Summary You Python"
include(":app")
 