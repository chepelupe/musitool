package com.example.musitool.notes

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class NoteTrainerUiState(
    val secondsPerNote: Float = 3f,
    val namingMode: NamingMode = NamingMode.SOSTENIDOS,
    val notation: Notation = Notation.AMERICANA,
    val showEnharmonic: Boolean = true,
    val enabledPitchClasses: Set<Int> = (0..11).toSet(),
    val isRunning: Boolean = false,
    val currentNote: NoteChoice? = null,
    val progress: Float = 0f,
    val notesShown: Int = 0
)

class NoteTrainerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NoteTrainerUiState())
    val uiState: StateFlow<NoteTrainerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun start() {
        if (timerJob?.isActive == true) return
        if (_uiState.value.enabledPitchClasses.isEmpty()) return

        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (isActive) {
                showNextNote()
                // Se lee la duración en cada nota para aplicar cambios del slider al momento
                val durationMs = (_uiState.value.secondsPerNote * 1000).toLong()
                val startTime = SystemClock.elapsedRealtime()
                while (true) {
                    val elapsed = SystemClock.elapsedRealtime() - startTime
                    if (elapsed >= durationMs) break
                    _uiState.update { it.copy(progress = elapsed.toFloat() / durationMs) }
                    delay(TICK_MS)
                }
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    /** Muestra la siguiente nota manualmente (reinicia el temporizador si está corriendo). */
    fun next() {
        if (_uiState.value.isRunning) {
            timerJob?.cancel()
            timerJob = null
            start()
        } else {
            showNextNote()
        }
    }

    fun reset() {
        pause()
        _uiState.update { it.copy(currentNote = null, progress = 0f, notesShown = 0) }
    }

    fun setSecondsPerNote(value: Float) {
        val rounded = ((value * 2).roundToInt() / 2f).coerceIn(MIN_SECONDS, MAX_SECONDS)
        _uiState.update { it.copy(secondsPerNote = rounded) }
    }

    fun setNamingMode(mode: NamingMode) = _uiState.update { it.copy(namingMode = mode) }

    fun setNotation(notation: Notation) = _uiState.update { it.copy(notation = notation) }

    fun setShowEnharmonic(show: Boolean) = _uiState.update { it.copy(showEnharmonic = show) }

    fun togglePitchClass(index: Int) {
        _uiState.update { state ->
            val enabled = if (index in state.enabledPitchClasses) state.enabledPitchClasses - index
            else state.enabledPitchClasses + index
            state.copy(enabledPitchClasses = enabled)
        }
        if (_uiState.value.enabledPitchClasses.isEmpty()) pause()
    }

    fun selectAll() = _uiState.update { it.copy(enabledPitchClasses = (0..11).toSet()) }

    fun selectNaturals() = _uiState.update { it.copy(enabledPitchClasses = NoteNames.naturalIndices) }

    fun selectAccidentals() = _uiState.update { it.copy(enabledPitchClasses = NoteNames.accidentalIndices) }

    private fun showNextNote() {
        val state = _uiState.value
        val choices = NoteNames.choicesFor(state.enabledPitchClasses, state.namingMode)
        if (choices.isEmpty()) {
            pause()
            return
        }
        val previous = state.currentNote
        // Evita repetir la misma nota dos veces seguidas
        val candidates = when {
            previous == null -> choices
            state.enabledPitchClasses.size > 1 -> choices.filter { it.pitchClass != previous.pitchClass }
            choices.size > 1 -> choices.filter { it.name != previous.name }
            else -> choices
        }.ifEmpty { choices }

        val next = candidates.random()
        _uiState.update { it.copy(currentNote = next, progress = 0f, notesShown = it.notesShown + 1) }
    }

    companion object {
        const val MIN_SECONDS = 0.5f
        const val MAX_SECONDS = 10f
        private const val TICK_MS = 50L
    }
}