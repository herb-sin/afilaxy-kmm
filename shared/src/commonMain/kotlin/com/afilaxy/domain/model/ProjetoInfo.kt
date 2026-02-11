package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjetoInfo(
    val id: String = "",
    val titulo: String,
    val texto: String,
    val imageUrl: String? = null,
    val link: String? = null,
    val categoria: String? = null
)
