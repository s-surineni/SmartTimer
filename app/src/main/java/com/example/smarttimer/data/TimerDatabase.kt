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
    version = 2,
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
            android.util.Log.d("TimerDatabase", "Populating database with default data")
            
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
            timerDao.insertTimerGroup(workoutGroup)
            timerDao.insertTimerGroup(cookingGroup)
            
            android.util.Log.d("TimerDatabase", "Created groups, defaultGroupId: $defaultGroupId")
            
            // Add sample timers only to Default group
            val defaultTimers = listOf(
                Timer(name = "5 Seconds", duration = 5 * 1000L, groupId = defaultGroupId),
                Timer(name = "30 Seconds", duration = 30 * 1000L, groupId = defaultGroupId),
                Timer(name = "1 Minute", duration = 60 * 1000L, groupId = defaultGroupId)
            )
            
            // Insert only default group timers
            defaultTimers.forEach { timer ->
                val timerId = timerDao.insertTimer(timer)
                android.util.Log.d("TimerDatabase", "Inserted timer: ${timer.name} with ID: $timerId, duration: ${timer.duration}")
            }
            
            android.util.Log.d("TimerDatabase", "Database population completed")
        }
    }
}
