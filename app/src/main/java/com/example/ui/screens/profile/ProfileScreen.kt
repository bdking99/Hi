package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPremium

val TealPremium = Color(0xFF00B4D8)
val DarkSurfaceMenu = Color(0xFF1E212B) // Slightly different surface for menus

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogoutClick: () -> Unit,
    onWalletClick: () -> Unit = {}
) {
    val user by viewModel.user.collectAsState()
    val profile by viewModel.profile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp) // space for bottom nav
    ) {
        // --- HEADER SECTION ---
        ProfileHeaderSection(
            displayName = user?.displayName ?: "Loading...",
            username = user?.username ?: "...",
            uid = user?.publicUserId ?: "...",
            vipLevel = profile?.vipLevel ?: 0,
            followersCount = profile?.followingCount ?: 0, // Using followingCount as concern
            fansCount = profile?.followersCount ?: 0,
            charmValue = profile?.earnings ?: 0L,
            onSettingsClick = {},
            onEditClick = {}
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // --- WALLET CARDS ---
        WalletSection(
            coinBalance = profile?.coinBalance ?: 0L,
            diamondBalance = profile?.earnings ?: 0L, // Mocking diamond balance via earnings
            onCoinClick = onWalletClick,
            onDiamondClick = {}
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- PREMIUM FEATURE GRID ---
        FeatureGridSection()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- MENU SECTION ---
        val showAgency = (profile?.agencyId != null) || (user?.roleId ?: 1) > 1
        MenuSection(
            showAgency = showAgency,
            onLogoutClick = { viewModel.logout(onLogoutClick) }
        )
    }
}

@Composable
fun ProfileHeaderSection(
    displayName: String,
    username: String,
    uid: String,
    vipLevel: Int,
    followersCount: Int,
    fansCount: Int,
    charmValue: Long,
    onSettingsClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TealPremium.copy(alpha = 0.2f), MaterialTheme.colorScheme.background)
                )
            )
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            
            // Top action bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(3.dp, if (vipLevel > 0) GoldPremium else TealPremium, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Avatar", modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                // VIP Badge
                if (vipLevel > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 10.dp)
                            .background(GoldPremium, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("VIP $vipLevel", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Name and ID
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.width(8.dp))
                // Verified badge mock
                Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = TealPremium, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ID: $uid", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy ID", modifier = Modifier.size(14.dp).clickable { /* copy */ }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Concern", followersCount.toString())
                StatItem("Fan", fansCount.toString())
                StatItem("Charm", charmValue.toString())
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WalletSection(
    coinBalance: Long,
    diamondBalance: Long,
    onCoinClick: () -> Unit,
    onDiamondClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WalletCard(
            modifier = Modifier.weight(1f),
            title = "Coin",
            balance = coinBalance.toString(),
            icon = Icons.Filled.Star, // Mocking coin icon
            iconTint = GoldPremium,
            onClick = onCoinClick
        )
        WalletCard(
            modifier = Modifier.weight(1f),
            title = "Diamond",
            balance = diamondBalance.toString(),
            icon = Icons.Filled.Favorite, // Mocking diamond icon
            iconTint = TealPremium,
            onClick = onDiamondClick
        )
    }
}

@Composable
fun WalletCard(
    modifier: Modifier = Modifier,
    title: String,
    balance: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(balance, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
fun FeatureGridSection() {
    val features = listOf(
        Pair("VIP", Icons.Filled.WorkspacePremium),
        Pair("Mall", Icons.Filled.Store),
        Pair("Backpack", Icons.Filled.ShoppingBag),
        Pair("Task", Icons.Filled.Assignment),
        Pair("Medal", Icons.Filled.EmojiEvents),
        Pair("Family", Icons.Filled.People),
        Pair("CP Nest", Icons.Filled.FavoriteBorder),
        Pair("Wealth", Icons.Filled.ShowChart)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // LazyVerticalGrid inside Scrollable Column is not allowed natively, so we use a custom implementation or fixed height
            // We can just use two Rows for 4x2 grid to avoid Nested Scroll issues
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 0..3) {
                    FeatureGridItem(features[i].first, features[i].second)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 4..7) {
                    FeatureGridItem(features[i].first, features[i].second)
                }
            }
        }
    }
}

@Composable
fun FeatureGridItem(title: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp).clickable { }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(TealPremium.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                .border(1.dp, GoldPremium.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = GoldPremium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MenuSection(
    showAgency: Boolean,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu)
    ) {
        Column {
            MenuItem(title = "Host Data", icon = Icons.Outlined.Analytics)
            if (showAgency) {
                MenuItem(title = "Agency Center", icon = Icons.Outlined.Business)
            }
            MenuItem(title = "Reward Records", icon = Icons.Outlined.CardGiftcard)
            MenuItem(title = "Interactive Games Records", icon = Icons.Outlined.VideogameAsset)
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu)
    ) {
        Column {
            MenuItem(title = "Feedback", icon = Icons.Outlined.Feedback)
            MenuItem(title = "Setting", icon = Icons.Outlined.Settings)
            MenuItem(title = "Logout", icon = Icons.Outlined.ExitToApp, onClick = onLogoutClick, showArrow = false, tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun MenuItem(
    title: String, 
    icon: ImageVector, 
    onClick: () -> Unit = {}, 
    showArrow: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.weight(1f))
        if (showArrow) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
