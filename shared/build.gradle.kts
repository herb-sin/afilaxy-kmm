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
            // Coroutines — 1.10.1 resolve incompatibilidade com iOS 26 + Kotlin/Native 2.x
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

            // Serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

            // DateTime
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

            // Firebase KMM — 2.7.0 é compatível com coroutines 1.10+
            implementation("dev.gitlive:firebase-auth:2.7.0")
            implementation("dev.gitlive:firebase-firestore:2.7.0")

            // Settings
            implementation("com.russhwolf:multiplatform-settings:1.3.0")

            // Koin DI — 3.6.0 compatível com Kotlin 2.x
            implementation("io.insert-koin:koin-core:3.6.0")

            // KMM-ViewModel — ALPHA-22 compatível com koin 3.6 e coroutines 1.10
            implementation("com.rickclephas.kmm:kmm-viewmodel-core:1.0.0-ALPHA-22")
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
            implementation("app.cash.turbine:turbine:1.1.0")
        }

        androidMain.dependencies {
            implementation("io.insert-koin:koin-android:3.6.0")
            implementation("com.google.android.gms:play-services-location:21.3.0")
        }

        // iosMain — intermediário que agrega os 3 targets iOS
        // Nota: em Kotlin 2.x com acessores new-style (commonMain.dependencies {}),
        // commonMain é NamedDomainObjectProvider e precisa de .get() na referência direta.
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

@Suppress("UnstableApiUsage")
configure<com.android.build.api.dsl.LibraryExtension> {
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
