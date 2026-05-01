package com.afilaxy.domain.repository

interface NotificationRepository {
    // notifyNearbyHelpers was removed: it performed an O(n) full users collection scan
    // exposing all helper FCM tokens and locations to the client. The Cloud Function
    // onEmergencyCreated handles this server-side with geohash bounds (no client exposure).
    suspend fun notifyHelperAccepted(emergencyId: String, userId: String)
    suspend fun notifyNewMessage(emergencyId: String, recipientId: String, senderName: String)
    suspend fun notifyEmergencyResolved(emergencyId: String, userId: String)
}
