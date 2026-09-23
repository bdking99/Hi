package com.example.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProfileFrame
import com.example.ui.components.ProfileAvatarWithFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFrameSelectionSheet(
    avatarUrl: String?,
    currentFrameId: String,
    availableFrames: List<ProfileFrame>,
    onDismiss: () -> Unit,
    onEquipFrame: (ProfileFrame) -> Unit
) {
    var selectedFrame by remember(currentFrameId, availableFrames) {
        mutableStateOf(availableFrames.find { it.id == currentFrameId } ?: availableFrames.firstOrNull())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF100722),
        contentColor = Color.White,
        modifier = Modifier.testTag("profile_frame_selection_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VIP Profile Frames",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Preview of Avatar inside Selected Frame
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E0F38)),
                contentAlignment = Alignment.Center
            ) {
                ProfileAvatarWithFrame(
                    avatarUrl = avatarUrl,
                    frame = selectedFrame,
                    size = 110.dp,
                    isOnline = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = selectedFrame?.name ?: "No Frame",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = selectedFrame?.glowColorHex?.let { Color(it.toInt()) } ?: Color(0xFFFFD700)
            )

            Text(
                text = "${selectedFrame?.rarity ?: "VIP"} • ${selectedFrame?.type ?: "Special"}",
                fontSize = 12.sp,
                color = Color.LightGray.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Grid of Available Frames
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(availableFrames, key = { it.id }) { frame ->
                    val isSelected = selectedFrame?.id == frame.id
                    val isEquipped = currentFrameId == frame.id
                    val frameColor = Color(frame.glowColorHex.toInt())

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedFrame = frame },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF261247) else Color(0xFF16092B),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) frameColor else Color(0xFF381B63)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(50.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ProfileAvatarWithFrame(
                                    avatarUrl = avatarUrl,
                                    frame = frame,
                                    size = 46.dp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = frame.name,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.LightGray,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            if (isEquipped) {
                                Text(
                                    text = "Equipped",
                                    fontSize = 9.sp,
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Equip Button
            Button(
                onClick = {
                    selectedFrame?.let {
                        onEquipFrame(it)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("equip_frame_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedFrame?.glowColorHex?.let { Color(it.toInt()) } ?: Color(0xFFFFD700),
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedFrame?.id == currentFrameId) "Equipped Frame" else "Equip Frame",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
