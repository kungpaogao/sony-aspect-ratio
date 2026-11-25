package org.andrewgao.sony.aspectratio.shooting

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

/**
 * Formats camera values the way the native shooting screen shows them (rules from Sony's own
 * views).
 */
object ExposureFormat {
    /** Sony reports BULB as the shutter speed 65535/1. */
    private const val BULB_NUMERATOR = 65535
    private const val SHOTS_CAP = 9999

    /** `1/250` below 0.4 s, `0.5"` up to 10 s, `30"` from 10 s or for whole seconds, `BULB`. */
    fun shutter(numerator: Int, denominator: Int): String {
        if (denominator == 0) return "--"
        if (numerator == BULB_NUMERATOR && denominator == 1) return "BULB"
        val seconds = roundOneDecimal(numerator.toFloat() / denominator)
        return when {
            seconds < 0.4f -> "$numerator/$denominator"
            seconds >= 10f || denominator == 1 -> format("%.0f\"", seconds)
            else -> format("%.1f\"", seconds)
        }
    }

    /** `F2.8` below f/10, `F11` from f/10, `F--` without a lens. */
    fun aperture(apertureX100: Int): String {
        val fNumber = apertureX100 / 100f
        return when {
            fNumber == 0f -> "F--"
            fNumber < 10f -> format("F%.1f", fNumber)
            else -> format("F%.0f", fNumber)
        }
    }

    /** Exposure compensation index times its EV step: `±0.0`, `+0.3`, `-1.0`. */
    fun exposure(index: Int, step: Float): String {
        val ev = roundOneDecimal(index * step)
        if (ev == 0f) return "±0.0"
        return (if (ev > 0f) "+" else "-") + format("%.1f", abs(ev))
    }

    /** ISO 0 means auto. */
    fun iso(value: Int): String = if (value == 0) "AUTO" else value.toString()

    /** Empty when unknown; the native screen caps the count at 9999. */
    fun shots(count: Int): String = if (count < 0) "" else minOf(count, SHOTS_CAP).toString()

    fun battery(percent: Int): String = if (percent < 0) "" else "$percent%"

    /** Native battery icon level: 4 above 80%, 3 above 50%, 2 above 20%, 1 above 0, -1 unknown. */
    fun batteryLevel(percent: Int): Int =
        when {
            percent < 0 -> -1
            percent > 80 -> 4
            percent > 50 -> 3
            percent > 20 -> 2
            percent > 0 -> 1
            else -> 0
        }

    private fun format(pattern: String, value: Float): String =
        String.format(Locale.US, pattern, value)

    /** Sony rounds the decimal string, so 0.35 becomes 0.4 rather than the float's 0.3499... */
    private fun roundOneDecimal(value: Float): Float =
        BigDecimal(value.toString()).setScale(1, RoundingMode.HALF_UP).toFloat()
}
