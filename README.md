# DirUtils: Android File and Directory utilities which suck less

Tired of `File.mkdir()` and `File.mkdirs()` and `File.delete()` not throwing `IOException`,
but returning a non-informative `false` instead? Try this library. Also supports basic stuff
like deleting a directory recursively, moving a file (even across different mount-points).

For Java usage, see the `DirUtils` class.
For Kotlin usage, see `DirUtilsKt` class which adds useful extension methods
to built-in `File` class.

## Using with your project

The library is present on Maven Central, simply add the library as
a Maven dependency `com.github.mvysny.dirutils:dirutils:2.1` to your project.

It runs on JDK 8+ and on any Android version: the jar is Java 8 bytecode, which the Android
Gradle Plugin 3.0+ desugars for old devices. Calling the Kotlin extensions needs a Kotlin
compiler at most one minor version behind the one the library was built with.

The library logs through slf4j 2.0. On Android, add an slf4j 2.0 provider such as
[slf4j-handroid](https://github.com/mvysny/slf4j-handroid), otherwise the log output goes
nowhere; `org.slf4j:slf4j-android` has no 2.x.

## Examples

Java:

```java
File thumbnails = new File(context.getCacheDir(), "thumbnails");
DirUtils.mkdirs(thumbnails);       // like `mkdir -p`; throws IOException instead of returning false
DirUtils.rename(new File(thumbnails, "a.tmp"), new File(thumbnails, "a.png"));  // copies+deletes across mount points
DirUtils.deleteNonRec(thumbnails); // java.io.IOException: Failed to delete '/data/.../thumbnails': '/data/.../thumbnails' is not empty
DirUtils.deleteRecursively(thumbnails);  // like `rm -rf`
```

Kotlin:

```kotlin
val downloads = File(context.filesDir, "downloads/2026")
downloads.mkdirp()                              // like `mkdir -p`
File(downloads, "old.zip").rmf()                // deletes a file or an empty directory
println(downloads.calculateLengthRecursively()) // size in bytes, directories included
downloads.rmrf()                                // like `rm -rf`
downloads.rmrfq()                               // like `rm -rf`, but logs a failure instead of throwing
```

Pass absolute paths: the mkdir and delete functions throw `IllegalArgumentException` on a
relative `File`, to catch the mistake of resolving it against an unexpected working directory.

# Developing

Please feel free to open bug reports to discuss new features; PRs are welcome as well :)

## Building from scratch

Head to dirutils root and type
```sh
$ ./gradlew
```

That will build the project and run all tests.

## Releasing

See [RELEASING.md](RELEASING.md).

# License

The MIT License (MIT).

See [LICENSE](LICENSE)
