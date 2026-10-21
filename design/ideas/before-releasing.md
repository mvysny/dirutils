# Before releasing 2.2

Loose ends found in the pre-release check (2026-10-21), to sort out before cutting 2.2.

## Dependencies

Latest stable as of 2026-10-21 (`maven-versions.rb latest`):

| What | Now | Latest |
|---|---|---|
| Kotlin plugin `org.jetbrains.kotlin.jvm` | 2.0.20 | 2.4.21 |
| `org.slf4j:slf4j-api` / `slf4j-simple` | 2.0.13 | 2.0.20 |
| Gradle wrapper | 8.14.6 | 9.8.2 |
| `io.github.gradle-nexus.publish-plugin` | 2.0.0 | 2.0.0 (current) |
| `com.github.mvysny.dynatest:dynatest` | 0.25 | 0.25 (current) |

- `Q_gradle9`: Gradle 9 itself needs JDK 17+ to run, but CI builds on JDK 8 and 11, and
  `RELEASING.md` publishes from JDK 11 so the bytecode stays Android-safe. Either stay on 8.x,
  or move to Gradle 9 + a JDK 8 toolchain (`java.toolchain`, `--release 8`), which would
  also make the "release with JDK ≤ 11" guard in `build.gradle.kts` unnecessary.
- `kotlinOptions.jvmTarget` is deprecated in Kotlin 2.x (an error in later versions):
  move to `compilerOptions { jvmTarget = JvmTarget.JVM_1_8 }` when bumping Kotlin.
- `Q_kotlin_metadata`: the jar's Kotlin metadata is now 2.0 (2.1 shipped 1.4), so a Kotlin
  consumer needs a 1.9+ compiler to call the `File` extensions. Pin `languageVersion` /
  `apiVersion` lower so older Android projects keep working? Bumping Kotlin without pinning
  raises that floor further.

## Javadoc

`./gradlew javadoc` prints 16 warnings:
- `DirUtils.copy(File, File)` and `DirUtils.isJava7Avail()` have no doc at all.
- `DirUtils.rename` has a bare `@throws IOException`.
- `copy(InputStream, …)` overloads lack `@throws IOException`.
- `FileSystemSpi.rename` lacks `@param source`.
- empty `<p></p>` tags throughout `DirUtils`.

Worth one pass over all public docs with the `writing-javadoc` skill, not just the warnings.

## Minor code issues

- `DirUtils.get()` isn't synchronized, while `set()` is; `SPI` is not volatile. `get()` reads
  `SPI` twice, so a `set()` racing it can make `get()` return `null`. Make `get()` synchronized,
  or read `SPI` once into a local.
- `rename` across mount points copies with `copy(File, File)`, which fails on a directory
  (`FileInputStream` on a dir). The README says "moving a file", so maybe just document it.
- `Q_canon_cache`: `DumbJavaFS.isSymlink` uses canonical paths; on JDK 8–11 the JVM caches them
  (`sun.io.useCanonCaches`, default on until JDK 12) for ~30 s, so a symlink created or
  removed meanwhile can be misreported. On a JVM `Java7FS` is picked anyway, so it only bites
  a forced `DumbJavaFS`; does Android's libcore cache canonical paths too?

## Release notes for 2.2

Things a 2.1 user notices:
- Recursive delete no longer follows symlinks (it used to delete the target's contents), and
  a dangling symlink is now deleted instead of being skipped.
- `FileSystemSpi` gained `isSymlink(File)`: a custom backend passed to `DirUtils.set()` must
  implement it.
- slf4j 1.7 → 2.0: an app with a 1.7-style binding gets a no-op logger for this library.
- Java 7 → 8 bytecode; Kotlin metadata 1.4 → 2.0 (see `Q_kotlin_metadata`).
- Every failure now names its culprit and reason, on both backends.
