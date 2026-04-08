package com.example.trainit.auth

import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_con_usuario_vacio_muestra_error() = runTest {
        val fakeRepo = mockk<AuthRepository>(relaxed = true)
        val viewModel = AuthViewModel(fakeRepo)

        viewModel.onUsernameChange("")
        viewModel.onPasswordChange("123456")
        viewModel.login {}

        advanceUntilIdle()

        assertEquals(
            "Introduce tu nombre de usuario",
            viewModel.uiState.value.errorMessage
        )
    }
}