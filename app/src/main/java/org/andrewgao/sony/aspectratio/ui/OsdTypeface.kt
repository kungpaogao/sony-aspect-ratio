package org.andrewgao.sony.aspectratio.ui

import android.graphics.Typeface
import java.lang.reflect.Modifier

/**
 * Sony's typefaces for the OSD. Sony's firmware adds static fields such as `Typeface.UNIVERS` to
 * [Typeface], so they aren't in the stock SDK we compile against and are read by reflection. Stock
 * Android, including the emulator, doesn't have them, so [univers] is null there.
 */
object OsdTypeface {
    val univers: Typeface? by lazy {
        try {
            Typeface::class.java.getField("UNIVERS").get(null) as? Typeface
        } catch (_: Exception) {
            null
        }
    }

    /** Names of the public static [Typeface] fields on [Typeface], to find a condensed variant. */
    fun names(): List<String> =
        Typeface::class
            .java
            .fields
            .filter { Modifier.isStatic(it.modifiers) && it.type == Typeface::class.java }
            .map { it.name }
}
