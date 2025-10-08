import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "2.1.21"
    kotlin("native.cocoapods") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.android.library") version "8.11.1"
    id("com.github.gmazzo.buildconfig") version "5.6.8"
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("signing")
}

val coreVersion = "0.1.13"
group = "com.sourcepoint"
version = coreVersion

val description = "The internal Network & Data layers used by our mobile SDKs"
val generatedSourcesPath = layout.buildDirectory.dir("generated").get()
val gitRepoUrl = "https://github.com/SourcePointUSA/mobile-core.git"

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
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
            isStatic = true
            transitiveExport = true
        }
    }

    sourceSets {
        val ktorVersion = "3.2.2"
        val coroutinesVersion = "1.10.2"
        val settingsVersion = "1.3.0"
        val dataTimeVersion = "0.6.2"
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
    compileSdk = 36
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
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

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    signAllPublications()

    coordinates(group.toString(), "core", version.toString())

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
}
