package com.example.utils

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * AppPermissionProvider automatically checks and requests critical permissions
 * (Audio recording, Camera, Notifications) on launch so real-time voice streaming
 * and camera functions work out of the box.
 */
@Composable
fun AppPermissionProvider(
    content: @Composable () -> Unit
) {
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissionsToRequest)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
    }
}
