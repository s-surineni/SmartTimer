package com.example.smarttimer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddTimerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, duration: Long) -> Unit,
    predefinedDurations: List<Pair<String, Long>>
) {
    var timerName by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf<Long?>(null) }
    var customMinutes by remember { mutableStateOf("") }
    var customSeconds by remember { mutableStateOf("") }
    var isCustomDuration by remember { mutableStateOf(false) }
    
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
                OutlinedTextField(
                    value = timerName,
                    onValueChange = { timerName = it },
                    label = { Text("Timer Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Select Duration:",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCustomDuration,
                        onClick = { isCustomDuration = false },
                        label = { Text("Predefined") }
                    )
                    FilterChip(
                        selected = isCustomDuration,
                        onClick = { isCustomDuration = true },
                        label = { Text("Custom") }
                    )
                }
                
                if (isCustomDuration) {
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
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(predefinedDurations) { (name, duration) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedDuration == duration) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surface
                                ),
                                onClick = { selectedDuration = duration }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name)
                                    if (selectedDuration == duration) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Selected"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = when {
                        isCustomDuration -> {
                            val minutes = customMinutes.toLongOrNull() ?: 0L
                            val seconds = customSeconds.toLongOrNull() ?: 0L
                            (minutes * 60 + seconds) * 1000L
                        }
                        else -> selectedDuration ?: 0L
                    }
                    
                    if (timerName.isNotBlank() && duration > 0) {
                        onConfirm(timerName, duration)
                        onDismiss()
                    }
                },
                enabled = timerName.isNotBlank() && 
                    (if (isCustomDuration) {
                        val minutes = customMinutes.toLongOrNull() ?: 0L
                        val seconds = customSeconds.toLongOrNull() ?: 0L
                        (customMinutes.isNotBlank() || customSeconds.isNotBlank()) && (minutes > 0 || seconds > 0)
                    } else selectedDuration != null)
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
