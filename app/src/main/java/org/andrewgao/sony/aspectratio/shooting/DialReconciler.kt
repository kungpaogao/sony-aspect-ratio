package org.andrewgao.sony.aspectratio.shooting

/**
 * Keeps a dial's readout from jumping back while the user is stepping. The camera reports values on
 * its own slow tick, so during a burst and for [settleMs] after the last step a report that differs
 * from the target is only remembered. Once the time has passed, the camera's latest value wins. Not
 * thread safe; the caller locks.
 */
class DialReconciler<T : Any>(
    private val settleMs: Long = SETTLE_MS,
    private val same: (T, T) -> Boolean = { a, b -> a == b },
) {
    var shown: T? = null
        private set

    private var target: T? = null
    private var lastStepAt = 0L
    private var reported: T? = null
    private var reportedSinceStep = false
    private var retried = false

    fun reset() {
        shown = null
        target = null
        reported = null
        reportedSinceStep = false
        retried = false
    }

    fun step(value: T, now: Long) {
        shown = value
        target = value
        lastStepAt = now
        reportedSinceStep = false
        retried = false
    }

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
     * Called [settleMs] after a step. Returns false to be called again later, while stepping
     * continues or before the camera has reported since the last step.
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
