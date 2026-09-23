package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val AVATAR_OPTIONS = listOf(
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
    "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=200",
    "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=200",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200"
)

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
    var selectedAvatar by remember { mutableStateOf(AVATAR_OPTIONS[0]) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            focusManager.clearFocus()
            viewModel.resetState()
            onRegisterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E0A1E),
                        Color(0xFF140F2D),
                        Color(0xFF090615)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Join Great Voice Room",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFD54F), Color(0xFFFF8A65), Color(0xFFFF4081))
                    )
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Create your account & unlock free 2,500 starter coins!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0A8D9),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Avatar Selector Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Selected Avatar Preview with Gold VIP Ring
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(3.dp, Brush.sweepGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00), Color(0xFFFFD54F))), CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = selectedAvatar,
                        contentDescription = "Selected Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Choose Your Voice Avatar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFCA28)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal Avatar Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AVATAR_OPTIONS.forEach { avatarUrl ->
                        val isSelected = avatarUrl == selectedAvatar
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFFFB300) else Color(0xFF3B335E),
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar = avatarUrl }
                                .padding(if (isSelected) 2.dp else 0.dp)
                                .clip(CircleShape)
                        ) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar Choice",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Main Registration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1B1633).copy(alpha = 0.94f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF3F3766), Color(0xFF221C42))
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Display Name
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name", color = Color(0xFF9E97C4)) },
                        placeholder = { Text("e.g. Prince of Voice", color = Color(0xFF6B648F)) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFFB300))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0xFF3D3560),
                            focusedContainerColor = Color(0xFF120E24),
                            unfocusedContainerColor = Color(0xFF120E24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Unique Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.filter { char -> char.isLetterOrDigit() || char == '_' }.lowercase() },
                        label = { Text("Username (@handle)", color = Color(0xFF9E97C4)) },
                        placeholder = { Text("e.g. voice_king", color = Color(0xFF6B648F)) },
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = Color(0xFFFFB300))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0xFF3D3560),
                            focusedContainerColor = Color(0xFF120E24),
                            unfocusedContainerColor = Color(0xFF120E24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = Color(0xFF9E97C4)) },
                        placeholder = { Text("yourname@gmail.com", color = Color(0xFF6B648F)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFFFB300))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0xFF3D3560),
                            focusedContainerColor = Color(0xFF120E24),
                            unfocusedContainerColor = Color(0xFF120E24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min 6 chars)", color = Color(0xFF9E97C4)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFB300))
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFFB0A8D9)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0xFF3D3560),
                            focusedContainerColor = Color(0xFF120E24),
                            unfocusedContainerColor = Color(0xFF120E24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password", color = Color(0xFF9E97C4)) },
                        leadingIcon = {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFFFFB300))
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFFB0A8D9)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFB300),
                            unfocusedBorderColor = Color(0xFF3D3560),
                            focusedContainerColor = Color(0xFF120E24),
                            unfocusedContainerColor = Color(0xFF120E24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error messages
                    val displayError = validationError ?: (uiState as? AuthUiState.Error)?.message
                    if (displayError != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFB71C1C).copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = displayError,
                                color = Color(0xFFFFCDD2),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Register Button with Gradient Glow
                    val isLoading = uiState is AuthUiState.Loading
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            validationError = null
                            if (displayName.isBlank()) {
                                validationError = "Please enter your display name."
                                return@Button
                            }
                            if (username.isBlank() || username.length < 3) {
                                validationError = "Username must be at least 3 characters."
                                return@Button
                            }
                            if (email.isBlank() || !email.contains("@")) {
                                validationError = "Please enter a valid email address."
                                return@Button
                            }
                            if (password.length < 6) {
                                validationError = "Password must be at least 6 characters."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                validationError = "Passwords do not match."
                                return@Button
                            }
                            viewModel.register(
                                username = username.trim(),
                                email = email.trim(),
                                displayName = displayName.trim(),
                                passwordRaw = password,
                                avatarUrl = selectedAvatar
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = Color(0xFFFF8F00),
                                spotColor = Color(0xFFFF6D00)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color(0xFF2C2448)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFFB300), Color(0xFFFF6D00), Color(0xFFFF4081))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Create Account ✨",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Already have an account? Login link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    color = Color(0xFFB0A8D9),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign In",
                    color = Color(0xFFFFCA28),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        focusManager.clearFocus()
                        onNavigateLogin()
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
