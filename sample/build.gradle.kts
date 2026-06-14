plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace    = "io.neuralheads.kmpshortcuts.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.neuralheads.kmpshortcuts.sample"
        minSdk        = 23
        targetSdk     = 35
        versionCode   = 1
        versionName   = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":kmpshortcuts"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.appcompat)
    implementation(libs.material)
}
