package com.example.smarttimer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.widget.RemoteViews
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.smarttimer.MainActivity
import com.example.smarttimer.R
import com.example.smarttimer.data.Timer
import com.example.smarttimer.data.TimerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class TimerService : Service() {
    private val binder = TimerBinder()
    private lateinit var timerRepository: TimerRepository
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _activeTimers = MutableStateFlow<Map<Long, Timer>>(emptyMap())
    val activeTimers: StateFlow<Map<Long, Timer>> = _activeTimers.asStateFlow()
    
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private var currentRingtone: android.media.Ringtone? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_service_channel"
        private const val TIMER_FINISHED_CHANNEL_ID = "timer_finished_channel"
        private const val ACTION_STOP_TIMER = "com.example.smarttimer.STOP_TIMER"
        private const val ACTION_DISMISS_NOTIFICATION = "com.example.smarttimer.DISMISS_NOTIFICATION"
        const val ACTION_STOP_ALL_TIMERS = "com.example.smarttimer.STOP_ALL_TIMERS"
    }
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("TimerService", "Service created")
        createNotificationChannels()
        val notification = createNotification()
        if (notification != null) {
            android.util.Log.d("TimerService", "Starting foreground service with notification: ${notification.extras.getString("android.title")} - ${notification.extras.getString("android.text")}")
            startForeground(NOTIFICATION_ID, notification)
        } else {
            android.util.Log.e("TimerService", "Failed to create notification for foreground service")
        }
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("TimerService", "onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_STOP_TIMER -> {
                val timerId = intent.getLongExtra("timer_id", -1L)
                android.util.Log.d("TimerService", "Stop timer action received for timer ID: $timerId")
                if (timerId != -1L) {
                    stopTimer(timerId)
                    // Also dismiss the timer finished notification if it exists
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    notificationManager?.cancel(NOTIFICATION_ID + 1)
                }
            }
            ACTION_STOP_ALL_TIMERS -> {
                android.util.Log.d("TimerService", "Stop all timers action received")
                stopAllTimers()
            }
            ACTION_DISMISS_NOTIFICATION -> {
                android.util.Log.d("TimerService", "Dismiss notification action received")
                stopAlarmSound()
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_ID + 1)
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        serviceScope.cancel()
    }
    
    fun setRepository(repository: TimerRepository) {
        timerRepository = repository
    }
    
    fun startTimer(timer: Timer) {
        if (activeJobs.containsKey(timer.id)) return
        
        if (!::timerRepository.isInitialized) {
            android.util.Log.e("TimerService", "Repository not initialized")
            return
        }
        
        val job = serviceScope.launch {
            try {
                var remainingTime = timer.remainingTime.ifZero { timer.duration }
                
                while (remainingTime > 0) {
                    // Update timer state
                    val updatedTimer = timer.copy(
                        isActive = true,
                        remainingTime = remainingTime,
                        lastStartedAt = System.currentTimeMillis()
                    )
                    
                    _activeTimers.value = _activeTimers.value + (timer.id to updatedTimer)
                    timerRepository.updateTimer(updatedTimer)
                    
                    // Update notification every second to show countdown
                    withContext(Dispatchers.Main) {
                        updateNotification()
                    }
                    
                    delay(1000) // Update every second
                    remainingTime -= 1000
                }
                
                // Timer finished
                timerRepository.stopTimer(timer.id)
                _activeTimers.value = _activeTimers.value - timer.id
                activeJobs.remove(timer.id)
                
                                    // Play sound and show completion notification on main thread
                    android.util.Log.d("TimerService", "Timer finished, about to play sound and show notification")
                    withContext(Dispatchers.Main) {
                        android.util.Log.d("TimerService", "On main thread, playing sound and notification")
                        playTimerFinishedSound()
                        showTimerFinishedNotification(timer)
                    }
                
                // Update notification on main thread
                withContext(Dispatchers.Main) {
                    updateNotification()
                }
            } catch (e: Exception) {
                android.util.Log.e("TimerService", "Error in timer job", e)
                _activeTimers.value = _activeTimers.value - timer.id
                activeJobs.remove(timer.id)
                withContext(Dispatchers.Main) {
                    updateNotification()
                }
            }
        }
        
        activeJobs[timer.id] = job
        serviceScope.launch(Dispatchers.Main) {
            updateNotification()
        }
    }
    
    fun stopTimer(timerId: Long) {
        android.util.Log.d("TimerService", "stopTimer called for timer ID: $timerId")
        activeJobs[timerId]?.cancel()
        activeJobs.remove(timerId)
        
        // Stop alarm sound when timer is stopped
        stopAlarmSound()
        
        // Cancel individual timer notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(timerId.toInt() + 2000)
        
        serviceScope.launch {
            try {
                if (::timerRepository.isInitialized) {
                    timerRepository.stopTimer(timerId)
                }
                _activeTimers.value = _activeTimers.value - timerId
                android.util.Log.d("TimerService", "Timer $timerId stopped successfully")
                updateNotification()
            } catch (e: Exception) {
                android.util.Log.e("TimerService", "Error stopping timer", e)
                _activeTimers.value = _activeTimers.value - timerId
                updateNotification()
            }
        }
    }
    
    fun stopAllTimers() {
        val activeTimerIds = _activeTimers.value.keys.toList()
        activeTimerIds.forEach { timerId ->
            stopTimer(timerId)
        }
    }
    
    fun pauseTimer(timerId: Long) {
        activeJobs[timerId]?.cancel()
        activeJobs.remove(timerId)
        updateNotification()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Service channel for active timers
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active timers"
            }
            
            // Timer finished channel with sound
            val finishedChannel = NotificationChannel(
                TIMER_FINISHED_CHANNEL_ID,
                "Timer Finished",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when timers finish"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                // Set alarm sound for this channel
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
            }
            
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(finishedChannel)
        }
    }
    
    private fun createNotification(): android.app.Notification? {
        val activeTimers = _activeTimers.value
        
        // Create intent to open the app when notification is tapped
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return if (activeTimers.isEmpty()) {
            // No active timers - show basic service notification
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Smart Timer")
                .setContentText("No active timers")
                .setSmallIcon(R.drawable.ic_timer)
                .setOngoing(true)
                .setContentIntent(openAppPendingIntent)
                .build()
        } else {
            createActiveTimersNotification(activeTimers, openAppPendingIntent)
        }
    }
    
    private fun createActiveTimersNotification(
        activeTimers: Map<Long, Timer>,
        openAppPendingIntent: PendingIntent
    ): android.app.Notification {
        // Create individual notifications for each timer
        createIndividualTimerNotifications(activeTimers, openAppPendingIntent)
        
        // Return a simple service notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Timer")
            .setContentText("${activeTimers.size} timers running")
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPendingIntent)
            .build()
    }
    
    private fun createIndividualTimerNotifications(
        activeTimers: Map<Long, Timer>,
        openAppPendingIntent: PendingIntent
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        activeTimers.forEach { (timerId, timer) ->
            val remainingTime = formatTime(timer.remainingTime)
            
            // Create stop timer action
            val stopTimerIntent = Intent(this, TimerService::class.java).apply {
                action = ACTION_STOP_TIMER
                putExtra("timer_id", timerId)
            }
            val stopTimerPendingIntent = PendingIntent.getService(
                this, timerId.toInt() + 1000, stopTimerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Create custom notification layout
            val customView = RemoteViews(packageName, R.layout.notification_timer)
            customView.setTextViewText(R.id.title, timer.getDisplayName())
            customView.setTextViewText(R.id.text, "Time remaining: $remainingTime")
            customView.setOnClickPendingIntent(R.id.stop_button, stopTimerPendingIntent)
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setCustomContentView(customView)
                .setCustomBigContentView(customView)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setShowWhen(false)
                .setContentIntent(openAppPendingIntent)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .build()
            
            // Use timer ID as notification ID to create separate notifications
            notificationManager.notify(timerId.toInt() + 2000, notification)
        }
    }
    
    private fun updateNotification() {
        try {
            val notification = createNotification() ?: return
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            android.util.Log.d("TimerService", "Updating notification: ${notification.extras.getString("android.title")} - ${notification.extras.getString("android.text")}")
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            // Also update individual timer notifications
            val activeTimers = _activeTimers.value
            if (activeTimers.isNotEmpty()) {
                val openAppIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val openAppPendingIntent = PendingIntent.getActivity(
                    this, 0, openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                createIndividualTimerNotifications(activeTimers, openAppPendingIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error updating notification", e)
        }
    }
    
    private fun playTimerFinishedSound() {
        try {
            android.util.Log.d("TimerService", "playTimerFinishedSound - using alarm sound")
            
            // Stop any existing ringtone first
            stopAlarmSound()
            
            // Play alarm sound instead of notification sound
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            currentRingtone = RingtoneManager.getRingtone(this, alarmUri)
            currentRingtone?.play()
            
            // Enhanced vibration pattern for alarm
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // More aggressive vibration pattern for alarm
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 200, 1000, 200, 1000), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 200, 1000, 200, 1000), -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error playing timer finished sound", e)
        }
    }
    
    private fun stopAlarmSound() {
        try {
            android.util.Log.d("TimerService", "stopAlarmSound called")
            currentRingtone?.stop()
            currentRingtone = null
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error stopping alarm sound", e)
        }
    }
    
    private fun showTimerFinishedNotification(timer: Timer) {
        try {
            android.util.Log.d("TimerService", "showTimerFinishedNotification - using service context")
            
            // Create intent to open app
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Create stop timer intent
            val stopIntent = Intent(this, TimerService::class.java).apply {
                action = ACTION_STOP_TIMER
                putExtra("timer_id", timer.id)
            }
            val stopPendingIntent = PendingIntent.getService(
                this, timer.id.toInt(), stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Create dismiss intent
            val dismissIntent = Intent(this, TimerService::class.java).apply {
                action = ACTION_DISMISS_NOTIFICATION
            }
            val dismissPendingIntent = PendingIntent.getService(
                this, 0, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(this, TIMER_FINISHED_CHANNEL_ID)
                .setContentTitle("Timer Finished!")
                .setContentText("${timer.getDisplayName()} has completed")
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(true)
                .setUsesChronometer(false)
                .addAction(R.drawable.ic_stop_inline, "Stop", stopPendingIntent)
                .addAction(R.drawable.ic_dismiss, "Dismiss", dismissPendingIntent)
                .build()
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error showing timer finished notification", e)
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
    
    private fun Long.ifZero(default: () -> Long): Long = if (this == 0L) default() else this
}
