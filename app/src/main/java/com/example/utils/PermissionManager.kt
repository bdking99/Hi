package com.example.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

/**
 * Utility for checking and requesting runtime permissions for Audio, Camera, and Notifications.
 */
object PermissionManager {

    val REQUIRED_PERMISSIONS = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

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
}

/**
 * Composable helper for requesting multiple permissions.
 */
@Composable
fun rememberAppPermissionLauncher(
    onAllGranted: () -> Unit = {},
    onPermissionsResult: (Map<String, Boolean>) -> Unit = {}
): ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onPermissionsResult(result)
        if (result.values.all { it }) {
            onAllGranted()
        }
    }
}
