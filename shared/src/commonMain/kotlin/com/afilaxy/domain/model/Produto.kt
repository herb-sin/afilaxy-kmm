package com.afilaxy.domain.model

data class Produto(
    val id: String,
    val nome: String,
    val descricao: String,
    val preco: Double,
    val categoria: String,
    val imagemUrl: String = ""
)
