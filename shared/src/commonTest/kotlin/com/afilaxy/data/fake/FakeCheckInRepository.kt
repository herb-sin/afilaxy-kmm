package com.afilaxy.data.fake

import com.afilaxy.domain.model.CheckInResponse
import com.afilaxy.domain.model.CheckInType
import com.afilaxy.domain.repository.CheckInRepository

class FakeCheckInRepository : CheckInRepository {

    private var savedCheckIns = mutableListOf<CheckInResponse>()
    private var todayCheckIn: CheckInResponse? = null

    override suspend fun saveCheckIn(response: CheckInResponse): Result<Unit> {
        savedCheckIns.add(response)
        return Result.success(Unit)
    }

    override suspend fun getTodayCheckIn(userId: String, type: CheckInType): Result<CheckInResponse?> =
        Result.success(todayCheckIn)

    override suspend fun getRecentCheckIns(userId: String, days: Int): Result<List<CheckInResponse>> =
        Result.success(savedCheckIns.toList())

    fun setTodayCheckIn(response: CheckInResponse?) { todayCheckIn = response }
    fun getSavedCount() = savedCheckIns.size
}
