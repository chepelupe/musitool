package com.example.musitool.tuner

import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.roundToInt

data class InstrumentString(
    val number: Int,        // número de cuerda (1 = la más aguda)
    val note: String,       // nombre americano
    val octave: Int,
    val latinName: String,  // nombre latino
    val frequency: Double   // Hz (La4 = 440 Hz)
) {
    val label: String get() = "$note$octave"
}

// Afinación estándar de guitarra: E2 A2 D3 G3 B3 E4 (de grave a aguda)
private val GUITAR_STANDARD = listOf(
    InstrumentString(6, "E", 2, "Mi", 82.41),
    InstrumentString(5, "A", 2, "La", 110.00),
    InstrumentString(4, "D", 3, "Re", 146.83),
    InstrumentString(3, "G", 3, "Sol", 196.00),
    InstrumentString(2, "B", 3, "Si", 246.94),
    InstrumentString(1, "E", 4, "Mi", 329.63)
)

// Afinación estándar de bajo de 4 cuerdas: E1 A1 D2 G2
private val BASS_STANDARD = listOf(
    InstrumentString(4, "E", 1, "Mi", 41.20),
    InstrumentString(3, "A", 1, "La", 55.00),
    InstrumentString(2, "D", 2, "Re", 73.42),
    InstrumentString(1, "G", 2, "Sol", 98.00)
)

enum class Instrument(
    val displayName: String,
    val tuningName: String,
    val minFrequency: Float,
    val maxFrequency: Float,
    val tip: String,
    val strings: List<InstrumentString>
) {
    GUITARRA_ACUSTICA(
        "Guitarra acústica", "E A D G B E", 60f, 700f,
        "Toca la cuerda cerca del micrófono del teléfono y déjala sonar.",
        GUITAR_STANDARD
    ),
    GUITARRA_ELECTRICA(
        "Guitarra eléctrica", "E A D G B E", 60f, 700f,
        "Acerca el teléfono al amplificador (sonido limpio, sin distorsión) o al cuerpo de la guitarra.",
        GUITAR_STANDARD
    ),
    BAJO_ELECTRICO(
        "Bajo eléctrico", "E A D G", 30f, 300f,
        "Acerca el teléfono al amplificador. Las notas graves tardan un poco más en detectarse: deja sonar la cuerda.",
        BASS_STANDARD
    )
}

object NoteUtils {
    private val NOTE_NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

    /** Diferencia en cents entre una frecuencia y la frecuencia objetivo (100 cents = 1 semitono). */
    fun centsBetween(frequency: Float, target: Double): Float =
        (1200.0 * ln(frequency / target) / ln(2.0)).toFloat()

    /** Nombre de la nota cromática más cercana, ej. 82.4 Hz -> "E2". */
    fun noteNameFor(frequency: Float): String {
        val midi = (69 + 12 * log2(frequency / 440.0)).roundToInt()
        val name = NOTE_NAMES[((midi % 12) + 12) % 12]
        val octave = midi / 12 - 1
        return "$name$octave"
    }
}