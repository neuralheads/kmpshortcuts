# Releasing KMPShortcuts

This document describes the step-by-step process to cut a release and publish to Maven Central.

---

## Prerequisites

- Write access to the `neuralheads/kmpshortcuts` GitHub repository.
- GPG signing key configured (see [vanniktech signing docs](https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets)).
- Maven Central credentials available as environment variables or `~/.gradle/gradle.properties`:
  ```properties
  mavenCentralUsername=<token-username>
  mavenCentralPassword=<token-password>
  ```

---

## Release Checklist

### 1. Update the version

Edit [`gradle.properties`](gradle.properties):

```properties
VERSION_NAME=0.1.0          # remove -alpha01 / -beta01 suffix for stable releases
```

Use [Semantic Versioning](https://semver.org):
- `PATCH` — bug fixes, no API changes.
- `MINOR` — new backwards-compatible API.
- `MAJOR` — breaking API changes.

### 2. Update `CHANGELOG.md`

- Move all entries from `[Unreleased]` into a new `[X.Y.Z] — YYYY-MM-DD` section.
- Add a fresh empty `[Unreleased]` block at the top.

### 3. Commit and tag

```bash
git add gradle.properties CHANGELOG.md
git commit -m "Release 0.1.0"
git tag v0.1.0
git push origin main --tags
```

### 4. Publish to Maven Central

```bash
./gradlew publishAllPublicationsToMavenCentral --no-configuration-cache
```

> vanniktech 0.33+ auto-closes and releases the staging repository when
> `SONATYPE_AUTOMATIC_RELEASE=true` is set in `gradle.properties`.

### 5. Verify on Maven Central

Check [central.sonatype.com](https://central.sonatype.com/artifact/com.neuralheads/kmpshortcuts)
for the new version (propagation takes ~10–30 minutes).

### 6. Create a GitHub Release

- Draft a new release at `https://github.com/neuralheads/kmpshortcuts/releases/new`.
- Use tag `vX.Y.Z`.
- Copy the relevant `CHANGELOG.md` section as the release body.

### 7. Bump to the next development version

Edit `gradle.properties` again:

```properties
VERSION_NAME=0.2.0-SNAPSHOT
```

Commit:

```bash
git add gradle.properties
git commit -m "Prepare next development iteration"
git push
```

---

## Snapshot Builds

Snapshots are **not** automatically published. To publish a snapshot manually:

```bash
VERSION_NAME=0.2.0-SNAPSHOT ./gradlew publishAllPublicationsToMavenCentral
```

Snapshot artifacts are available from `https://s01.oss.sonatype.org/content/repositories/snapshots/`.
