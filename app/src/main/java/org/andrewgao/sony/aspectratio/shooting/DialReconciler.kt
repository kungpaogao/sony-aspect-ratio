package org.andrewgao.sony.aspectratio.shooting

/**
 * Keeps a dial's readout from jumping back while the user is stepping. The camera reports values on
 * its own slow tick, so during a burst (and [settleMs] after the last step) a report that differs
 * from the target is only remembered. Once the time has passed, the camera's latest value wins. Not
 * thread safe; the caller locks.
 */
class DialReconciler<T : Any>(
    private val settleMs: Long = SETTLE_MS,
    private val same: (T, T) -> Boolean = { a, b -> a == b },
) {
    /** What the readout should show, or null before anything is known. */
    var shown: T? = null
        private set

    private var target: T? = null
    private var lastStepAt = 0L
    private var reported: T? = null
    private var reportedSinceStep = false
    private var retried = false

    /** Forgets everything, for a new camera. */
    fun reset() {
        shown = null
        target = null
        reported = null
        reportedSinceStep = false
        retried = false
    }

    /** The user stepped to [value] at time [now]. */
    fun step(value: T, now: Long) {
        shown = value
        target = value
        lastStepAt = now
        reportedSinceStep = false
        retried = false
    }

    /** The camera reported [value]; returns what to show. */
    fun report(value: T, now: Long): T {
        reported = value
        val wanted = target
        if (wanted == null) {
            shown = value
            return value
        }
        reportedSinceStep = true
        if (now - lastStepAt < settleMs && !same(value, wanted)) return shown ?: value
        shown = value
        if (now - lastStepAt >= settleMs) target = null
        return value
    }

    /**
     * Called [settleMs] after a step. Returns true when it is done; false means call again later
     * (stepping continues, or the camera has not reported since the last step yet).
     */
    fun settle(now: Long): Boolean {
        if (target == null) return true
        if (now - lastStepAt < settleMs) return false
        if (!reportedSinceStep && !retried) {
            // The camera ticks about every 200 ms; give it one more chance before trusting the
            // older value.
            retried = true
            return false
        }
        reported?.let { shown = it }
        target = null
        return true
    }

    companion object {
        const val SETTLE_MS = 400L
    }
}
