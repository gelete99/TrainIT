package com.example.trainit.ui.theme.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trainit.R
import com.example.trainit.auth.AuthViewModel
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.layout.ContentScale

@Composable
fun RegisterScreen(
    onGoToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val vm: AuthViewModel = viewModel()
    val state by vm.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {

        // 🔵 Imagen arriba centrada
        Image(
            painter = painterResource(id = R.drawable.trainit_logo),
            contentDescription = "TrainIT Logo",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
                .size(180.dp),
            contentScale = ContentScale.Fit
        )

        // 🔵 Formulario centrado
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 60.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Crear cuenta",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = state.username,
                onValueChange = vm::onUsernameChange,
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.email,
                onValueChange = vm::onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = vm::onPasswordChange,
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = vm::onConfirmPasswordChange,
                label = { Text("Repetir contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { vm.register(onRegisterSuccess) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text("Registrarme")
            }

            TextButton(onClick = onGoToLogin) {
                Text("Ya tengo cuenta")
            }
        }
    }
}