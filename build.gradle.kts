import org.gradle.api.publish.PublishingExtension
import java.net.URI

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library)      apply false
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.vanniktech.publish)   apply false
    alias(libs.plugins.dokka)                apply false
}

val versionName = properties["VERSION_NAME"]?.toString() ?: "0.1.0-alpha04"
val groupId     = properties["GROUP"]?.toString()        ?: "io.neuralheads"

allprojects {
    group   = groupId
    version = versionName
}

// ── LocalRelease repository (used by publish.ps1 on Windows) ─────────────────
// Adds a local Maven repository with a correct file:/// URI so that
// publish.ps1 can stage signed artifacts on Windows before uploading.
// The URI is hardcoded with forward slashes so no patching is needed.
subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        project.afterEvaluate {
            extensions.findByType(PublishingExtension::class.java)?.repositories {
                maven {
                    name = "LocalRelease"
                    url = URI("file:///C:/kmpshortcuts-release/")
                }
            }
        }
    }
}
