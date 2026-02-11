package com.afilaxy.domain.repository

interface NotificationRepository {
    suspend fun notifyNearbyHelpers(emergencyId: String, latitude: Double, longitude: Double)
    suspend fun notifyHelperAccepted(emergencyId: String, userId: String)
    suspend fun notifyNewMessage(emergencyId: String, recipientId: String, senderName: String)
    suspend fun notifyEmergencyResolved(emergencyId: String, userId: String)
}
