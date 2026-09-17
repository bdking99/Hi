package com.example.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppTextField
import com.example.ui.components.PrimaryButton

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateLogin: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            focusManager.clearFocus()
            viewModel.resetState()
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        AppTextField(value = displayName, onValueChange = { displayName = it }, label = "Display Name")
        Spacer(modifier = Modifier.height(16.dp))
        
        AppTextField(value = username, onValueChange = { username = it }, label = "Username")
        Spacer(modifier = Modifier.height(16.dp))
        
        AppTextField(value = email, onValueChange = { email = it }, label = "Email or Phone")
        Spacer(modifier = Modifier.height(16.dp))
        
        AppTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        AppTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            visualTransformation = PasswordVisualTransformation()
        )
        
        if (uiState is AuthUiState.Error) {
            Text(
                text = (uiState as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            text = "Register",
            onClick = {
                if (password != confirmPassword) {
                    // Quick validation
                    return@PrimaryButton
                }
                viewModel.register(username, email, displayName, password)
            },
            isLoading = uiState is AuthUiState.Loading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { 
            focusManager.clearFocus()
            onNavigateLogin() 
        }) {
            Text("Already have an account? Login")
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}
