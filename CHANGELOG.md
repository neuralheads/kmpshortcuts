# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0-alpha04] — 2026-06-14

### Changed
- Migrated package group and name from `com.neuralheads.kmpshortcuts` to `io.neuralheads.kmpshortcuts`.
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
