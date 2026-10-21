# Changelog

What a user of the previous release notices, newest release first.

## 2.2 — unreleased

- Recursive delete no longer follows symlinks (it used to delete the target's contents), and
  a dangling symlink is now deleted instead of being skipped.
- `FileSystemSpi` gained `isSymlink(File)`, and its `rename` now does the whole move, across
  mount points too, returning nothing: a custom backend passed to `DirUtils.set()` must follow.
- `rename` and `copy(File, File)` now behave like `Files.move` / `Files.copy`:
  - Across mount points, `rename` moves an empty directory, the modification time, and (on
    Android 8.0+) a symlink as itself. A non-empty directory still fails, now with a clear reason.
  - `copy` copies a directory as an empty one and replaces a target symlink instead of writing
    through it. It no longer leaves a half-written target behind.
  - Neither destroys the file any more when the target is a second path to the source.
- `copy(InputStream, OutputStream)` now throws when the final `flush()` fails, or the output's
  `close()` after a successful copy; it used to only log them. A failed copy is no longer flushed.
- slf4j 1.7 → 2.0: an app with a 1.7-style binding gets a no-op logger for this library;
  `org.slf4j:slf4j-android` has no 2.x, [slf4j-handroid](https://github.com/mvysny/slf4j-handroid) does.
- Java 7 → 8 bytecode: still any Android version, but the app's build needs AGP 3.0+.
- Kotlin metadata 1.4 → 2.4: calling the `File` extensions needs Kotlin 2.3+.
- Every failure now names its culprit and reason, on both backends.
