package com.afilaxy.presentation.chat

import app.cash.turbine.test
import com.afilaxy.data.fake.FakeAuthRepository
import com.afilaxy.data.fake.FakeChatRepository
import com.afilaxy.domain.model.ChatMessage
import com.afilaxy.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var chatRepository: FakeChatRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: ChatViewModel
    
    private val testEmergencyId = "test-emergency-123"
    private val testUser = User(
        uid = "test-user-id",
        email = "test@example.com",
        name = "Test User",
        fcmToken = null,
        isHelper = false
    )
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        chatRepository = FakeChatRepository()
        authRepository = FakeAuthRepository(currentUser = testUser)
        viewModel = ChatViewModel(
            emergencyId = testEmergencyId,
            chatRepository = chatRepository,
            authRepository = authRepository
        )
    }
    
    @Test
    fun `initial state should have emergencyId and currentUserId`() = runTest(testDispatcher) {
        val state = viewModel.state.value
        
        assertEquals(testEmergencyId, state.emergencyId)
        assertEquals(testUser.uid, state.currentUserId)
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.currentMessage)
        assertNull(state.error)
    }
    
    @Test
    fun `onMessageChange should update currentMessage in state`() = runTest(testDispatcher) {
        viewModel.onMessageChange("Hello, World!")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertEquals("Hello, World!", state.currentMessage)
    }
    
    @Test
    fun `sendMessage should send message successfully`() = runTest(testDispatcher) {
        val messageText = "Test message"
        
        viewModel.sendMessage(messageText)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertNull(state.error)
        assertEquals(1, chatRepository.getMessageCount())
    }
    
    @Test
    fun `sendMessage should not send blank message`() = runTest(testDispatcher) {
        viewModel.sendMessage("   ")
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertEquals(0, chatRepository.getMessageCount())
    }
    
    @Test
    fun `sendMessage should set error when user not authenticated`() = runTest(testDispatcher) {
        authRepository.setCurrentUser(null)
        val newViewModel = ChatViewModel(
            emergencyId = testEmergencyId,
            chatRepository = chatRepository,
            authRepository = authRepository
        )
        
        newViewModel.sendMessage("Test message")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = newViewModel.state.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("não autenticado"))
    }
    
    @Test
    fun `sendMessage should set error when repository fails`() = runTest(testDispatcher) {
        chatRepository.setShouldSucceed(false)
        
        viewModel.sendMessage("Test message")
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.state.value
        assertNotNull(state.error)
    }
    
    @Test
    fun `clearError should remove error from state`() = runTest(testDispatcher) {
        chatRepository.setShouldSucceed(false)
        viewModel.sendMessage("Test message")
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error exists
        assertNotNull(viewModel.state.value.error)
        
        // Clear error
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertNull(viewModel.state.value.error)
    }
    
    @Test
    fun `should observe messages from repository`() = runTest(testDispatcher) {
        val testMessages = listOf(
            ChatMessage(
                id = "1",
                emergencyId = testEmergencyId,
                senderId = testUser.uid,
                senderName = testUser.name ?: "",
                message = "Message 1",
                timestamp = 1000L,
                isFromHelper = false
            ),
            ChatMessage(
                id = "2",
                emergencyId = testEmergencyId,
                senderId = "other-user",
                senderName = "Other User",
                message = "Message 2",
                timestamp = 2000L,
                isFromHelper = true
            )
        )
        
        chatRepository.setMessages(testMessages)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.messages.size)
            assertEquals("Message 1", state.messages[0].message)
            assertEquals("Message 2", state.messages[1].message)
        }
    }
}
