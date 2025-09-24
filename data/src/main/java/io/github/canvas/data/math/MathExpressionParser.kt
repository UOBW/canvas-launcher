package io.github.canvas.data.math

import io.github.canvas.data.R
import java.lang.Double.min
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

private const val EXPRESSION_END = CharStream.STREAM_END

object MathExpressionParser {
    /**
     * Parse the given expression and return the result as a string
     * Returns an error message instead if the expression could not be parsed
     */
    fun parse(expression: String): ParseResult? {
        var showError = false
        try {
            val exp = CharStream(expression)
            if (exp.char == '=') {
                exp.next()
                showError = true
            }
            val result = parseTerm(exp)
            if (exp.char != EXPRESSION_END) throwUnexpectedCharacterException(exp)
            return ParseResult(
                R.string.math_result,
                listOf(result.format(unlimitedPrecision = true)),
                successful = !result.isNaN()
            )
        } catch (exc: ParserException) {
            return if (showError) ParseResult(
                exc.localizedMessage,
                exc.formatArgs.toList(),
                successful = false,
            ) else null
        }
    }
}

/*
 * Internal parse methods
 * The general contract is to parse from exp as long as possible, until an unknown character is encountered.
 * At that point the function returns, allowing the caller to deal with the unknown character.
 * If no caller is able to interpret the character, it propagates up to parse(), which then throws a ParserException.
 */

/**
 * Parses the expression until an unexpected character is found
 * @param inAbsolute whether the term is inside the absolute operator |...|
 */
private fun parseTerm(exp: CharStream, inAbsolute: Boolean = false): Double {
    val addends = mutableListOf<Double>()
    while (true) {
        when (exp.char) {
            '+' -> {
                exp.next()
                addends += when (exp.char) {
                    '-' -> {
                        //Allow addition of negative numbers
                        exp.next()
                        -parseProduct(exp, inAbsolute)
                    }

                    else -> parseProduct(exp, inAbsolute)
                }
            }

            '-' -> {
                exp.next()
                addends += when (exp.char) {
                    '+' -> {
                        exp.next()
                        parseProduct(exp, inAbsolute)
                    }

                    else -> -parseProduct(exp, inAbsolute)
                }
            }

            else -> {
                when (addends.size) {
                    0 -> addends += parseProduct(
                        exp,
                        inAbsolute
                    ) // The first addend can be without sign
                    else -> return addends.sumOf { it }
                }
            }
        }
    }
}

/**
 * Parses a single addend (a term with no no + or - operators, so basically a multiplication)
 * @param inAbsolute whether the term is inside the absolute operator |...|
 */
private fun parseProduct(exp: CharStream, inAbsolute: Boolean): Double {
    val factors = mutableListOf<Double>()
    var inverseNextFactor = false
    fun addFactor(factor: Double) {
        if (inverseNextFactor) {
            factors += 1 / factor
            inverseNextFactor = false
        } else {
            factors += factor
        }
    }

    while (true) {
        when (exp.char) {
            '*', '·', '×', '•', '∙' -> exp.next()

            '/', '÷', ':' -> {
                exp.next()
                inverseNextFactor = true
            }

            in '0'..'9', '.' -> addFactor(parseNumber(exp))

            '(', '[', '{' -> addFactor(parseBracket(exp))

            '|' -> {
                if (inAbsolute) {
                    //The end of the absolute expression |...| has been reached
                    return when (factors.size) {
                        0 -> throwUnexpectedCharacterException(exp)
                        else -> factors.fold(1.0) { previous, current -> previous * current }
                    }
                }
                addFactor(parseAbsolute(exp))
            }

            in 'a'..'z', in 'A'..'Z', 'π', '∞', in '¼'..'¾', in '⅐'..'⅞' ->
                addFactor(parseFunction(exp))

            '!' -> {
                //Factorial of last factor
                if (factors.isEmpty()) throwUnexpectedCharacterException(exp)
                val r = factors.removeAt(factors.lastIndex)
                if (r % 1 != 0.0) {
                    throw ParserException(
                        R.string.math_error_fractional_factorial, r
                    )
                }
                val n = r.toLong()
                var result = 1.0
                for (i in 1..n) {
                    result *= i
                }
                factors.add(result)
                exp.next()
            }

            '⁻', '⁰', '¹', '²', '³', in '⁴'..'⁹' -> {
                //Power using superscript numbers
                if (factors.isEmpty()) throwUnexpectedCharacterException(exp)
                val exponent = buildString {
                    if (exp.char == '⁻') {
                        append('-')
                        exp.next()
                    }
                    while (true) {
                        append(
                            when (exp.char) {
                                '⁰' -> '0'
                                '¹' -> '1'
                                '²' -> '2'
                                '³' -> '3'
                                '⁴' -> '4'
                                '⁵' -> '5'
                                '⁶' -> '6'
                                '⁷' -> '7'
                                '⁸' -> '8'
                                '⁹' -> '9'
                                else -> break
                            }
                        )
                        exp.next()
                    }
                }.toInt()
                val base = factors.removeAt(factors.lastIndex)
                factors.add(base.pow(exponent))
            }

            '^' -> {
                //Power using ^ character
                if (factors.isEmpty()) throwUnexpectedCharacterException(exp)
                exp.next()
                val base = factors.removeAt(factors.lastIndex)
                val exponent = when (exp.char) {
                    in '0'..'9' -> parseNumber(exp)
                    '(', '[', '{' -> parseBracket(exp)
                    else -> throwUnexpectedCharacterException(exp)
                }
                factors.add(base.pow(exponent))
            }

            '√' -> factors.add(parseSquareRoot(exp))

            else -> {
                return when (factors.size) {
                    0 -> throwUnexpectedCharacterException(exp)
                    else -> factors.fold(1.0) { previous, current -> previous * current }
                }
            }
        }
    }
}

/** Parses a bracket ((), [] or {}) */
private fun parseBracket(exp: CharStream): Double {
    val bracket = exp.char
    exp.next()
    val result = parseTerm(exp)
    if (
        (bracket == '(' && exp.char != ')') ||
        (bracket == '[' && exp.char != ']') ||
        (bracket == '{' && exp.char != '}')
    ) {
        //Validate that the bracket is closed correctly
        throwUnexpectedCharacterException(exp)
    }
    exp.next()
    return result
}

/** Parses the absolute operator |...| */
private fun parseAbsolute(exp: CharStream): Double {
    exp.next()
    val result = parseTerm(exp, inAbsolute = true)
    exp.nextIf('|')
    return result.absoluteValue
}

/** Parses the square root operator √ */
private fun parseSquareRoot(exp: CharStream): Double {
    exp.next()
    val radicand = when (exp.char) {
        in '0'..'9' -> parseNumber(exp)
        '(', '[', '{' -> parseBracket(exp)
        else -> throwUnexpectedCharacterException(exp)
    }
    if (radicand < 0) throw ParserException(R.string.math_error_fun_of_negative, "√")
    return sqrt(radicand)
}

/** Parses a positive number (e.g. 1, 4.5, 8) */
private fun parseNumber(exp: CharStream): Double {
    val number = StringBuilder()
    var decimalSeparatorFound = false
    while (true) {
        when (exp.char) {
            in '0'..'9' -> number.append(exp.char)

            '.' -> {
                if (decimalSeparatorFound) {
                    break
                }
                number.append(exp.char)
                decimalSeparatorFound = true
            }

            '%' -> {
                //Percentage
                val s = number.toString()
                if (s.isEmpty() || s == ".") throwUnexpectedCharacterException(exp)
                exp.next()
                return s.toDouble() / 100
            }

            else -> break
        }
        exp.next()
    }

    val s = number.toString()
    if (s.isEmpty() || s == ".") throwUnexpectedCharacterException(exp)
    return s.toDouble()
}

@Suppress("SpellCheckingInspection")
private fun parseFunction(exp: CharStream): Double {
    fun Double.ensureNotInfinite(funName: String) = when {
        isInfinite() -> throw ParserException(R.string.math_error_fun_of_inf, funName)
        else -> this
    }

    fun Double.ensureNotGreaterOne(funName: String) = when {
        absoluteValue > 1 -> throw ParserException(R.string.math_error_fun_of_greater_1, funName)
        else -> this
    }

    fun Double.ensureNotSmallerOne(funName: String) = when {
        this < 1 -> throw ParserException(R.string.math_error_fun_of_smaller_1, funName)
        else -> this
    }

    fun Double.ensureNotNegative(funName: String) = when {
        this < 0 -> throw ParserException(R.string.math_error_fun_of_negative, funName)
        else -> this
    }

    val sb = StringBuilder()
    while (true) {
        when (exp.char) {
            in 'a'..'z', 'π', '∞', in '¼'..'¾', in '⅐'..'⅞' -> sb.append(exp.char)
            in 'A'..'Z' -> sb.append(exp.char.lowercaseChar())
            else -> break
        }
        exp.next()
    }
    val name = sb.toString()

    val args = mutableListOf<Double>()
    if (exp.char == '(') {
        do {
            exp.next()
            args += parseTerm(exp)
        } while (exp.char == ',')
        exp.nextIf(')')
    }
    return when (args.size) {
        0 -> when (name) {
            "inf", "∞" -> POSITIVE_INFINITY
            "pi", "π" -> PI
            "e" -> E
            "½" -> 0.5
            "⅓" -> 1.0 / 3.0
            "¼" -> 0.25
            "⅕" -> 0.2
            "⅙" -> 1.0 / 6.0
            "⅐" -> 1.0 / 7.0
            "⅛" -> 0.125
            "⅑" -> 1.0 / 9.0
            "⅒" -> 0.1
            "⅔" -> 2.0 / 3.0
            "⅖" -> 0.4
            "¾" -> 0.75
            "⅗" -> 0.6
            "⅜" -> 0.375
            "⅘" -> 0.8
            "⅚" -> 5.0 / 6.0
            "⅝" -> 0.625
            "⅞" -> 0.875
            else -> throw ParserException(R.string.math_error_unknown_variable, name)
        }

        1 -> when (name) {
            "sin" -> sin(args[0].ensureNotInfinite("sin"))
            "asin" -> asin(args[0].ensureNotGreaterOne("asin"))
            "sinh" -> sinh(args[0])
            "asinh" -> asinh(args[0])
            "cos" -> cos(args[0].ensureNotInfinite("cos"))
            "acos" -> acos(args[0].ensureNotGreaterOne("acos"))
            "cosh" -> cosh(args[0])
            "acosh" -> acosh(args[0].ensureNotSmallerOne("acosh"))
            "tan" -> tan(args[0].ensureNotInfinite("tan"))
            "atan" -> atan(args[0])
            "tanh" -> tanh(args[0])
            "atanh" -> atanh(args[0].ensureNotGreaterOne("atanh"))

            "sqrt" -> sqrt(args[0].ensureNotNegative("sqrt"))
            "cbrt" -> cbrt(args[0])

            "ceil" -> ceil(args[0])
            "floor" -> floor(args[0])
            "round" -> round(args[0])

            "ln" -> ln(args[0].ensureNotNegative("ln"))
            "abs" -> abs(args[0])
            "sign" -> sign(args[0])
            else -> throw ParserException(R.string.math_error_unknown_function, name, args[0])
        }

        2 -> when (name) {
            "log" -> {
                if (args[0] < 0 || args[1] < 0)
                    throw ParserException(R.string.math_error_fun_of_negative, "log")
                if (args[1] == 0.0) throw ParserException(R.string.math_error_log_of_0)
                if (args[1] == 1.0) throw ParserException(R.string.math_error_log_of_1)
                if (args[0].isInfinite() && args[1].isInfinite()) throw ParserException(R.string.math_error_log_of_inf_inf)
                if (args[0] == 0.0 && args[1] == 1.0) throw ParserException(R.string.math_error_log_of_0_1)
                log(args[0], args[1])
            }

            "min" -> min(args[0], args[1])
            "max" -> max(args[0], args[1])
            else -> throw ParserException(
                R.string.math_error_unknown_function, name, args.joinToString()
            )
        }

        else -> throw ParserException(
            R.string.math_error_unknown_function, name, args.joinToString()
        )
    }
}
