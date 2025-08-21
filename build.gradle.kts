plugins {
    java
    alias(libs.plugins.kotlin)
    `java-gradle-plugin`
    `maven-publish`
}

group = "me.owdding"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("meowdding-gradle") {
            id = "me.owdding.gradle"
            implementationClass = "me.owdding.gradle.MeowddingGradlePlugin"
        }
    }
}

repositories {
    maven("https://maven.teamresourceful.com/repository/maven-private/")
    maven(url = "https://maven.msrandom.net/repository/root")
    maven(url = "https://maven.msrandom.net/repository/cloche")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("net.msrandom:minecraft-codev-fabric:0.6.4-1") {
        version { strictly("0.6.4-1") }
    }

    compileOnly(libs.terrarium.cloche)

    implementation("net.msrandom:minecraft-codev-core:0.6.2")
    implementation("net.msrandom:minecraft-codev-runs:0.6.4")
    implementation("net.msrandom:minecraft-codev-includes:0.6.0")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.2.0-2.0.2")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        // Gradle plugin development plugin automatically creates publications
        afterEvaluate {
            named<MavenPublication>("pluginMaven") {
                pom {
                    name.set("meowdding-gradle")
                    url.set("https://github.com/meowdding/meowdding-gradle")

                    scm {
                        connection.set("git:https://github.com/meowdding/meowdding-gradle.git")
                        developerConnection.set("git:https://github.com/meowdding/meowdding-gradle.git")
                        url.set("https://github.com/meowdding/meowdding-gradle")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            setUrl("https://maven.teamresourceful.com/repository/thatgravyboat/")
            credentials {
                username = System.getenv("MAVEN_USER") ?: providers.gradleProperty("maven_username").orNull
                password = System.getenv("MAVEN_PASS") ?: providers.gradleProperty("maven_password").orNull
            }
        }
    }
}
