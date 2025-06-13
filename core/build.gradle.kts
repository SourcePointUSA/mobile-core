import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.0.21"
    kotlin("native.cocoapods") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("com.github.ben-manes.versions") version "0.51.0"
    id("com.android.library") version "8.7.3"
    id("com.github.gmazzo.buildconfig") version "5.5.0"
    id("maven-publish")
    id("signing")
}

val coreVersion = "0.1.7"
group = "com.sourcepoint"
version = coreVersion

val description = "The internal Network & Data layers used by our mobile SDKs"
val generatedSourcesPath = layout.buildDirectory.dir("generated").get()
val gitRepoUrl = "https://github.com/SourcePointUSA/mobile-core.git"
val deviceName = project.findProperty("iosDevice") as? String ?: "iPhone 15"

// this generates a kotlin file with constants that can be used inside the project
buildConfig {
    buildConfigField("Version", coreVersion)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    applyDefaultHierarchyTemplate()
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        publishLibraryVariants("release", "debug")
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    targets.withType<KotlinNativeTarget> {
        binaries.withType<Framework> {
            isStatic = true
        }
    }

    cocoapods {
        name = "SPMobileCore"
        summary = description
        homepage = gitRepoUrl
        license = "{ :type => 'APACHE 2' }"
        source = "{ :git => '$gitRepoUrl', :tag => '$coreVersion' }"
        authors = "André Herculano"
        version = coreVersion
        ios.deploymentTarget = "10.0"
        tvos.deploymentTarget = "10.0"
        framework {
            binaryOptions["bundleId"] = "com.sourcepoint.SPMobileCore"
            baseName = "SPMobileCore"
            transitiveExport = true
            isStatic = true
        }
    }

    sourceSets {
        val ktorVersion = "3.0.0"
        val coroutinesVersion = "1.9.0"
        val settingsVersion = "1.2.0"
        val dataTimeVersion = "0.6.1"
        val commonMain by getting {
            dependencies {
                implementation("com.russhwolf:multiplatform-settings-no-arg:$settingsVersion")
                implementation("com.russhwolf:multiplatform-settings-test:$settingsVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$dataTimeVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-client-logging:$ktorVersion")
            }
        }
        commonMain.kotlin.srcDir("$generatedSourcesPath/src/main/kotlin")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.russhwolf:multiplatform-settings-test:$settingsVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
                implementation("io.ktor:ktor-client-mock:$ktorVersion")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            }
        }
        val appleMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:$ktorVersion")
            }
        }
    }
}

android {
    namespace = "com.sourcepoint.mobile_core"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    packaging {
        resources.excludes += "DebugProbesKt.bin"
    }
}

tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

fun fromProjectOrEnv(key: String): String? = findProperty(key) as String? ?: System.getenv(key)

publishing {
    // These values should not be checked in to GitHub.
    // They should be stored in your ~/.gradle/gradle.properties
    // They can also be passed as environment variables
    val signingKey = fromProjectOrEnv("SIGNING_KEY")
    val signingPassword = fromProjectOrEnv("SIGNING_PASSWORD")
    val ossrhUsername = fromProjectOrEnv("OSSRH_USERNAME")
    val ossrhPassword = fromProjectOrEnv("OSSRH_PASSWORD")

    publications {
        withType<MavenPublication>().configureEach {
            pom {
                name = "SP Core Module"
                description = "The internal Network & Data layers used by our mobile SDKs"
                url = "https://github.com/SourcePointUSA/mobile-core"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "andresilveirah"
                        name = "Andre Herculano"
                        email = "andresilveirah@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:github.com/SourcePointUSA/mobile-core.git"
                    developerConnection = "scm:git:ssh://github.com/SourcePointUSA/mobile-core.git"
                    url = "https://github.com/SourcePointUSA/mobile-core/tree/main"
                }
            }
            signing {
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(this@configureEach)
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"

            val releasesRepoUrl = uri(project.findProperty("releasesRepoUrl") as String)
            val snapshotsRepoUrl = uri(findProperty("snapshotsRepoUrl") as String)
            url = if (coreVersion.contains("beta")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}
