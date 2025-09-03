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
    private var serviceContext: Context? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_service_channel"
        private const val TIMER_FINISHED_CHANNEL_ID = "timer_finished_channel"
        private const val ACTION_STOP_TIMER = "com.example.smarttimer.STOP_TIMER"
        private const val ACTION_DISMISS_NOTIFICATION = "com.example.smarttimer.DISMISS_NOTIFICATION"
    }
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    override fun onCreate() {
        super.onCreate()
        serviceContext = this
        createNotificationChannels()
        val notification = createNotification()
        if (notification != null) {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_TIMER -> {
                val timerId = intent.getLongExtra("timer_id", -1L)
                if (timerId != -1L) {
                    stopTimer(timerId)
                }
            }
            ACTION_DISMISS_NOTIFICATION -> {
                val notificationManager = serviceContext?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_ID + 1)
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
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
                    
                    delay(1000) // Update every second
                    remainingTime -= 1000
                }
                
                // Timer finished
                timerRepository.stopTimer(timer.id)
                _activeTimers.value = _activeTimers.value - timer.id
                activeJobs.remove(timer.id)
                
                // Play sound and show completion notification on main thread
                withContext(Dispatchers.Main) {
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
        activeJobs[timerId]?.cancel()
        activeJobs.remove(timerId)
        serviceScope.launch {
            try {
                if (::timerRepository.isInitialized) {
                    timerRepository.stopTimer(timerId)
                }
                _activeTimers.value = _activeTimers.value - timerId
                updateNotification()
            } catch (e: Exception) {
                android.util.Log.e("TimerService", "Error stopping timer", e)
                _activeTimers.value = _activeTimers.value - timerId
                updateNotification()
            }
        }
    }
    
    fun pauseTimer(timerId: Long) {
        activeJobs[timerId]?.cancel()
        activeJobs.remove(timerId)
        updateNotification()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = serviceContext ?: return
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
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
            }
            
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(finishedChannel)
        }
    }
    
    private fun createNotification(): android.app.Notification? {
        val context = serviceContext ?: return null
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Smart Timer")
            .setContentText("${_activeTimers.value.size} active timer(s)")
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification() {
        try {
            val context = serviceContext ?: return
            val notification = createNotification() ?: return
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error updating notification", e)
        }
    }
    
    private fun playTimerFinishedSound() {
        try {
            val context = serviceContext ?: return
            
            // Play default notification sound
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
            
            // Vibrate
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 200, 500), -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error playing timer finished sound", e)
        }
    }
    
    private fun showTimerFinishedNotification(timer: Timer) {
        try {
            val context = serviceContext ?: return
            
            // Create intent to open app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Create stop timer intent
            val stopIntent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP_TIMER
                putExtra("timer_id", timer.id)
            }
            val stopPendingIntent = PendingIntent.getService(
                context, timer.id.toInt(), stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Create dismiss intent
            val dismissIntent = Intent(context, TimerService::class.java).apply {
                action = ACTION_DISMISS_NOTIFICATION
            }
            val dismissPendingIntent = PendingIntent.getService(
                context, 0, dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, TIMER_FINISHED_CHANNEL_ID)
                .setContentTitle("Timer Finished!")
                .setContentText("${timer.name} has completed")
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .addAction(R.drawable.ic_timer, "Stop Timer", stopPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                .build()
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
        } catch (e: Exception) {
            android.util.Log.e("TimerService", "Error showing timer finished notification", e)
        }
    }
    
    private fun Long.ifZero(default: () -> Long): Long = if (this == 0L) default() else this
}
