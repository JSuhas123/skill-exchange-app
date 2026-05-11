package com.example.skillexchange.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.skillexchange.data.repository.AuthRepository
import com.example.skillexchange.utils.Resource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import com.google.firebase.auth.FirebaseUser

/**
 * Unit tests for AuthViewModel
 */
class AuthViewModelTest {

    @Mock
    private lateinit var authRepository: AuthRepository

    @Mock
    private lateinit var firebaseUser: FirebaseUser

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = AuthViewModel(authRepository)
    }

    @Test
    fun testInitializeAuthWithValidSession() = runTest {
        whenever(authRepository.isSessionValid()).thenReturn(true)
        whenever(authRepository.currentUser).thenReturn(firebaseUser)

        viewModel.initializeAuth()

        assertTrue(viewModel.isAuthenticated.first())
    }

    @Test
    fun testInitializeAuthWithInvalidSession() = runTest {
        whenever(authRepository.isSessionValid()).thenReturn(false)
        whenever(authRepository.recoverSession())
            .thenReturn(Result.success(firebaseUser))

        viewModel.initializeAuth()

        assertTrue(viewModel.isAuthenticated.first())
    }

    @Test
    fun testSignOut() = runTest {
        whenever(authRepository.signOut()).thenReturn(Result.success(Unit))

        viewModel.signOut()

        assertFalse(viewModel.isAuthenticated.first())
    }

    @Test
    fun testErrorHandling() = runTest {
        val errorMessage = "Auth failed"
        whenever(authRepository.signInAnonymously())
            .thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.signInAnonymously()

        val error = viewModel.error.first()
        assertNotNull(error)
        assertTrue(error?.contains(errorMessage) ?: false)
    }

    @Test
    fun testLoadingState() = runTest {
        // Should start as not loading
        assertFalse(viewModel.isLoading.first())
    }
}

/**
 * Unit tests for CreatePostViewModel
 */
class CreatePostViewModelTest {

    @Mock
    private lateinit var postRepository: PostRepository

    private lateinit var viewModel: CreatePostViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = CreatePostViewModel(postRepository)
    }

    @Test
    fun testCreatePostWithValidData() = runTest {
        val skillRequired = "Photography"
        val skillOffered = "Web Development"
        val description = "This is a valid description for skill exchange"

        whenever(postRepository.createPost(any()))
            .thenReturn(Result.success(Unit))

        viewModel.createPost(skillRequired, skillOffered, description)

        val uiState = viewModel.uiState.first()
        assertTrue(uiState is Resource.Success)
    }

    @Test
    fun testCreatePostWithInvalidSkill() = runTest {
        val skillRequired = "X" // Too short
        val skillOffered = "Web Development"
        val description = "Valid description"

        viewModel.createPost(skillRequired, skillOffered, description)

        val errors = viewModel.validationErrors.first()
        assertTrue(errors.containsKey("skillRequired"))
    }

    @Test
    fun testCreatePostWithShortDescription() = runTest {
        val skillRequired = "Photography"
        val skillOffered = "Web Development"
        val description = "ABC" // Too short

        viewModel.createPost(skillRequired, skillOffered, description)

        val errors = viewModel.validationErrors.first()
        assertTrue(errors.containsKey("description"))
    }

    @Test
    fun testResetState() = runTest {
        viewModel.resetState()

        val uiState = viewModel.uiState.first()
        assertNull(uiState)

        val errors = viewModel.validationErrors.first()
        assertTrue(errors.isEmpty())
    }
}

/**
 * Unit tests for SwapViewModel
 */
class SwapViewModelTest {

    @Mock
    private lateinit var swapRepository: SwapRepository

    private lateinit var viewModel: SwapViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = SwapViewModel(swapRepository)
    }

    @Test
    fun testInitiateSwapWithValidData() = runTest {
        whenever(swapRepository.createSwap(any()))
            .thenReturn(Result.success(Unit))

        viewModel.initiateSwap(
            userB = "user123",
            skillA = "Photography",
            skillB = "Web Development",
            hours = 50
        )

        val actionStatus = viewModel.actionStatus.first()
        assertTrue(actionStatus is Resource.Success)
    }

    @Test
    fun testInitiateSwapWithInvalidHours() = runTest {
        viewModel.initiateSwap(
            userB = "user123",
            skillA = "Photography",
            skillB = "Web Development",
            hours = 169 // Too many
        )

        val errors = viewModel.validationErrors.first()
        assertTrue(errors.containsKey("hours"))
    }

    @Test
    fun testDuplicateActionPrevention() = runTest {
        viewModel.addPendingAction("swap123")
        assertTrue(viewModel.isActionPending("swap123"))

        viewModel.removePendingAction("swap123")
        assertFalse(viewModel.isActionPending("swap123"))
    }
}
