package com.example.smarttimer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimerRepository(private val timerDao: TimerDao) {
    
    // Timer Group operations
    fun getAllTimerGroups(): Flow<List<TimerGroup>> = timerDao.getAllTimerGroups()
    
    suspend fun insertTimerGroup(timerGroup: TimerGroup): Long = timerDao.insertTimerGroup(timerGroup)
    
    suspend fun updateTimerGroup(timerGroup: TimerGroup) = timerDao.updateTimerGroup(timerGroup)
    
    suspend fun deleteTimerGroup(timerGroup: TimerGroup) = timerDao.deleteTimerGroup(timerGroup)
    
    // Timer operations
    fun getTimersByGroup(groupId: Long): Flow<List<Timer>> = timerDao.getTimersByGroup(groupId)
    
    fun getAllTimers(): Flow<List<Timer>> = timerDao.getAllTimers()
    
    suspend fun insertTimer(timer: Timer): Long = timerDao.insertTimer(timer)
    
    suspend fun updateTimer(timer: Timer) = timerDao.updateTimer(timer)
    
    suspend fun deleteTimer(timer: Timer) = timerDao.deleteTimer(timer)
    
    fun getActiveTimers(): Flow<List<Timer>> = timerDao.getActiveTimers()
    
    suspend fun stopTimer(timerId: Long) = timerDao.stopTimer(timerId)
    
    suspend fun updateTimerRemainingTime(timerId: Long, remainingTime: Long) = 
        timerDao.updateTimerRemainingTime(timerId, remainingTime)
    
    // Combined data for UI
    fun getTimerGroupsWithTimers(): Flow<List<TimerGroupWithTimers>> {
        return getAllTimerGroups().map { groups ->
            groups.map { group ->
                TimerGroupWithTimers(group, emptyList()) // We'll populate timers separately
            }
        }
    }
}

data class TimerGroupWithTimers(
    val timerGroup: TimerGroup,
    val timers: List<Timer>
)
