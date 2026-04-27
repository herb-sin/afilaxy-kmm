buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
    }
}

plugins {
    // Trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidApplication).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinAndroid).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.kotlinCocoapods).apply(false)
    alias(libs.plugins.detekt)
}

// ── Detekt — Análise Estática Kotlin ──────────────────────────────────────────
detekt {
    // Arquivo de configuração com as regras customizadas
    config.setFrom(files("$rootDir/detekt.yml"))
    // Inclui regras padrão do Detekt além das customizadas
    buildUponDefaultConfig = true
    // Analisa todos os módulos KMM: shared + androidApp
    source.setFrom(
        files(
            "shared/src/commonMain/kotlin",
            "shared/src/androidMain/kotlin",
            "androidApp/src/main/kotlin"
        )
    )
    // Não falha o build em violations — apenas gera relatório (mudar para true no CI)
    ignoreFailures = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)   // relatório visual
        xml.required.set(false)   // para integração CI futura (SonarQube etc.)
        txt.required.set(false)
    }
}

