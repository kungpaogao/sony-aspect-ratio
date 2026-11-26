package org.andrewgao.sony.aspectratio.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewModelTest {
    private class TestViewModel(scope: CoroutineScope) : ViewModel<Int>(0, scope) {
        var cleared = false

        fun bump() {
            setState { this + 1 }
        }

        override fun onCleared() {
            cleared = true
        }
    }

    @Test
    fun setStateUpdatesStateValue() {
        val model = TestViewModel(CoroutineScope(Dispatchers.Unconfined))
        model.bump()
        assertEquals(1, model.state.value)
    }

    @Test
    fun clearCancelsTheScopeAndCallsOnCleared() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val model = TestViewModel(scope)
        model.clear()
        assertFalse(scope.isActive)
        assertTrue(model.cleared)
    }
}
