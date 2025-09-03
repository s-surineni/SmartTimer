package com.example.smarttimer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {
    // Timer Group operations
    @Query("SELECT * FROM timer_groups ORDER BY createdAt ASC")
    fun getAllTimerGroups(): Flow<List<TimerGroup>>
    
    @Insert
    suspend fun insertTimerGroup(timerGroup: TimerGroup): Long
    
    @Update
    suspend fun updateTimerGroup(timerGroup: TimerGroup)
    
    @Delete
    suspend fun deleteTimerGroup(timerGroup: TimerGroup)
    
    // Timer operations
    @Query("SELECT * FROM timers WHERE groupId = :groupId ORDER BY createdAt ASC")
    fun getTimersByGroup(groupId: Long): Flow<List<Timer>>
    
    @Query("SELECT * FROM timers ORDER BY createdAt ASC")
    fun getAllTimers(): Flow<List<Timer>>
    
    @Insert
    suspend fun insertTimer(timer: Timer): Long
    
    @Update
    suspend fun updateTimer(timer: Timer)
    
    @Delete
    suspend fun deleteTimer(timer: Timer)
    
    @Query("SELECT * FROM timers WHERE isActive = 1")
    fun getActiveTimers(): Flow<List<Timer>>
    
    @Query("UPDATE timers SET isActive = 0, remainingTime = 0 WHERE id = :timerId")
    suspend fun stopTimer(timerId: Long)
    
    @Query("UPDATE timers SET remainingTime = :remainingTime WHERE id = :timerId")
    suspend fun updateTimerRemainingTime(timerId: Long, remainingTime: Long)
}
