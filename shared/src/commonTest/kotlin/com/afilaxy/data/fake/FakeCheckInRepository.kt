package com.afilaxy.data.fake

import com.afilaxy.domain.model.CheckInResponse
import com.afilaxy.domain.model.CheckInType
import com.afilaxy.domain.repository.CheckInRepository

class FakeCheckInRepository : CheckInRepository {

    private var savedCheckIns = mutableListOf<CheckInResponse>()
    private var todayCheckIn: CheckInResponse? = null
    private var rescueInhalerName: String? = null
    private var medicalContext: Triple<String?, String?, String?> = Triple(null, null, null)

    override suspend fun saveCheckIn(response: CheckInResponse): Result<Unit> {
        savedCheckIns.add(response)
        return Result.success(Unit)
    }

    override suspend fun getTodayCheckIn(userId: String, type: CheckInType): Result<CheckInResponse?> =
        Result.success(todayCheckIn)

    override suspend fun getRescueInhalerName(userId: String): Result<String?> =
        Result.success(rescueInhalerName)

    override suspend fun getAsmaTypeInfo(userId: String): Result<Pair<String?, String?>> =
        Result.success(Pair(medicalContext.second, medicalContext.third))

    override suspend fun getMedicalContext(userId: String): Result<Triple<String?, String?, String?>> =
        Result.success(medicalContext)

    override suspend fun getRecentCheckIns(userId: String, days: Int): Result<List<CheckInResponse>> =
        Result.success(savedCheckIns.toList())

    fun setTodayCheckIn(response: CheckInResponse?) { todayCheckIn = response }
    fun setRescueInhalerName(name: String?) { rescueInhalerName = name }
    fun setMedicalContext(inhaler: String?, asmaType: String?, severity: String?) {
        medicalContext = Triple(inhaler, asmaType, severity)
    }
    fun getSavedCount() = savedCheckIns.size
}
