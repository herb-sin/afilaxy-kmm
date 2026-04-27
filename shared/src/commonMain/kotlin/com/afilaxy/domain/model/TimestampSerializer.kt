package com.afilaxy.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

import com.afilaxy.util.Logger

object TimestampSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.LONG)
    
    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }
    
    override fun deserialize(decoder: Decoder): Long {
        return try {
            // Tenta decodificar como Long primeiro
            decoder.decodeLong()
        } catch (e: Exception) {
            // Firestore Timestamp não é diretamente deserializável como Long — fallback seguro.
            // Exceção esperada durante migração de documentos antigos.
            Logger.d("TimestampSerializer", "fallback para 0L — ${e::class.simpleName}")
            0L
        }
    }
}
