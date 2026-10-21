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
| `org.junit:junit-bom` (replaced DynaTest) | 5.14.4 | 6.1.3 (staying on 5; why in `build.gradle.kts`) |

The release notes for 2.2 are in `CHANGELOG.md`.
