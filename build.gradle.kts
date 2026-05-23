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
