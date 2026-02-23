package com.afilaxy.presentation.history

import com.afilaxy.data.fake.FakeAuthRepository
import com.afilaxy.data.fake.FakeEmergencyRepository
import com.afilaxy.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var emergencyRepository: FakeEmergencyRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: HistoryViewModel

    private val testUser = User(
        uid = "test-uid",
        email = "test@example.com",
        name = "Test User",
        fcmToken = null,
        isHelper = false
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        emergencyRepository = FakeEmergencyRepository()
        authRepository = FakeAuthRepository(currentUser = testUser)
        viewModel = HistoryViewModel(emergencyRepository, authRepository)
    }

    @Test
    fun `loadHistory should populate history and filteredHistory`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.history.size)
        assertEquals(2, state.filteredHistory.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadHistory should set error when not authenticated`() = runTest(testDispatcher) {
        authRepository.setCurrentUser(null)
        val vm = HistoryViewModel(emergencyRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("autenticado") || state.error!!.contains("Usuário"))
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadHistory should set error when repository fails`() = runTest(testDispatcher) {
        emergencyRepository.setShouldSucceed(false)
        val vm = HistoryViewModel(emergencyRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.error)
        assertTrue(state.history.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `applyFilter ALL should return all history items`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.applyFilter(HistoryFilter.ALL)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(state.history.size, state.filteredHistory.size)
        assertEquals(HistoryFilter.ALL, state.selectedFilter)
    }

    @Test
    fun `applyFilter RESOLVED should return only resolved emergencies`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.applyFilter(HistoryFilter.RESOLVED)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.filteredHistory.all { it.status == "resolved" })
        assertEquals(1, state.filteredHistory.size)
        assertEquals(HistoryFilter.RESOLVED, state.selectedFilter)
    }

    @Test
    fun `applyFilter CANCELLED should return only cancelled emergencies`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.applyFilter(HistoryFilter.CANCELLED)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.filteredHistory.all { it.status == "cancelled" })
        assertEquals(1, state.filteredHistory.size)
        assertEquals(HistoryFilter.CANCELLED, state.selectedFilter)
    }

    @Test
    fun `applyFilter AS_REQUESTER should return emergencies where user is requester`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.applyFilter(HistoryFilter.AS_REQUESTER)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.filteredHistory.all { it.requesterId == testUser.uid })
        assertEquals(1, state.filteredHistory.size)
    }

    @Test
    fun `applyFilter AS_HELPER should return emergencies where user is helper`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.applyFilter(HistoryFilter.AS_HELPER)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.filteredHistory.all { it.helperId == testUser.uid })
        assertEquals(1, state.filteredHistory.size)
    }
}
