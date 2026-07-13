package com.afilaxy.data.repository

import com.afilaxy.domain.model.HealthSnapshot
import com.afilaxy.domain.repository.HealthRepository

// HealthKit em standby — paridade com Android (Health Connect também pausado).
// Para reativar: restaurar implementação completa com HKHealthStore.
class IosHealthRepository : HealthRepository {
    override fun isAvailable(): Boolean = false
    override suspend fun hasPermissions(): Boolean = false
    override suspend fun requestPermissions(): Boolean = false
    override suspend fun getSnapshot(isEvening: Boolean): HealthSnapshot? = null
}
