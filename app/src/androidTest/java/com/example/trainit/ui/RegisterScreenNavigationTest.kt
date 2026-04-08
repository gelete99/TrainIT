package com.example.trainit.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.trainit.ui.theme.TrainITTheme
import com.example.trainit.ui.theme.screens.RegisterScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterScreenNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun registerScreen_al_pulsar_ya_tengo_cuenta_llama_a_onGoToLogin() {
        var navigatedToLogin = false

        composeTestRule.setContent {
            TrainITTheme {
                RegisterScreen(
                    onGoToLogin = { navigatedToLogin = true },
                    onRegisterSuccess = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Ya tengo cuenta").performClick()

        assertTrue(navigatedToLogin)
    }
}