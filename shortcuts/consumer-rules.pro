# KMPShortcuts — ProGuard / R8 consumer rules
# These rules are automatically included in consumer APKs via the AAR.

# Keep the entire public API surface
-keep class com.neuralheads.kmpshortcuts.** { *; }

# Keep the @Volatile singleton field for correct cross-thread visibility
-keepclassmembers class com.neuralheads.kmpshortcuts.KMPShortcuts {
    volatile <fields>;
}

# Keep MaterialSymbolMapper lookup table (referenced by string key)
-keepclassmembers class com.neuralheads.kmpshortcuts.ShortcutIconResolver {
    *;
}
-keepclassmembers class com.neuralheads.kmpshortcuts.MaterialSymbolMapper {
    *;
}
