package org.andrewgao.sony.aspectratio.shooting

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

/** A shutter speed as Sony's numerator/denominator pair. */
data class ShutterSpeed(val numerator: Int, val denominator: Int) {
    val seconds: Double
        get() = numerator.toDouble() / denominator
}

/**
 * The aperture and shutter steps the dials walk through, so the readout can move before the camera
 * reports the new value. Pure, so it can be tested without a camera.
 */
object ExposureSteps {
    /** Two values closer than this many stops are the same step. */
    const val SAME_STOPS = 0.1

    /** Half-stop exposure steps (evStep 0.5) use the half-stop tables. */
    fun isHalfStop(evStep: Float): Boolean = abs(evStep - 0.5f) < 0.1f

    /** F-number x100 in 1/3-stop steps, as Sony labels them. */
    val APERTURE_THIRDS =
        listOf(
            100,
            110,
            120,
            140,
            160,
            180,
            200,
            220,
            250,
            280,
            320,
            350,
            400,
            450,
            500,
            560,
            630,
            710,
            800,
            900,
            1000,
            1100,
            1300,
            1400,
            1600,
            1800,
            2000,
            2200,
            2500,
            2900,
            3200,
            3600,
            4000,
            4500,
            5100,
            5700,
            6400,
        )

    val APERTURE_HALVES =
        listOf(
            100,
            120,
            140,
            170,
            200,
            240,
            280,
            330,
            400,
            480,
            560,
            670,
            800,
            950,
            1100,
            1300,
            1600,
            1900,
            2200,
            2700,
            3200,
            3800,
            4500,
            5400,
            6400,
        )

    /**
     * Longest first. Pairs are what [ExposureFormat.shutter] turns into the label: whole seconds
     * over 1, tenths over 10, and Sony's own 10/16 for 0.6".
     */
    val SHUTTER_THIRDS =
        pairs(
            "30/1 25/1 20/1 15/1 13/1 10/1 8/1 6/1 5/1 4/1 32/10 25/10 2/1 16/10 13/10 1/1 8/10 " +
                "10/16 5/10 4/10 1/3 1/4 1/5 1/6 1/8 1/10 1/13 1/15 1/20 1/25 1/30 1/40 1/50 " +
                "1/60 1/80 1/100 1/125 1/160 1/200 1/250 1/320 1/400 1/500 1/640 1/800 1/1000 " +
                "1/1250 1/1600 1/2000 1/2500 1/3200 1/4000 1/5000 1/6400 1/8000",
        )

    val SHUTTER_HALVES =
        pairs(
            "30/1 20/1 15/1 10/1 8/1 6/1 4/1 3/1 2/1 15/10 1/1 7/10 5/10 1/3 1/4 1/6 1/8 1/10 " +
                "1/15 1/20 1/30 1/45 1/60 1/90 1/125 1/180 1/250 1/350 1/500 1/750 1/1000 " +
                "1/1500 1/2000 1/3000 1/4000 1/6000 1/8000",
        )

    /**
     * The aperture [steps] away from [currentX100] (positive is a larger f-number), inside [range]
     * (`{min, max}` x100) when known. Null without a lens.
     */
    fun nextAperture(currentX100: Int, steps: Int, halfStop: Boolean, range: IntArray?): Int? {
        if (currentX100 <= 0) return null
        val table = if (halfStop) APERTURE_HALVES else APERTURE_THIRDS
        val limited =
            if (range == null || range.size < 2 || range[0] <= 0 || range[1] <= 0) table
            else {
                val low = min(range[0], range[1])
                val high = maxOf(range[0], range[1])
                table.filter {
                    apertureStops(it, low) > -SAME_STOPS && apertureStops(high, it) > -SAME_STOPS
                }
            }
        val usable = limited.ifEmpty { table }
        val index = usable.indices.minByOrNull { abs(apertureStops(usable[it], currentX100)) }!!
        return usable[(index + steps).coerceIn(0, usable.lastIndex)]
    }

    /**
     * The shutter speed [steps] away from the current one (positive is faster), inside [range]
     * (`{minN, minD, maxN, maxD}`) when known. Null for BULB or an unknown speed.
     */
    fun nextShutter(
        numerator: Int,
        denominator: Int,
        steps: Int,
        halfStop: Boolean,
        range: IntArray?,
    ): ShutterSpeed? {
        if (numerator <= 0 || denominator <= 0) return null
        val current = ShutterSpeed(numerator, denominator)
        val table = if (halfStop) SHUTTER_HALVES else SHUTTER_THIRDS
        val limited =
            if (range == null || range.size < 4 || range[1] <= 0 || range[3] <= 0) table
            else {
                // The camera's min and max may name either end, so order them by value.
                val a = range[0].toDouble() / range[1]
                val b = range[2].toDouble() / range[3]
                val shortest = min(a, b)
                val longest = maxOf(a, b)
                table.filter {
                    stops(it.seconds, shortest) > -SAME_STOPS &&
                        stops(longest, it.seconds) > -SAME_STOPS
                }
            }
        val usable = limited.ifEmpty { table }
        val index = usable.indices.minByOrNull { abs(stops(usable[it].seconds, current.seconds)) }!!
        return usable[(index + steps).coerceIn(0, usable.lastIndex)]
    }

    fun sameAperture(a: Int, b: Int): Boolean =
        a > 0 && b > 0 && abs(apertureStops(a, b)) < SAME_STOPS

    fun sameShutter(a: ShutterSpeed, b: ShutterSpeed): Boolean =
        a.denominator > 0 &&
            b.denominator > 0 &&
            a.numerator > 0 &&
            b.numerator > 0 &&
            abs(stops(a.seconds, b.seconds)) < SAME_STOPS

    /** Stops of [a] over [b]: positive when [a] is the longer time. */
    private fun stops(a: Double, b: Double): Double = ln(a / b) / LN2

    /** Stops of f-number [a] over [b]: light falls with the square of the f-number. */
    private fun apertureStops(a: Int, b: Int): Double = 2 * ln(a.toDouble() / b) / LN2

    private val LN2 = ln(2.0)

    private fun pairs(text: String): List<ShutterSpeed> =
        text.split(' ').map {
            val (n, d) = it.split('/')
            ShutterSpeed(n.toInt(), d.toInt())
        }
}

/** Steps through the camera's ISO list, which may hold 0 for AUTO. */
object IsoSteps {
    /** The ISO [steps] away from [current] in [supported]'s own order; null if nothing moves. */
    fun next(supported: List<Int>, current: Int, steps: Int): Int? {
        if (supported.isEmpty()) return null
        var index = supported.indexOf(current)
        if (index < 0) {
            index = supported.indices.minByOrNull { abs(supported[it] - current) } ?: 0
        }
        val next = (index + steps).coerceIn(0, supported.lastIndex)
        return if (next == index && supported[index] == current) null else supported[next]
    }
}
