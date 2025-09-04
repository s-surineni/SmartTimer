package com.example.smarttimer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smarttimer.data.Timer
import com.example.smarttimer.data.TimerGroupWithTimers
import com.example.smarttimer.ui.components.AddGroupDialog
import com.example.smarttimer.ui.components.TimerCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    timerGroups: List<TimerGroupWithTimers>,
    currentGroupIndex: Int,
    activeTimers: Map<Long, Timer>,
    onAddGroup: (name: String, color: Int) -> Unit,
    onAddTimer: (name: String?, duration: Long) -> Unit,
    onStartTimer: (Timer) -> Unit,
    onStopTimer: (Long) -> Unit,
    onDeleteTimer: (Timer) -> Unit,
    onDeleteGroup: (TimerGroupWithTimers) -> Unit,
    onGroupIndexChange: (Int) -> Unit,
    formatTime: (Long) -> String,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { timerGroups.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Filter timers based on search query
    val filteredTimerGroups = remember(timerGroups, searchQuery) {
        if (searchQuery.isBlank()) {
            timerGroups
        } else {
            timerGroups.map { group ->
                val filteredTimers = group.timers.filter { timer ->
                    timer.getDisplayName().contains(searchQuery, ignoreCase = true)
                }
                group.copy(timers = filteredTimers)
            }.filter { it.timers.isNotEmpty() }
        }
    }
    
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { 
                        isSearchMode = !isSearchMode
                        if (!isSearchMode) {
                            searchQuery = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchMode) "Close Search" else "Search"
                    )
                }
                IconButton(onClick = { showAddGroupDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add group"
                    )
                }
            }
        }
        
        // Search Bar
        if (isSearchMode) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search timers by name...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
        }
        
        // Group indicator (only show when not in search mode)
        if (timerGroups.isNotEmpty() && !isSearchMode) {
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
        
        // Search results header
        if (isSearchMode && searchQuery.isNotEmpty()) {
            Text(
                text = "Search Results (${filteredTimerGroups.sumOf { it.timers.size }} timers found)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
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
        } else if (isSearchMode) {
            // Search results view
            if (filteredTimerGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No timers found matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredTimerGroups.forEach { group ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = group.timerGroup.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(group.timerGroup.color)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    group.timers.forEach { timer ->
                                        TimerCard(
                                            timer = timer,
                                            isActive = activeTimers.containsKey(timer.id),
                                            remainingTime = activeTimers[timer.id]?.remainingTime ?: timer.duration,
                                            onStart = { onStartTimer(timer) },
                                            onStop = { onStopTimer(timer.id) },
                                            onDelete = { onDeleteTimer(timer) },
                                            formatTime = formatTime,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Normal group view
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
