package com.example.musitool.tuner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val InTuneColor = Color(0xFF2E7D32)
private val CloseColor = Color(0xFFF9A825)
private val OffColor = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(viewModel: TunerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.start()
    }

    // Al cambiar de pestaña se apaga el micrófono
    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Afinador", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Selección de instrumento
        Text("Instrumento", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Instrument.entries.forEach { instrument ->
                FilterChip(
                    selected = state.instrument == instrument,
                    onClick = { viewModel.selectInstrument(instrument) },
                    label = { Text(instrument.displayName) }
                )
            }
        }

        // Selección de cuerda
        Text(
            "Cuerda · afinación estándar (${state.instrument.tuningName})",
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.selectedStringIndex == null,
                onClick = { viewModel.selectString(null) },
                label = { Text("Auto") }
            )
            state.instrument.strings.forEachIndexed { index, string ->
                FilterChip(
                    selected = state.selectedStringIndex == index,
                    onClick = { viewModel.selectString(index) },
                    label = { Text("${string.number}ª ${string.label}") }
                )
            }
        }

        // Tarjeta principal
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val target = state.targetString
                Text(target?.label ?: "--", fontSize = 72.sp, fontWeight = FontWeight.Bold)
                Text(
                    target?.let { "${it.number}ª cuerda · ${it.latinName}" } ?: "Toca una cuerda",
                    style = MaterialTheme.typography.bodyMedium
                )

                CentsMeter(
                    cents = state.cents,
                    active = state.frequency != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )

                val (status, color) = statusFor(state)
                Text(
                    status,
                    color = color,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    state.frequency?.let { "Detectado: %.2f Hz (%s)".format(it, state.detectedNote) }
                        ?: "Detectado: --"
                )
                target?.let { Text("Objetivo: %.2f Hz".format(it.frequency)) }
            }
        }

        Button(
            onClick = {
                when {
                    state.isListening -> viewModel.stop()
                    hasPermission -> viewModel.start()
                    else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isListening) "Detener" else "Iniciar afinador")
        }

        if (!hasPermission) {
            Text(
                "Se necesita permiso de micrófono para afinar.",
                color = MaterialTheme.colorScheme.error
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Text(
            state.instrument.tip,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun statusFor(state: TunerUiState): Pair<String, Color> = when {
    !state.isListening -> "Afinador detenido" to Color.Gray
    state.frequency == null -> "Esperando sonido…" to Color.Gray
    abs(state.cents) <= 5f -> "¡Afinado! (%+.0f cents)".format(state.cents) to InTuneColor
    state.cents < 0f -> "Grave: tensa la cuerda ↑ (%+.0f cents)".format(state.cents) to
            (if (abs(state.cents) <= 15f) CloseColor else OffColor)
    else -> "Agudo: afloja la cuerda ↓ (%+.0f cents)".format(state.cents) to
            (if (abs(state.cents) <= 15f) CloseColor else OffColor)
}

/** Medidor semicircular de -50 a +50 cents. */
@Composable
fun CentsMeter(cents: Float, active: Boolean, modifier: Modifier = Modifier) {
    val targetValue = if (active) cents.coerceIn(-50f, 50f) else 0f
    val animated by animateFloatAsState(targetValue = targetValue, label = "aguja")

    val needleColor = when {
        !active -> Color.Gray
        abs(cents) <= 5f -> InTuneColor
        abs(cents) <= 15f -> CloseColor
        else -> OffColor
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val radius = min(size.width / 2f, size.height) - strokeWidth
        val center = Offset(size.width / 2f, size.height - strokeWidth / 2f)
        val arcTopLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        // Arco de fondo
        drawArc(
            color = trackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        // Zona "afinado" (±5 cents = ±9°)
        drawArc(
            color = InTuneColor.copy(alpha = 0.6f),
            startAngle = 261f,
            sweepAngle = 18f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )
        // Marcas cada 10 cents
        for (c in -50..50 step 10) {
            val angle = Math.toRadians(270.0 + c / 50.0 * 90.0)
            val outer = radius - strokeWidth
            val inner = outer - if (c == 0) 18.dp.toPx() else 10.dp.toPx()
            drawLine(
                color = tickColor,
                start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                strokeWidth = 2.dp.toPx()
            )
        }
        // Aguja
        val needleAngle = Math.toRadians(270.0 + animated / 50.0 * 90.0)
        val needleLength = radius - strokeWidth
        drawLine(
            color = needleColor,
            start = center,
            end = Offset(
                center.x + cos(needleAngle).toFloat() * needleLength,
                center.y + sin(needleAngle).toFloat() * needleLength
            ),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = needleColor, radius = 8.dp.toPx(), center = center)
    }
}