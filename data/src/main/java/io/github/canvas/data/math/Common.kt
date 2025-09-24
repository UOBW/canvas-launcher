package io.github.canvas.data.math

import android.icu.number.Notation
import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.text.NumberFormat
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.StringRes
import io.github.canvas.data.R
import java.util.Locale
import kotlin.math.absoluteValue

internal class CharStream(private val exp: String) {
    companion object {
        const val STREAM_END: Char = '\u0000'
    }

    private var i: Int = -1

    /** The current char, or \u0000, if at the end of the expression */
    var char: Char = '\u0000'
        private set

    init {
        next()
    }

    fun next() {
        do {
            char = exp.getOrNull(++i) ?: STREAM_END
        } while (char.isWhitespace()) //Ignore whitespace characters
    }

    /** Asserts that this.char == char and moves to the next character */
    fun nextIf(char: Char) {
        if (this.char != char) {
            throwUnexpectedCharacterException(this)
        }
        next()
    }
}

data class ParseResult(
    @StringRes
    val result: Int = R.string.empty,
    val formatArgs: List<Any> = emptyList(),
    val successful: Boolean = true,
)

internal class ParserException(
    @StringRes val localizedMessage: Int,
    vararg val formatArgs: Any,
) : Exception()

internal fun throwUnexpectedCharacterException(exp: CharStream): Nothing {
    if (exp.char == CharStream.STREAM_END) {
        throw ParserException(R.string.charstream_error_end_of_input)
    } else {
        throw ParserException(R.string.charstream_error_unexpected_character, exp.char)
    }
}

private const val SCIENTIFIC_NOTATION_MAX_THRESHOLD = 999999999999.0
private const val SCIENTIFIC_NOTATION_MIN_THRESHOLD = 0.0000001

fun Double.format(unlimitedPrecision: Boolean = false): String =
    if (VERSION.SDK_INT >= VERSION_CODES.R) {
        NumberFormatter
            .withLocale(Locale.getDefault())
            .run {
                @Suppress("KotlinConstantConditions") // I don't know whats going on here, but this expression is not constant
                if (this@format.absoluteValue !in SCIENTIFIC_NOTATION_MIN_THRESHOLD..<SCIENTIFIC_NOTATION_MAX_THRESHOLD &&
                    this@format != 0.0
                ) {
                    notation(Notation.scientific())
                } else this
            }
            .run {
                if (unlimitedPrecision) precision(Precision.unlimited()) else this
            }
            .format(this)
            .toString()
    } else {
        NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
    }
