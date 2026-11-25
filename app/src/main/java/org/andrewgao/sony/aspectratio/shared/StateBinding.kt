package org.andrewgao.sony.aspectratio.shared

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Renders every state while started, like repeatOnLifecycle(STARTED). Start in onResume, stop in
 * onPause.
 */
class StateBinding<S>(private val state: StateFlow<S>, private val render: (S) -> Unit) {
    private val scope = MainScope()
    private var job: Job? = null

    fun start() {
        if (job == null) {
            job =
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    state.collect { render(it) }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
