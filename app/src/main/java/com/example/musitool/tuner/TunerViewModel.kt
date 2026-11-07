package com.example.musitool.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musitool.audio.MicrophonePitchSource
import com.example.musitool.audio.PitchReading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class TunerUiState(
    val instrument: Instrument = Instrument.GUITARRA_ACUSTICA,
    val selectedStringIndex: Int? = null,      // null = modo automático
    val targetString: InstrumentString? = null,
    val isListening: Boolean = false,
    val frequency: Float? = null,
    val detectedNote: String = "--",
    val cents: Float = 0f,
    val error: String? = null
)

class TunerViewModel : ViewModel() {

    private val pitchSource = MicrophonePitchSource()

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private val recentFrequencies = mutableListOf<Float>()
    private var framesWithoutSound = 0

    fun selectInstrument(instrument: Instrument) {
        recentFrequencies.clear()
        _uiState.update {
            it.copy(
                instrument = instrument,
                selectedStringIndex = null,
                targetString = null,
                frequency = null,
                detectedNote = "--",
                cents = 0f
            )
        }
    }

    fun selectString(index: Int?) {
        recentFrequencies.clear()
        _uiState.update { state ->
            state.copy(
                selectedStringIndex = index,
                targetString = index?.let { i -> state.instrument.strings[i] },
                frequency = null,
                detectedNote = "--",
                cents = 0f
            )
        }
    }

    fun start() {
        if (listenJob?.isActive == true) return
        _uiState.update { it.copy(isListening = true, error = null) }
        listenJob = viewModelScope.launch {
            try {
                pitchSource.pitchFlow().collect { reading -> onReading(reading) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isListening = false, error = e.message ?: "Error con el micrófono")
                }
            }
        }
    }

    fun stop() {
        listenJob?.cancel()
        listenJob = null
        recentFrequencies.clear()
        framesWithoutSound = 0
        _uiState.update {
            it.copy(isListening = false, frequency = null, detectedNote = "--", cents = 0f)
        }
    }

    private fun onReading(reading: PitchReading) {
        val state = _uiState.value
        val frequency = reading.frequency

        val valid = frequency > 0f &&
                frequency >= state.instrument.minFrequency &&
                frequency <= state.instrument.maxFrequency

        if (!valid) {
            framesWithoutSound++
            // ~1 segundo sin sonido: se limpia la lectura
            if (framesWithoutSound > 10) {
                recentFrequencies.clear()
                _uiState.update { it.copy(frequency = null, detectedNote = "--", cents = 0f) }
            }
            return
        }
        framesWithoutSound = 0

        // Suavizado con la mediana de las últimas 5 lecturas
        recentFrequencies.add(frequency)
        if (recentFrequencies.size > 5) recentFrequencies.removeAt(0)
        val smooth = recentFrequencies.sorted()[recentFrequencies.size / 2]

        val target = state.selectedStringIndex?.let { state.instrument.strings[it] }
            ?: state.instrument.strings.minBy { abs(NoteUtils.centsBetween(smooth, it.frequency)) }

        _uiState.update {
            it.copy(
                frequency = smooth,
                detectedNote = NoteUtils.noteNameFor(smooth),
                targetString = target,
                cents = NoteUtils.centsBetween(smooth, target.frequency)
            )
        }
    }

    override fun onCleared() {
        listenJob?.cancel()
        super.onCleared()
    }
}