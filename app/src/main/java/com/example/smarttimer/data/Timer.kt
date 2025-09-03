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
    val name: String,
    val duration: Long, // Duration in milliseconds
    val groupId: Long,
    val isActive: Boolean = false,
    val remainingTime: Long = 0, // Remaining time in milliseconds
    val createdAt: Long = System.currentTimeMillis(),
    val lastStartedAt: Long = 0
) : Parcelable
