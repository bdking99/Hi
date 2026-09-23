package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FirebaseUserProfile
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchSheet(
    userRepository: UserRepository,
    currentUserId: String,
    onDismiss: () -> Unit,
    onUserSelected: (FirebaseUserProfile) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FirebaseUserProfile>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Load available frames for displaying user frames
    val availableFrames by userRepository.getAvailableFramesStream().collectAsState(initial = userRepository.getDefaultFrames())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF100722),
        contentColor = Color.White,
        modifier = Modifier.testTag("user_search_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Find Real Users",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Input Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { query ->
                    searchQuery = query
                    searchJob?.cancel()
                    if (query.trim().length >= 2) {
                        isSearching = true
                        searchJob = coroutineScope.launch {
                            delay(350) // Debounce
                            searchResults = userRepository.searchUsers(query.trim())
                            isSearching = false
                            hasSearched = true
                        }
                    } else {
                        searchResults = emptyList()
                        isSearching = false
                        hasSearched = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_search_input"),
                placeholder = { Text("Search by Public ID (e.g. 1000001) or Name", fontSize = 13.sp, color = Color.Gray) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFFFD700))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            searchResults = emptyList()
                            hasSearched = false
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD700),
                    unfocusedBorderColor = Color(0xFF2C1947),
                    focusedContainerColor = Color(0xFF1B0B33),
                    unfocusedContainerColor = Color(0xFF1B0B33),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loading / Results / Empty states
            if (isSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFFD700), strokeWidth = 2.dp)
                }
            } else if (hasSearched && searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No users found for \"$searchQuery\"",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try searching by their exact 7-digit Public User ID",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(searchResults, key = { it.userId }) { user ->
                        val frame = availableFrames.find { it.id == user.profileFrameId }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_result_user_${user.publicUserId}")
                                .clickable { onUserSelected(user) },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1A0C33),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF32185C))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with frame
                                ProfileAvatarWithFrame(
                                    avatarUrl = user.avatar,
                                    frame = frame,
                                    size = 54.dp,
                                    isOnline = user.isOnline
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.displayName,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (user.vipLevel > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFFD700)
                                            ) {
                                                Text(
                                                    text = "VIP ${user.vipLevel}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Public User ID badge
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF2B144E)
                                        ) {
                                            Text(
                                                text = "ID: ${user.publicUserId}",
                                                color = Color(0xFFFFD700),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = if (user.isOnline) "🟢 Online" else "⚪ Offline",
                                            fontSize = 11.sp,
                                            color = if (user.isOnline) Color(0xFF00E676) else Color.Gray
                                        )
                                    }
                                }

                                // Visit Profile Icon Button
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "View Profile",
                                    tint = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
