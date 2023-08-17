import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.9.0"
    `maven-publish`
    signing
}

defaultTasks("clean", "build")

group = "com.github.mvysny.dirutils"
version = "2.2-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"  // stay on 1.8 to retain compatibility with Android
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.google.android:android:4.1.1.4")
    implementation("org.slf4j:slf4j-api:2.0.7")
    compileOnly(kotlin("stdlib"))
    // don't add any further dependencies, to keep Android app's size at check.

    testImplementation("org.slf4j:slf4j-simple:2.0.7")
    testImplementation("com.github.mvysny.dynatest:dynatest:0.24")
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
    repositories {
        maven {
            setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.properties["ossrhUsername"] as String? ?: "Unknown user"
                password = project.properties["ossrhPassword"] as String? ?: "Unknown user"
            }
        }
    }
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

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        // to see the exceptions of failed tests in the CI console.
        exceptionFormat = TestExceptionFormat.FULL
    }
}

if (JavaVersion.current() > JavaVersion.VERSION_11 && gradle.startParameter.taskNames.contains("publish")) {
    throw GradleException("Release this library with JDK 11 or lower, to ensure Android compatibility; current JDK is ${JavaVersion.current()}")
}

