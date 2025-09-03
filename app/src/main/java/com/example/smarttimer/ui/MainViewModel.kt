package com.example.smarttimer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarttimer.data.TimerGroup
import com.example.smarttimer.data.TimerGroupWithTimers
import com.example.smarttimer.data.TimerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class MainViewModel(private val repository: TimerRepository) : ViewModel() {
    
    private val _timerGroups = MutableStateFlow<List<TimerGroupWithTimers>>(emptyList())
    val timerGroups: StateFlow<List<TimerGroupWithTimers>> = _timerGroups.asStateFlow()
    
    private val _currentGroupIndex = MutableStateFlow(0)
    val currentGroupIndex: StateFlow<Int> = _currentGroupIndex.asStateFlow()
    
    init {
        loadTimerGroups()
    }
    
    private fun loadTimerGroups() {
        viewModelScope.launch {
            repository.getAllTimerGroups()
                .combine(repository.getAllTimers()) { groups, timers ->
                    android.util.Log.d("MainViewModel", "Loaded ${groups.size} groups and ${timers.size} timers")
                    groups.map { group ->
                        val groupTimers = timers.filter { it.groupId == group.id }
                        android.util.Log.d("MainViewModel", "Group ${group.name} has ${groupTimers.size} timers")
                        groupTimers.forEach { timer ->
                            android.util.Log.d("MainViewModel", "Timer: ${timer.name}, duration: ${timer.duration}")
                        }
                        TimerGroupWithTimers(
                            timerGroup = group,
                            timers = groupTimers
                        )
                    }
                }
                .collect { groupsWithTimers ->
                    _timerGroups.value = groupsWithTimers
                    android.util.Log.d("MainViewModel", "Updated timer groups: ${groupsWithTimers.size}")
                }
        }
    }
    
    fun addTimerGroup(name: String, color: Int) {
        viewModelScope.launch {
            val newGroup = TimerGroup(
                name = name,
                color = color
            )
            repository.insertTimerGroup(newGroup)
        }
    }
    
    fun deleteTimerGroup(timerGroup: TimerGroup) {
        viewModelScope.launch {
            repository.deleteTimerGroup(timerGroup)
            // Adjust current index if needed
            if (_currentGroupIndex.value >= _timerGroups.value.size - 1) {
                _currentGroupIndex.value = maxOf(0, _timerGroups.value.size - 2)
            }
        }
    }
    
    fun setCurrentGroupIndex(index: Int) {
        if (index in 0 until _timerGroups.value.size) {
            _currentGroupIndex.value = index
        }
    }
    
    fun getCurrentGroup(): TimerGroupWithTimers? {
        val groups = _timerGroups.value
        val index = _currentGroupIndex.value
        return if (index < groups.size) groups[index] else null
    }
    
    fun getNextGroup(): TimerGroupWithTimers? {
        val groups = _timerGroups.value
        val nextIndex = _currentGroupIndex.value + 1
        return if (nextIndex < groups.size) groups[nextIndex] else null
    }
    
    fun getPreviousGroup(): TimerGroupWithTimers? {
        val groups = _timerGroups.value
        val prevIndex = _currentGroupIndex.value - 1
        return if (prevIndex >= 0) groups[prevIndex] else null
    }
    
    fun canSwipeLeft(): Boolean = _currentGroupIndex.value > 0
    
    fun canSwipeRight(): Boolean = _currentGroupIndex.value < _timerGroups.value.size - 1
}
