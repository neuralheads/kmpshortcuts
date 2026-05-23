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
            api(project(":testing"))
        }
        androidMain.dependencies {
            api(project(":android"))
        }
        val iosMain by creating {
            dependsOn(commonMain.get())
            dependencies { api(project(":ios")) }
        }
        val iosX64Main by getting           { dependsOn(iosMain) }
        val iosArm64Main by getting         { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace  = "com.neuralheads.kmpshortcuts"
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
        artifactId = "kmpshortcuts",
        version    = project.version.toString()
    )
}
