# KMPShortcuts

[![Maven Central](https://img.shields.io/maven-central/v/com.neuralheads/kmpshortcuts)](https://central.sonatype.com/artifact/com.neuralheads/kmpshortcuts)
[![CI](https://github.com/neuralheads/kmpshortcuts/actions/workflows/ci.yml/badge.svg)](https://github.com/neuralheads/kmpshortcuts/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-7F52FF.svg)](https://kotlinlang.org)

**Kotlin Multiplatform App Shortcuts** — a unified, coroutine-native API for managing Home Screen / Launcher quick actions across Android and iOS from a single shared codebase.

Wraps **`ShortcutManagerCompat`** (Android) and **`UIApplicationShortcutItem`** (iOS) behind one clean interface — no platform imports in your shared code.

---

## Features

- 🔗 **Unified API** — one `AppShortcutManager` interface, both platforms
- ⚡ **Coroutine-native** — all mutations are `suspend`; tap events delivered via a hot `Flow`
- 🛠 **DSL builder** — `shortcut("id") { shortLabel = "…"; deepLink = "…" }` for clean setup
- 🔴 **`ShortcutBadge`** — unified app icon badge count API (Android + iOS)
- 📡 **Reactive** — `observeShortcuts()` `StateFlow` emits on every change; use with `ShortcutStore`
- 💬 **Conversation shortcuts** — `ShortcutPerson` + `ShortcutCategory` for Android share sheet integration
- 🎨 **Cross-platform icons** — 60+ SF Symbols → Material mapping, asset catalog images, raw bitmaps
- 🔧 **Extensible icons** — `MaterialSymbolMapper.registerCustomMapping()` for your own SF Symbol → drawable pairs
- 📌 **Pinned shortcut support** — request pinning on Android; graceful no-op on iOS
- 🔄 **Usage reporting** — `reportUsed()` boosts ranking on Android; re-orders in-memory on iOS
- 🧪 **Test-ready** — `FakeAppShortcutManager` with `reset()`, `simulateActivation()`, and `observeShortcuts()`
- 📦 **Minimal footprint** — depends only on `kotlinx-coroutines-core` and `androidx-core-ktx`

---

## Installation

Add to your shared module's `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.neuralheads:kmpshortcuts:0.1.0-beta01")
        }

        // Test double — zero platform dependencies
        commonTest.dependencies {
            implementation("com.neuralheads:kmpshortcuts-testing:0.1.0-beta01")
        }
    }
}
```

And to your `androidApp` module's `build.gradle.kts` (required for `AndroidShortcutManager` platform initialization):

```kotlin
dependencies {
    implementation("com.neuralheads:kmpshortcuts:0.1.0-beta01")
}
```

---

## Quick Start

### 1. Initialize (once at app startup)

**Android** — `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KMPShortcuts.initialize(
            manager = AndroidShortcutManager(this),
            badge   = AndroidShortcutBadge(this)   // optional — for badge support
        )
    }
}
```

**iOS** — `AppDelegate.application(_:didFinishLaunchingWithOptions:)`:

```swift
import kmpshortcuts

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        KMPShortcuts.shared.initialize(
            manager: IOSShortcutManager(),
            badge:   IOSShortcutBadge()   // optional
        )
        return true
    }
}
```

### 2. Set shortcuts — DSL style (recommended)

```kotlin
suspend fun setupShortcuts() {
    KMPShortcuts.manager.setShortcuts {
        shortcut("new_post") {
            shortLabel = "New Post"
            longLabel  = "Create a new post"
            icon       = ShortcutIcon.System("square.and.pencil")
            deepLink   = "myapp://new-post"
        }
        shortcut("search") {
            shortLabel = "Search"
            icon       = ShortcutIcon.System("magnifyingglass")
            deepLink   = "myapp://search"
        }
        shortcut("inbox") {
            shortLabel = "Inbox"
            icon       = ShortcutIcon.System("envelope")
            deepLink   = "myapp://inbox"
        }
    }
}
```

### 3. Observe shortcut list reactively

```kotlin
// In a ViewModel:
val shortcuts = KMPShortcuts.manager.observeShortcuts()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
```

### 4. Observe activations (shared Kotlin code)

```kotlin
fun observeShortcuts(scope: CoroutineScope) {
    scope.launch {
        KMPShortcuts.manager.observeActivations().collect { event ->
            when (event.shortcutId) {
                "new_post" -> navigateTo(Screen.NewPost)
                "search"   -> navigateTo(Screen.Search)
            }
        }
    }
}
```

---

## Platform Setup

### Android

Feed shortcut tap intents into the activation flow from your launcher `Activity`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Routes the shortcut ID into KMPShortcuts.manager.observeActivations()
        AndroidShortcutManager.handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        AndroidShortcutManager.handleIntent(intent)
    }
}
```

### iOS

Apply pending shortcuts to `UIApplication` and route tap events from your `AppDelegate`:

```swift
func applicationDidBecomeActive(_ application: UIApplication) {
    // Flush the in-memory shortcut list to the home screen (synchronous)
    let manager = KMPShortcuts.shared.manager as? IOSShortcutManager
    application.shortcutItems = manager?.pendingShortcutItemsSnapshot
}

func application(
    _ application: UIApplication,
    performActionFor shortcutItem: UIApplicationShortcutItem,
    completionHandler: @escaping (Bool) -> Void
) {
    // Routes the tap into KMPShortcuts.manager.observeActivations()
    IOSShortcutManager.companion.handleShortcutItem(item: shortcutItem)
    completionHandler(true)
}
```

> **Note:** `pendingShortcutItemsSnapshot` is a non-suspend property that reads the in-memory
> cache directly — safe for use in `applicationDidBecomeActive`. A suspend variant
> `pendingShortcutItems()` is also available for coroutine contexts.

---

## API Reference

### `AppShortcutManager`

| Method / Property | Description |
|-------------------|-------------|
| `setShortcuts(shortcuts)` | Replace **all** dynamic shortcuts. List is trimmed to the platform limit automatically. |
| `setShortcuts { shortcut(…) }` | DSL overload — build the list inline. |
| `addShortcut(shortcut)` | Add or update a single shortcut. Pushes the oldest out when at the platform limit. |
| `addOrUpdate(shortcut)` | Add if new, update if ID exists. |
| `updateShortcut(id, transform)` | Mutate an existing shortcut in-place via a `copy` lambda. No-op if `id` not found. |
| `removeShortcut(id)` | Remove a shortcut by ID. Silent no-op for unknown IDs. |
| `removeShortcuts(vararg ids)` | Remove multiple shortcuts by ID in one call. |
| `clearShortcuts()` | Remove all dynamic shortcuts. |
| `getShortcuts()` | Return all current dynamic shortcuts, including `extras`. |
| `reportUsed(shortcutId)` | Report usage — boosts ranking on Android; moves to top on iOS. |
| `requestPin(shortcut)` | Request home-screen pinning (Android only). Returns `false` on iOS. |
| `isPinSupported()` | `true` on Android launchers that support pinning, always `false` on iOS. |
| `isAtCapacity()` | `true` when the platform limit has been reached. |
| `observeActivations()` | Hot `Flow<ShortcutActivationEvent>` — collect to respond to shortcut taps. |
| `observeShortcuts()` | `StateFlow<List<ShortcutInfo>>` — emits on every mutation. |
| `maxShortcutCount` | Platform dynamic shortcut limit (Android: up to 5; iOS: 4). |

### `ShortcutInfo`

```kotlin
data class ShortcutInfo(
    val id: String,                               // Stable identifier — must be unique
    val shortLabel: String,                       // ~12 chars — shown on iOS and space-constrained Android
    val longLabel: String = shortLabel,           // ~25 chars — shown on Android when space allows
    val icon: ShortcutIcon = ShortcutIcon.None,
    val deepLinkAction: String? = null,           // URI, e.g. "myapp://feed"
    val extras: Map<String, String> = emptyMap(), // Delivered in ShortcutActivationEvent.extras
    val rank: Int = 0,                            // Lower = higher position (Android only)
    val categories: Set<ShortcutCategory> = emptySet(), // Android: share-sheet / conversation ranking
    val person: ShortcutPerson? = null            // Android: conversation shortcut person
)
```

### `ShortcutBadge`

Set the app icon badge number from shared code:

```kotlin
// At startup:
val granted = KMPShortcuts.badge.requestPermission()  // iOS only — Android always returns true

// Show badge:
KMPShortcuts.badge.setBadgeCount(5)

// Clear:
KMPShortcuts.badge.clearBadge()
```

### `ShortcutStore` — Reactive Cache

```kotlin
val store = ShortcutStore.create(
    manager = KMPShortcuts.manager,
    initialShortcuts = savedList  // restored from disk on cold launch
)

// ViewModel — react to changes:
val shortcuts = store.shortcuts  // StateFlow<List<ShortcutInfo>>

// Mutate:
store.set {
    shortcut("new_post") { shortLabel = "New Post" }
}
```

### Conversation Shortcuts (Android)

```kotlin
shortcut("chat_alice") {
    shortLabel = "Alice"
    icon       = ShortcutIcon.Resource("avatar_alice")
    categories = setOf(ShortcutCategory.CONVERSATION)
    person     = ShortcutPerson(name = "Alice", key = "user_42")
    deepLink   = "myapp://chat/42"
}
```

### `ShortcutIcon`

| Variant | Android | iOS |
|---------|---------|-----|
| `ShortcutIcon.None` | Platform default | Platform default |
| `ShortcutIcon.System(name)` | SF Symbol name mapped to a Material drawable via `MaterialSymbolMapper` | SF Symbol name (e.g. `"square.and.pencil"`) |
| `ShortcutIcon.Resource(name)` | Drawable resource name looked up via `getIdentifier()` | Asset catalog image name |
| `ShortcutIcon.Bitmap(data)` | Raw PNG/JPEG bytes decoded to a `Bitmap` | Falls back to `"photo"` SF Symbol (iOS does not support raw bitmap shortcut icons) |

### `ShortcutActivationEvent`

```kotlin
data class ShortcutActivationEvent(
    val shortcutId: String,              // Matches ShortcutInfo.id
    val deepLinkAction: String? = null,  // Matches ShortcutInfo.deepLinkAction
    val extras: Map<String, String> = emptyMap() // Matches ShortcutInfo.extras
)
```

---

## Testing

Use `FakeAppShortcutManager` from the `kmpshortcuts-testing` artifact. It has no platform
dependencies and runs in any `commonTest`, `androidTest`, or JVM test:

```kotlin
class ShortcutsViewModelTest {

    private val fake = FakeAppShortcutManager()

    @BeforeTest
    fun setUp() {
        KMPShortcuts.initialize(fake)
    }

    @Test
    fun `setup registers the correct shortcuts`() = runTest {
        setupShortcuts() // calls KMPShortcuts.manager.setShortcuts(...)

        assertEquals(2, fake.shortcuts.size)
        assertEquals("new_post", fake.shortcuts[0].id)
        assertEquals("myapp://new-post", fake.shortcuts[0].deepLinkAction)
    }

    @Test
    fun `tapping a shortcut emits an activation event`() = runTest {
        val events = mutableListOf<ShortcutActivationEvent>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            fake.observeActivations().collect { events.add(it) }
        }

        fake.simulateTap("new_post", deepLinkAction = "myapp://new-post")

        assertEquals(1, events.size)
        assertEquals("new_post", events.first().shortcutId)

        job.cancel()
    }
}
```

### `FakeAppShortcutManager` inspection properties

| Property | Type | Description |
|----------|------|-------------|
| `shortcuts` | `MutableList<ShortcutInfo>` | All currently registered shortcuts |
| `reportedUsage` | `MutableList<String>` | Shortcut IDs passed to `reportUsed()`, in call order |
| `pinRequests` | `MutableList<ShortcutInfo>` | Shortcuts passed to `requestPin()` |

---

## Platform Limits

| Platform | Max Dynamic Shortcuts | Pinning |
|----------|-----------------------|---------|
| Android | Up to 5 (reported by `ShortcutManagerCompat`) | ✅ Launcher-dependent |
| iOS | 4 total (static + dynamic combined) | ❌ Not supported |

---

## Modules

| Artifact | Description |
|----------|-------------|
| com.neuralheads:kmpshortcuts | Core library — `commonMain` + `androidMain` + `iosMain` |
| com.neuralheads:kmpshortcuts-testing | `FakeAppShortcutManager` — zero-dependency test double |

---

## Requirements

| Tool | Version |
|------|---------|
| Kotlin | 2.1.21+ |
| Android `minSdk` | 23 |
| Android `compileSdk` | 35 |
| iOS targets | `iosX64`, `iosArm64`, `iosSimulatorArm64` |
| Gradle | 8.x with version catalog support |

---

## Contributing

Pull requests are welcome. Please open an issue first for significant changes.

See [RELEASING.md](RELEASING.md) for the release process.

---

## License

```
Copyright 2026 NeuralHeads

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
