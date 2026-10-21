# Before releasing 2.2

Loose ends found in the pre-release check (2026-10-21), to sort out before cutting 2.2.

## Dependencies

Latest stable as of 2026-10-21 (`maven-versions.rb latest`):

| What | Now | Latest |
|---|---|---|
| Kotlin plugin `org.jetbrains.kotlin.jvm` | 2.4.21 (bumped) | 2.4.21 |
| `org.slf4j:slf4j-api` / `slf4j-simple` | 2.0.20 (bumped) | 2.0.20 |
| Gradle wrapper | 8.14.6 | 9.8.2 (staying on 8; why in `build.gradle.kts`) |
| `io.github.gradle-nexus.publish-plugin` | 2.0.0 | 2.0.0 (current) |
| `com.github.mvysny.dynatest:dynatest` | 0.25 | 0.25 (current) |


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
  a forced `DumbJavaFS`; does Android's libcore cache canonical paths too? (The parked
  `android-os-errno-backend.md` would sidestep it on API 21–25.)

## Release notes for 2.2

Things a 2.1 user notices:
- Recursive delete no longer follows symlinks (it used to delete the target's contents), and
  a dangling symlink is now deleted instead of being skipped.
- `FileSystemSpi` gained `isSymlink(File)`: a custom backend passed to `DirUtils.set()` must
  implement it.
- slf4j 1.7 → 2.0: an app with a 1.7-style binding gets a no-op logger for this library;
  `org.slf4j:slf4j-android` has no 2.x, [slf4j-handroid](https://github.com/mvysny/slf4j-handroid) does.
- Java 7 → 8 bytecode: still any Android version, but the app's build needs AGP 3.0+.
- Kotlin metadata 1.4 → 2.4: calling the `File` extensions needs Kotlin 2.3+.
- Every failure now names its culprit and reason, on both backends.
