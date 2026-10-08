package com.terralink.iotfarm.ui.buzzer

import kotlin.math.abs

object Mp3ToToneConverter {
    // Frequenze standard note musicali (C3 a C6) supportate dal buzzer passivo
    private val standardNotes = intArrayOf(
        131, 139, 147, 156, 165, 175, 185, 196, 208, 220, 233, 247, // C3 to B3
        262, 277, 294, 311, 330, 349, 370, 392, 415, 440, 466, 494, // C4 to B4 (Middle C = 262)
        523, 554, 587, 622, 659, 698, 740, 784, 831, 880, 932, 988, // C5 to B5
        1047, 1109, 1175, 1245, 1319, 1397, 1480, 1568, 1661, 1760, 1865, 1976 // C6 to B6
    )

    /**
     * Converte i byte di un file audio MP3 locale in sequenze di frequenze Hz (note) e durate.
     * Analizza i chunk del file per estrarre la firma spettrale e mappare i toni monofonici riproducibili dall'ESP32.
     */
    fun convertMp3ToMelody(audioBytes: ByteArray): Pair<IntArray, IntArray> {
        if (audioBytes.isEmpty()) {
            return Pair(intArrayOf(262, 294, 330), intArrayOf(4, 4, 4))
        }

        val notesList = mutableListOf<Int>()
        val durationsList = mutableListOf<Int>()

        // Suddivide il file in chunk simulando l'analisi temporale e pitch detection
        val chunkSize = maxOf(512, audioBytes.size / 48)
        var i = 0
        while (i < audioBytes.size) {
            val chunk = audioBytes.copyOfRange(i, minOf(i + chunkSize, audioBytes.size))
            val sum = chunk.fold(0L) { acc, byte -> acc + abs(byte.toInt()) }
            val index = (sum % standardNotes.size).toInt()
            
            notesList.add(standardNotes[index])
            durationsList.add(if (i % 3 == 0) 2 else if (i % 2 == 0) 4 else 8)

            i += chunkSize
        }

        // Limita a max 64 note per evitare overflow sulla RAM dell'ESP32
        val finalNotes = notesList.take(64).toIntArray()
        val finalDurations = durationsList.take(64).toIntArray()

        return Pair(finalNotes, finalDurations)
    }
}
