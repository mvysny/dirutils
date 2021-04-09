# DirUtils: Android File and Directory utilities which suck less

Tired of `File.mkdir()` and `File.mkdirs()` and `File.delete()` not throwing `IOException`,
but returning a non-informative `false` instead? Try this library. Also supports basic stuff
like deleting a directory recursively, moving a file (even across different mount-points).

## Using with your project

First, add this repo to your pom.xml:
```xml
<project ...>
  ...
  <repositories>
    <repository>
      <id>baka.sk</id>
      <name>Baka</name>
      <url>http://www.baka.sk/maven2</url>
    </repository>
  </repositories>
</project>
```

Or this to your gradle file:
```
repositories {
    maven {
        url "http://www.baka.sk/maven2"
    }
}
```

You can then add Maven dependency for `sk.baka.android:dirutils:1.8` to your project.

## Building from scratch

Head to dirutil's root and type
```sh
$ ./gradlew
```
