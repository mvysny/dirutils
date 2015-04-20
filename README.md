# DirUtils: Android File and Directory utilities which suck less

Tired of `File.mkdir()` and `File.mkdirs()` and `File.delete()` not throwing `IOException`,
but returning a non-informative `false` instead? Try this library. Also supports basic stuff
like deleting a directory recursively, moving a file (even across different mount-points).

## Using with your project

Not yet in Maven official repo, just clone the appropriate tag from git and type
```mvn clean install -P '!build-native'```
You can then add Maven dependency for `sk.baka.android:dirutils:1.0`.

Afterwards, download the native part of the library and unpack it to your Android project. Done!

## Building from scratch

You will need to install Android NDK and add the `ndk-build` script to your PATH. Then, head to dirutil's root and type
```mvn clean install```
