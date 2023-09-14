import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.signing
import java.util.*

plugins {
    `maven-publish`
    signing
}

// Stub secrets to let the project sync and build without the publication values set up
ext["signing.keyId"] = null
ext["signing.key"] = null
ext["signing.password"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null


// Grabbing secrets from local.properties file or from environment variables, which could be used on CI
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
} else {
    ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.key"] = System.getenv("SIGNING_KEY")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

fun getExtraString(name: String) = ext[name]?.toString()

publishing {
    // Configure maven central repository
    repositories {
        maven {
            name = "sonatype"
            setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = getExtraString("ossrhUsername")
                password = getExtraString("ossrhPassword")
            }
        }
    }

    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        artifact(javadocJar.get())

        // Provide artifacts information requited by Maven Central
        pom {
            name.set("Trap Library for Android")
            description.set("Touch and sensor data collector Android library for the Cursor Insight trap server.")
            url.set("https://github.com/cursorinsight/ci-trap-android")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("mtolmacs")
                    name.set("Mark Tolmacs")
                    email.set("mark@lazycat.hu")
                }
                developer {
                    id.set("gbence")
                    name.set("Bence Golda")
                    email.set("bence@cursorinsight.com")
                }
                developer {
                    id.set("denessapi")
                    name.set("Denes Sapi")
                    email.set("denes@cursorinsight.com")
                }
            }
            scm {
                url.set("https://github.com/cursorinsight/ci-trap-android")
            }
        }
    }
}

// Signing artifacts. Signing.* extra properties values will be used
signing {
    val signingKeyId = findProperty("signing.keyId") as String
    val signingKey = findProperty("signing.key") as String
    val signingPassword = findProperty("signing.password") as String
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)
}