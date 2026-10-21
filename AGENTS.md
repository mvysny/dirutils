# DirUtils

## What this is

Tired of `File.mkdir()` and `File.mkdirs()` and `File.delete()` not throwing `IOException`,
but returning a non-informative `false` instead? Try this library. Also supports basic stuff
like deleting a directory recursively, moving a file (even across different mount-points).

## Promises

- **Fail loudly, with a reason.** Every operation throws an `IOException` saying why it failed, never a bare `false`.
- **Runs on any Android version.** Java 8 bytecode (desugared by AGP 3.0+), no Android API, and nothing loads a class the device lacks; reasoned, not device-tested; see `D_runtime_spi`.
- **Adds nothing to an app's size.** `slf4j-api` is the only runtime dependency; Kotlin stdlib and Android are `compileOnly`.

## Design docs

| File | Owns | Loaded |
|---|---|---|
| `README.md` | the pitch, usage | — |
| `RELEASING.md` | the release procedure | — |
| `AGENTS.md` (this) | promises, invariants, the module map, conventions, commands | every turn |
| `design/decisions.md` | why this and not that — `D_` entries, FAQ-shaped | lazy |
| doc comments | what one symbol does and why it is shaped so | at the symbol |

Every fact lives in exactly one of these; the others link to it.

## Invariants

- **`java.nio.file` is referenced from `Java7FS` only.** Any other class touching it throws `VerifyError` on load on pre-Oreo Android. See `D_runtime_spi`.
- **Every mkdir, delete and rename goes through `DirUtils.get()`'s `FileSystemSpi`.** A direct `File` call returns a bare `false` and escapes the per-backend tests.

## Module map

- `sk.baka.android` — the public API: static helpers for Java, `File` extension functions for Kotlin.
- `sk.baka.android.spi` — the filesystem backend seam, its `java.nio.file` and plain-`File` implementations.
- `src/test/kotlin` — the test suites.

## Conventions

- **Java for the library, Kotlin for the `File` extensions and the tests**; both compile to 1.8 bytecode.
- **Tests: JUnit 5, every suite a `@ParameterizedClass` over the `FS` enum** so it runs once per backend.
- **Paths must be absolute**; a relative one throws `IllegalArgumentException`, to catch relative-path mistakes.
- **`*Quietly` variants log the failure through slf4j** instead of throwing; nothing else swallows an `IOException`.

## Commands

- `./gradlew` — `clean build`: compile, tests, then `design/verify_design_tripwires.sh` via `check`; what CI runs on push on JDK 8, 11, 17 and 21 (`.github/workflows/gradle.yml`).
- `./gradlew test --tests 'sk.baka.android.DirUtilsKtTest'` — one test class.
- Every `test` run writes JaCoCo coverage to `build/reports/jacoco/test/` (`html/index.html`, `jacocoTestReport.xml`).
- `mise.toml` pins the local build JDK to jetbrains-17; Gradle 8.x runs on JDK 8–24, not on a newer default.

## Maintenance of this file

Loaded every turn; cap 34 KB, a module's own `AGENTS.md` 10 KB. Over it, in this order:
delete what has no home — status, history, class lists, what the code already says; trim
each line to its fact plus one clause and send the explanation home — why →
`design/decisions.md`, how across symbols → `design/architecture.md`, how in one symbol →
its doc comment, what upstream does → `design/research.md`; only then a module's own
`AGENTS.md`, peripheral modules first, never the core. Never paraphrase a lazy entry into a
line here. `design/verify_design_tripwires.sh` checks the caps and the cites.
