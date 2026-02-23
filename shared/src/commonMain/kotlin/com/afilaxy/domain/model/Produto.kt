package com.afilaxy.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Produto(
    val id: String,
    val nome: String,
    val descricao: String,
    val preco: Double,
    val categoria: String,
    val imagemUrl: String = "",
    // Campos extras para exibição de detalhe
    val precoOriginal: String? = null,
    val desconto: String? = null,
    val cupom: String? = null,
    val validadeCupom: String? = null,
    val farmacia: String? = null
)
