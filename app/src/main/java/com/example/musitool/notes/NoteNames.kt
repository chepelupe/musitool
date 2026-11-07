package com.example.musitool.notes

enum class NamingMode(val label: String, val description: String) {
    SOSTENIDOS("Solo ♯", "Un solo nombre por nota, con sostenidos (C♯, D♯, F♯…)."),
    BEMOLES("Solo ♭", "Un solo nombre por nota, con bemoles (D♭, E♭, G♭…)."),
    ENARMONICOS("Enarmónicos", "Aparecen todos los nombres: C♯ y D♭, D♯ y E♭, etc.")
}

enum class Notation(val label: String) {
    AMERICANA("C D E"),
    LATINA("Do Re Mi")
}

/** Una de las 12 notas de la escala cromática. */
data class PitchClass(val index: Int, val sharpName: String, val flatName: String) {
    val isNatural: Boolean get() = sharpName == flatName
}

/** Nota que se muestra en pantalla: nombre y, si aplica, su equivalente enarmónico. */
data class NoteChoice(val pitchClass: Int, val name: String, val enharmonic: String?)

object NoteNames {

    val pitchClasses = listOf(
        PitchClass(0, "C", "C"),
        PitchClass(1, "C♯", "D♭"),
        PitchClass(2, "D", "D"),
        PitchClass(3, "D♯", "E♭"),
        PitchClass(4, "E", "E"),
        PitchClass(5, "F", "F"),
        PitchClass(6, "F♯", "G♭"),
        PitchClass(7, "G", "G"),
        PitchClass(8, "G♯", "A♭"),
        PitchClass(9, "A", "A"),
        PitchClass(10, "A♯", "B♭"),
        PitchClass(11, "B", "B")
    )

    val naturalIndices: Set<Int> = pitchClasses.filter { it.isNatural }.map { it.index }.toSet()
    val accidentalIndices: Set<Int> = pitchClasses.filterNot { it.isNatural }.map { it.index }.toSet()

    private val latinNames = mapOf(
        'C' to "Do", 'D' to "Re", 'E' to "Mi", 'F' to "Fa",
        'G' to "Sol", 'A' to "La", 'B' to "Si"
    )

    /** Convierte "C♯" a "Do♯" si la notación es latina. */
    fun format(name: String, notation: Notation): String =
        if (notation == Notation.AMERICANA) name
        else (latinNames[name.first()] ?: name.first().toString()) + name.drop(1)

    /** Lista de nombres posibles según las notas activas y el modo de nombres. */
    fun choicesFor(enabled: Set<Int>, mode: NamingMode): List<NoteChoice> =
        pitchClasses.filter { it.index in enabled }.flatMap { pc ->
            when {
                pc.isNatural -> listOf(NoteChoice(pc.index, pc.sharpName, null))
                mode == NamingMode.SOSTENIDOS -> listOf(NoteChoice(pc.index, pc.sharpName, null))
                mode == NamingMode.BEMOLES -> listOf(NoteChoice(pc.index, pc.flatName, null))
                else -> listOf(
                    NoteChoice(pc.index, pc.sharpName, pc.flatName),
                    NoteChoice(pc.index, pc.flatName, pc.sharpName)
                )
            }
        }

    /** Texto del botón de selección de cada nota. */
    fun chipLabel(pc: PitchClass, mode: NamingMode, notation: Notation): String = when {
        pc.isNatural -> format(pc.sharpName, notation)
        mode == NamingMode.SOSTENIDOS -> format(pc.sharpName, notation)
        mode == NamingMode.BEMOLES -> format(pc.flatName, notation)
        else -> "${format(pc.sharpName, notation)}/${format(pc.flatName, notation)}"
    }
}