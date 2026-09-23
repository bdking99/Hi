package com.example.ui.screens.moderation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppealDialog(
    actionId: String = "general",
    viewModel: ModerationViewModel,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Submit Moderation Appeal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    "If you believe your account restriction was applied in error, you may submit an appeal to the moderation team.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Appeal") },
                    placeholder = { Text("e.g., Mistaken identity, account compromised") },
                    modifier = Modifier.fillMaxWidth().testTag("appeal_reason_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Detailed Explanation") },
                    placeholder = { Text("Provide details and context...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("appeal_message_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.submitAppeal(
                                actionId = actionId,
                                reason = reason.trim(),
                                message = message.trim(),
                                onSuccess = onDismiss
                            )
                        },
                        enabled = reason.isNotBlank() && message.isNotBlank() && !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("submit_appeal_btn")
                    ) {
                        Text("Submit Appeal")
                    }
                }
            }
        }
    }
}
