package com.afilaxy.domain.model

data class Evento(
    val id: String,
    val titulo: String,
    val descricao: String,
    val data: String,
    val local: String,
    val imagemUrl: String = ""
)
