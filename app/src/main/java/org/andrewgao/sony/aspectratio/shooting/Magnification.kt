package org.andrewgao.sony.aspectratio.shooting

import kotlin.math.roundToInt
import org.andrewgao.sony.aspectratio.CropRect

/**
 * The camera's focus magnifier ("punch in"). [ratio] is ×100 (590 is ×5.9); [x] and [y] run from
 * -1000 to 1000 with 0 at the center and y increasing downward. When [on] is false the rest are 0.
 */
data class Magnification(
    val on: Boolean = false,
    val ratio: Int = 0,
    val level: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
) {
    /** "×5.9" for 590, "" when off. */
    val label: String
        get() = if (on) "×" + (ratio / 100) + "." + (ratio % 100 / 10) else ""
}

/**
 * The magnified area as a rectangle in a [width] by [height] frame, or null when off. It is `100 /
 * ratio` of the frame in each direction, centered at [x],[y] and shifted to stay inside the frame.
 */
fun Magnification.zoneIn(width: Int, height: Int): CropRect? {
    if (!on || ratio <= 0) return null
    val zoneWidth = (width * 100 / ratio).coerceIn(1, maxOf(width, 1))
    val zoneHeight = (height * 100 / ratio).coerceIn(1, maxOf(height, 1))
    val centerX = (x + 1000) * width / 2000
    val centerY = (y + 1000) * height / 2000
    val left = (centerX - zoneWidth / 2).coerceIn(0, maxOf(width - zoneWidth, 0))
    val top = (centerY - zoneHeight / 2).coerceIn(0, maxOf(height - zoneHeight, 0))
    return CropRect(left, top, zoneWidth, zoneHeight)
}

/**
 * The button cycle: off (null) → levels[0] → levels[1] → … → off. A [current] level that isn't in
 * [levels] starts the cycle again at the first one; no levels means there is nothing to cycle.
 */
fun nextLevel(levels: List<Int>, current: Int?): Int? {
    if (levels.isEmpty()) return null
    if (current == null) return levels[0]
    val index = levels.indexOf(current)
    if (index < 0) return levels[0]
    return levels.getOrNull(index + 1)
}

/**
 * Sony's step grid for the magnified view of a 3:2 frame. The magnified window is `200000 /
 * lowestRatio` units wide; the window's center moves in [stepsH] by [stepsV] equal steps either
 * side of the middle, which keeps its edge inside the ±1000 coordinate range.
 */
class MagnifierGrid(lowestRatio: Int, val stepsH: Int = 12, val stepsV: Int = 12) {
    private val frame = if (lowestRatio <= 0) 0f else 200000f / lowestRatio
    private val stepX = (2000 - frame) / (2 * stepsH)
    private val stepY = (2000 - frame) / (2 * stepsV)

    /** The grid step (−steps..steps) nearest the coordinate. */
    fun toStep(x: Int, y: Int): Pair<Int, Int> =
        Pair(toStep(x, stepX, stepsH), toStep(y, stepY, stepsV))

    /** The coordinate of a grid step; steps outside the grid are clamped first. */
    fun toCoordinate(stepX: Int, stepY: Int): Pair<Int, Int> =
        Pair(
            (stepX.coerceIn(-stepsH, stepsH) * this.stepX).roundToInt(),
            (stepY.coerceIn(-stepsV, stepsV) * this.stepY).roundToInt(),
        )

    /** The coordinate [dx] by [dy] steps from the grid step nearest ([x], [y]). */
    fun moved(x: Int, y: Int, dx: Int, dy: Int): Pair<Int, Int> {
        val (sx, sy) = toStep(x, y)
        return toCoordinate(sx + dx, sy + dy)
    }

    private fun toStep(value: Int, step: Float, steps: Int): Int =
        if (step <= 0f) 0 else (value / step).roundToInt().coerceIn(-steps, steps)
}
