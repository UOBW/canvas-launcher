package io.github.canvas.data.math

import io.github.canvas.data.math.SiPrefixedUnit.Prefix.BASE
import kotlin.math.PI
import kotlin.math.pow

/**
 * This file contains the definitions of all the units supported by the unit converter, as well as the data required to convert them.
 * Units included are limited to those still in common use today.
 */

internal enum class UnitType {
    /** Time, base unit: second (s) */
    TIME,

    /** Length (distance), base unit: meter (m) */
    LENGTH,

    /**
     * Mass, base unit: gram (g)
     * Note: The SI defines the kilogram (kg) as the base unit, but that would break the prefix system here
     */
    MASS,

    /** Temperature, base unit: degree kelvin (K) */
    TEMPERATURE,

    /** Area, base unit: square meter (m²) */
    AREA,

    /** Volume, base unit: cubic meter (m³) */
    VOLUME,

    /** Angle, base unit: radians (rad) */
    ANGLE,

    /** Speed, base unit: meter per second (m/s */
    SPEED,

    /** Digital storage, base unit: bit */
    STORAGE,
}

internal sealed class Unit(
    val type: UnitType,
) {
    /** Converts a value of that unit to a value of the base unit of that unit type */
    abstract fun toBaseUnit(value: Double): Double

    /** Converts a value of the base unit of that unit type to this unit */
    abstract fun fromBaseUnit(value: Double): Double
}

internal data class SiPrefixedUnit(
    val baseUnit: BaseUnit,
    val prefix: Prefix,
) : Unit(
    type = baseUnit.type
) {
    override fun toBaseUnit(value: Double): Double = baseUnit.toBaseUnit(value, prefix)
    override fun fromBaseUnit(value: Double): Double = baseUnit.fromBaseUnit(value, prefix)

    override fun toString(): String = prefix.symbols[0] + baseUnit.symbols[0]

    @Suppress("SpellCheckingInspection")
    enum class Prefix(
        val symbols: List<String>,
        val scale: Double,
    ) {
        //@formatter:off
        // Decimal prefixed
        QUECTO(listOf("q", "quecto"), 0.000_000_000_000_000_000_000_000_000_001),
        RONTO (listOf("r", "ronto"),  0.000_000_000_000_000_000_000_000_001),
        YOCTO (listOf("y", "yocto"),  0.000_000_000_000_000_000_000_001),
        ZEPTO (listOf("z", "zepto"),  0.000_000_000_000_000_000_001),
        ATTO  (listOf("a", "atto"),   0.000_000_000_000_000_001),
        FEMTO (listOf("f", "femto"),  0.000_000_000_000_001),
        PICO  (listOf("p", "pico"),   0.000_000_000_001),
        NANO  (listOf("n", "nano"),   0.000_000_001),
        MICRO (listOf("μ", "micro"),  0.000_001),
        MILLI (listOf("m", "milli"),  0.001),
        CENTI (listOf("c", "centi"),  0.01),
        DECI  (listOf("d", "deci"),   0.1),
        BASE  (listOf(""),            1.0),
        DECA  (listOf("da", "deca"),  10.0),
        HECTO (listOf("h", "hecto"),  100.0),
        KILO  (listOf("k", "kilo"),   1_000.0),
        MEGA  (listOf("M", "mega"),   1_000_000.0),
        GIGA  (listOf("G", "giga"),   1_000_000_000.0),
        TERA  (listOf("T", "tera"),   1_000_000_000_000.0),
        PETA  (listOf("P", "peta"),   1_000_000_000_000_000.0),
        EXA   (listOf("E", "exa"),    1_000_000_000_000_000_000.0),
        ZETTA (listOf("Z", "zetta"),  1_000_000_000_000_000_000_000.0),
        YOTTA (listOf("Y", "yotta"),  1_000_000_000_000_000_000_000_000.0),
        RONNA (listOf("R", "ronna"),  1_000_000_000_000_000_000_000_000_000.0),
        QUETTA(listOf("Q", "quetta"), 1_000_000_000_000_000_000_000_000_000_000.0),

        // Binary prefixes (mainly used with bit and byte)
        KIBI  (listOf("Ki", "kibi"),  1_024.0),
        MEBI  (listOf("Mi", "mebi"),  1_024.0.pow(2)),
        GIBI  (listOf("Gi", "gibi"),  1_024.0.pow(3)),
        TEBI  (listOf("Ti", "tebi"),  1_024.0.pow(4)),
        PEBI  (listOf("Pi", "pebi"),  1_024.0.pow(5)),
        EXBI  (listOf("Ei", "exbi"),  1_024.0.pow(6)),
        ZEBI  (listOf("Zi", "zebi"),  1_024.0.pow(7)),
        YOBI  (listOf("Yi", "yobi"),  1_024.0.pow(8)),
        ROBI  (listOf("Ri", "robi"),  1_024.0.pow(9)),
        QUEBI (listOf("Qi", "quebi"), 1_024.0.pow(10));
        //@formatter:on
    }

    @Suppress("SpellCheckingInspection")
    enum class BaseUnit(
        val symbols: List<String>,
        val type: UnitType,
        val toBaseUnit: (Double, Prefix) -> Double = { value, prefix -> value * prefix.scale },
        val fromBaseUnit: (Double, Prefix) -> Double = { value, prefix -> value / prefix.scale },
    ) {
        SECOND(listOf("s", "second", "seconds"), UnitType.TIME),
        METER(listOf("m", "meter", "metre", "meters", "metres"), UnitType.LENGTH),

        // In theory, the kilogram is the SI base unit, but that would break the prefix feature here
        GRAM(listOf("g", "gram", "grams"), UnitType.MASS),
        TONNE(
            listOf("t", "tonne", "tonnes"), UnitType.MASS,
            { value, prefix -> value * prefix.scale * 1000000 },
            { value, prefix -> value / prefix.scale / 1000000 }),
        DEGREE_KELVIN(listOf("K", "kelvin", "kelvins"), UnitType.TEMPERATURE),
        SQUARE_METER(
            listOf(
                "m²", "m2", "squaremeter", "squaremetre", "squaremeters", "squaremetres"
            ), UnitType.AREA,
            { value, prefix -> value * prefix.scale.pow(2) },
            { value, prefix -> value / prefix.scale.pow(2) }),
        CUBIC_METER(
            listOf(
                "m³", "m3", "cubicmeter", "cubicmetre", "cubicmeters", "cubicmetres"
            ), UnitType.VOLUME,
            { value, prefix -> value * prefix.scale.pow(3) },
            { value, prefix -> value / prefix.scale.pow(3) }),
        LITER(
            listOf("l", "L", "liter", "litre", "liters", "litres"), UnitType.VOLUME,
            { value, prefix -> value * prefix.scale / 1000 },
            { value, prefix -> value / prefix.scale * 1000 }),

        BIT(listOf("bit", "bits"), UnitType.STORAGE),
        BYTE(
            listOf("B", "byte", "bytes", "octet", "octets"), UnitType.STORAGE,
            { value, prefix -> value * prefix.scale * 8 },
            { value, prefix -> value / prefix.scale / 8 }
        )
    }

    companion object {
        fun BaseUnit.unit(prefix: Prefix = BASE): SiPrefixedUnit =
            SiPrefixedUnit(this, prefix)
    }
}

/** SI and non-SI units without prefix */
internal data class NonPrefixedUnit(
    val unit: Unit,
) : Unit(
    type = unit.type
) {
    override fun toBaseUnit(value: Double): Double = unit.toBaseUnit(value)
    override fun fromBaseUnit(value: Double): Double = unit.fromBaseUnit(value)

    override fun toString(): String = unit.symbols[0]

    @Suppress("SpellCheckingInspection")
    enum class Unit(
        val symbols: List<String>,
        val type: UnitType,
        val toBaseUnit: (value: Double) -> Double,
        val fromBaseUnit: (value: Double) -> Double,
    ) {
        // SI
        MINUTE(listOf("min", "minute", "minutes"), UnitType.TIME, 60.0),
        HOUR(listOf("h", "hour", "hours"), UnitType.TIME, 3600.0),
        DAY(listOf("d", "day", "days"), UnitType.TIME, 86400.0),
        YEAR(listOf("y", "year", "years"), UnitType.TIME, 31556952.0),

        RADIANS(
            listOf("rad", "radian", "radians"),
            UnitType.ANGLE, 1.0
        ), // radian is the angle base unit
        DEGREE(listOf("°", "deg", "degree", "degrees"), UnitType.ANGLE, PI / 180),

        METERS_PER_SECOND(
            listOf("m/s", "mps", "meterspersecond", "metrespersecond"),
            UnitType.SPEED, 1.0
        ), // m/s is the speed base unit
        KILOMETERS_PER_HOUR(
            listOf("km/h", "kmh", "kilometersperhour", "kilometresperhour"),
            UnitType.SPEED, 0.2777777777777778
        ),

        HECTARE(listOf("ha", "hectare", "hectares"), UnitType.AREA, 10000.0),
        DEGREE_CELSIUS(
            listOf("°C", "C", "degreecelcius", "degreescelcius", "celcius"),
            UnitType.TEMPERATURE, { it + 273.15 }, { it - 273.15 }),

        NIBBLE(listOf("nibble", "nybble", "nibbles", "nybbles"), UnitType.STORAGE, 4.0),

        // Imperial / US
        INCH(listOf("in", "´´", "''", "\"", "inch", "inches"), UnitType.LENGTH, 0.0254),
        FOOT(listOf("ft", "´", "'", "foot", "feet"), UnitType.LENGTH, 0.3048),
        YARD(listOf("yd", "yard", "yards"), UnitType.LENGTH, 0.9144),
        MILE(listOf("mi", "mile", "miles"), UnitType.LENGTH, 1609.344),

        SQUARE_INCH(
            listOf("in²", "in2", "sqin", "squareinch", "squareinches"),
            UnitType.AREA, 0.00064516
        ),
        SQUARE_FOOT(
            listOf("ft²", "ft2", "sqft", "sf", "squarefoot", "squarefeet"),
            UnitType.AREA, 0.09290304
        ),
        SQUARE_YARD(
            listOf("yd²", "yd2", "sqyd", "squareyard", "squareyards"),
            UnitType.AREA, 0.83612736
        ),
        SQUARE_MILE(
            listOf("mi²", "mi2", "sqmi", "squaremile", "squaremiles"),
            UnitType.AREA, 2589988.1
        ),
        ACRE(listOf("acre", "ac", "acres"), UnitType.AREA, 4046.8564),

        CUBIC_INCH(
            listOf("in³", "in3", "cuin", "cubicinch", "cubicinches"),
            UnitType.VOLUME, 0.000016387064
        ),
        CUBIC_FOOT(
            listOf("ft³", "ft3", "cuft", "cubicfoot", "cubicfeet"),
            UnitType.VOLUME, 0.028316847
        ),
        CUBIC_YARD(
            listOf("yd³", "yd3", "cuyd", "cubicyard", "cubicyards"),
            UnitType.VOLUME, 0.76455486
        ),
        CUBIC_MILE(
            listOf("mi³", "mi3", "cumi", "cubicmile", "cubicmiles"),
            UnitType.VOLUME, 4168181800.0
        ),

        //Fluids
        TEASPOON(listOf("tsp", "teaspoon", "teaspoons"), UnitType.VOLUME, 0.0000049289216),
        TABLESPOON(listOf("tbsp", "tablespoon", "tablespoons"), UnitType.VOLUME, 0.000014786765),
        FLUID_OUNCE(listOf("floz", "fluidounce", "fluidounces"), UnitType.VOLUME, 0.0000002957353),
        CUP(listOf("c", "cup", "cups"), UnitType.VOLUME, 0.00023658824),
        FLUID_PINT(
            listOf("pt", "pint", "pints", "fluidpint", "fluidpints"),
            UnitType.VOLUME, 0.00047317647
        ),
        FLUID_QUART(
            listOf("qt", "quart", "quarts", "fluidquart", "fluidquarts"),
            UnitType.VOLUME,
            0.00094635295
        ),
        GALLON(listOf("gal", "gallon", "gallons"), UnitType.VOLUME, 0.0037854118),

        //Dry goods
        DRY_PINT(listOf("drypt", "dpt", "drypint", "drypints"), UnitType.VOLUME, 0.00055061047),
        DRY_QUART(listOf("dryqt", "dqt", "dryquart", "dryquarts"), UnitType.VOLUME, 0.0011012209),
        PECK(listOf("pk", "peck", "pecks"), UnitType.VOLUME, 0.0088097675),
        BUSHEL(listOf("bu", "bushel", "bushels"), UnitType.VOLUME, 0.03523907),

        OUNCE(listOf("oz", "ounce", "ounces"), UnitType.MASS, 28.349523125),
        POUND(listOf("lb", "#", "pound", "pounds"), UnitType.MASS, 453.59237),
        TON(listOf("ton", "tons"), UnitType.MASS, 907184.74),

        DEGREE_FAHRENHEIT(
            listOf("°F", "F", "degreefahrenheit", "degreesfahrenheit", "fahrenheit"),
            UnitType.TEMPERATURE,
            { (it + 459.67) / 1.8 },
            { it * 1.8 - 459.67 }),

        MILES_PER_HOUR(
            listOf("mph", "mi/h", "mileperhour", "milesperhour"),
            UnitType.SPEED,
            0.44704
        );

        constructor(
            symbol: List<String>,
            type: UnitType,
            scale: Double,
        ) : this(symbol, type, { it * scale }, { it / scale })
    }

    companion object {
        fun Unit.unit(): NonPrefixedUnit = NonPrefixedUnit(this)
    }
}
