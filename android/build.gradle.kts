plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.vanniktech.publish)
}
apply(from = rootProject.file("gradle/publish.gradle.kts"))

android {
    namespace  = "com.neuralheads.kmpshortcuts.android"
    compileSdk = 35
    defaultConfig { minSdk = 25 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":core"))
    api(libs.androidx.core.ktx)
    api(libs.kotlinx.coroutines.android)
}

