package com.oorbitt.launcher.search.provider

import com.oorbitt.launcher.model.SearchResult
import com.oorbitt.launcher.model.SearchResultAction
import com.oorbitt.launcher.search.SearchProvider
import kotlin.math.roundToInt

/**
 * Offline unit conversion search provider.
 *
 * Detects queries like "5kg in lbs", "100 cm to inches", "72°F in °C"
 * and returns the converted result as a [SearchResultAction.ShowAnswer].
 */
class UnitConverterProvider : SearchProvider {

    override val priority: Int = 85

    // ── regex ────────────────────────────────────────────────────────────
    // Matches: 5kg in lbs | 100 cm to inches | 72°F in °C | 1.5GB to MB
    // Groups:  1=number  2=fromUnit  3=toUnit
    private val conversionPattern: Regex =
        """(-?\d+\.?\d*)\s*([a-zA-Z°/]+)\s+(?:in|to)\s+([a-zA-Z°/]+)""".toRegex(RegexOption.IGNORE_CASE)

    override suspend fun search(query: String): List<SearchResult> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        val match = conversionPattern.find(trimmed) ?: return emptyList()

        val valueStr = match.groupValues[1]
        val fromRaw = match.groupValues[2]
        val toRaw = match.groupValues[3]

        val value = valueStr.toDoubleOrNull() ?: return emptyList()
        val fromUnit = normalizeUnit(fromRaw)
        val toUnit = normalizeUnit(toRaw)

        if (fromUnit == toUnit) {
            return listOf(buildResult(value, fromRaw, toRaw, value))
        }

        val result = convert(value, fromUnit, toUnit) ?: return emptyList()

        return listOf(buildResult(value, fromRaw, toRaw, result))
    }

    // ── conversion router ────────────────────────────────────────────────

    private fun convert(value: Double, from: String, to: String): Double? {
        // Temperature is formula-based, handle separately.
        if (from in temperatureUnits && to in temperatureUnits) {
            return convertTemperature(value, from, to)
        }

        // For every other category, convert via a common base unit.
        val table = findTable(from, to) ?: return null
        val fromFactor = table[from] ?: return null
        val toFactor = table[to] ?: return null
        // value in base = value * fromFactor; result = base / toFactor
        return value * fromFactor / toFactor
    }

    private fun findTable(from: String, to: String): Map<String, Double>? {
        return conversionTables.firstOrNull { from in it && to in it }
    }

    // ── temperature ──────────────────────────────────────────────────────

    private val temperatureUnits = setOf("c", "f", "k")

    private fun convertTemperature(value: Double, from: String, to: String): Double? {
        // Convert to Celsius first, then to target.
        val celsius = when (from) {
            "c" -> value
            "f" -> (value - 32.0) * 5.0 / 9.0
            "k" -> value - 273.15
            else -> return null
        }
        return when (to) {
            "c" -> celsius
            "f" -> celsius * 9.0 / 5.0 + 32.0
            "k" -> celsius + 273.15
            else -> null
        }
    }

    // ── unit normalisation ───────────────────────────────────────────────

    private fun normalizeUnit(raw: String): String {
        return when (raw.lowercase().replace("°", "")) {
            // Weight
            "kg", "kgs", "kilogram", "kilograms" -> "kg"
            "g", "gram", "grams" -> "g"
            "mg", "milligram", "milligrams" -> "mg"
            "lb", "lbs", "pound", "pounds" -> "lbs"
            "oz", "ounce", "ounces" -> "oz"
            "ton", "tons", "tonne", "tonnes" -> "ton"
            "st", "stone", "stones" -> "stone"

            // Length
            "km", "kilometer", "kilometers", "kilometre", "kilometres" -> "km"
            "m", "meter", "meters", "metre", "metres" -> "m"
            "cm", "centimeter", "centimeters", "centimetre", "centimetres" -> "cm"
            "mm", "millimeter", "millimeters", "millimetre", "millimetres" -> "mm"
            "mi", "mile", "miles" -> "mi"
            "yd", "yard", "yards" -> "yd"
            "ft", "foot", "feet" -> "ft"
            "in", "inch", "inches" -> "in"

            // Temperature
            "c", "celsius", "centigrade" -> "c"
            "f", "fahrenheit" -> "f"
            "k", "kelvin" -> "k"

            // Volume
            "l", "liter", "liters", "litre", "litres" -> "l"
            "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> "ml"
            "gal", "gallon", "gallons" -> "gal"
            "qt", "quart", "quarts" -> "qt"
            "pt", "pint", "pints" -> "pt"
            "cup", "cups" -> "cup"
            "floz", "fl oz" -> "floz"
            "tbsp", "tablespoon", "tablespoons" -> "tbsp"
            "tsp", "teaspoon", "teaspoons" -> "tsp"

            // Time
            "s", "sec", "second", "seconds" -> "s"
            "min", "minute", "minutes" -> "min"
            "hr", "hour", "hours" -> "hr"
            "day", "days" -> "day"
            "week", "weeks", "wk" -> "week"
            "month", "months", "mo" -> "month"
            "year", "years", "yr" -> "year"

            // Data
            "b", "byte", "bytes" -> "b"
            "kb", "kilobyte", "kilobytes" -> "kb"
            "mb", "megabyte", "megabytes" -> "mb"
            "gb", "gigabyte", "gigabytes" -> "gb"
            "tb", "terabyte", "terabytes" -> "tb"
            "pb", "petabyte", "petabytes" -> "pb"
            "bit", "bits" -> "bit"
            "kbit", "kilobit", "kilobits" -> "kbit"
            "mbit", "megabit", "megabits" -> "mbit"
            "gbit", "gigabit", "gigabits" -> "gbit"

            // Speed
            "m/s", "mps" -> "m/s"
            "km/h", "kmh", "kph" -> "km/h"
            "mph" -> "mph"
            "knot", "knots", "kn", "kt" -> "knots"
            "ft/s", "fps" -> "ft/s"

            else -> raw.lowercase().replace("°", "")
        }
    }

    // ── conversion tables (value relative to base unit) ──────────────────
    // Each map: unit -> factor-to-base.  base unit has factor 1.0.

    private val weightTable: Map<String, Double> = mapOf(
        "kg"    to 1.0,            // base
        "g"     to 0.001,
        "mg"    to 0.000_001,
        "lbs"   to 0.453_592_37,
        "oz"    to 0.028_349_523_125,
        "ton"   to 1_000.0,
        "stone" to 6.350_293_18,
    )

    private val lengthTable: Map<String, Double> = mapOf(
        "m"  to 1.0,               // base
        "km" to 1_000.0,
        "cm" to 0.01,
        "mm" to 0.001,
        "mi" to 1_609.344,
        "yd" to 0.9144,
        "ft" to 0.3048,
        "in" to 0.0254,
    )

    private val volumeTable: Map<String, Double> = mapOf(
        "l"    to 1.0,             // base
        "ml"   to 0.001,
        "gal"  to 3.785_411_784,
        "qt"   to 0.946_352_946,
        "pt"   to 0.473_176_473,
        "cup"  to 0.236_588_236_5,
        "floz" to 0.029_573_529_562_5,
        "tbsp" to 0.014_786_764_781_25,
        "tsp"  to 0.004_928_921_593_75,
    )

    private val timeTable: Map<String, Double> = mapOf(
        "s"     to 1.0,            // base
        "min"   to 60.0,
        "hr"    to 3_600.0,
        "day"   to 86_400.0,
        "week"  to 604_800.0,
        "month" to 2_629_746.0,    // average month (365.2425 / 12 * 86400)
        "year"  to 31_556_952.0,   // average Gregorian year
    )

    private val dataTable: Map<String, Double> = mapOf(
        "b"    to 1.0,             // base (bytes)
        "kb"   to 1_024.0,
        "mb"   to 1_048_576.0,
        "gb"   to 1_073_741_824.0,
        "tb"   to 1_099_511_627_776.0,
        "pb"   to 1_125_899_906_842_624.0,
        "bit"  to 0.125,
        "kbit" to 128.0,
        "mbit" to 131_072.0,
        "gbit" to 134_217_728.0,
    )

    private val speedTable: Map<String, Double> = mapOf(
        "m/s"   to 1.0,            // base
        "km/h"  to 1.0 / 3.6,
        "mph"   to 0.447_04,
        "knots" to 0.514_444,
        "ft/s"  to 0.3048,
    )

    private val conversionTables: List<Map<String, Double>> = listOf(
        weightTable,
        lengthTable,
        volumeTable,
        timeTable,
        dataTable,
        speedTable,
    )

    // ── result builder ───────────────────────────────────────────────────

    private fun buildResult(
        value: Double,
        fromLabel: String,
        toLabel: String,
        result: Double,
    ): SearchResult {
        val formattedValue = formatNumber(value)
        val formattedResult = formatNumber(result)
        val answer = "$formattedValue $fromLabel = $formattedResult $toLabel"

        return SearchResult(
            id = "unit_conversion_${fromLabel}_${toLabel}",
            title = answer,
            subtitle = "Unit Conversion",
            iconUri = null,
            action = SearchResultAction.ShowAnswer(answer),
            relevanceScore = 0.95f,
            providerName = "Units",
        )
    }

    /**
     * Formats a number for display:
     * - Integers are shown without decimals (e.g. 5, 100)
     * - Decimals are shown with up to 6 significant fractional digits,
     *   trailing zeros stripped.
     */
    private fun formatNumber(value: Double): String {
        if (value == value.toLong().toDouble() && value in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) {
            return value.toLong().toString()
        }
        // Up to 6 decimal places, strip trailing zeros.
        val formatted = "%.6f".format(value).trimEnd('0').trimEnd('.')
        return formatted
    }
}
