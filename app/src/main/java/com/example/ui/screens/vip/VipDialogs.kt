package com.example.ui.screens.vip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserAnimationPreferences
import com.example.data.model.VipHistoryItem
import com.example.data.model.VipLevelConfig
import java.text.SimpleDateFormat
import java.util.*

// ⚙️ ANIMATION & ACCESSIBILITY SETTINGS DIALOG
@Composable
fun VipAnimationSettingsDialog(
    preferences: UserAnimationPreferences,
    onSave: (showVip: Boolean, showEntry: Boolean, showGift: Boolean, reduceMotion: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var showVip by remember(preferences) { mutableStateOf(preferences.showVipAnimations) }
    var showEntry by remember(preferences) { mutableStateOf(preferences.showRoomEntryAnimations) }
    var showGift by remember(preferences) { mutableStateOf(preferences.showGiftAnimations) }
    var reduceMotion by remember(preferences) { mutableStateOf(preferences.reduceMotion) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(1.dp, Color(0xFF37474F), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFFFD54F)
                    )
                    Text(
                        text = "VIP & Visual Preferences",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Control premium visual effects, room entry banners, and reduced-motion mode for accessibility.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                HorizontalDivider(color = Color(0xFF2E3244))

                // Setting 1
                SettingSwitchRow(
                    title = "VIP Frame Animations",
                    subtitle = "Display animated spinning rings and sparkles around VIP avatars",
                    checked = showVip,
                    onCheckedChange = { showVip = it }
                )

                // Setting 2
                SettingSwitchRow(
                    title = "Voice Room Entry Banners",
                    subtitle = "Show top floating banner when VIP / SVIP members join rooms",
                    checked = showEntry,
                    onCheckedChange = { showEntry = it }
                )

                // Setting 3
                SettingSwitchRow(
                    title = "Luxury Gift Full-Screen Effects",
                    subtitle = "Play celebratory animated gifts across active voice rooms",
                    checked = showGift,
                    onCheckedChange = { showGift = it }
                )

                // Setting 4 (Accessibility)
                SettingSwitchRow(
                    title = "Reduce Motion Mode",
                    subtitle = "Disable continuous rotations and fast spring effects for performance & accessibility",
                    checked = reduceMotion,
                    onCheckedChange = { reduceMotion = it }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            onSave(showVip, showEntry, showGift, reduceMotion)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color.Gray, fontSize = 10.sp, lineHeight = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFFFB300),
                checkedTrackColor = Color(0xFF5D4037)
            )
        )
    }
}

// 📜 VIP HISTORY AUDIT DIALOG
@Composable
fun VipHistoryDialog(
    historyList: List<VipHistoryItem>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191924)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f)
                .border(1.dp, Color(0xFF33334D), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 VIP Status Audit History",
                        color = Color(0xFFFFD54F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Text(
                    text = "Immutable server audit records of VIP rank progressions and qualifying thresholds.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (historyList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No VIP history records yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(historyList) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF222233)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.previousType} ${item.previousLevel} ➔ ${item.newType} ${item.newLevel}",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.reason,
                                            color = Color(0xFFFFD54F),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Qualifying Recharge: %,d Coins".format(item.qualifyingAmount),
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = dateFormat.format(Date(item.createdAt)),
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🛡️ ADMIN VIP THRESHOLD CONFIGURATION DIALOG
@Composable
fun AdminVipConfigDialog(
    levelConfig: VipLevelConfig,
    onSave: (newThreshold: Long, benefits: List<String>, isActive: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var thresholdInput by remember(levelConfig) { mutableStateOf(levelConfig.requiredRechargeAmount.toString()) }
    var benefitsInput by remember(levelConfig) { mutableStateOf(levelConfig.benefits.joinToString("\n")) }
    var isActive by remember(levelConfig) { mutableStateOf(levelConfig.isActive) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(1.dp, Color(0xFF512DA8), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "⚙️ Admin VIP Threshold Editor",
                    color = Color(0xFFB388FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Editing tier: ${levelConfig.type} ${levelConfig.level} (${levelConfig.name})",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it },
                    label = { Text("Required Qualifying Coins / Recharge") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = benefitsInput,
                    onValueChange = { benefitsInput = it },
                    label = { Text("Configured Benefits (one per line)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tier Active in System", color = Color.LightGray, fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            val newAmount = thresholdInput.toLongOrNull() ?: levelConfig.requiredRechargeAmount
                            val benefitsList = benefitsInput.lines().map { it.trim() }.filter { it.isNotBlank() }
                            onSave(newAmount, benefitsList, isActive)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update Tier", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
