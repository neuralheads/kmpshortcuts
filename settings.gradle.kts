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

// Each project is named to match its Maven artifact ID.
// vanniktech uses project.name as artifactId and project.group as groupId
// — no coordinates() call needed.
include(":kmpshortcuts-core")
project(":kmpshortcuts-core").projectDir = file("core")

include(":kmpshortcuts-android")
project(":kmpshortcuts-android").projectDir = file("android")

include(":kmpshortcuts-ios")
project(":kmpshortcuts-ios").projectDir = file("ios")

include(":kmpshortcuts-testing")
project(":kmpshortcuts-testing").projectDir = file("testing")

include(":kmpshortcuts")
project(":kmpshortcuts").projectDir = file("umbrella")
