package com.afilaxy.data.repository

import com.afilaxy.domain.model.HealthSnapshot
import com.afilaxy.domain.repository.HealthRepository

// Health Connect em standby — dependência removida até CNPJ/conta organização aprovada.
// Para reativar: descomentar dependência no androidApp/build.gradle.kts e restaurar implementação.
class AndroidHealthRepository : HealthRepository {
    override fun isAvailable(): Boolean = false
    override suspend fun hasPermissions(): Boolean = false
    override suspend fun requestPermissions(): Boolean = false
    override suspend fun getSnapshot(isEvening: Boolean): HealthSnapshot? = null
}
