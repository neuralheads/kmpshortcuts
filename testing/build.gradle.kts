plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    androidTarget { publishLibraryVariants("release") }
    iosX64(); iosArm64(); iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":kmpshortcuts"))
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
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    pom {
        name.set("KMPShortcuts Testing")
        description.set("In-memory FakeAppShortcutManager test double for the KMPShortcuts library.")
        inceptionYear.set("2024")
        url.set("https://github.com/neuralheads/kmpshortcuts")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("neuralheads")
                name.set("NeuralHeads")
                url.set("https://github.com/neuralheads")
            }
        }
        scm {
            url.set("https://github.com/neuralheads/kmpshortcuts")
            connection.set("scm:git:git://github.com/neuralheads/kmpshortcuts.git")
            developerConnection.set("scm:git:ssh://git@github.com/neuralheads/kmpshortcuts.git")
        }
    }
}
