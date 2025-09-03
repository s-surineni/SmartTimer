package com.example.smarttimer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TimerGroup::class, Timer::class],
    version = 1,
    exportSchema = false
)
abstract class TimerDatabase : RoomDatabase() {
    abstract fun timerDao(): TimerDao
    
    companion object {
        @Volatile
        private var INSTANCE: TimerDatabase? = null
        
        fun getDatabase(context: Context): TimerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimerDatabase::class.java,
                    "timer_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.timerDao())
                    }
                }
            }
        }
        
        private suspend fun populateDatabase(timerDao: TimerDao) {
            // Create default groups
            val defaultGroup = TimerGroup(
                name = "Default",
                color = android.graphics.Color.parseColor("#FF6200EE").toInt()
            )
            val workoutGroup = TimerGroup(
                name = "Workout",
                color = android.graphics.Color.parseColor("#FF03DAC6").toInt()
            )
            val cookingGroup = TimerGroup(
                name = "Cooking",
                color = android.graphics.Color.parseColor("#FFFF6B35").toInt()
            )
            
            // Insert groups and get their IDs
            val defaultGroupId = timerDao.insertTimerGroup(defaultGroup)
            val workoutGroupId = timerDao.insertTimerGroup(workoutGroup)
            val cookingGroupId = timerDao.insertTimerGroup(cookingGroup)
            
            // Add default timers to each group
            val defaultTimers = listOf(
                Timer(name = "5 Minute Break", duration = 5 * 60 * 1000L, groupId = defaultGroupId),
                Timer(name = "10 Minute Focus", duration = 10 * 60 * 1000L, groupId = defaultGroupId),
                Timer(name = "25 Minute Pomodoro", duration = 25 * 60 * 1000L, groupId = defaultGroupId)
            )
            
            val workoutTimers = listOf(
                Timer(name = "Plank Hold", duration = 60 * 1000L, groupId = workoutGroupId),
                Timer(name = "Rest Between Sets", duration = 90 * 1000L, groupId = workoutGroupId),
                Timer(name = "Cardio Session", duration = 20 * 60 * 1000L, groupId = workoutGroupId),
                Timer(name = "Stretching", duration = 5 * 60 * 1000L, groupId = workoutGroupId)
            )
            
            val cookingTimers = listOf(
                Timer(name = "Boil Water", duration = 5 * 60 * 1000L, groupId = cookingGroupId),
                Timer(name = "Rice Cooking", duration = 15 * 60 * 1000L, groupId = cookingGroupId),
                Timer(name = "Pasta", duration = 8 * 60 * 1000L, groupId = cookingGroupId),
                Timer(name = "Baking", duration = 25 * 60 * 1000L, groupId = cookingGroupId)
            )
            
            // Insert all timers
            (defaultTimers + workoutTimers + cookingTimers).forEach { timer ->
                timerDao.insertTimer(timer)
            }
        }
    }
}
