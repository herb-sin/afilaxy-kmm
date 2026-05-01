package com.afilaxy.data.fake

import com.afilaxy.domain.repository.NotificationRepository

/**
 * Fake implementation of NotificationRepository for testing
 */
class FakeNotificationRepository : NotificationRepository {

    var notifyHelperAcceptedCallCount = 0
    var notifyNewMessageCallCount = 0
    var notifyEmergencyResolvedCallCount = 0

    override suspend fun notifyHelperAccepted(emergencyId: String, userId: String) {
        notifyHelperAcceptedCallCount++
    }

    override suspend fun notifyNewMessage(emergencyId: String, recipientId: String, senderName: String) {
        notifyNewMessageCallCount++
    }

    override suspend fun notifyEmergencyResolved(emergencyId: String, userId: String) {
        notifyEmergencyResolvedCallCount++
    }

    fun resetCounts() {
        notifyHelperAcceptedCallCount = 0
        notifyNewMessageCallCount = 0
        notifyEmergencyResolvedCallCount = 0
    }
}
