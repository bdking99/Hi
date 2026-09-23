package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPremium

/**
 * Authoritative Admin & Moderator Badges:
 * Renders verified server-side role markers (ADMIN / SUPER_ADMIN / MODERATOR / HOST).
 */
@Composable
fun AdminBadge(
    role: String,
    modifier: Modifier = Modifier
) {
    val cleanRole = role.uppercase()
    if (cleanRole != "ADMIN" && cleanRole != "SUPER_ADMIN" && cleanRole != "MODERATOR") return

    val isSuper = cleanRole == "SUPER_ADMIN"
    val gradientBrush = if (isSuper) {
        Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFFF9100), Color(0xFFFF1744)))
    } else if (cleanRole == "ADMIN") {
        Brush.horizontalGradient(listOf(Color(0xFFD50000), Color(0xFFFF5252)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF00B0FF), Color(0xFF0091EA)))
    }

    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .background(gradientBrush, RoundedCornerShape(8.dp))
            .border(0.8.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = if (isSuper) Icons.Default.VerifiedUser else Icons.Default.Shield,
            contentDescription = "Admin Badge",
            tint = Color.White,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = if (isSuper) "SUPER ADMIN" else if (cleanRole == "ADMIN") "ADMIN" else "MOD",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}
