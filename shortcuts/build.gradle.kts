plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    androidTarget { publishLibraryVariants("release") }
    iosX64(); iosArm64(); iosSimulatorArm64()
    // JVM target: used only for running commonTest on the host JVM (not published).
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            api(libs.androidx.core.ktx)
            api(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":kmpshortcuts-testing"))
        }
    }
}

android {
    namespace  = "io.neuralheads.kmpshortcuts"
    compileSdk = 35
    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Publishing target, automatic release, and signing are all configured via
// gradle.properties (SONATYPE_HOST / SONATYPE_AUTOMATIC_RELEASE / RELEASE_SIGNING_ENABLED)
// so vanniktech can finalize publishingType during apply() without conflict.
mavenPublishing {
    pom {
        name.set("KMPShortcuts")
        description.set(
            "Kotlin Multiplatform App Shortcuts. Unified API over Android ShortcutManagerCompat " +
            "and iOS UIApplicationShortcutItem with coroutine-native activation Flow."
        )
        inceptionYear.set("2026")
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
