import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    // Gradle 8, because it runs on JDK 8 and CI builds there. When Kotlin drops Gradle 8 or
    // JDK 8, move to Gradle 9 and JDK 17+: by then no sensible phone runs an Android that can't.
    kotlin("jvm") version "2.4.21"
    `maven-publish`
    signing
    jacoco
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

defaultTasks("clean", "build")

group = "com.github.mvysny.dirutils"
version = "2.2-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    // stay on 1.8 to retain compatibility with Android. No apiVersion pin: a Kotlin consumer
    // needs a compiler at most one minor behind ours to read the File extensions' metadata.
    compilerOptions { jvmTarget = JvmTarget.JVM_1_8 }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.google.android:android:4.1.1.4")
    implementation("org.slf4j:slf4j-api:2.0.20")
    compileOnly(kotlin("stdlib"))
    // don't add any further dependencies, to keep Android app's size at check.

    testImplementation("org.slf4j:slf4j-simple:2.0.20")
    // JUnit 5, not 6: JUnit 6 needs JDK 17, and CI runs the tests on JDK 8 and 11 too.
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// following https://dev.to/kengotoda/deploying-to-ossrh-with-gradle-in-2020-1lhi
java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Javadoc> {
    isFailOnError = false
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class.java).apply {
            groupId = project.group.toString()
            this.artifactId = "dirutils"
            version = project.version.toString()
            pom {
                description.set("DirUtils: Android File and Directory utilities which do not suck")
                name.set("DirUtils")
                url.set("https://github.com/mvysny/dirutils")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("mavi")
                        name.set("Martin Vysny")
                        email.set("martin@vysny.me")
                    }
                }
                scm {
                    url.set("https://github.com/mvysny/dirutils")
                }
            }
            from(components["java"])
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

nexusPublishing {
    repositories {
        // see https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // to see the exceptions of failed tests in the CI console.
        exceptionFormat = TestExceptionFormat.FULL
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

val verifyDesignTripwires by tasks.registering(Exec::class) {
    commandLine("design/verify_design_tripwires.sh")
}
tasks.check { dependsOn(verifyDesignTripwires) }

// Kotlin binds calls against the JDK it compiles on: on JDK 17 it can pick a JDK 9+ member
// (a stream method, String.lines()) over the stdlib one, and the jar no longer runs on
// JDK <= 11 or Android. Not JDK 8 either: publishing from it to Maven Central is untested.
if (JavaVersion.current() > JavaVersion.VERSION_11 && gradle.startParameter.taskNames.contains("publish")) {
    throw GradleException("Release this library with JDK 11, to ensure Android compatibility; current JDK is ${JavaVersion.current()}")
}

