package com.example.smarttimer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smarttimer.data.Timer
import com.example.smarttimer.data.TimerGroupWithTimers
import com.example.smarttimer.ui.components.AddGroupDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    timerGroups: List<TimerGroupWithTimers>,
    currentGroupIndex: Int,
    activeTimers: Map<Long, Timer>,
    onAddGroup: (name: String, color: Int) -> Unit,
    onAddTimer: (name: String, duration: Long) -> Unit,
    onStartTimer: (Timer) -> Unit,
    onStopTimer: (Long) -> Unit,
    onDeleteTimer: (Timer) -> Unit,
    onDeleteGroup: (TimerGroupWithTimers) -> Unit,
    onGroupIndexChange: (Int) -> Unit,
    formatTime: (Long) -> String,
    predefinedDurations: List<Pair<String, Long>>,
    modifier: Modifier = Modifier
) {
    var showAddGroupDialog by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { timerGroups.size })
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(currentGroupIndex) {
        if (currentGroupIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(currentGroupIndex)
        }
    }
    
    LaunchedEffect(pagerState.currentPage) {
        onGroupIndexChange(pagerState.currentPage)
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top app bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Smart Timer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showAddGroupDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add group"
                )
            }
        }
        
        // Group indicator
        if (timerGroups.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timerGroups.forEachIndexed { index, group ->
                    val isSelected = index == currentGroupIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        label = { Text(group.timerGroup.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
        
        // Content
        if (timerGroups.isEmpty()) {
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
                        text = "No timer groups yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create your first group to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { showAddGroupDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add group"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Group")
                    }
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val group = timerGroups[page]
                TimerGroupScreen(
                    timerGroup = group,
                    activeTimers = activeTimers,
                    onAddTimer = onAddTimer,
                    onStartTimer = onStartTimer,
                    onStopTimer = onStopTimer,
                    onDeleteTimer = onDeleteTimer,
                    onDeleteGroup = { onDeleteGroup(group) },
                    formatTime = formatTime,
                    predefinedDurations = predefinedDurations,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    
    if (showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { name, color ->
                onAddGroup(name, color)
                showAddGroupDialog = false
            }
        )
    }
}
