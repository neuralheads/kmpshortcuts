plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.library)      apply false
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

// Only inject group/version/credentials into all projects.
// vanniktech is configured fully inside each module's build.gradle.kts using
// the mavenPublishing {} DSL — this avoids the Gradle 9.1 timing bug where
// the plugins.withId() callback fires DURING vanniktech's apply() before its
// lazy properties have finished initializing.
allprojects {
    group   = groupId
    version = versionName

    if (globalSigningKey  != null) extensions.extraProperties["signingInMemoryKey"]  = globalSigningKey
    extensions.extraProperties["signingInMemoryKeyPassword"]                          = globalSigningPass
    if (globalCentralUser != null) extensions.extraProperties["mavenCentralUsername"] = globalCentralUser
    if (globalCentralPass != null) extensions.extraProperties["mavenCentralPassword"] = globalCentralPass
}
