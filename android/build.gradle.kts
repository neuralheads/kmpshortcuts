plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
}
apply(from = rootProject.file("gradle/publish.gradle.kts"))

kotlin {
    androidTarget { publishLibraryVariants("release") }

    sourceSets {
        androidMain.dependencies {
            api(project(":kmpshortcuts-core"))
            api(libs.androidx.core.ktx)
            api(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace  = "com.neuralheads.kmpshortcuts.android"
    compileSdk = 35
    defaultConfig { minSdk = 25 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
