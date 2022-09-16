import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.4.32"
    `maven-publish`
    signing
}

defaultTasks("clean", "build")

group = "com.github.mvysny.dirutils"
version = "2.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"  // retain compatibility with Android
    if (System.getProperty("kotlin.jdkHome") != null) {
        kotlinOptions.jdkHome = System.getProperty("kotlin.jdkHome")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.google.android:android:4.1.1.4")
    implementation("org.slf4j:slf4j-api:1.7.30")
    compileOnly(kotlin("stdlib"))
    // don't add any further dependencies, to keep Android app's size at check.

    testImplementation("org.slf4j:slf4j-simple:1.7.30")
    testImplementation("com.github.mvysny.dynatest:dynatest-engine:0.19") {
        // we need to use an older version of junit5 since 5.6.2 is not compatible with java 7
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
    }
    // we need to use an older version of junit5 since 5.6.2 is not compatible with java 7
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testImplementation("org.junit.platform:junit-platform-engine:1.5.2")
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
