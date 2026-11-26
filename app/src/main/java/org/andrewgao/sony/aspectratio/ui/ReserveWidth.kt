package org.andrewgao.sony.aspectratio.ui

import android.widget.TextView
import kotlin.math.ceil

/**
 * Reserves room for the widest of [samples] so the views around this one don't move when its text
 * changes. Returns the reserved width in px; `TextView.getMinWidth()` doesn't exist on API 10.
 */
fun TextView.reserveWidthFor(vararg samples: String): Int {
    val widest = samples.maxOf { paint.measureText(it) }
    val width = ceil(widest).toInt() + paddingLeft + paddingRight
    minWidth = width
    return width
}
