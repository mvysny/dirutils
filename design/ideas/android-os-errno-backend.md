# A `FileSystemSpi` on `android.system.Os` for API 21–25

**Parked (2026-10-21):** a good idea, but `DumbJavaFS` is good enough for now.

## Problem

On Android before API 26 there's no `java.nio.file`, so `DirUtils` falls back to
`DumbJavaFS` (`D_runtime_spi`), which *guesses* why a `File` call returned `false` from what
`File` can still see. Its `isSymlink` compares canonical paths, which JDK 8–11 cache
(`DumbJavaFS.isSymlink`); the API 21 and 24 emulators showed no such cache (2026-10-21), but
`Os.lstat` tells a symlink directly.

## Proposal

API 21 (Lollipop) added `android.system.Os`, thin wrappers over the POSIX calls, which throw
`ErrnoException` carrying the real `errno` and a message like
`rename failed: EXDEV (Cross-device link)`. A third backend, say `AndroidOsFS`, would use:
- `Os.rename`, `Os.mkdir(path, mode)`, `Os.remove` (or `Os.rmdir` / `Os.unlink`);
- `Os.lstat(path).st_mode` with `OsConstants.S_ISLNK` for `isSymlink`, no canonical paths.

Selection becomes: `Java7FS` where `java.nio.file.Files` loads, else `AndroidOsFS` where
`android.system.Os` loads, else `DumbJavaFS` (pre-21 and anything exotic). The same rule as
for `Java7FS` holds: only the probe in `DirUtils` may touch the class name, so a device without
`android.system.Os` never loads `AndroidOsFS`.

## Open questions

- `Q_os_compile`: how to compile against `android.system.Os`? `com.google.android:android` on
  Central stops at 4.1.1.4 (API 16). Options: reflection (no stub, but every call is a
  `Method.invoke` and errors unwrap from `InvocationTargetException`); or our own copy of the
  few stub classes (`Os`, `OsConstants`, `ErrnoException`, `StructStat`) in a compile-only
  source set, stripped from the jar.
- `Q_os_testing`: a JVM can't run the real `Os`. With the stub approach, the test classpath
  could supply a JVM implementation of the stub (on `java.nio.file`), so the backend runs
  under `withAllFS` — testing our errno mapping, not Android's libcore. Enough?
- `Q_os_mkdir_mode`: which mode for `Os.mkdir`? `0777` and let the umask trim it, like
  `File.mkdir()` does?
- `Q_os_cross_device`: `rename` across mount points: is `EXDEV` the trigger for the
  copy-and-delete fallback, instead of today's "rename failed, try copy"?
