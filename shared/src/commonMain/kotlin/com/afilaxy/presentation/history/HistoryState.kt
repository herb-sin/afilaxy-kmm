package com.afilaxy.presentation.history

import com.afilaxy.domain.model.EmergencyHistory
import kotlinx.serialization.Serializable

@Serializable
data class HistoryState(
    val history: List<EmergencyHistory> = emptyList(),
    val filteredHistory: List<EmergencyHistory> = emptyList(),
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class HistoryFilter {
    ALL, RESOLVED, CANCELLED, AS_REQUESTER, AS_HELPER
}
