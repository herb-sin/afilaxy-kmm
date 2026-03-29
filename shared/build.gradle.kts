plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
}

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
        commonMain.dependencies {
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
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            implementation("app.cash.turbine:turbine:1.0.0")
        }

        androidMain.dependencies {
            implementation("io.insert-koin:koin-android:3.5.0")
            implementation("com.google.android.gms:play-services-location:21.3.0")
        }

        // iosMain é criado automaticamente pelo Kotlin 2.x Default Hierarchy Template.
        // Não é necessário declarar dependsOn() manualmente.
    }
}

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
}
