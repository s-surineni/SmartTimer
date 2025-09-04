package com.example.smarttimer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Entity(
    tableName = "timers",
    foreignKeys = [
        ForeignKey(
            entity = TimerGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@Parcelize
data class Timer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String? = null, // Optional name, will be generated from duration if null
    val duration: Long, // Duration in milliseconds
    val groupId: Long,
    val isActive: Boolean = false,
    val remainingTime: Long = 0, // Remaining time in milliseconds
    val createdAt: Long = System.currentTimeMillis(),
    val lastStartedAt: Long = 0
) : Parcelable {
    // Generate display name from duration if name is null
    fun getDisplayName(): String {
        return name ?: formatDuration(duration)
    }
    
    private fun formatDuration(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
}
