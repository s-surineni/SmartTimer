package com.example.smarttimer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttimer.data.Timer
import com.example.smarttimer.data.TimerRepository
import com.example.smarttimer.service.TimerService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TimerViewModel(
    private val repository: TimerRepository,
    private val timerService: TimerService
) : ViewModel() {
    
    private val _timers = MutableStateFlow<List<Timer>>(emptyList())
    val timers: StateFlow<List<Timer>> = _timers.asStateFlow()
    
    private val _activeTimers = MutableStateFlow<Map<Long, Timer>>(emptyMap())
    val activeTimers: StateFlow<Map<Long, Timer>> = _activeTimers.asStateFlow()
    
    init {
        timerService.setRepository(repository)
        observeActiveTimers()
    }
    
    private fun observeActiveTimers() {
        viewModelScope.launch {
            timerService.activeTimers.collect { activeTimers ->
                _activeTimers.value = activeTimers
            }
        }
    }
    
    fun loadTimersForGroup(groupId: Long) {
        viewModelScope.launch {
            repository.getTimersByGroup(groupId).collect { timers ->
                _timers.value = timers
            }
        }
    }
    
    fun addTimer(name: String, duration: Long, groupId: Long) {
        viewModelScope.launch {
            val newTimer = Timer(
                name = name,
                duration = duration,
                groupId = groupId
            )
            repository.insertTimer(newTimer)
        }
    }
    
    fun deleteTimer(timer: Timer) {
        viewModelScope.launch {
            if (timer.isActive) {
                timerService.stopTimer(timer.id)
            }
            repository.deleteTimer(timer)
        }
    }
    
    fun startTimer(timer: Timer) {
        timerService.startTimer(timer)
    }
    
    fun stopTimer(timerId: Long) {
        timerService.stopTimer(timerId)
    }
    
    fun pauseTimer(timerId: Long) {
        timerService.pauseTimer(timerId)
    }
    
    fun getActiveTimer(timerId: Long): Timer? {
        return _activeTimers.value[timerId]
    }
    
    fun isTimerActive(timerId: Long): Boolean {
        return _activeTimers.value.containsKey(timerId)
    }
    
    fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    fun getPredefinedDurations(): List<Pair<String, Long>> {
        return listOf(
            "5 minutes" to (5 * 60 * 1000L),
            "10 minutes" to (10 * 60 * 1000L),
            "15 minutes" to (15 * 60 * 1000L),
            "30 minutes" to (30 * 60 * 1000L),
            "1 hour" to (60 * 60 * 1000L),
            "2 hours" to (2 * 60 * 60 * 1000L)
        )
    }
}
