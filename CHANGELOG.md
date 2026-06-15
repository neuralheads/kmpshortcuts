# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0-beta01] — 2026-06-15

### Added
- **`shortcut { }` DSL builder** — `shortcut("id") { shortLabel = "…"; icon = … }` and
  `shortcuts { shortcut(…) { … } }` list builder. Annotated with `@DslMarker` to prevent accidental
  nesting. Both functions are top-level in the `io.neuralheads.kmpshortcuts` package.
- **`ShortcutStore`** — reactive write-through cache exposing `shortcuts: StateFlow<List<ShortcutInfo>>`.
  Updates whenever any mutation (`set`, `add`, `update`, `remove`, `clear`) is called.
  Includes optional `ShortcutStore.Persister` interface so apps can save/restore the list across
  cold launches without adding a dependency to the core library.
- **`ShortcutBadge`** — unified app icon badge count API (`setBadgeCount`, `clearBadge`,
  `requestPermission`, `getBadgeCount`, `isBadgeSupported`).
  - `AndroidShortcutBadge` — uses a silent `NotificationChannel` (IMPORTANCE_MIN) for broad
    launcher compatibility (Samsung One UI, Nova, etc.).
  - `IOSShortcutBadge` — uses `UNUserNotificationCenter` (iOS 16+ recommended API).
  - Accessible via `KMPShortcuts.badge` after initialization.
- **`observeShortcuts(): StateFlow<List<ShortcutInfo>>`** — added to `AppShortcutManager` interface.
  All three implementations (`AndroidShortcutManager`, `IOSShortcutManager`,
  `FakeAppShortcutManager`) emit on every mutation.
- **Conversation shortcuts** — `ShortcutInfo` now has optional `categories: Set<ShortcutCategory>`
  and `person: ShortcutPerson?` fields.
  - `ShortcutCategory.CONVERSATION` / `MEDIA` — map to Android system category strings;
    conversation shortcuts appear in the Android share sheet and notification shade. No-op on iOS.
  - `ShortcutPerson(name, key, uri, isBot)` — mapped to `androidx.core.app.Person` on Android
    for full conversation ranking integration. No-op on iOS.
- **Extension functions** on `AppShortcutManager`:
  - `setShortcuts(vararg shortcuts)` — vararg overload
  - `setShortcuts { shortcut(…) { … } }` — DSL overload
  - `addOrUpdate(shortcut)` — insert or update by ID
  - `removeShortcuts(vararg ids)` — batch removal
  - `isAtCapacity()` — returns `true` when at platform limit
- **`KMPShortcuts.resetForTesting()`** — resets singleton to uninitialized state for clean
  test isolation. Call from `@AfterTest`.
- **`KMPShortcuts.initialize(manager, badge)`** — optional `badge` parameter to register
  the `ShortcutBadge` implementation at startup.
- **60+ SF Symbol → Material icon mappings** (up from 35):
  Added `person.2`, `lock`, `key`, `doc`, `folder`, `wifi`, `battery.100`, `clock`, `calendar`,
  `music.note`, `play`, `pause`, `stop`, `mic`, `video`, `lightbulb`, `tag`, `flag`, `paperplane`,
  `square.and.arrow.up`, `qrcode`, `chart.bar`, `square.stack`, `sun.max`, `moon`, `pencil`, and more.
  Plus 25 new bundled Android drawable XML files.
- **`MaterialSymbolMapper.registerCustomMapping(sfSymbol, drawableName)`** — public API to register
  app-specific SF Symbol → drawable mappings at runtime. Custom entries take precedence over built-ins.
- **`MaterialSymbolMapper`** is now `public` (was `internal`).
- **`FakeAppShortcutManager` improvements**:
  - `reset()` — clears `shortcuts`, `reportedUsage`, `pinRequests` and resets the StateFlow
  - `simulateActivation(event: ShortcutActivationEvent)` — full-event overload of `simulateTap`
  - `simulatePinSupported: Boolean` — set to `false` to simulate unsupported launchers
  - `maxShortcutCount` is now mutable (`var`) so tests can change it mid-run
  - Implements the new `observeShortcuts()` StateFlow

### Changed
- `updateShortcut` lambda parameter renamed from `update` to `transform` for clarity (source compatible).
- `ShortcutInfo` gains two new optional fields (`categories`, `person`) — binary compatible
  (default values supplied).
- `KMPShortcuts.initialize()` now accepts an optional `badge` parameter — source compatible.

---

## [0.1.0-alpha04] — 2026-06-14


- Migrated Kotlin package names from `com.neuralheads.kmpshortcuts` to `io.neuralheads.kmpshortcuts`, but kept the published Maven Group ID as `com.neuralheads` for Sonatype Central validation compatibility.
- Lowered Android `minSdk` to 23 (was 25).
- Bundled fallback Material Design icons locally in Android resources.
- Fixed JDK 21 compatibility issue with `removeLast()`.

---

## [0.1.0-alpha03] — 2026-05-24

### Fixed
- **Critical:** iOS native klib artifacts (`iosArm64`, `iosX64`, `iosSimulatorArm64`) were missing
  from `0.1.0-alpha02`. The Gradle module metadata referenced them but they returned 404, causing
  resolution failure for any project with iOS targets. This version is built on macOS via GitHub
  Actions, producing all iOS klibs correctly.

---

## [0.1.0-alpha02] — 2026-05-24

All fixes from the full audit applied on top of the original alpha01.

### Changed
- Kotlin `2.0.21` → `2.1.21`, AGP `8.5.2` → `8.9.0`, Dokka `1.9.20` → `2.0.0`
- `handleIntent()` — multi-key detection for maximum launcher compatibility
- `removeShortcut()` / `clearShortcuts()` — now guarded by `Mutex` (race condition fix)
- Added `pendingShortcutItemsSnapshot` non-suspend property for Swift
- Added ProGuard `consumer-rules.pro`
- Added `publish.ps1` for one-click Windows deployment
- CI fixed: triggers on `master` (actual default branch)

---

## [0.1.0-alpha01] — 2026-05-24

### Added
- **`AppShortcutManager`** — platform-agnostic interface with:
  - `setShortcuts()` / `addShortcut()` / `updateShortcut()` / `removeShortcut()` / `clearShortcuts()`
  - `getShortcuts()` — returns all current shortcuts including extras (full round-trip)
  - `reportUsed()` — boosts ranking on Android; moves entry to top on iOS
  - `requestPin()` / `isPinSupported()` — home-screen pinning (Android only)
  - `observeActivations(): Flow<ShortcutActivationEvent>` — hot flow of tap events
  - `maxShortcutCount` — platform limit property
- **`ShortcutInfo`** — data class with `id`, `shortLabel`, `longLabel`, `icon`, `deepLinkAction`, `extras`, `rank`
- **`ShortcutIcon`** — sealed class: `None`, `System(name)`, `Resource(name)`, `Bitmap(data)`
- **`ShortcutActivationEvent`** — data class with `shortcutId`, `deepLinkAction`, `extras`
- **`KMPShortcuts`** — `@Volatile` thread-safe singleton entry point with `initialize()` and `isInitialized`
- **`ExperimentalKMPShortcutsApi`** — `@RequiresOptIn` annotation for unstable API surface
- **`AndroidShortcutManager`** — `ShortcutManagerCompat` implementation:
  - All operations dispatched to `Dispatchers.Main` with `Mutex` guards (including `removeShortcut` and `clearShortcuts`)
  - `handleIntent()` companion function routes shortcut tap `Intent`s into the activation flow
  - Multi-key intent detection for maximum launcher compatibility (`EXTRA_KMP_SHORTCUT_ID`, `EXTRA_SHORTCUT_ID`, `shortcut_id`)
  - `ShortcutIconResolver` — resolves `ShortcutIcon` to `IconCompat`
  - `MaterialSymbolMapper` — maps 35+ SF Symbol names to Android Material drawables
  - Full `extras` round-trip: `getShortcuts()` restores `extras` from the backing `Intent`
- **`IOSShortcutManager`** — `UIApplicationShortcutItem` Kotlin/Native implementation:
  - In-memory cache guarded by a single `Mutex` (all operations atomic)
  - `pendingShortcutItemsSnapshot` — non-suspend property for synchronous Swift access
  - `pendingShortcutItems()` — `suspend` method returning items under the Mutex
  - `handleShortcutItem()` companion function routes AppDelegate taps into the activation flow
  - `IOSShortcutIconResolver` — SF Symbols, asset catalog images, raw bitmap fallback
- **`FakeAppShortcutManager`** (`kmpshortcuts-testing` artifact):
  - Zero platform dependencies — usable in `commonTest`, `androidTest`, or any JVM test
  - `simulateTap()` — emits a `ShortcutActivationEvent` into the activation flow
  - `shortcuts`, `reportedUsage`, `pinRequests` lists for direct assertion in tests
- **ProGuard consumer rules** — automatic R8 keep rules for the public API
- **Tests** — 46 unit tests across two suites (`commonTest`, JVM target):
  - `AppShortcutManagerContractTest` — 29 tests covering every `AppShortcutManager` operation
  - `FakeAppShortcutManagerTest` — 17 tests verifying the fake's own correctness
- **CI** — GitHub Actions:
  - `ci.yml` — JVM unit tests on Ubuntu + full assemble on macOS (triggered on push/PR to `master`)
  - `publish.yml` — publishes to Maven Central on version tags (`v*`)
- **Documentation** — `CHANGELOG.md`, `RELEASING.md`, `.gitignore`

### Fixed
- `KMPShortcuts._manager` — marked `@Volatile` for correct JVM visibility across threads
- `AndroidShortcutManager.handleIntent()` — checks multiple intent extra keys for launcher compatibility
- `AndroidShortcutManager.removeShortcut()` / `clearShortcuts()` — now guarded by `Mutex`
- `AndroidShortcutManager.getShortcuts()` — restores `extras` map from backing `Intent` (was silently dropped)
- `IOSShortcutManager.updateShortcut()` — consolidated from two sequential `Mutex.withLock` calls into one atomic block
- `IOSShortcutManager.pendingShortcutItems()` — now reads `_cache` under the `Mutex` (was a data race)
- CI workflow branch — triggers on `master` (actual default branch), not `main`
- Removed redundant same-package imports from `ShortcutIconResolver` and `IOSShortcutIconResolver`
- `LICENSE` — corrected copyright holder from `KMPWorker Contributors` to `KMPShortcuts Contributors`
