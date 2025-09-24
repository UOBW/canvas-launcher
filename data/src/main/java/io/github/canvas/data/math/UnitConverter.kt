package io.github.canvas.data.math

import io.github.canvas.data.R
import io.github.canvas.data.math.NonPrefixedUnit.Companion.unit
import io.github.canvas.data.math.SiPrefixedUnit.Companion.unit
import kotlin.math.absoluteValue

/**Stores a value together with its unit*/
internal data class UnitValue(
    val value: Double,
    val unit: Unit,
) {
    override fun toString(): String = "${value.format()} $unit"
}

internal fun List<UnitValue>.convertTo(other: Unit): UnitValue {
    var total = 0.0

    for (value in this) {
        if (value.unit.type != other.type) throw ParserException(
            R.string.unit_conversion_error_incompatible_units,
            value.unit, value.unit.type.toString().lowercase(),
            other, other.type.toString().lowercase()
        )
        total += value.unit.toBaseUnit(value.value)
    }

    return UnitValue(value = other.fromBaseUnit(total), unit = other)
}


internal fun UnitValue.defaultConversion(): UnitValue? {
    fun UnitValue.to(other: Unit): UnitValue {
        require(this.unit.type == other.type)
        if (this.unit == other) return this // Optimization
        return UnitValue(value = other.fromBaseUnit(this.unit.toBaseUnit(this.value)), unit = other)
    }

    fun UnitValue.storageDefaultConversion(): UnitValue? {
        when (this.to(SiPrefixedUnit.BaseUnit.BIT.unit()).value.absoluteValue) {
            in 0.0..<8.0 -> return this.to(SiPrefixedUnit.BaseUnit.BIT.unit())
            in 8.0..<8_000.0 -> return this.to(SiPrefixedUnit.BaseUnit.BYTE.unit())
        }

        for (prefix in SiPrefixedUnit.Prefix.entries.slice(
            SiPrefixedUnit.Prefix.KILO.ordinal..SiPrefixedUnit.Prefix.RONNA.ordinal
        )) {
            this.to(SiPrefixedUnit.BaseUnit.BYTE.unit(prefix))
                .takeIf { it.value.absoluteValue < 1000.0 }?.let { return it }
        }

        // else: value is extremely high
        return this.to(SiPrefixedUnit.BaseUnit.BYTE.unit(prefix = SiPrefixedUnit.Prefix.QUETTA))
    }

    // @formatter:off
    val value: UnitValue? = when (unit) {
        is SiPrefixedUnit -> when (unit.baseUnit) {
            SiPrefixedUnit.BaseUnit.SECOND -> when (
                this.to(SiPrefixedUnit.BaseUnit.SECOND.unit()).value.absoluteValue
            ) {
                in 0.0..<60.0 -> this.to(SiPrefixedUnit.BaseUnit.SECOND.unit(prefix = SiPrefixedUnit.Prefix.MILLI))
                in 60.0..<3_600.0 -> this.to(NonPrefixedUnit.Unit.MINUTE.unit())
                in 3_600.0..<86_400.0 -> this.to(NonPrefixedUnit.Unit.HOUR.unit())
                in 86_400.0..<31_556_952.0 -> this.to(NonPrefixedUnit.Unit.DAY.unit())
                in 31_556_952.0..Double.POSITIVE_INFINITY -> this.to(NonPrefixedUnit.Unit.YEAR.unit())
                else -> null
            }

            SiPrefixedUnit.BaseUnit.METER -> when (
                this.to(NonPrefixedUnit.Unit.INCH.unit()).value.absoluteValue
            ) {
                in 0.0..<12.0 -> this.to(NonPrefixedUnit.Unit.INCH.unit())
                in 12.0..<36.0 -> this.to(NonPrefixedUnit.Unit.FOOT.unit())
                in 36.0..<63_360.0 -> this.to(NonPrefixedUnit.Unit.YARD.unit())
                in 63_360.0..Double.POSITIVE_INFINITY -> this.to(NonPrefixedUnit.Unit.MILE.unit())
                else -> null
            }

            SiPrefixedUnit.BaseUnit.GRAM, SiPrefixedUnit.BaseUnit.TONNE -> when (this.to(
                NonPrefixedUnit.Unit.OUNCE.unit()
            ).value.absoluteValue) {
                in 0.0..<16.0 -> this.to(NonPrefixedUnit.Unit.OUNCE.unit())
                in 16.0..<32_000.0 -> this.to(NonPrefixedUnit.Unit.POUND.unit())
                in 32_000.0..<Double.POSITIVE_INFINITY -> this.to(NonPrefixedUnit.Unit.TON.unit())
                else -> null
            }

            SiPrefixedUnit.BaseUnit.DEGREE_KELVIN -> to(NonPrefixedUnit.Unit.DEGREE_CELSIUS.unit())

            SiPrefixedUnit.BaseUnit.SQUARE_METER -> when (
                this.to(NonPrefixedUnit.Unit.SQUARE_INCH.unit()).value.absoluteValue
            ) {
                in 0.0..<144.0 -> this.to(NonPrefixedUnit.Unit.SQUARE_INCH.unit())
                in 144.0..<1_296.0 -> this.to(NonPrefixedUnit.Unit.SQUARE_FOOT.unit())
                in 1_296.0..<4_014_489_600.0 -> this.to(NonPrefixedUnit.Unit.SQUARE_YARD.unit())
                in 4_014_489_600.0..<Double.POSITIVE_INFINITY -> this.to(NonPrefixedUnit.Unit.SQUARE_MILE.unit())
                else -> null
            }

            SiPrefixedUnit.BaseUnit.CUBIC_METER, SiPrefixedUnit.BaseUnit.LITER -> when (
                this.to(NonPrefixedUnit.Unit.CUBIC_INCH.unit()).value.absoluteValue
            ) {
                in 0.0..<1728.0 -> this.to(NonPrefixedUnit.Unit.CUBIC_INCH.unit())
                in 1_728.0..<46_656.0 -> this.to(NonPrefixedUnit.Unit.CUBIC_FOOT.unit())
                in 46_656.0..<2.5435806e+14 -> this.to(NonPrefixedUnit.Unit.CUBIC_YARD.unit())
                in 2.5435806e+14..<Double.POSITIVE_INFINITY -> this.to(NonPrefixedUnit.Unit.CUBIC_MILE.unit())
                else -> null
            }

            SiPrefixedUnit.BaseUnit.BYTE, SiPrefixedUnit.BaseUnit.BIT -> this.storageDefaultConversion()
        }

        is NonPrefixedUnit -> when (unit.unit) {
            NonPrefixedUnit.Unit.MINUTE, NonPrefixedUnit.Unit.HOUR, NonPrefixedUnit.Unit.DAY, NonPrefixedUnit.Unit.YEAR -> when (this.to(
                SiPrefixedUnit.BaseUnit.SECOND.unit()
            ).value.absoluteValue) {
                in 0.0..<60.0 -> this.to(SiPrefixedUnit.BaseUnit.SECOND.unit(prefix = SiPrefixedUnit.Prefix.MILLI))
                in 60.0..<3_600.0 -> this.to(NonPrefixedUnit.Unit.MINUTE.unit())
                in 3_600.0..<86_400.0 -> this.to(NonPrefixedUnit.Unit.HOUR.unit())
                in 86_400.0..<31_556_952.0 -> this.to(NonPrefixedUnit.Unit.DAY.unit())
                in 31_556_952.0..Double.POSITIVE_INFINITY -> this.to(NonPrefixedUnit.Unit.YEAR.unit())
                else -> null
            }

            NonPrefixedUnit.Unit.RADIANS -> this.to(NonPrefixedUnit.Unit.DEGREE.unit())
            NonPrefixedUnit.Unit.DEGREE -> this.to(NonPrefixedUnit.Unit.RADIANS.unit())

            NonPrefixedUnit.Unit.METERS_PER_SECOND -> this.to(NonPrefixedUnit.Unit.KILOMETERS_PER_HOUR.unit())
            NonPrefixedUnit.Unit.KILOMETERS_PER_HOUR -> this.to(NonPrefixedUnit.Unit.MILES_PER_HOUR.unit())
            NonPrefixedUnit.Unit.MILES_PER_HOUR -> this.to(NonPrefixedUnit.Unit.KILOMETERS_PER_HOUR.unit())

            NonPrefixedUnit.Unit.DEGREE_CELSIUS -> this.to(NonPrefixedUnit.Unit.DEGREE_FAHRENHEIT.unit())
            NonPrefixedUnit.Unit.DEGREE_FAHRENHEIT -> this.to(NonPrefixedUnit.Unit.DEGREE_CELSIUS.unit())

            NonPrefixedUnit.Unit.INCH, NonPrefixedUnit.Unit.FOOT, NonPrefixedUnit.Unit.YARD, NonPrefixedUnit.Unit.MILE ->
                when (this.to(SiPrefixedUnit.BaseUnit.METER.unit()).value.absoluteValue) {
                    in 0.0..<0.01 -> this.to(SiPrefixedUnit.BaseUnit.METER.unit(prefix = SiPrefixedUnit.Prefix.MILLI))
                    in 0.01..<1.0 -> this.to(SiPrefixedUnit.BaseUnit.METER.unit(prefix = SiPrefixedUnit.Prefix.CENTI))
                    in 1.0..<1_000.0 -> this.to(SiPrefixedUnit.BaseUnit.METER.unit())
                    in 1_000.0..Double.POSITIVE_INFINITY -> this.to(SiPrefixedUnit.BaseUnit.METER.unit(prefix = SiPrefixedUnit.Prefix.KILO))
                    else -> null
                }

            NonPrefixedUnit.Unit.SQUARE_INCH, NonPrefixedUnit.Unit.SQUARE_FOOT, NonPrefixedUnit.Unit.SQUARE_YARD, NonPrefixedUnit.Unit.SQUARE_MILE, NonPrefixedUnit.Unit.ACRE, NonPrefixedUnit.Unit.HECTARE ->
                when (this.to(SiPrefixedUnit.BaseUnit.SQUARE_METER.unit()).value.absoluteValue) {
                    in 0.0..<0.0001 -> this.to(SiPrefixedUnit.BaseUnit.SQUARE_METER.unit(prefix = SiPrefixedUnit.Prefix.MILLI))
                    in 0.0001..<1.0 -> this.to(SiPrefixedUnit.BaseUnit.SQUARE_METER.unit(prefix = SiPrefixedUnit.Prefix.CENTI))
                    in 1.0..<1_000_000.0 -> this.to(SiPrefixedUnit.BaseUnit.SQUARE_METER.unit())
                    in 1_000_000.0..Double.POSITIVE_INFINITY -> this.to(SiPrefixedUnit.BaseUnit.SQUARE_METER.unit(prefix = SiPrefixedUnit.Prefix.KILO))
                    else -> null
                }

            NonPrefixedUnit.Unit.CUBIC_INCH, NonPrefixedUnit.Unit.CUBIC_FOOT, NonPrefixedUnit.Unit.CUBIC_YARD, NonPrefixedUnit.Unit.CUBIC_MILE, NonPrefixedUnit.Unit.TEASPOON, NonPrefixedUnit.Unit.TABLESPOON, NonPrefixedUnit.Unit.FLUID_OUNCE, NonPrefixedUnit.Unit.CUP, NonPrefixedUnit.Unit.FLUID_PINT, NonPrefixedUnit.Unit.FLUID_QUART, NonPrefixedUnit.Unit.GALLON, NonPrefixedUnit.Unit.DRY_PINT, NonPrefixedUnit.Unit.DRY_QUART, NonPrefixedUnit.Unit.PECK, NonPrefixedUnit.Unit.BUSHEL ->
                when (this.to(SiPrefixedUnit.BaseUnit.CUBIC_METER.unit()).value.absoluteValue) {
                    in 0.0..<0.000001 -> this.to(SiPrefixedUnit.BaseUnit.CUBIC_METER.unit(prefix = SiPrefixedUnit.Prefix.MILLI))
                    in 0.000001..<1.0 -> this.to(SiPrefixedUnit.BaseUnit.CUBIC_METER.unit(prefix = SiPrefixedUnit.Prefix.CENTI))
                    in 1.0..<1_000_000_000.0 -> this.to(SiPrefixedUnit.BaseUnit.CUBIC_METER.unit())
                    in 1_000_000_000.0..Double.POSITIVE_INFINITY -> this.to(SiPrefixedUnit.BaseUnit.CUBIC_METER.unit(prefix = SiPrefixedUnit.Prefix.KILO))
                    else -> null
                }

            NonPrefixedUnit.Unit.OUNCE, NonPrefixedUnit.Unit.POUND, NonPrefixedUnit.Unit.TON -> when (this.to(
                SiPrefixedUnit.BaseUnit.GRAM.unit()
            ).value.absoluteValue) {
                in 0.0..<1.0 -> this.to(SiPrefixedUnit.BaseUnit.GRAM.unit(prefix = SiPrefixedUnit.Prefix.MILLI))
                in 1.0..<1_000.0 -> this.to(SiPrefixedUnit.BaseUnit.GRAM.unit())
                in 1_000.0..Double.POSITIVE_INFINITY -> this.to(SiPrefixedUnit.BaseUnit.GRAM.unit(prefix = SiPrefixedUnit.Prefix.KILO))
                else -> null
            }

            NonPrefixedUnit.Unit.NIBBLE -> this.storageDefaultConversion()
        }
    }
    // @formatter:on
    return value
}
