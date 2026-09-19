package com.example.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Comprehensive Permission Manager using Accompanist Permissions.
 * Handles runtime permission requests, status checks, and rationale UI flows
 * before voice room streaming, camera capture, and notifications initialize.
 */
object PermissionManager {

    /**
     * Standard list of permissions required for full voice room and media experiences.
     */
    val appRequiredPermissions: List<String>
        get() = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

    /**
     * Synchronous check if a given permission is currently granted.
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if voice streaming permissions (Microphone) are granted.
     */
    fun isVoicePermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Check if Camera permissions are granted.
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.CAMERA)
    }
}

/**
 * Accompanist-based permission wrapper that requests essential permissions on launch
 * and shows an elegant VIP explanation dialog if permissions are not granted.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppPermissionProvider(
    permissions: List<String> = PermissionManager.appRequiredPermissions,
    content: @Composable () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    var rationaleDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    content()

    // Elegant VIP Permission Rationale Banner/Dialog when permissions are missing
    if (!permissionState.allPermissionsGranted && !rationaleDismissed && permissionState.shouldShowRationale) {
        AlertDialog(
            onDismissRequest = { rationaleDismissed = true },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permissions Required",
                    tint = GoldPremium,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "👑 Enable Permissions for VIP Club",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To enjoy real-time voice chat, take speaker seats, upload custom avatars, and receive live party gifts, please allow access:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PermissionItemRow(icon = Icons.Default.Mic, title = "Microphone", subtitle = "Speak in live voice rooms & party stages")
                    PermissionItemRow(icon = Icons.Default.CameraAlt, title = "Camera", subtitle = "Capture & update your VIP profile avatar")
                    PermissionItemRow(icon = Icons.Default.Notifications, title = "Notifications", subtitle = "Receive live gifts & room invitations")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        rationaleDismissed = true
                        permissionState.launchMultiplePermissionRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("Allow All Permissions", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rationaleDismissed = true }) {
                    Text("Later", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1B162C),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PermissionItemRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = title, tint = TealPremium, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
    }
}

/**
 * Reusable Accompanist Single Permission Handler for Action triggers (e.g. Taking stage seat, Camera capture)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AccompanistPermissionHandler(
    permission: String,
    title: String = "Permission Required",
    description: String = "This action requires device permission to proceed.",
    onGranted: () -> Unit,
    content: @Composable (trigger: () -> Unit) -> Unit
) {
    val permissionState = rememberPermissionState(permission = permission)
    var showDialog by remember { mutableStateOf(false) }

    val trigger: () -> Unit = {
        if (permissionState.status.isGranted) {
            onGranted()
        } else {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title, fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text(description, color = Color.White.copy(alpha = 0.85f)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        permissionState.launchPermissionRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPremium)
                ) {
                    Text("Grant Permission", color = Color(0xFF0D1117), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = Color(0xFF1E2430),
            shape = RoundedCornerShape(16.dp)
        )
    }

    content(trigger)
}
