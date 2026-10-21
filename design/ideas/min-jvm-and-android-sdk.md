# Minimum JVM and Android SDK

Which bytecode level, build JDK and Android version do we promise from 2.2 on? The only known
consumers are the owner's own apps: Java 17, minSdk 21, Kotlin 2.4.x. Aim: keep Java 8
bytecode if it costs nothing; else go all the way to Java 17 + SDK 21.

## Where we are

| | 2.1 (released) | 2.2-SNAPSHOT |
|---|---|---|
| Java bytecode | 7 | 8 |
| Kotlin | jvmTarget 1.6, metadata 1.4 | jvmTarget 1.8, metadata 2.0 |
| slf4j-api | 1.7.30 | 2.0.13 |
| Android API called | none (compiled against API 16 stubs) | none |

The library calls no Android API and no JDK API past 7; `java.nio.file` (API 26) is only
touched from `Java7FS`, behind the probe (`D_runtime_spi`). So the Android floor is set by the
*toolchain*, not by any API: Java 8 bytecode needs D8 desugaring (AGP 3.0+, 2017) for
minSdk < 24; slf4j-api 2.0's default interface methods are desugared the same way.

## Findings

- **Kotlin 2.4 still compiles to Java 8, and still runs on JDK 8** (verified 2026-10-21): a copy
  bumped to Kotlin 2.4.21 on Gradle 8.14.6, run on Temurin 8u504: `clean build` green,
  158 tests pass, classes are major 52, `kotlin.Metadata` `mv=[2,4,0]`. KGP 2.4.21's Gradle
  module metadata declares `org.gradle.jvm.version: 8` and plugin variants from Gradle 8.0
  up to 9.6. The bump needs `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_1_8 } }` in
  place of the `tasks.withType<KotlinCompile> { kotlinOptions… }` block.
- **Java 17 bytecode buys only syntax** (records, switch expressions) across five small
  classes; JDK 11+ *APIs* aren't on Android without core-library desugaring anyway.
- **minSdk 21 buys nothing by itself**: `java.nio.file` still needs 26, so `DumbJavaFS` and
  the two-backend tests stay. It does mean ART only, so the Dalvik `VerifyError` that
  `D_runtime_spi` guards against can't happen (the guard is free, keep it).
- **minSdk 26 would be the real prize** (delete `DumbJavaFS`, the SPI, half the tests), but
  the apps aren't there.

## Proposal

Java 8 bytecode, stay on Gradle 8 (runs on JDK 8, so CI keeps one JDK per job, no
toolchain split), and promise *any Android version*, given a toolchain that desugars Java 8
(AGP 3.0+).

## Open questions

- `Q_android_claim`: "any Android version" is reasoned, not tested: nothing runs on a device
  or emulator; `DumbJavaFS` under `withAllFS` on a JVM is the stand-in for pre-Oreo. Good
  enough to promise? Wording for the **Runs on old Android** promise and the README.
- `Q_kotlin_metadata`: the jar's Kotlin metadata follows the compiler — 2.0 now, 2.4 after a
  bump — and a Kotlin consumer's compiler reads metadata at most one minor ahead, so a
  2.4-built jar needs Kotlin 2.3+ to call the `File` extensions. The apps are on 2.4, so no
  problem for them; pin `apiVersion` / `languageVersion` lower for anyone else? Java-only
  consumers don't care.
- `Q_slf4j_android` (answered): the apps log through
  [slf4j-handroid](https://github.com/mvysny/slf4j-handroid), which supports slf4j
  2.0.20, so slf4j-api is now 2.0.20. Left for the release notes: `org.slf4j:slf4j-android`
  stops at 1.7.36, so an app logging through it gets a no-op logger and `*Quietly` failures
  vanish silently.
- `Q_release_flag`: the "publish with JDK ≤ 11" guard in `build.gradle.kts` (and RELEASING's
  `mise exec java@temurin-11`) could go: set `options.release = 8` and Kotlin's
  `-Xjdk-release=1.8` when the build JDK is newer than 8 (javac 8 has no `--release`), so a
  JDK 17 publish links against the JDK 8 API. Or simply publish from JDK 8.
- `Q_gradle8_eol`: staying on Gradle 8 holds while KGP ships a Gradle 8 variant and runs on
  JDK 8. When either stops: Gradle 9 on JDK 17 with `--release 8`, and CI runs the build on
  17+ only (the JDK 8/11 jobs only test `Java7FS` on JVMs nobody runs this on).
- `Q_android_os`: on API 21–25, `android.system.Os` throws `ErrnoException` with the real
  errno, and `Os.lstat` tells a symlink apart; a backend on it would replace `DumbJavaFS`'s
  guessed reasons (and `Q_canon_cache`) on those devices. Costs: no API 21 stub on Central
  (`com.google.android:android` stops at 4.1.1.4), and it can't be tested on a JVM. Park it as
  its own idea?
