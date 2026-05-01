import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

// ── WAQI API Token (lido de local.properties ou env var) ─────────────────────
val waqiToken: String = run {
    System.getenv("WAQI_API_TOKEN")?.takeIf { it.isNotBlank() }
        ?: rootProject.file("local.properties").let { file ->
            if (file.exists()) {
                val props = Properties().apply { file.inputStream().use { load(it) } }
                props.getProperty("WAQI_API_TOKEN", "")
            } else ""
        }
}

// Gera WaqiConfig.kt na fase de configuração (antes de qualquer compilação)
val waqiDir = layout.buildDirectory.dir("generated/waqi/com/afilaxy/config").get().asFile
waqiDir.mkdirs()
// Split token into parts so R8 cannot inline it as a single string literal.
// This is a defence-in-depth measure; the definitive fix is routing WAQI calls
// through a Firebase Function so no token reaches the client binary at all.
val tokenParts = if (waqiToken.isNotBlank()) {
    val mid = waqiToken.length / 2
    Pair(waqiToken.substring(0, mid), waqiToken.substring(mid))
} else Pair("", "")

waqiDir.resolve("WaqiConfig.kt").writeText(
    """
    |package com.afilaxy.config
    |
    |/** Auto-generated — NÃO editar manualmente. Veja shared/build.gradle.kts */
    |object WaqiConfig {
    |    private val p1 = "${tokenParts.first}"
    |    private val p2 = "${tokenParts.second}"
    |    val API_TOKEN: String get() = if (p1.isNotBlank()) p1 + p2 else "demo"
    |}
    """.trimMargin()
)

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Shared code for Afilaxy"
        homepage = "https://github.com/herb-sin/afilaxy"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/waqi"))
            dependencies {
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                // Firebase KMM
                implementation("dev.gitlive:firebase-auth:2.1.0")
                implementation("dev.gitlive:firebase-firestore:2.1.0")

                // Settings
                implementation("com.russhwolf:multiplatform-settings:1.1.1")

                // Koin DI
                implementation("io.insert-koin:koin-core:3.5.0")

                // KMM-ViewModel
                implementation("com.rickclephas.kmm:kmm-viewmodel-core:1.0.0-ALPHA-16")

                // Ktor HTTP client (OpenMeteo + WAQI)
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            implementation("app.cash.turbine:turbine:1.0.0")
        }

        androidMain.dependencies {
            implementation("io.insert-koin:koin-android:3.5.0")
            implementation("com.google.android.gms:play-services-location:21.3.0")
            // Ktor engine Android
            implementation("io.ktor:ktor-client-okhttp:2.3.7")
        }

        iosMain.dependencies {
            // Ktor engine iOS
            implementation("io.ktor:ktor-client-darwin:2.3.7")
        }

    } // sourceSets
} // kotlin

android {
    namespace = "com.afilaxy.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}
