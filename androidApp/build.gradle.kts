plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.afilaxy.app"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.afilaxy.app"
        minSdk = 23
        targetSdk = 35

        // Leitura única do local.properties para todo o defaultConfig
        val properties = org.jetbrains.kotlin.konan.properties.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }

        // versionCode lido de VERSION_CODE em local.properties ou variável de ambiente CI.
        // toLongOrNull() + range check garante valor válido sem overflow (CWE-190).
        val rawVersionCodeStr = System.getenv("VERSION_CODE")
            ?: properties.getProperty("VERSION_CODE")
            ?: "1" // fallback para builds locais
        val rawVersionCodeLong = rawVersionCodeStr.trim().toLongOrNull()
            ?: error("VERSION_CODE não é um número válido: '$rawVersionCodeStr'")
        require(rawVersionCodeLong in 1L..2_100_000_000L) {
            "VERSION_CODE deve estar entre 1 e 2100000000, recebido: $rawVersionCodeStr"
        }
        versionCode = rawVersionCodeLong.toInt()
        versionName = "2.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Maps API Key Android: env var (CI) > local.properties (local) > placeholder
        val mapsApiKey = System.getenv("MAPS_API_KEY_ANDROID")
            ?: properties.getProperty("MAPS_API_KEY_ANDROID")?.takeIf { it.isNotBlank() }
            ?: run {
                println("⚠️  MAPS_API_KEY_ANDROID não encontrada — mapa não funcionará neste build")
                "MISSING_MAPS_API_KEY"
            }
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Inclui símbolos de debug nativos no AAB para stack traces legíveis
            // no painel de Crashes & ANRs do Play Console.
            ndk {
                debugSymbolLevel = "FULL"
            }
            
            // Signing config from local.properties
            val properties = org.jetbrains.kotlin.konan.properties.Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { properties.load(it) }
                val keystorePath = properties.getProperty("KEYSTORE_FILE")
                val keystorePassword = properties.getProperty("KEYSTORE_PASSWORD")
                val releaseKeyAlias = properties.getProperty("KEY_ALIAS")
                val keyPassword = properties.getProperty("KEY_PASSWORD")
                
                if (keystorePath != null && keystorePassword != null && releaseKeyAlias != null && keyPassword != null) {
                    signingConfig = signingConfigs.create("release") {
                        storeFile = file(keystorePath)
                        storePassword = keystorePassword
                        keyAlias = releaseKeyAlias
                        this.keyPassword = keyPassword
                    }
                }
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    lint {
        disable.add("MissingTranslation")
        disable.add("ExtraTranslation")
        abortOnError = true
        // Suppress noisy warnings that aren't actionable yet, but keep security checks active
        warning += "UnusedResources"
    }
    
    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {
    // Shared KMM module
    implementation(project(":shared"))
    
    // Compose BOM — atualizado 2025-03 (era 2024.06.00)
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    
    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // Schedule, FilterList, etc.
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.activity.compose)
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Material3 Adaptive — NOVO
    implementation("androidx.compose.material3.adaptive:adaptive:1.1.0")
    implementation("androidx.compose.material3.adaptive:adaptive-layout:1.1.0")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.1.0")
    implementation("androidx.window:window:1.3.0")

    // Coil — carregamento de imagens (AsyncImage)
    implementation("io.coil-kt:coil-compose:2.6.0")

    
    // Navigation Compose — atualizado 2025-03 (era 2.7.7)
    implementation("androidx.navigation:navigation-compose:2.8.9")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    
    // Koin — atualizado 2025-03 (era 3.5.0)
    implementation("io.insert-koin:koin-android:3.5.6")
    implementation("io.insert-koin:koin-androidx-compose:3.5.6")
    
    // KMM ViewModel — ALPHA-16 mantido: migração para 2.x requer refactor de API (tech debt)
    implementation("com.rickclephas.kmm:kmm-viewmodel-core:1.0.0-ALPHA-16")
    
    // Firebase (Android specific) — BOM atualizado 2025-03 (era 33.0.0)
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    
    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // WorkManager — agendamento de check-ins matinal e noturno
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    
    // Firebase Analytics
    implementation("com.google.firebase:firebase-analytics-ktx")
    
    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
