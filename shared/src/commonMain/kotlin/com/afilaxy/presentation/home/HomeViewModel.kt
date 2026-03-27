package com.afilaxy.presentation.home

import com.afilaxy.domain.model.*
import com.afilaxy.domain.repository.*
import com.afilaxy.util.TimeUtils
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmm.viewmodel.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val isLoading: Boolean = false,
    val feedPosts: List<SocialPost> = emptyList(),
    val communityStats: CommunityStats? = null,
    val quickActions: List<QuickAction> = emptyList(),
    val airQuality: AirQuality? = null,
    val selectedTab: FeedTab = FeedTab.APOIO,
    val error: String? = null
)

enum class FeedTab {
    APOIO, RECENTES, DESTAQUES
}

class HomeViewModel(
    private val socialRepository: SocialRepository,
    private val emergencyRepository: EmergencyRepository,
    private val locationRepository: LocationRepository
) : KMMViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeState()
    )

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            try {
                val quickActions = socialRepository.getQuickActions()
                _state.value = _state.value.copy(quickActions = quickActions)
                
                loadFeedPosts()
                
                socialRepository.getCommunityStats().collect { stats ->
                    _state.value = _state.value.copy(communityStats = stats)
                }
                
                loadAirQuality()
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    private fun loadFeedPosts() {
        viewModelScope.coroutineScope.launch {
            val postType = when (_state.value.selectedTab) {
                FeedTab.APOIO -> PostType.SUPPORT
                FeedTab.RECENTES -> null
                FeedTab.DESTAQUES -> PostType.TIP
            }
            
            socialRepository.getFeedPosts(postType).collect { posts ->
                _state.value = _state.value.copy(
                    feedPosts = posts,
                    isLoading = false
                )
            }
        }
    }

    private fun loadAirQuality() {
        viewModelScope.coroutineScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                location?.let {
                    val airQuality = socialRepository.getAirQuality("${it.latitude},${it.longitude}")
                    airQuality.onSuccess { quality ->
                        _state.value = _state.value.copy(airQuality = quality)
                    }
                }
            } catch (e: Exception) {
                // Air quality is optional
            }
        }
    }

    fun selectTab(tab: FeedTab) {
        _state.value = _state.value.copy(selectedTab = tab)
        loadFeedPosts()
    }

    fun likePost(postId: String, userId: String) {
        viewModelScope.coroutineScope.launch {
            socialRepository.likePost(postId, userId)
        }
    }

    fun requestHelp() {
        viewModelScope.coroutineScope.launch {
            try {
                val location = locationRepository.getCurrentLocation()
                location?.let {
                    val emergency = Emergency(
                        id = "",
                        userId = "",
                        userName = "User",
                        location = it,
                        description = "Preciso de ajuda com bombinha de resgate",
                        timestamp = TimeUtils.currentTimeMillis()
                    )
                    emergencyRepository.createEmergency(emergency)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}