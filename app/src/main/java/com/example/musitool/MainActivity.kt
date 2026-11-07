package com.example.musitool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.musitool.notes.NoteTrainerScreen
import com.example.musitool.notes.NoteTrainerViewModel
import com.example.musitool.tuner.TunerScreen
import com.example.musitool.tuner.TunerViewModel
import com.example.musitool.ui.theme.MusitoolTheme

class MainActivity : ComponentActivity() {

    private val tunerViewModel: TunerViewModel by viewModels()
    private val noteTrainerViewModel: NoteTrainerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusitoolTheme {
                MusitoolApp(tunerViewModel, noteTrainerViewModel)
            }
        }
    }

    // Si la app pasa a segundo plano, se libera el micrófono y se pausa el entrenador
    override fun onStop() {
        super.onStop()
        tunerViewModel.stop()
        noteTrainerViewModel.pause()
    }
}

enum class Section(val label: String, val icon: String) {
    TUNER("Afinador", "🎸"),
    NOTES("Notas", "🎼")
}

@Composable
fun MusitoolApp(
    tunerViewModel: TunerViewModel,
    noteTrainerViewModel: NoteTrainerViewModel
) {
    var current by rememberSaveable { mutableStateOf(Section.TUNER) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Section.entries.forEach { section ->
                    NavigationBarItem(
                        selected = current == section,
                        onClick = { current = section },
                        icon = { Text(section.icon, fontSize = 22.sp) },
                        label = { Text(section.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (current) {
            Section.TUNER -> TunerScreen(tunerViewModel, modifier)
            Section.NOTES -> NoteTrainerScreen(noteTrainerViewModel, modifier)
        }
    }
}