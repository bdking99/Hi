package com.example.data.local.dao

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.data.model.GiftItem

@Composable
fun GiftPanelScreen(
    roomId: String,
    receiverId: String,
    selectedGift: GiftItem,
    viewModel: RoomViewModel = hiltViewModel()
) {
    val isGifting by viewModel.isGifting.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen for one-time events from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.giftEvent.collect { event ->
            when (event) {
                is GiftEvent.Success -> {
                    if (event.transaction.isBroadcastEvent) {
                        // TODO: Trigger Full-Screen Lottie/SVGA Animation
                        println("Triggering massive ${event.transaction.giftName} animation!")
                    } else {
                        // TODO: Show small combo animation above the chat
                        println("Sent ${event.transaction.comboCount}x ${event.transaction.giftName}")
                    }
                }
                is GiftEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    viewModel.sendGift(
                        roomId = roomId,
                        receiverId = receiverId,
                        gift = selectedGift,
                        comboCount = 1
                    )
                },
                enabled = !isGifting, // Disable button during network call
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (isGifting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending...")
                } else {
                    Text("Send ${selectedGift.name} (${selectedGift.costCoins} Coins)")
                }
            }
        }
    }
}
