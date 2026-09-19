package com.example.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.*

/**
 * AppPermissionProvider automatically checks and requests critical permissions
 * (Audio recording, Camera, Notifications) on launch using Accompanist.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppPermissionProvider(
    content: @Composable () -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionManager.REQUIRED_APP_PERMISSIONS
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
    }
}
