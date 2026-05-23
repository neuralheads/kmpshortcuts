import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library)      apply false
    alias(libs.plugins.kotlin.android)       apply false
    alias(libs.plugins.vanniktech.publish)   apply false
    alias(libs.plugins.dokka)                apply false
}

val localProps = java.util.Properties().also { props ->
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.inputStream()?.use(props::load)
}

fun getSecret(key: String): String? =
    System.getenv(key) ?: localProps.getProperty(key)

val globalSigningKey  = getSecret("SIGNING_KEY")
val globalSigningPass = getSecret("SIGNING_PASSWORD") ?: ""
val globalCentralUser = getSecret("MAVEN_CENTRAL_USERNAME")
val globalCentralPass = getSecret("MAVEN_CENTRAL_PASSWORD")

val versionName = properties["VERSION_NAME"]?.toString() ?: "0.1.0-alpha01"
val groupId     = properties["GROUP"]?.toString()        ?: "com.neuralheads"

allprojects {
    group   = groupId
    version = versionName

    if (globalSigningKey  != null) extensions.extraProperties["signingInMemoryKey"]      = globalSigningKey
    extensions.extraProperties["signingInMemoryKeyPassword"]                               = globalSigningPass
    if (globalCentralUser != null) extensions.extraProperties["mavenCentralUsername"]     = globalCentralUser
    if (globalCentralPass != null) extensions.extraProperties["mavenCentralPassword"]     = globalCentralPass
}

subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
            signAllPublications()

            pom {
                val artifactId =
                    if (project.name == "umbrella") "kmpshortcuts"
                    else "kmpshortcuts-${project.name}"

                name.set("KMPShortcuts — $artifactId")
                description.set(
                    "Kotlin Multiplatform App Shortcuts library. Wraps Android ShortcutManagerCompat " +
                    "and iOS UIApplicationShortcutItem behind a single, coroutine-native API."
                )
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

        project.afterEvaluate {
            project.tasks.matching { it.name == "javaDocReleaseGeneration" }
                .configureEach { enabled = false }
        }
    }
}
