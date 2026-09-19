package com.example.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import com.google.accompanist.permissions.*

/**
 * Accompanist-based PermissionManager for CAMERA, RECORD_AUDIO, and POST_NOTIFICATIONS.
 * Enforces runtime authorization before joining live voice rooms or accessing profile camera/media features.
 */
object PermissionManager {

    val REQUIRED_APP_PERMISSIONS = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isAudioPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }

    fun isCameraPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.CAMERA)
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    fun areAllRequiredGranted(context: Context): Boolean {
        return REQUIRED_APP_PERMISSIONS.all { isPermissionGranted(context, it) }
    }
}

/**
 * Accompanist wrapper that manages permissions and displays a luxury rationale dialog when needed.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AccompanistPermissionHandler(
    onPermissionsGranted: () -> Unit,
    showRationaleDialog: Boolean,
    onDismissRationale: () -> Unit
) {
    val context = LocalContext.current
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionManager.REQUIRED_APP_PERMISSIONS
    )

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    if (showRationaleDialog && !permissionsState.allPermissionsGranted) {
        AlertDialog(
            onDismissRequest = onDismissRationale,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPremium.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Permissions",
                            tint = GoldPremium
                        )
                    }
                    Text(
                        text = "App Permissions Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Great Voice Room needs access to device features for the ultimate VIP live audio experience:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    PermissionItemRow(
                        icon = Icons.Default.Mic,
                        title = "Microphone (RECORD_AUDIO)",
                        description = "Speak on live room stages and participate in audio party clubs."
                    )

                    PermissionItemRow(
                        icon = Icons.Default.CameraAlt,
                        title = "Camera (CAMERA)",
                        description = "Update custom profile avatars, covers, and live video feeds."
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionItemRow(
                            icon = Icons.Default.Notifications,
                            title = "Notifications (POST_NOTIFICATIONS)",
                            description = "Receive gift alerts, room invitations, and direct messages."
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        permissionsState.launchMultiplePermissionRequest()
                        onDismissRationale()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Grant Permissions",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Open system settings if permissions are permanently denied
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    onDismissRationale()
                }) {
                    Text("Open Settings", color = TealPremium)
                }
            },
            containerColor = Color(0xFF1E1435),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, GoldPremium.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        )
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(TealPremium.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TealPremium,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 13.sp
            )
        }
    }
}
