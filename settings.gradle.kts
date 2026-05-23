rootProject.name = "kmpshortcuts"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(
    ":core",
    ":android",
    ":ios",
    ":testing",
    ":umbrella"
)
