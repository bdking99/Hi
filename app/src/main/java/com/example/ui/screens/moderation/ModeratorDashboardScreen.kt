package com.example.ui.screens.moderation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeratorDashboardScreen(
    viewModel: ModerationViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Reports Queue, 1: Audit Log, 2: Direct Action, 3: Appeals
    val reports by viewModel.allReports.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val appeals by viewModel.appeals.collectAsState()
    val statusNotice by viewModel.statusNotice.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusNotice) {
        statusNotice?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotice()
        }
    }

    var selectedReportForAction by remember { mutableStateOf<ReportRecord?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Moderation Center", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Reports (${reports.count { it.status == ReportStatuses.PENDING }})", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Audit Logs", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Quick Action", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Appeals (${appeals.count { it.status == "PENDING" }})", fontSize = 12.sp) }
                )
            }

            when (selectedTab) {
                0 -> ReportsQueueTab(
                    reports = reports,
                    onSelectReport = { selectedReportForAction = it }
                )
                1 -> AuditLogsTab(auditLogs = auditLogs)
                2 -> DirectActionTab(viewModel = viewModel)
                3 -> AppealsTab(appeals = appeals, viewModel = viewModel)
            }
        }
    }

    // Modal Sheet or Dialog when a report is selected to resolve
    selectedReportForAction?.let { report ->
        ResolveReportDialog(
            report = report,
            viewModel = viewModel,
            onDismiss = { selectedReportForAction = null }
        )
    }
}

@Composable
fun ReportsQueueTab(
    reports: List<ReportRecord>,
    onSelectReport: (ReportRecord) -> Unit
) {
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reports found in queue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reports, key = { it.reportId }) { report ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectReport(report) }
                        .testTag("mod_report_${report.reportId}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (report.status == ReportStatuses.PENDING) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${report.targetDisplayName} (${report.targetType})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (report.status == ReportStatuses.PENDING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            ) {
                                Text(
                                    text = report.status,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Violation: ${report.reason}",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        if (report.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Details: ${report.description}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (report.evidence.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Evidence: ${report.evidence}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Reporter: ${report.reporterUid.take(8)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Click to take action →",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResolveReportDialog(
    report: ReportRecord,
    viewModel: ModerationViewModel,
    onDismiss: () -> Unit
) {
    var selectedAction by remember { mutableStateOf(ModerationActions.WARN) }
    var notes by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("60") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Report: ${report.targetDisplayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Violation: ${report.reason}", fontWeight = FontWeight.Bold)
                if (report.description.isNotBlank()) {
                    Text("Details: ${report.description}", fontSize = 13.sp)
                }
                HorizontalDivider()
                Text("Select Action:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == ModerationActions.WARN, onClick = { selectedAction = ModerationActions.WARN })
                        Text("Issue Warning")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == ModerationActions.MUTE, onClick = { selectedAction = ModerationActions.MUTE })
                        Text("Mute Chat (Timed)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == ModerationActions.SUSPEND, onClick = { selectedAction = ModerationActions.SUSPEND })
                        Text("Suspend Account (Timed)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == ModerationActions.BAN, onClick = { selectedAction = ModerationActions.BAN })
                        Text("Permanent Ban")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedAction == "DISMISS", onClick = { selectedAction = "DISMISS" })
                        Text("Dismiss Report (No Violation)")
                    }
                }

                if (selectedAction in listOf(ModerationActions.MUTE, ModerationActions.SUSPEND)) {
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        label = { Text("Duration in minutes (e.g. 60)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Moderator Notes") },
                    placeholder = { Text("Reason for resolution...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedAction == "DISMISS") {
                        viewModel.updateReportStatus(report.reportId, ReportStatuses.DISMISSED, notes.ifBlank { "Dismissed: No violation found." })
                    } else {
                        val durationMs = (durationMinutes.toLongOrNull() ?: 60L) * 60 * 1000L
                        viewModel.executeModerationAction(
                            targetUid = report.targetUid,
                            action = selectedAction,
                            reason = "${report.reason}: $notes",
                            durationMs = durationMs,
                            reportId = report.reportId
                        )
                    }
                    onDismiss()
                }
            ) {
                Text("Confirm Action")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AuditLogsTab(auditLogs: List<ModerationAuditLog>) {
    if (auditLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No moderation logs recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(auditLogs, key = { it.logId }) { log ->
                val timeFormatted = remember(log.createdAt) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.createdAt))
                }
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Action: ${log.action}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(text = timeFormatted, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Target UID: ${log.targetUid}", fontSize = 12.sp)
                        Text(text = "Actor: ${log.actorDisplayName} (${log.actorRole})", fontSize = 12.sp)
                        Text(text = "Reason: ${log.reason}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun DirectActionTab(viewModel: ModerationViewModel) {
    var targetUid by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf(ModerationActions.WARN) }
    var reason by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("60") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Direct Moderator Enforcement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = targetUid,
            onValueChange = { targetUid = it },
            label = { Text("Target Firebase User UID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Text("Action Type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedAction == ModerationActions.WARN,
                onClick = { selectedAction = ModerationActions.WARN },
                label = { Text("Warn") }
            )
            FilterChip(
                selected = selectedAction == ModerationActions.MUTE,
                onClick = { selectedAction = ModerationActions.MUTE },
                label = { Text("Mute") }
            )
            FilterChip(
                selected = selectedAction == ModerationActions.SUSPEND,
                onClick = { selectedAction = ModerationActions.SUSPEND },
                label = { Text("Suspend") }
            )
            FilterChip(
                selected = selectedAction == ModerationActions.BAN,
                onClick = { selectedAction = ModerationActions.BAN },
                label = { Text("Ban") }
            )
        }

        if (selectedAction in listOf(ModerationActions.MUTE, ModerationActions.SUSPEND)) {
            OutlinedTextField(
                value = durationMinutes,
                onValueChange = { durationMinutes = it },
                label = { Text("Duration (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason for Enforcement") },
            placeholder = { Text("Explain violation...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = {
                val durMs = (durationMinutes.toLongOrNull() ?: 60L) * 60 * 1000L
                viewModel.executeModerationAction(
                    targetUid = targetUid.trim(),
                    action = selectedAction,
                    reason = reason.trim(),
                    durationMs = durMs
                )
            },
            enabled = targetUid.isNotBlank() && reason.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Execute Action", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AppealsTab(appeals: List<AppealRecord>, viewModel: ModerationViewModel) {
    if (appeals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No user appeals pending.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(appeals, key = { it.appealId }) { appeal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("User: ${appeal.userUid.take(8)}...", fontWeight = FontWeight.Bold)
                            Text(appeal.status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Reason: ${appeal.reason}", style = MaterialTheme.typography.bodyMedium)
                        Text("Message: ${appeal.message}", style = MaterialTheme.typography.bodySmall)

                        if (appeal.status == "PENDING") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resolveAppeal(appeal.appealId, "REJECTED", "Appeal rejected.") },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Reject", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.resolveAppeal(appeal.appealId, "ACCEPTED", "Appeal accepted. Restriction lifted.") },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Accept & Restore", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
