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

// Single KMP module — commonMain / androidMain / iosMain
// Separate testing module for the in-memory fake
include(":kmpshortcuts")
project(":kmpshortcuts").projectDir = file("shortcuts")

include(":kmpshortcuts-testing")
project(":kmpshortcuts-testing").projectDir = file("testing")

include(":kmpshortcuts-sample")
project(":kmpshortcuts-sample").projectDir = file("sample")
