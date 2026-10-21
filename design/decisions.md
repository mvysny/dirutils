# Decisions

Why this project is the way it is and not otherwise — FAQ-shaped: each entry is a question and
its current answer. Rewrite the answer when it changes; delete the entry when nobody asks any
more. An entry is earned by what it would cost to reverse — half the code base — or by research
the next person would otherwise redo (cited as its `R_`). Not an entry: windows → panels
"because that's the trend", this red over that red, `get_foo` over `is_foo?`, the testing library,
the CI host, a version bump — a comment at the site of the choice, or nothing; nothing about
`design/` itself. Cite by slug, `D_<slug>`, never by position; `grep '^## D_' design/decisions.md`
is the index. The first entry is the ruler: every later one trims to its length — which is how
long this file gets, so keep it short. When you have written an entry, re-read it against the one
above, open the doc comments it touches and cut what they already say, then cut the fat.

---

## D_runtime_spi — Why pick the filesystem backend at runtime rather than call `java.nio.file` directly?

`java.nio.file.Files` says why a mkdir or delete failed and can move atomically, which is the
whole point of the library; `java.io.File` answers `false`. But Android has `java.nio.file`
only from API 26 (Oreo), and **Runs on any Android version** promises the devices before it. So every
mutation goes through `FileSystemSpi`, chosen once on first use: `Java7FS` where
`java.nio.file.Files` loads, else `DumbJavaFS`, which guesses its messages from what `File`
can still see — the path is a file, the parent is not writable. The probe sits in `DirUtils`,
not in `Java7FS`: on Android 4.4 merely loading a class that references `java.nio.file` throws
`VerifyError`. Why not NIO only: it drops every pre-Oreo device. Why not `File` only: its bare
`false` is what the library exists to replace. The cost we carry: two implementations of every
operation, so every test runs under both.

## D_absolute_paths — Why throw on a relative path, even from `*Quietly`, rather than resolve it?

Nobody chooses the working directory a relative path would resolve against: on a JVM it is
wherever the app was launched from, on Android usually `/`. So `File("cache")` points somewhere
the caller did not mean, and for `deleteRecursively` that wipes the wrong tree. A relative path
is a programming error, not an I/O failure: it fails on the first run, every run. That is why
even the `*Quietly` variants throw it. They exist for best-effort cleanup, swallowing what the
caller can't prevent (a busy file, a missing permission). Swallowed, the bug would become a log
line while the cleanup silently never happens. The cost we carry: callers write
`File(context.cacheDir, "x")`, never `File("x")`.
