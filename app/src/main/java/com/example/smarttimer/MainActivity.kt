package com.example.smarttimer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
    private var _timerService = mutableStateOf<TimerService?>(null)
    private var isBound = false
    
    // Notification permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
        } else {
            android.util.Log.w("MainActivity", "Notification permission denied")
        }
    }
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            android.util.Log.d("MainActivity", "Service connected")
            val binder = service as TimerService.TimerBinder
            _timerService.value = binder.getService()
            isBound = true
            android.util.Log.d("MainActivity", "Service bound successfully")
        }
        
        override fun onServiceDisconnected(arg0: ComponentName?) {
            android.util.Log.d("MainActivity", "Service disconnected")
            _timerService.value = null
            isBound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    android.util.Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    android.util.Log.d("MainActivity", "Requesting notification permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        
        // Start and bind to timer service
        android.util.Log.d("MainActivity", "Starting and binding to TimerService")
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
                    SmartTimerApp(_timerService.value)
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

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun SmartTimerApp(timerService: TimerService?) {
    val context = LocalContext.current
    val database = TimerDatabase.getDatabase(context)
    val repository = TimerRepository(database.timerDao())
    
    // Track if service is ready
    var isServiceReady by remember { mutableStateOf(false) }
    
    // Set repository on the service if it's available
    LaunchedEffect(timerService) {
        android.util.Log.d("MainActivity", "LaunchedEffect triggered, timerService: ${timerService != null}")
        if (timerService != null) {
            timerService.setRepository(repository)
            isServiceReady = true
            android.util.Log.d("MainActivity", "Service is ready for UI")
        } else {
            isServiceReady = false
            android.util.Log.d("MainActivity", "Service is not ready for UI")
        }
    }
    
    // Also check if service became available after initial render
    LaunchedEffect(Unit) {
        // This effect runs once and then we'll rely on the timerService parameter
        android.util.Log.d("MainActivity", "Initial LaunchedEffect - timerService: ${timerService != null}")
    }
    
    val mainViewModel: MainViewModel = viewModel { MainViewModel(repository) }
    
    // Only create TimerViewModel when service is available and ready
    val timerViewModel: TimerViewModel? = if (timerService != null && isServiceReady) {
        android.util.Log.d("MainActivity", "Creating TimerViewModel - service is ready")
        viewModel { TimerViewModel(repository, timerService) }
    } else {
        android.util.Log.d("MainActivity", "TimerViewModel not created - timerService: ${timerService != null}, isServiceReady: $isServiceReady")
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
        onAddTimer = { duration ->
            if (timerGroups.isNotEmpty() && currentGroupIndex < timerGroups.size) {
                val currentGroup = timerGroups[currentGroupIndex]
                timerViewModel?.addTimer(duration, currentGroup.timerGroup.id)
            }
        },
        onStartTimer = { timer ->
            if (timerViewModel != null) {
                timerViewModel.startTimer(timer)
            } else if (!isServiceReady) {
                // Service is still binding
                android.widget.Toast.makeText(
                    context,
                    "Timer service is starting up. Please wait a moment.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                // Service binding failed
                android.widget.Toast.makeText(
                    context,
                    "Timer service is not available. Please restart the app.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
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
            formatTime(milliseconds)
        }
    )
}
