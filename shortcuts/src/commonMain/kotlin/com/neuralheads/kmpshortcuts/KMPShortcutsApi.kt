package com.neuralheads.kmpshortcuts

/**
 * Marks KMPShortcuts APIs that are not yet stable and may change in future releases.
 *
 * Annotated declarations are subject to breaking changes without a deprecation cycle.
 * Opt in at the call site with `@OptIn(ExperimentalKMPShortcutsApi::class)` or
 * propagate the opt-in with `@ExperimentalKMPShortcutsApi` on your own declaration.
 */
@RequiresOptIn(
    message = "This API is experimental and may change in future versions of KMPShortcuts.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.ANNOTATION_CLASS
)
annotation class ExperimentalKMPShortcutsApi
