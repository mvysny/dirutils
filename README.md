# DirUtils: Android File and Directory utilities which suck less

Tired of `File.mkdir()` and `File.mkdirs()` and `File.delete()` not throwing `IOException`,
but returning a non-informative `false` instead? Try this library. Also supports basic stuff
like deleting a directory recursively, moving a file (even across different mount-points).

For Java usage, see the `DirUtils` class.
For Kotlin usage, see `DirUtilsKt` class which adds useful extension methods
to built-in `File` class.

## Using with your project

The library is present on Maven Central, simply add the library as
a Maven dependency `com.github.mvysny.dirutils:dirutils:2.0` to your project.

# Developing

Please feel free to open bug reports to discuss new features; PRs are welcome as well :)

## Building from scratch

Head to dirutils root and type
```sh
$ ./gradlew
```

That will build the project and run all tests.

## Releasing

To release the library to Maven Central:

1. Edit `build.gradle.kts` and remove `-SNAPSHOT` in the `version=` stanza
2. Commit with the commit message of simply being the version being released, e.g. "1.2.13"
3. git tag the commit with the same tag name as the commit message above, e.g. `1.2.13`
4. `git push`, `git push --tags`
5. Run `./gradlew clean build publish`
6. Continue to the [OSSRH Nexus](https://oss.sonatype.org/#stagingRepositories) and follow the [release procedure](https://central.sonatype.org/pages/releasing-the-deployment.html).
7. Add the `-SNAPSHOT` back to the `version=` while increasing the version to something which will be released in the future,
   e.g. 1.2.14, then commit with the commit message "1.2.14-SNAPSHOT" and push.

# License

The MIT License (MIT).

See [LICENSE](LICENSE)
