package com.example.ui.screens.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReportProblem
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
import com.example.data.model.ReportCategories

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportDialog(
    targetType: String = "USER", // "USER", "MESSAGE", "ROOM", "GIFT"
    targetId: String,
    targetUid: String,
    targetDisplayName: String,
    viewModel: ModerationViewModel,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf(ReportCategories.SPAM) }
    var description by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val statusNotice by viewModel.statusNotice.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusNotice) {
        statusNotice?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotice()
            if (it.contains("submitted", ignoreCase = true)) {
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Report Content",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Target: $targetDisplayName ($targetType)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Target UID: $targetUid",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        "Select a Reason",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // FlowRow for Category selection chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ReportCategories.ALL_CATEGORIES.forEach { category ->
                            FilterChip(
                                selected = selectedReason == category,
                                onClick = { selectedReason = category },
                                label = { Text(category, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { if (it.length <= 500) description = it },
                        label = { Text("Details & Description (Optional)") },
                        placeholder = { Text("Please explain what happened...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("report_description_input"),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = { Text("${description.length}/500") }
                    )

                    OutlinedTextField(
                        value = evidence,
                        onValueChange = { if (it.length <= 300) evidence = it },
                        label = { Text("Evidence / Message Link (Optional)") },
                        placeholder = { Text("e.g., Message timestamp, room context") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_evidence_input"),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = { Text("${evidence.length}/300") }
                    )

                    Button(
                        onClick = {
                            viewModel.submitReport(
                                targetType = targetType,
                                targetId = targetId,
                                targetUid = targetUid,
                                targetDisplayName = targetDisplayName,
                                reason = selectedReason,
                                description = description,
                                evidence = evidence
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_report_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Submit Report", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
