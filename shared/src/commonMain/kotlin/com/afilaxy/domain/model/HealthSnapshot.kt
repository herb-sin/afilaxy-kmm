package com.afilaxy.domain.model

/**
 * Dados coletados de smartwatch via Health Connect (Android) ou HealthKit (iOS).
 * Todos os campos são opcionais — o app funciona sem smartwatch.
 */
data class HealthSnapshot(
    val avgHeartRateBpm: Int? = null,       // FC média nas últimas 24h
    val sleepDurationHours: Float? = null,  // duração do sono da última noite
    val sleepInterruptions: Int? = null,    // despertares detectados
    val minSpo2Percent: Float? = null,      // SpO₂ mínimo durante o sono (%)
    val avgRespiratoryRate: Float? = null   // FR média (incursões/min)
)
