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

You can then add Maven dependency for `sk.baka.android:dirutils:1.7` to your project.

Afterwards, download the native part of the library here: http://www.baka.sk/maven2/sk/baka/android/dirutils/ (search for
`dirutils-*-native.zip`) and unpack it to your Android project. Done!

## Building from scratch

You will need to install Android NDK and add the `ndk-build` script to your PATH. Then, head to dirutil's root and type
```sh
$ ./gradlew
```

