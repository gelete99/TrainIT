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
class AuthViewModelRegisterTest {

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
    fun register_con_contrasenas_distintas_muestra_error() = runTest {
        val fakeRepo = mockk<AuthRepository>(relaxed = true)
        val viewModel = AuthViewModel(fakeRepo)

        viewModel.onUsernameChange("gelete")
        viewModel.onEmailChange("gelete@test.com")
        viewModel.onPasswordChange("123456")
        viewModel.onConfirmPasswordChange("654321")

        viewModel.register {}

        advanceUntilIdle()

        assertEquals(
            "Las contraseñas no coinciden",
            viewModel.uiState.value.errorMessage
        )
    }
}