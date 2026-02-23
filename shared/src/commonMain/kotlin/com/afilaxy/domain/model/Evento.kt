package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Evento(
    val id: String,
    val titulo: String,
    val descricao: String,
    val data: String,
    val local: String,
    val imagemUrl: String = "",
    // Campos extras para exibição de detalhe
    val organizador: String? = null,
    val horario: String? = null
)
