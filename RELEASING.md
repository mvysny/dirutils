# Releasing

To release the library to Maven Central:

1. Edit `build.gradle.kts` and remove `-SNAPSHOT` in the `version=` stanza, e.g. "1.2.13"
2. Edit `README.md`: bump the version in the Maven dependency under "Using with your project" to match the release.
   Edit `CHANGELOG.md`: replace "unreleased" in the release's heading with today's date, e.g. `## 1.2.13 — 2026-10-21`
3. Run `mise exec java@temurin-11 -- ./gradlew clean build publish closeAndReleaseStagingRepositories`.
   Release with JDK 11; the build refuses `publish` on a newer JDK, and says why in `build.gradle.kts`.
4. (Optional) watch [Maven Central Publishing Deployments](https://central.sonatype.com/publishing/deployments) as the deployment is published.
5. Commit with the commit message of simply being the version being released, e.g. "1.2.13"
6. git tag the commit with the same tag name as the commit message above, e.g. `1.2.13`
7. `git push`, `git push --tags`
8. Add the `-SNAPSHOT` back to the `version=` while increasing the version to something which will be released in the future,
   e.g. 1.2.14-SNAPSHOT, then commit with the commit message "1.2.14-SNAPSHOT" and push.

Publishing needs `sonatypeUsername`, `sonatypePassword` and the `signing.*` properties in `~/.gradle/gradle.properties`.
