package com.example.smarttimer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
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
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_service_channel"
    }
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
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
        
        val job = serviceScope.launch {
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
            
            // Update notification
            updateNotification()
        }
        
        activeJobs[timer.id] = job
        updateNotification()
    }
    
    fun stopTimer(timerId: Long) {
        activeJobs[timerId]?.cancel()
        activeJobs.remove(timerId)
        serviceScope.launch {
            timerRepository.stopTimer(timerId)
            _activeTimers.value = _activeTimers.value - timerId
            updateNotification()
        }
    }
    
    fun pauseTimer(timerId: Long) {
        activeJobs[timerId]?.cancel()
        activeJobs.remove(timerId)
        updateNotification()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active timers"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Smart Timer")
        .setContentText("${_activeTimers.value.size} active timer(s)")
        .setSmallIcon(R.drawable.ic_timer)
        .setOngoing(true)
        .build()
    
    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
    
    private fun Long.ifZero(default: () -> Long): Long = if (this == 0L) default() else this
}
