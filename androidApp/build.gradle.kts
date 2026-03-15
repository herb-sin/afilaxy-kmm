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
        // BigInteger parseia sem overflow por construção; intValueExact() lança ArithmeticException
        // se o valor não couber em Int, eliminando CWE-190 sem nenhuma conversão implícita.
        val rawVersionCodeStr = System.getenv("VERSION_CODE")
            ?: properties.getProperty("VERSION_CODE")
            ?: error("VERSION_CODE não definido em local.properties nem como variável de ambiente")
        val versionCodeBig = runCatching { java.math.BigInteger(rawVersionCodeStr) }
            .getOrElse { error("VERSION_CODE não é um número válido: '$rawVersionCodeStr'") }
        require(versionCodeBig >= java.math.BigInteger.ONE) {
            "VERSION_CODE deve ser >= 1, recebido: $rawVersionCodeStr"
        }
        versionCode = runCatching { versionCodeBig.intValueExact() }
            .getOrElse { error("VERSION_CODE excede o limite máximo permitido para versionCode Android: '$rawVersionCodeStr'") }
        versionName = "2.1.0-kmm"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Maps API Key from local.properties
        val mapsApiKey = properties.getProperty("MAPS_API_KEY") ?: "YOUR_MAPS_API_KEY_HERE"
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
    
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    
    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // Schedule, FilterList, etc.
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.activity.compose)
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Coil — carregamento de imagens (AsyncImage)
    implementation("io.coil-kt:coil-compose:2.6.0")

    
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")
    
    // Koin
    implementation("io.insert-koin:koin-android:3.5.0")
    implementation("io.insert-koin:koin-androidx-compose:3.5.0")
    
    // KMM ViewModel
    implementation("com.rickclephas.kmm:kmm-viewmodel-core:1.0.0-ALPHA-16")
    
    // Firebase (Android specific)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    
    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")
    
    // Firebase Analytics
    implementation("com.google.firebase:firebase-analytics-ktx")
    
    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
