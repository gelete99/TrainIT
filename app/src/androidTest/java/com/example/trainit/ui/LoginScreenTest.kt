package com.example.trainit.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.trainit.ui.theme.TrainITTheme
import com.example.trainit.ui.theme.screens.LoginScreen
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_muestra_elementos_principales() {
        composeTestRule.setContent {
            TrainITTheme {
                LoginScreen(
                    onGoToRegister = {},
                    onLoginSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Iniciar sesión").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nombre de usuario").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entrar").assertIsDisplayed()
    }
}