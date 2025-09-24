package io.github.canvas.data.math

import io.github.canvas.data.R
import io.github.canvas.data.math.CharStream.Companion.STREAM_END
import io.github.canvas.data.math.NonPrefixedUnit.Companion.unit
import io.github.canvas.data.math.SiPrefixedUnit.Companion.unit

object UnitConversionParser {
    /**
     * Parses a given unit conversion expression and returns the result as a string
     * The expression should be in the form {_value_ _sourceUnit_}+ _delimiter_ _targetUnit_
     */
    fun parse(expression: String): ParseResult? {
        val parts = expression.split(" to ", " in ", ignoreCase = true, limit = 2)
        when (parts.size) {
            2 -> {
                try {
                    val sourceValues = parseSourceValues(parts[0])
                    val targetUnit = parseUnit(parts[1])
                    val result = sourceValues.convertTo(targetUnit)
                    return ParseResult(R.string.unit_conversion_result, listOf(result.toString()))
                } catch (exc: ParserException) {
                    return ParseResult(
                        exc.localizedMessage,
                        exc.formatArgs.toList(),
                        successful = false
                    )
                }
            }

            1 -> {
                try {
                    //No conversion target given, guess target
                    val sourceValues = parseSourceValues(expression)
                    val result = sourceValues
                        .convertTo(sourceValues[0].unit) // Use first unit to guess target
                        .defaultConversion()
                        ?: return null
                    return ParseResult(R.string.unit_conversion_result, listOf(result.toString()))
                } catch (_: ParserException) {
                    return null
                }
            }

            else -> return null // Unreachable
        }
    }

    private fun parseSourceValues(sourceValue: String): List<UnitValue> {
        val source = CharStream(sourceValue)
        val values = mutableListOf<Pair<String, String>>()

        while (true) {
            val value = buildString {
                while (true) {
                    when (source.char) {
                        in '0'..'9', '.', '-' -> append(source.char)
                        else -> break
                    }
                    source.next()
                }
            }

            val unit = buildString {
                while (true) {
                    when (source.char) {
                        in '0'..'9', '.', '-', STREAM_END -> break
                        else -> append(source.char)
                    }
                    source.next()
                }
            }

            values += value to unit
            if (source.char == STREAM_END) break
        }

        if (values.size > 1 && values.last().second.isEmpty()) { // Parse e.g. 1km2 as 1km² instead of reporting an error
            val singleNumber = values.removeAt(values.lastIndex).first
            values[values.lastIndex] = values.last().first to (values.last().second + singleNumber)
        }

        return values.map { (value, unit) ->
            UnitValue(
                value = value.toDoubleOrNull()
                    ?: throw ParserException(R.string.unit_conversion_error_number_format, value),
                unit = parseUnit(unit)
            )
        }
    }

    private fun parseUnit(symbol: String): Unit {
        val sym = symbol.filterNot { it.isWhitespace() }
        // Try the unprefixed units first, to take precedence over prefixed units
        // (i.e., ft is feet rather than femto-tonne)
        NonPrefixedUnit.Unit.entries
            .firstOrNull { it.symbols.contains(sym) }
            ?.let { return it.unit() }
        // Try to find a matching SI unit
        for (baseUnit in SiPrefixedUnit.BaseUnit.entries) {
            if (!baseUnit.symbols.any { sym.endsWith(it) }) continue

            val prefixSymbol = baseUnit.symbols
                .first { sym.endsWith(it) }
                .let { sym.removeSuffix(it) }
            SiPrefixedUnit.Prefix.entries
                .firstOrNull { it.symbols.contains(prefixSymbol) }
                ?.let { return baseUnit.unit(prefix = it) }
        }
        // Try case-insensitive search next
        NonPrefixedUnit.Unit.entries
            .firstOrNull { unit -> unit.symbols.any { it.equals(sym, ignoreCase = true) } }
            ?.let { return it.unit() }
        SiPrefixedUnit.BaseUnit.entries
            .firstOrNull { unit -> unit.symbols.any { sym.endsWith(it, ignoreCase = true) } }
            ?.let { baseUnit ->
                val prefixSymbol = baseUnit.symbols
                    .first { sym.endsWith(it, ignoreCase = true) }
                    .let { sym.dropLast(it.length) }
                val prefix =
                    SiPrefixedUnit.Prefix.entries.firstOrNull { it.symbols.contains(prefixSymbol) } // Case sensitive
                        ?: SiPrefixedUnit.Prefix.entries.firstOrNull { prefix -> // Case-insensitive
                            prefix.symbols.any { it.equals(prefixSymbol, ignoreCase = true) }
                        }
                if (prefix != null) return baseUnit.unit(prefix = prefix)
            }
        // Nothing found
        throw ParserException(R.string.unit_conversion_error_unknown_unit, sym)
    }
}
