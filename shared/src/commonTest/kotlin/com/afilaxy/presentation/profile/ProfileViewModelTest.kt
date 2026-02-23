package com.afilaxy.presentation.profile

import com.afilaxy.data.fake.FakeAuthRepository
import com.afilaxy.data.fake.FakeProfileRepository
import com.afilaxy.domain.model.EmergencyContact
import com.afilaxy.domain.model.User
import com.afilaxy.domain.model.UserHealthData
import com.afilaxy.domain.model.UserProfile
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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: ProfileViewModel

    private val testUser = User(
        uid = "test-uid",
        email = "test@example.com",
        name = "Test User",
        fcmToken = null,
        isHelper = false
    )

    private val testProfile = UserProfile(
        uid = "test-uid",
        name = "Test User",
        email = "test@example.com",
        phone = "11999999999"
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = FakeProfileRepository(currentProfile = testProfile)
        authRepository = FakeAuthRepository(currentUser = testUser)
        viewModel = ProfileViewModel(profileRepository, authRepository)
    }

    @Test
    fun `loadProfile should load existing profile from repository`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.profile)
        assertEquals("test-uid", state.profile!!.uid)
        assertEquals("Test User", state.profile!!.name)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadProfile should create default profile when repository returns null`() = runTest(testDispatcher) {
        profileRepository.setCurrentProfile(null)
        val vm = ProfileViewModel(profileRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.profile)
        assertEquals("test-uid", state.profile!!.uid)
        assertEquals("Test User", state.profile!!.name)
        assertEquals("test@example.com", state.profile!!.email)
    }

    @Test
    fun `loadProfile should set error when not authenticated`() = runTest(testDispatcher) {
        authRepository.setCurrentUser(null)
        val vm = ProfileViewModel(profileRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("autenticado") || state.error!!.contains("Usuário"))
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadProfile should set error when repository fails`() = runTest(testDispatcher) {
        profileRepository.setShouldSucceed(false)
        val vm = ProfileViewModel(profileRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun `updateProfile should update profile and show success message`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedProfile = testProfile.copy(phone = "11988888888")
        viewModel.updateProfile(updatedProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(updatedProfile, state.profile)
        assertNotNull(state.successMessage)
        assertFalse(state.isSaving)
        assertNull(state.error)
    }

    @Test
    fun `updateProfile should set error when repository fails`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        profileRepository.setShouldSucceed(false)
        viewModel.updateProfile(testProfile.copy(phone = "11977777777"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.error)
        assertFalse(state.isSaving)
        assertNull(state.successMessage)
    }

    @Test
    fun `updateHealthData should update health data and show success`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val healthData = UserHealthData(bloodType = "O+", allergies = listOf("Penicilina"))
        viewModel.updateHealthData(healthData)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(healthData, state.profile?.healthData)
        assertNotNull(state.successMessage)
        assertFalse(state.isSaving)
        assertEquals(1, profileRepository.updateHealthDataCallCount)
    }

    @Test
    fun `updateEmergencyContact should update contact and show success`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val contact = EmergencyContact(name = "Maria", phone = "11966666666", relationship = "Mãe")
        viewModel.updateEmergencyContact(contact)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(contact, state.profile?.emergencyContact)
        assertNotNull(state.successMessage)
        assertFalse(state.isSaving)
        assertEquals(1, profileRepository.updateEmergencyContactCallCount)
    }

    @Test
    fun `clearMessages should clear error and successMessage`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger a success to set successMessage
        viewModel.updateProfile(testProfile.copy(phone = "11955555555"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.successMessage)

        viewModel.clearMessages()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.error)
        assertNull(state.successMessage)
    }
}
