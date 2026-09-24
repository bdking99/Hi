package com.example.ui.screens.auth

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.utils.GoogleSignInHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // 1. Name
    var name by remember { mutableStateOf("") }
    // 2. Username
    var username by remember { mutableStateOf("") }
    // 3. Gmail
    var gmail by remember { mutableStateOf("") }
    // 4. Phone Number
    var phoneNumber by remember { mutableStateOf("") }
    // 5. Password
    var password by remember { mutableStateOf("") }
    // 6. Confirm Password
    var confirmPassword by remember { mutableStateOf("") }

    var selectedAvatar by remember { mutableStateOf(AVATAR_OPTIONS[0]) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Phone OTP Verification Dialog State for Phone Registration
    var showOtpDialog by remember { mutableStateOf(false) }
    var activeVerificationId by remember { mutableStateOf("") }
    var activePhoneForOtp by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    // Google Sign-In Fallback Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (!idToken.isNullOrBlank()) {
                    viewModel.signInWithGoogle(
                        idToken = idToken,
                        email = account.email,
                        displayName = account.displayName,
                        photoUrl = account.photoUrl?.toString() ?: selectedAvatar
                    )
                } else {
                    infoMessage = "Failed to retrieve Google ID token."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                infoMessage = "Google Sign-In failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        } else {
            infoMessage = "Google Sign-In cancelled."
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                focusManager.clearFocus()
                showOtpDialog = false
                viewModel.resetState()
                onRegisterSuccess()
            }
            is AuthUiState.OtpCodeSent -> {
                activeVerificationId = state.verificationId
                activePhoneForOtp = state.phoneNumber
                showOtpDialog = true
            }
            else -> {}
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
            .testTag("register_screen")
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
                text = "Create Real Account",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 25.sp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFD54F), Color(0xFFFF8A65), Color(0xFFFF4081))
                    )
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Join Great Voice Chat & connect with authentic rooms",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0A8D9),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Avatar Selector Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(
                            2.5.dp,
                            Brush.sweepGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF8F00), Color(0xFFFFD54F))),
                            CircleShape
                        )
                        .padding(3.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = selectedAvatar,
                        contentDescription = "Selected Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose Profile Avatar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFCA28)
                )

                Spacer(modifier = Modifier.height(6.dp))

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
                                .padding(horizontal = 4.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
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

            Spacer(modifier = Modifier.height(18.dp))

            // Main Registration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1B1633).copy(alpha = 0.94f)
                ),
                border = BorderStroke(
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
                    // 1. Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                        },
                        label = { Text("Name", color = Color(0xFF9E97C4)) },
                        placeholder = { Text("e.g. John Doe", color = Color(0xFF6B648F)) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = "Name", tint = Color(0xFFFFB300))
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Username Field
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it.filter { char -> char.isLetterOrDigit() || char == '_' }.lowercase()
                            validationError = null
                        },
                        label = { Text("Username", color = Color(0xFF9E97C4)) },
                        placeholder = { Text("e.g. user_pro", color = Color(0xFF6B648F)) },
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = "Username", tint = Color(0xFFFFB300))
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_username_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Gmail Field
                    OutlinedTextField(
                        value = gmail,
                        onValueChange = {
                            gmail = it
                            validationError = null
                        },
                        label = { Text("Gmail", color = Color(0xFF9E97C4)) },
                        placeholder = { Text("yourname@gmail.com", color = Color(0xFF6B648F)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Gmail", tint = Color(0xFFFFB300))
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_gmail_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Phone Number Field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            validationError = null
                        },
                        label = { Text("Phone Number", color = Color(0xFF9E97C4)) },
                        placeholder = { Text("+1234567890", color = Color(0xFF6B648F)) },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = "Phone Number", tint = Color(0xFFFFB300))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_phone_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
                        },
                        label = { Text("Password (min 6 chars)", color = Color(0xFF9E97C4)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password", tint = Color(0xFFFFB300))
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_password_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 6. Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            validationError = null
                        },
                        label = { Text("Confirm Password", color = Color(0xFF9E97C4)) },
                        leadingIcon = {
                            Icon(Icons.Default.LockReset, contentDescription = "Confirm Password", tint = Color(0xFFFFB300))
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
                            onDone = { focusManager.clearFocus() }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_confirm_password_input")
                    )

                    // Error & Info Feedback
                    if (infoMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1B5E20).copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = infoMessage ?: "",
                                color = Color(0xFF81C784),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    val displayError = validationError ?: (uiState as? AuthUiState.Error)?.message
                    if (displayError != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFB71C1C).copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color(0xFFE57373)),
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

                    Spacer(modifier = Modifier.height(18.dp))

                    // 7. Create Account Button
                    val isLoading = uiState is AuthUiState.Loading
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            validationError = null
                            infoMessage = null

                            // Strict Validation
                            if (name.trim().isBlank()) {
                                validationError = "Name is required."
                                return@Button
                            }
                            if (username.trim().isBlank()) {
                                validationError = "Username is required."
                                return@Button
                            }
                            if (username.trim().length < 3) {
                                validationError = "Username must be at least 3 characters."
                                return@Button
                            }
                            if (gmail.trim().isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(gmail.trim()).matches()) {
                                validationError = "Please enter a valid Gmail / email address."
                                return@Button
                            }
                            if (phoneNumber.trim().isBlank() || phoneNumber.trim().length < 7) {
                                validationError = "Please enter a valid phone number (e.g. +1234567890)."
                                return@Button
                            }
                            if (password.isBlank()) {
                                validationError = "Password is required."
                                return@Button
                            }
                            if (password.length < 6) {
                                validationError = "Password must be at least 6 characters."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                validationError = "Confirm Password must exactly match Password."
                                return@Button
                            }

                            viewModel.register(
                                name = name,
                                username = username,
                                email = gmail,
                                phone = phoneNumber,
                                passwordRaw = password,
                                confirmPassword = confirmPassword,
                                photoUrl = selectedAvatar
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
                            )
                            .testTag("create_account_button"),
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
                                    text = "Create Account",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Divider with "OR"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF352D56)
                        )
                        Text(
                            text = "  OR  ",
                            color = Color(0xFF8880AB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF352D56)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 8. Continue with Google Button
                    OutlinedButton(
                        onClick = {
                            focusManager.clearFocus()
                            validationError = null
                            infoMessage = null
                            if (activity is ComponentActivity) {
                                coroutineScope.launch {
                                    val credResult = GoogleSignInHelper.signInWithCredentialManager(activity)
                                    if (credResult.isSuccess) {
                                        val userData = credResult.getOrThrow()
                                        viewModel.signInWithGoogle(
                                            idToken = userData.idToken,
                                            email = userData.email,
                                            displayName = userData.displayName ?: name.ifBlank { null },
                                            photoUrl = userData.photoUrl ?: selectedAvatar
                                        )
                                    } else {
                                        val client = GoogleSignInHelper.getGoogleSignInClient(context)
                                        googleSignInLauncher.launch(client.signInIntent)
                                    }
                                }
                            } else {
                                val client = GoogleSignInHelper.getGoogleSignInClient(context)
                                googleSignInLauncher.launch(client.signInIntent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_register_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF453C70)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF141028),
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color(0xFF4285F4)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
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
                    modifier = Modifier
                        .clickable {
                            focusManager.clearFocus()
                            onNavigateLogin()
                        }
                        .testTag("navigate_login_button")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Phone OTP Verification Dialog if Phone Registration triggered
        if (showOtpDialog) {
            AlertDialog(
                onDismissRequest = { showOtpDialog = false },
                containerColor = Color(0xFF1B1633),
                title = {
                    Text(
                        text = "Verify Phone OTP",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "A 6-digit verification code was sent to $activePhoneForOtp. Enter it below to complete registration:",
                            color = Color(0xFFB0A8D9),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = otpCodeInput,
                            onValueChange = { otpCodeInput = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("6-Digit OTP Code") },
                            placeholder = { Text("123456") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFFB300),
                                unfocusedBorderColor = Color(0xFF3D3560)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("register_otp_input")
                        )

                        val otpError = (uiState as? AuthUiState.Error)?.message
                        if (!otpError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = otpError,
                                color = Color(0xFFFF8A80),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    if (activity != null && activePhoneForOtp.isNotBlank()) {
                                        viewModel.sendPhoneOtp(activePhoneForOtp, activity)
                                    }
                                }
                            ) {
                                Text("Resend Code", color = Color(0xFFFFCA28), fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (activeVerificationId.isNotBlank() && otpCodeInput.isNotBlank()) {
                                viewModel.verifyPhoneOtp(
                                    verificationId = activeVerificationId,
                                    smsCode = otpCodeInput,
                                    name = name,
                                    username = username,
                                    email = gmail,
                                    photoUrl = selectedAvatar
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                    ) {
                        Text("Verify & Create", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOtpDialog = false }) {
                        Text("Cancel", color = Color(0xFFB0A8D9))
                    }
                }
            )
        }
    }
}
