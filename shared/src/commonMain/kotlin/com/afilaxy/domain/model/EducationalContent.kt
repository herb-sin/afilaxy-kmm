package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EducationalContent(
    val id: String = "",
    val title: String = "",
    val category: ContentCategory = ContentCategory.RESCUE,
    val summary: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val author: String = "",
    val readTimeMinutes: Int = 5,
    val createdAt: Long = 0L,
    val isPremium: Boolean = false
)

@Serializable
enum class ContentCategory {
    RESCUE,        // Medicação de Resgate
    MAINTENANCE,   // Medicação de Manutenção
    SUS,           // Como conseguir no SUS
    FAQ            // Perguntas Frequentes
}
