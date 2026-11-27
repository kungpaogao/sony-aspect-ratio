package org.andrewgao.sony.aspectratio.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * AndroidX isn't available on API 10, so this is the part of androidx.lifecycle.ViewModel the app
 * needs: a scope that lives until [clear] and a StateFlow the view renders.
 */
abstract class ViewModel<S>(initial: S, protected val scope: CoroutineScope = MainScope()) {
    private val mutableState = MutableStateFlow(initial)
    val state: StateFlow<S> = mutableState

    protected fun setState(transform: S.() -> S) {
        mutableState.value = mutableState.value.transform()
    }

    /** Called from Activity.onResume. */
    open fun onStart() {}

    /** Called from Activity.onPause. */
    open fun onStop() {}

    fun clear() {
        scope.cancel()
        onCleared()
    }

    protected open fun onCleared() {}
}
