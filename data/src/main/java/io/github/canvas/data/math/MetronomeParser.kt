package io.github.canvas.data.math

import androidx.annotation.StringRes
import io.github.canvas.data.R
import io.github.canvas.data.log

data class Metronome(
    val bpm: Int,
    val beats: Int,
)

object MetronomeParser {
    fun parse(specification: String): Metronome? {
        val spec = specification.filterNot { it.isWhitespace() }.lowercase()

        val (tempoSpec, beats) = when {
            '/' in spec || ("in" in spec && spec != "andantino") -> {
                (if ('/' in spec) spec.split('/') else spec.split("in"))
                    .takeIf { it.size == 2 }
                    ?.let { (t, b) ->
                        t to (b.toIntOrNull().takeIf { it in 1..8 } ?: return null)
                    } ?: return null
            }

            else -> spec to 1
        }

        val bpm = tempoNameToBpm(tempoSpec)
            ?: (if (tempoSpec.endsWith("bpm")) {
                tempoSpec.removeSuffix("bpm").toIntOrNull()?.takeIf { it in 1..300 }
            } else null)
            ?: return null

        return Metronome(bpm, beats)
    }

    /**
     * https://en.wikipedia.org/wiki/Tempo#Approximately_from_the_slowest_to_the_fastest
     * Licensed under the Creative Commons Attribution-ShareAlike 4.0 International license (https://creativecommons.org/licenses/by-sa/4.0/deed.en)
     */
    private fun tempoNameToBpm(name: String): Int? = when (name) {
        "larghissimo" -> 1..24
        "adagissimo" -> 24..40
        "grave" -> 24..40
        "largo" -> 40..66
        "larghetto" -> 44..66
        "adagio" -> 44..66
        "adagietto" -> 46..80
        "lento" -> 52..108
        "andante" -> 56..108
        "andantino" -> 80..108
        "marciamoderato" -> 66..80
        "andantemoderato" -> 80..108
        "moderato" -> 108..120
        "allegretto" -> 112..120
        "allegromoderato" -> 116..120
        "allegro" -> 120..156
        "moltoallegro" -> 124..156
        "allegro vivace" -> 124..156
        "vivace" -> 156..176
        "vivacissimo" -> 172..176
        "allegrissimo" -> 172..176
        "presto" -> 168..200
        "prestissimo" -> 200..300
        else -> null
    }?.let { (it.first + it.last) / 2 } // Average

    /** This subjective list was created by me, UOBW, based on the above Wikipedia list and my own musical experience */
    @StringRes
    fun bpmToTempoName(bpm: Int): Int = when (bpm) {
        in 1..24 -> R.string.tempo_larghissimo
        in 25..42 -> R.string.tempo_grave
        in 43..61 -> R.string.tempo_adagio
        in 62..108 -> R.string.tempo_andante
        in 109..120 -> R.string.tempo_moderato
        in 121..156 -> R.string.tempo_allegro
        in 157..200 -> R.string.tempo_presto
        in 200..300 -> R.string.tempo_prestissimo
        else -> R.string.empty.also { log.e("Invalid BPM $bpm") }
    }
}
