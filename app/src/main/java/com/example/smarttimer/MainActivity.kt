package com.example.smarttimer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarttimer.data.Timer
import com.example.smarttimer.data.TimerDatabase
import com.example.smarttimer.data.TimerRepository
import com.example.smarttimer.service.TimerService
import com.example.smarttimer.ui.MainViewModel
import com.example.smarttimer.ui.TimerViewModel
import com.example.smarttimer.ui.screens.MainScreen
import com.example.smarttimer.ui.theme.SmartTimerTheme

class MainActivity : ComponentActivity() {
    private var timerService: TimerService? = null
    private var isBound = false
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true
        }
        
        override fun onServiceDisconnected(arg0: ComponentName?) {
            isBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start and bind to timer service
        Intent(this, TimerService::class.java).also { intent ->
            startService(intent)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        
        setContent {
            SmartTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartTimerApp(timerService)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

@Composable
fun SmartTimerApp(timerService: TimerService?) {
    val context = LocalContext.current
    val database = TimerDatabase.getDatabase(context)
    val repository = TimerRepository(database.timerDao())
    
    // Set repository on the service if it's available
    LaunchedEffect(timerService) {
        timerService?.setRepository(repository)
    }
    
    val mainViewModel: MainViewModel = viewModel { MainViewModel(repository) }
    
    // Only create TimerViewModel when service is available
    val timerViewModel: TimerViewModel? = if (timerService != null) {
        viewModel { TimerViewModel(repository, timerService) }
    } else {
        null
    }
    
    val timerGroups by mainViewModel.timerGroups.collectAsStateWithLifecycle()
    val currentGroupIndex by mainViewModel.currentGroupIndex.collectAsStateWithLifecycle()
    val activeTimers = if (timerViewModel != null) {
        timerViewModel.activeTimers.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(emptyMap<Long, Timer>()) }
    }
    
    // Load timers for current group
    LaunchedEffect(currentGroupIndex, timerGroups) {
        if (timerGroups.isNotEmpty() && currentGroupIndex < timerGroups.size) {
            val currentGroup = timerGroups[currentGroupIndex]
            timerViewModel?.loadTimersForGroup(currentGroup.timerGroup.id)
        }
    }
    

    
    MainScreen(
        timerGroups = timerGroups,
        currentGroupIndex = currentGroupIndex,
        activeTimers = activeTimers.value,
        onAddGroup = { name, color ->
            mainViewModel.addTimerGroup(name, color)
        },
        onAddTimer = { name, duration ->
            if (timerGroups.isNotEmpty() && currentGroupIndex < timerGroups.size) {
                val currentGroup = timerGroups[currentGroupIndex]
                timerViewModel?.addTimer(name, duration, currentGroup.timerGroup.id)
            }
        },
        onStartTimer = { timer ->
            timerViewModel?.startTimer(timer)
        },
        onStopTimer = { timerId ->
            timerViewModel?.stopTimer(timerId)
        },
        onDeleteTimer = { timer ->
            timerViewModel?.deleteTimer(timer)
        },
        onDeleteGroup = { group ->
            mainViewModel.deleteTimerGroup(group.timerGroup)
        },
        onGroupIndexChange = { index ->
            mainViewModel.setCurrentGroupIndex(index)
        },
        formatTime = { milliseconds ->
            timerViewModel?.formatTime(milliseconds) ?: "00:00"
        },
        predefinedDurations = timerViewModel?.getPredefinedDurations() ?: emptyList()
    )
}
