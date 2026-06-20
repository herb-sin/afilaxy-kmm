package com.afilaxy.domain.repository

import com.afilaxy.domain.model.HealthSnapshot

interface HealthRepository {
    /** Verdadeiro se a plataforma suporta leitura de dados de saúde. */
    fun isAvailable(): Boolean

    /** Verdadeiro se as permissões necessárias já foram concedidas. */
    suspend fun hasPermissions(): Boolean

    /**
     * Retorna snapshot de saúde ou null se não disponível / sem permissão.
     * [isEvening] = true lê dados do dia; false lê dados da noite anterior.
     */
    suspend fun getSnapshot(isEvening: Boolean): HealthSnapshot?

    /**
     * Solicita permissões ao usuário.
     * Android: no-op (permissão é solicitada via ActivityResultLauncher no CheckInScreen).
     * iOS: exibe diálogo nativo do HealthKit.
     * Retorna true se permissão foi concedida.
     */
    suspend fun requestPermissions(): Boolean
}
