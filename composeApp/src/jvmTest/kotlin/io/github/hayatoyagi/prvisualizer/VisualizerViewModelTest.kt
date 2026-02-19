package io.github.hayatoyagi.prvisualizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VisualizerViewModelTest {

    @Test
    fun navigateBackReturnsToRootAfterResetAndFirstNavigation() {
        val viewModel = VisualizerViewModel()

        viewModel.resetNavigation()
        viewModel.selectDirectory("src")

        assertTrue(viewModel.navigateBack())
        assertEquals("", viewModel.focusPath)
    }
}
