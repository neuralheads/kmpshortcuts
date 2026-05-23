plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
}
apply(from = rootProject.file("gradle/publish.gradle.kts"))

kotlin {
    androidTarget { publishLibraryVariants("release") }
    iosX64(); iosArm64(); iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
        }
    }
}

android {
    namespace  = "com.neuralheads.kmpshortcuts.testing"
    compileSdk = 35
    defaultConfig { minSdk = 25 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

mavenPublishing {
    coordinates(
        groupId    = "com.neuralheads",
        artifactId = "kmpshortcuts-testing",
        version    = project.version.toString()
    )
}
