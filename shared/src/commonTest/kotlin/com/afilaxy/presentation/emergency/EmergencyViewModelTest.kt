package com.afilaxy.presentation.emergency

import com.afilaxy.data.fake.FakeAuthRepository
import com.afilaxy.data.fake.FakeEmergencyRepository
import com.afilaxy.data.fake.FakeLocationRepository
import com.afilaxy.data.fake.FakeNotificationRepository
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
class EmergencyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var emergencyRepository: FakeEmergencyRepository
    private lateinit var locationRepository: FakeLocationRepository
    private lateinit var notificationRepository: FakeNotificationRepository
    private lateinit var viewModel: EmergencyViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        emergencyRepository = FakeEmergencyRepository()
        locationRepository = FakeLocationRepository()
        notificationRepository = FakeNotificationRepository()
        viewModel = EmergencyViewModel(emergencyRepository, locationRepository, notificationRepository)
    }

    @Test
    fun `initial state should be empty when no active emergency`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.hasActiveEmergency)
        assertNull(state.emergencyId)
        assertNull(state.error)
        assertFalse(state.isLoading)
        assertFalse(state.isCreatingEmergency)
    }

    @Test
    fun `init should detect existing active emergency`() = runTest(testDispatcher) {
        // Create fresh VM with an active emergency already set
        emergencyRepository.setActiveEmergencyId("existing-id")
        val vm = EmergencyViewModel(emergencyRepository, locationRepository, notificationRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.hasActiveEmergency)
        assertEquals("existing-id", state.emergencyId)
    }

    @Test
    fun `onCreateEmergency should succeed and set hasActiveEmergency`() = runTest(testDispatcher) {
        viewModel.onCreateEmergency()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.hasActiveEmergency)
        assertEquals("test-emergency-id", state.emergencyId)
        assertFalse(state.isCreatingEmergency)
        assertNull(state.error)
    }

    @Test
    fun `onCreateEmergency should notify nearby helpers on success`() = runTest(testDispatcher) {
        viewModel.onCreateEmergency()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, notificationRepository.notifyNearbyHelpersCallCount)
    }

    @Test
    fun `onCreateEmergency should set error when location is null`() = runTest(testDispatcher) {
        locationRepository.setCurrentLocation(null)

        viewModel.onCreateEmergency()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.hasActiveEmergency)
        assertFalse(state.isCreatingEmergency)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("localização"))
    }

    @Test
    fun `onCreateEmergency should set error when repository fails`() = runTest(testDispatcher) {
        emergencyRepository.setShouldSucceed(false)

        viewModel.onCreateEmergency()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.hasActiveEmergency)
        assertFalse(state.isCreatingEmergency)
        assertNotNull(state.error)
    }

    @Test
    fun `onCancelEmergency should clear emergency state`() = runTest(testDispatcher) {
        // First create an emergency
        viewModel.onCreateEmergency()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.hasActiveEmergency)

        // Then cancel
        viewModel.onCancelEmergency()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.hasActiveEmergency)
        assertNull(state.emergencyId)
        assertNull(state.error)
    }

    @Test
    fun `onCancelEmergency should do nothing when no emergency id`() = runTest(testDispatcher) {
        // No emergency set — onCancelEmergency should return early
        viewModel.onCancelEmergency()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, emergencyRepository.cancelEmergencyCallCount)
    }

    @Test
    fun `onToggleHelperMode enable should set isHelperMode to true`() = runTest(testDispatcher) {
        viewModel.onToggleHelperMode(enable = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isHelperMode)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `onToggleHelperMode enable should set error when location is null`() = runTest(testDispatcher) {
        locationRepository.setCurrentLocation(null)

        viewModel.onToggleHelperMode(enable = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isHelperMode)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("localização"))
    }

    @Test
    fun `onToggleHelperMode enable should set error when repository fails`() = runTest(testDispatcher) {
        emergencyRepository.setShouldSucceed(false)

        viewModel.onToggleHelperMode(enable = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isHelperMode)
        assertNotNull(state.error)
    }

    @Test
    fun `onToggleHelperMode disable should set isHelperMode to false`() = runTest(testDispatcher) {
        // First enable
        viewModel.onToggleHelperMode(enable = true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.isHelperMode)

        // Then disable
        viewModel.onToggleHelperMode(enable = false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isHelperMode)
    }

    @Test
    fun `onAcceptEmergency should set hasActiveEmergency to true`() = runTest(testDispatcher) {
        viewModel.onAcceptEmergency("emergency-123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.hasActiveEmergency)
        assertEquals("emergency-123", state.emergencyId)
        assertFalse(state.isLoading)
    }

    @Test
    fun `onAcceptEmergency should set error when repository fails`() = runTest(testDispatcher) {
        emergencyRepository.setShouldSucceed(false)

        viewModel.onAcceptEmergency("emergency-123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.hasActiveEmergency)
        assertNotNull(state.error)
    }

    @Test
    fun `onResolveEmergency should clear emergency state`() = runTest(testDispatcher) {
        // First create an emergency
        viewModel.onCreateEmergency()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onResolveEmergency()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.hasActiveEmergency)
        assertNull(state.emergencyId)
    }

    @Test
    fun `clearError should remove error from state`() = runTest(testDispatcher) {
        locationRepository.setCurrentLocation(null)
        viewModel.onCreateEmergency()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.error)
    }
}
