package com.craftworks.music.managers

import android.util.Log
import com.craftworks.music.player.ChoraMediaLibraryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SleepTimerManager {
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + supervisorJob)
    private var timerJob: Job? = null

    private val _remainingTimeMs = MutableStateFlow(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()

    private val _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    enum class TimerDuration(val minutes: Int, val displayName: String) {
        OFF(0, "Off"),
        MIN_5(5, "5 minutes"),
        MIN_10(10, "10 minutes"),
        MIN_15(15, "15 minutes"),
        MIN_30(30, "30 minutes"),
        MIN_45(45, "45 minutes"),
        MIN_60(60, "1 hour"),
        MIN_90(90, "1.5 hours"),
        MIN_120(120, "2 hours"),
        END_OF_TRACK(-1, "End of track")
    }

    fun startTimer(duration: TimerDuration) {
        cancelTimer()

        if (duration == TimerDuration.OFF) {
            return
        }

        if (duration == TimerDuration.END_OF_TRACK) {
            startEndOfTrackTimer()
            return
        }

        val durationMs = duration.minutes * 60 * 1000L
        _remainingTimeMs.value = durationMs
        _isTimerActive.value = true

        timerJob = scope.launch {
            while (_remainingTimeMs.value > 0) {
                delay(1000)
                _remainingTimeMs.value = (_remainingTimeMs.value - 1000).coerceAtLeast(0)
            }
            onTimerExpired()
        }

        Log.d("SleepTimer", "Started timer for ${duration.minutes} minutes")
    }

    private fun startEndOfTrackTimer() {
        _isTimerActive.value = true
        _remainingTimeMs.value = -1L // Special value for end of track

        timerJob = scope.launch {
            while (_isTimerActive.value) {
                // Re-check player each iteration in case service changes
                val player = ChoraMediaLibraryService.getInstance()?.player
                if (player == null) {
                    Log.w("SleepTimer", "Player became unavailable, cancelling end-of-track timer")
                    cancelTimer()
                    break
                }

                val duration = player.duration
                val position = player.currentPosition

                if (duration > 0) {
                    _remainingTimeMs.value = (duration - position).coerceAtLeast(0)
                }

                if (duration > 0 && position >= duration - 500) {
                    onTimerExpired()
                    break
                }
                delay(500)
            }
        }

        Log.d("SleepTimer", "Started end-of-track timer")
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingTimeMs.value = 0L
        _isTimerActive.value = false
        Log.d("SleepTimer", "Timer cancelled")
    }

    private fun onTimerExpired() {
        Log.d("SleepTimer", "Timer expired, pausing playback")
        ChoraMediaLibraryService.getInstance()?.player?.pause()
        _isTimerActive.value = false
        _remainingTimeMs.value = 0L
    }

    fun formatRemainingTime(ms: Long): String {
        if (ms <= 0) return ""
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
