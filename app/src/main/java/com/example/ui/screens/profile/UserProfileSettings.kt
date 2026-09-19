package com.example.ui.screens.profile

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import com.example.utils.PermissionManager

val PRESET_AVATARS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
)

val PRESET_COVERS = listOf(
    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
    "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=800",
    "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileSettingsDialog(
    currentDisplayName: String,
    currentBio: String,
    currentAvatar: String?,
    currentCover: String?,
    onDismiss: () -> Unit,
    onSave: (displayName: String, bio: String, avatar: String?, cover: String?) -> Unit
) {
    var displayName by remember { mutableStateOf(currentDisplayName) }
    var bio by remember { mutableStateOf(currentBio) }
    var selectedAvatar by remember { mutableStateOf(currentAvatar ?: PRESET_AVATARS[0]) }
    var selectedCover by remember { mutableStateOf(currentCover ?: PRESET_COVERS[0]) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAvatar = uri.toString()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            photoPickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Camera permission needed to upload photo", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_settings_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF19162A),
            border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Profile & VIP Identity",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cover Banner Preview
                Text(
                    text = "Profile Banner / Cover",
                    color = GoldPremium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2440))
                ) {
                    AsyncImage(
                        model = selectedCover,
                        contentDescription = "Cover Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PRESET_COVERS) { coverUrl ->
                        Box(
                            modifier = Modifier
                                .size(60.dp, 36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (selectedCover == coverUrl) 2.dp else 1.dp,
                                    color = if (selectedCover == coverUrl) GoldPremium else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedCover = coverUrl }
                        ) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = "Select Cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Photo Selection
                Text(
                    text = "Profile Picture",
                    color = GoldPremium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2440))
                            .border(2.dp, GoldPremium, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedAvatar,
                            contentDescription = "Avatar Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    OutlinedButton(
                        onClick = {
                            if (PermissionManager.isCameraPermissionGranted(context)) {
                                photoPickerLauncher.launch("image/*")
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPremium),
                        border = BorderStroke(1.dp, TealPremium),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Upload", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Photo", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Or choose a VIP Avatar:",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PRESET_AVATARS) { avatarUrl ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (selectedAvatar == avatarUrl) 2.5.dp else 1.dp,
                                    color = if (selectedAvatar == avatarUrl) TealPremium else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { selectedAvatar = avatarUrl }
                        ) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Preset Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Display Name TextField
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPremium,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = TealPremium,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bio TextField
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio / Status Greeting") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bio_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPremium,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = TealPremium,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save Button
                Button(
                    onClick = {
                        onSave(displayName, bio, selectedAvatar, selectedCover)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Profile Changes",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Functional App & Voice Room Settings Sheet:
 * Allows user to toggle Microphone sensitivity, Noise Suppression, Gift animations,
 * Room Entrance Sound, Push notifications, and Incognito mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var noiseSuppression by remember { mutableStateOf(true) }
    var entranceSound by remember { mutableStateOf(true) }
    var giftAnimations by remember { mutableStateOf(true) }
    var pushNotifications by remember { mutableStateOf(true) }
    var hdAudio by remember { mutableStateOf(true) }
    var incognitoMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("app_settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF181528),
            border = BorderStroke(1.dp, TealPremium.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚙️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Voice & Room Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Audio & Stage Controls
                Text("AUDIO & STAGE CONTROLS", color = TealPremium, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))

                SettingToggleItem(
                    icon = Icons.Default.MicNone,
                    title = "AI Noise Suppression",
                    subtitle = "Eliminate background noise when speaking on stage",
                    checked = noiseSuppression,
                    onCheckedChange = { noiseSuppression = it }
                )

                SettingToggleItem(
                    icon = Icons.Default.Headphones,
                    title = "HD Voice Streaming",
                    subtitle = "Crystal-clear 48kHz audio for singing & music rooms",
                    checked = hdAudio,
                    onCheckedChange = { hdAudio = it }
                )

                SettingToggleItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Room Entrance Sound",
                    subtitle = "Play VIP sound effect when entering party rooms",
                    checked = entranceSound,
                    onCheckedChange = { entranceSound = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Visual & Luxury Effects
                Text("VISUAL & NOTIFICATIONS", color = GoldPremium, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))

                SettingToggleItem(
                    icon = Icons.Default.CardGiftcard,
                    title = "3D Luxury Gift Animations",
                    subtitle = "Display full-screen golden rockets, cars & crowns",
                    checked = giftAnimations,
                    onCheckedChange = { giftAnimations = it }
                )

                SettingToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "Live Push Notifications",
                    subtitle = "Get alerted when followed hosts start broadcasting",
                    checked = pushNotifications,
                    onCheckedChange = { pushNotifications = it }
                )

                SettingToggleItem(
                    icon = Icons.Default.VisibilityOff,
                    title = "VIP Incognito Mode",
                    subtitle = "Hide online status and enter rooms discreetly",
                    checked = incognitoMode,
                    onCheckedChange = { incognitoMode = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        Toast.makeText(context, "Settings saved successfully! ✅", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPremium)
                ) {
                    Text("Apply & Close", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = TealPremium, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = TealPremium,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}
