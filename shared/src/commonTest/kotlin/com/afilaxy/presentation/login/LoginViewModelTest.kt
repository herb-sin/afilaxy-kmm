package com.afilaxy.presentation.login

import com.afilaxy.data.fake.FakeAuthRepository
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
class LoginViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        viewModel = LoginViewModel(authRepository)
    }
    
    @Test
    fun `initial state should be empty`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isLoggedIn)
    }
    
    @Test
    fun `onEmailChange should update email in state`() = runTest(testDispatcher) {
        viewModel.onEmailChange("test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals("test@example.com", viewModel.state.value.email)
    }
    
    @Test
    fun `onPasswordChange should update password in state`() = runTest(testDispatcher) {
        viewModel.onPasswordChange("password123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals("password123", viewModel.state.value.password)
    }
    
    @Test
    fun `onLogin should login successfully with valid credentials`() = runTest(testDispatcher) {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertTrue(state.isLoggedIn)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }
    
    @Test
    fun `onLogin should not login with empty email`() = runTest(testDispatcher) {
        viewModel.onPasswordChange("password123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertFalse(state.isLoggedIn)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("email") || state.error!!.contains("Email"))
    }
    
    @Test
    fun `onLogin should not login with empty password`() = runTest(testDispatcher) {
        viewModel.onEmailChange("test@example.com")
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertFalse(state.isLoggedIn)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("senha") || state.error!!.contains("Senha"))
    }
    
    @Test
    fun `onLogin should set error when repository fails`() = runTest(testDispatcher) {
        authRepository.setShouldSucceed(false)
        
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.onLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertFalse(state.isLoggedIn)
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }
    
    @Test
    fun `clearError should remove error from state`() = runTest(testDispatcher) {
        authRepository.setShouldSucceed(false)
        
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onLoginClick()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error exists
        assertNotNull(viewModel.state.value.error)
        
        // Clear error
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertNull(viewModel.state.value.error)
    }
}
