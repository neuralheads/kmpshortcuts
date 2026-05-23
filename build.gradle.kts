import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import java.net.URI

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library)      apply false
    alias(libs.plugins.vanniktech.publish)   apply false
    alias(libs.plugins.dokka)                apply false
}

// Read group and version from gradle.properties (set via VERSION_NAME and GROUP).
// Credentials are NOT injected here — vanniktech 0.33.0 auto-configures from
// ORG_GRADLE_PROJECT_mavenCentralUsername / mavenCentralPassword env vars,
// finalizing publishingType during apply(). Injecting them again via
// allprojects.extraProperties would trigger a "property is final" conflict.
val versionName = properties["VERSION_NAME"]?.toString() ?: "0.1.0-alpha01"
val groupId     = properties["GROUP"]?.toString()        ?: "com.neuralheads"

allprojects {
    group   = groupId
    version = versionName
}

// ── Windows publish fix ──────────────────────────────────────────────────────
// Adds a local Maven repository and patches vanniktech staging URI on Windows.
subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        project.afterEvaluate {
            // Fix vanniktech staging directory URI on Windows.
            // Java's File.absolutePath uses backslashes; vanniktech constructs the staging
            // repo URI as "file://" + absolutePath which produces "file://C:\..." — invalid
            // on Windows (needs "file:///C:/..."). Patch it here at execution time.
            project.tasks.withType(PublishToMavenRepository::class.java).configureEach {
                val pub = this
                doFirst {
                    val current = pub.repository.url
                    val str = current.toString()
                    if (str.startsWith("file:") && !str.startsWith("file:///")) {
                        pub.repository.setUrl(
                            URI("file:///" + str.removePrefix("file://").replace('\\', '/'))
                        )
                    }
                }
            }

            // Add a local Maven repository with a correct file:/// URI
            // so publish.ps1 can stage signed artifacts on Windows.
            extensions.findByType(PublishingExtension::class.java)?.repositories {
                maven {
                    name = "LocalRelease"
                    url = URI("file:///C:/kmpshortcuts-release/")
                }
            }
        }
    }
}
