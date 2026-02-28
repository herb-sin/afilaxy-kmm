package com.afilaxy.domain.repository

import com.afilaxy.domain.model.Location
import com.afilaxy.domain.model.UBS

interface UBSRepository {
    suspend fun getNearby(location: Location, radiusKm: Double = 10.0): List<UBS>
    suspend fun getById(id: String): UBS?
}
