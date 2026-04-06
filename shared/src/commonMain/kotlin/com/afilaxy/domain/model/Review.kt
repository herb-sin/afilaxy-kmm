package com.afilaxy.domain.model

data class Review(
    val id: String = "",
    val emergencyId: String,
    val reviewerId: String,
    val reviewedId: String,
    val rating: Int,           // 1–5
    val comment: String? = null,
    val timestamp: Long = 0L
)
