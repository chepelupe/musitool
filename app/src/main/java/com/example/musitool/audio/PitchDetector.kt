package com.example.musitool.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.math.sqrt

/** Lectura del micrófono: frecuencia en Hz (-1 si no se detectó) y nivel de volumen (RMS). */
data class PitchReading(val frequency: Float, val level: Float)

/**
 * Detector de tono basado en el algoritmo YIN (de Cheveigné & Kawahara, 2002).
 */
class YinPitchDetector(
    private val sampleRate: Int,
    bufferSize: Int,
    private val threshold: Float = 0.15f
) {
    private val halfSize = bufferSize / 2
    private val yinBuffer = FloatArray(halfSize)

    fun detect(buffer: FloatArray): Float {
        // 1. Función de diferencia
        yinBuffer[0] = 0f
        for (tau in 1 until halfSize) {
            var sum = 0f
            for (i in 0 until halfSize) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            yinBuffer[tau] = sum
        }

        // 2. Diferencia media acumulada normalizada
        yinBuffer[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfSize) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] = if (runningSum == 0f) 1f else yinBuffer[tau] * tau / runningSum
        }

        // 3. Umbral absoluto: primer mínimo por debajo del umbral
        var tauEstimate = -1
        var tau = 2
        while (tau < halfSize) {
            if (yinBuffer[tau] < threshold) {
                while (tau + 1 < halfSize && yinBuffer[tau + 1] < yinBuffer[tau]) tau++
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate == -1) return -1f

        // 4. Interpolación parabólica para mayor precisión
        val betterTau = if (tauEstimate in 1 until halfSize - 1) {
            val s0 = yinBuffer[tauEstimate - 1]
            val s1 = yinBuffer[tauEstimate]
            val s2 = yinBuffer[tauEstimate + 1]
            val denominator = 2f * (2f * s1 - s2 - s0)
            if (denominator != 0f) tauEstimate + (s2 - s0) / denominator else tauEstimate.toFloat()
        } else {
            tauEstimate.toFloat()
        }

        return sampleRate / betterTau
    }
}

/**
 * Lee el micrófono continuamente y emite lecturas de tono.
 * El permiso RECORD_AUDIO se valida en la pantalla antes de llamar a pitchFlow().
 */
class MicrophonePitchSource(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 4096,
    private val minLevel: Float = 0.005f
) {

    @SuppressLint("MissingPermission")
    fun pitchFlow(): Flow<PitchReading> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBuffer, bufferSize * 2)
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("No se pudo acceder al micrófono")
        }

        val shorts = ShortArray(bufferSize)
        val samples = FloatArray(bufferSize)
        val detector = YinPitchDetector(sampleRate, bufferSize)

        record.startRecording()
        try {
            while (currentCoroutineContext().isActive) {
                var read = 0
                while (read < bufferSize) {
                    val result = record.read(shorts, read, bufferSize - read)
                    if (result < 0) throw IllegalStateException("Error al leer el micrófono ($result)")
                    read += result
                }

                var sumSquares = 0.0
                for (i in 0 until bufferSize) {
                    val value = shorts[i] / 32768f
                    samples[i] = value
                    sumSquares += value * value
                }
                val rms = sqrt(sumSquares / bufferSize).toFloat()

                // Si hay silencio no se analiza (ahorra batería y evita lecturas falsas)
                val frequency = if (rms >= minLevel) detector.detect(samples) else -1f
                emit(PitchReading(frequency, rms))
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.flowOn(Dispatchers.Default)
}