package com.example.smarttimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddTimerDialog(
    onDismiss: () -> Unit,
    onConfirm: (duration: Long) -> Unit
) {
    var customMinutes by remember { mutableStateOf("") }
    var customSeconds by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add New Timer")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter timer duration:",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Text(
                    text = "Enter time in minutes and/or seconds:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        label = { Text("Minutes") },
                        placeholder = { Text("0") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customSeconds,
                        onValueChange = { customSeconds = it },
                        label = { Text("Seconds") },
                        placeholder = { Text("0") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = customMinutes.toLongOrNull() ?: 0L
                    val seconds = customSeconds.toLongOrNull() ?: 0L
                    val duration = (minutes * 60 + seconds) * 1000L
                    
                    if (duration > 0) {
                        onConfirm(duration)
                        onDismiss()
                    }
                },
                enabled = {
                    val minutes = customMinutes.toLongOrNull() ?: 0L
                    val seconds = customSeconds.toLongOrNull() ?: 0L
                    (customMinutes.isNotBlank() || customSeconds.isNotBlank()) && (minutes > 0 || seconds > 0)
                }()
            ) {
                Text("Add Timer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
