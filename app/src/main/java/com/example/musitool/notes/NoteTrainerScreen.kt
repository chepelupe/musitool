package com.example.musitool.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteTrainerScreen(viewModel: NoteTrainerViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    // Al salir de la pestaña se pausa el temporizador
    DisposableEffect(Unit) {
        onDispose { viewModel.pause() }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Entrenador de notas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Nota actual
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val note = state.currentNote
                Text(
                    text = note?.let { NoteNames.format(it.name, state.notation) } ?: "—",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                val alternative = note?.enharmonic
                Text(
                    text = if (alternative != null && state.showEnharmonic)
                        "= ${NoteNames.format(alternative, state.notation)}" else " ",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Notas mostradas: ${state.notesShown}", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Controles
        val canRun = state.enabledPitchClasses.isNotEmpty()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { if (state.isRunning) viewModel.pause() else viewModel.start() },
                enabled = canRun,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.isRunning) "Pausar" else "Iniciar") }
            OutlinedButton(
                onClick = viewModel::next,
                enabled = canRun,
                modifier = Modifier.weight(1f)
            ) { Text("Siguiente") }
            OutlinedButton(
                onClick = viewModel::reset,
                modifier = Modifier.weight(1f)
            ) { Text("Reiniciar") }
        }

        Text("Configuración", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        // Duración
        Text("Duración de cada nota: %.1f s".format(state.secondsPerNote))
        Slider(
            value = state.secondsPerNote,
            onValueChange = viewModel::setSecondsPerNote,
            valueRange = NoteTrainerViewModel.MIN_SECONDS..NoteTrainerViewModel.MAX_SECONDS,
            steps = 18 // pasos de 0.5 s
        )

        // Modo de nombres
        Text("Nombres de las notas", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NamingMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.namingMode == mode,
                    onClick = { viewModel.setNamingMode(mode) },
                    label = { Text(mode.label) }
                )
            }
        }
        Text(
            state.namingMode.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.namingMode == NamingMode.ENARMONICOS) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = state.showEnharmonic, onCheckedChange = viewModel::setShowEnharmonic)
                Spacer(Modifier.width(12.dp))
                Text("Mostrar el nombre equivalente debajo")
            }
        }

        // Notación
        Text("Notación", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Notation.entries.forEach { notation ->
                FilterChip(
                    selected = state.notation == notation,
                    onClick = { viewModel.setNotation(notation) },
                    label = { Text(notation.label) }
                )
            }
        }

        // Selección de notas
        Text("Notas que pueden aparecer", style = MaterialTheme.typography.titleSmall)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick = viewModel::selectAll) { Text("Todas") }
            TextButton(onClick = viewModel::selectNaturals) { Text("Naturales") }
            TextButton(onClick = viewModel::selectAccidentals) { Text("Alteradas") }
        }
        NoteSelectionGrid(state = state, onToggle = viewModel::togglePitchClass)

        if (!canRun) {
            Text("Selecciona al menos una nota.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteSelectionGrid(state: NoteTrainerUiState, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        NoteNames.pitchClasses.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { pc ->
                    FilterChip(
                        selected = pc.index in state.enabledPitchClasses,
                        onClick = { onToggle(pc.index) },
                        label = {
                            Text(
                                NoteNames.chipLabel(pc, state.namingMode, state.notation),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}