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
