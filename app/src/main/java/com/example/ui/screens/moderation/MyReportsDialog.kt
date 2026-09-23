package com.example.ui.screens.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ReportRecord
import com.example.data.model.ReportStatuses
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsDialog(
    viewModel: ModerationViewModel,
    onDismiss: () -> Unit
) {
    val myReports by viewModel.myReports.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "My Reports & Safety",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (myReports.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No reports submitted", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "When you report harmful content or policy violations, their resolution status will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(myReports, key = { it.reportId }) { report ->
                                ReportItemCard(report = report)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItemCard(report: ReportRecord) {
    val dateFormatted = remember(report.createdAt) {
        SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(report.createdAt))
    }

    val statusColor = when (report.status) {
        ReportStatuses.PENDING -> Color(0xFFFF9800)
        ReportStatuses.UNDER_REVIEW -> Color(0xFF2196F3)
        ReportStatuses.RESOLVED, ReportStatuses.ACTION_TAKEN -> Color(0xFF4CAF50)
        ReportStatuses.DISMISSED, ReportStatuses.NO_ACTION -> Color(0xFF9E9E9E)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_item_${report.reportId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${report.targetType}: ${report.targetDisplayName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = report.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Reason: ${report.reason}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            if (report.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!report.resolutionNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Moderator Response:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            report.resolutionNotes,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Reported on $dateFormatted",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
