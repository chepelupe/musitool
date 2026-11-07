# MusiTool 🎼

Aplicación Android (Kotlin + Jetpack Compose) para músicos.

## Funcionalidades

### Afinador
- Guitarra acústica, guitarra eléctrica y bajo eléctrico (4 cuerdas).
- Afinación estándar: guitarra E A D G B E · bajo E A D G (La4 = 440 Hz).
- Modo automático (detecta la cuerda más cercana) o selección manual de cuerda.
- Medidor de cents con aguja e indicación de tensar/aflojar.
- Detección de tono con el algoritmo YIN usando el micrófono (AudioRecord).

### Entrenador de notas
- Muestra una nota aleatoria cada N segundos (0.5 a 10 s).
- Modos de nombre: solo sostenidos, solo bemoles o enarmónicos (C♯ y D♭…).
- Notación americana (C D E) o latina (Do Re Mi).
- Selección de las notas que pueden aparecer (todas, naturales, alteradas o personalizadas).

## Arquitectura
MVVM con `ViewModel` + `StateFlow`; UI en Compose.

```
audio/   -> captura de micrófono y detector de tono (YIN)
tuner/   -> instrumentos, ViewModel y pantalla del afinador
notes/   -> nombres de notas, ViewModel y pantalla del entrenador
```

## Requisitos
- Android 7.0 (API 24) o superior.
- Permiso de micrófono.

## Ejecución en emulador
En el emulador: **Extended controls (⋯) → Microphone → activar "Virtual microphone uses host audio input"**.