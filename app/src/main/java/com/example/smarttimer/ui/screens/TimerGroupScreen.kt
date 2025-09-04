package com.example.smarttimer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smarttimer.data.Timer
import com.example.smarttimer.data.TimerGroupWithTimers
import com.example.smarttimer.ui.components.AddTimerDialog
import com.example.smarttimer.ui.components.TimerCard

@Composable
fun TimerGroupScreen(
    timerGroup: TimerGroupWithTimers,
    activeTimers: Map<Long, Timer>,
    onAddTimer: (duration: Long) -> Unit,
    onStartTimer: (Timer) -> Unit,
    onStopTimer: (Long) -> Unit,
    onDeleteTimer: (Timer) -> Unit,
    onDeleteGroup: () -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier
) {
    var showAddTimerDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header with group info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(timerGroup.timerGroup.color).copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = timerGroup.timerGroup.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${timerGroup.timers.size} timer(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(onClick = { showAddTimerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add timer"
                        )
                    }
                    
                    IconButton(onClick = onDeleteGroup) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete group",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        // Timers list
        if (timerGroup.timers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No timers yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { showAddTimerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add timer"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Your First Timer")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(timerGroup.timers) { timer ->
                    val isActive = activeTimers.containsKey(timer.id)
                    val activeTimer = activeTimers[timer.id]
                    val remainingTime = activeTimer?.remainingTime ?: timer.duration
                    
                    TimerCard(
                        timer = timer,
                        isActive = isActive,
                        remainingTime = remainingTime,
                        onStart = { onStartTimer(timer) },
                        onStop = { onStopTimer(timer.id) },
                        onDelete = { onDeleteTimer(timer) },
                        formatTime = formatTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    if (showAddTimerDialog) {
        AddTimerDialog(
            onDismiss = { showAddTimerDialog = false },
            onConfirm = { duration ->
                onAddTimer(duration)
                showAddTimerDialog = false
            }
        )
    }
}
