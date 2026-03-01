package io.github.hayatoyagi.prvisualizer.navigation

import io.github.hayatoyagi.prvisualizer.NavigationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigationManagerTest {
    @Test
    fun `selectDirectory should update focusPath and increment token`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        val initialToken = capturedState?.viewportResetToken ?: 0

        manager.selectDirectory("src/main")

        assertEquals("src/main", capturedState?.focusPath)
        assertEquals(initialToken + 1, capturedState?.viewportResetToken)
    }

    @Test
    fun `selectDirectory should expand ancestor chain`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        manager.selectDirectory("src/ui/components")

        val state = assertNotNull(capturedState)
        assertTrue(state.explorerState.expandedPaths.contains("src"))
        assertTrue(state.explorerState.expandedPaths.contains("src/ui"))
        assertTrue(state.explorerState.expandedPaths.contains("src/ui/components"))
    }

    @Test
    fun `selectFile should update both paths and increment token`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        val initialToken = capturedState?.viewportResetToken ?: 0

        manager.selectFile("src/main/App.kt")

        assertEquals("src/main/App.kt", capturedState?.selectedPath)
        assertEquals("src/main", capturedState?.focusPath)
        assertEquals(initialToken + 1, capturedState?.viewportResetToken)
    }

    @Test
    fun `changeFocusPath should update focusPath and increment token`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        val initialToken = capturedState?.viewportResetToken ?: 0

        manager.changeFocusPath("new/path")

        assertEquals("new/path", capturedState?.focusPath)
        assertEquals(initialToken + 1, capturedState?.viewportResetToken)
    }

    @Test
    fun `updateSelectedPath should only update selectedPath`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        manager.updateSelectedPath("file.kt")

        assertEquals("file.kt", capturedState?.selectedPath)
    }

    @Test
    fun `resetNavigation should clear paths`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        manager.selectFile("src/App.kt")
        manager.resetNavigation()

        assertEquals("", capturedState?.focusPath)
        assertNull(capturedState?.selectedPath)
    }

    @Test
    fun `resetViewport should increment token`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        val initialToken = capturedState?.viewportResetToken ?: 0

        manager.resetViewport()

        assertEquals(initialToken + 1, capturedState?.viewportResetToken)
    }

    @Test
    fun `toggleDirectoryExpanded should toggle expansion state`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        // Expand
        manager.toggleDirectoryExpanded("src")
        val expandedState = assertNotNull(capturedState)
        assertTrue(expandedState.explorerState.expandedPaths.contains("src"))

        // Update manager state
        manager.updateState(expandedState)

        // Collapse
        manager.toggleDirectoryExpanded("src")
        val collapsedState = assertNotNull(capturedState)
        assertFalse(collapsedState.explorerState.expandedPaths.contains("src"))
    }

    @Test
    fun `navigateBack should return to previous path`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        manager.selectDirectory("src")
        capturedState?.let { manager.updateState(it) }

        manager.selectDirectory("src/main")
        capturedState?.let { manager.updateState(it) }

        val result = manager.navigateBack()

        assertTrue(result)
        assertEquals("src", capturedState?.focusPath)
    }

    @Test
    fun `navigateForward should move to next path`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        manager.selectDirectory("src")
        capturedState?.let { manager.updateState(it) }

        manager.selectDirectory("src/main")
        capturedState?.let { manager.updateState(it) }

        manager.navigateBack()
        capturedState?.let { manager.updateState(it) }

        val result = manager.navigateForward()

        assertTrue(result)
        assertEquals("src/main", capturedState?.focusPath)
    }

    @Test
    fun `navigateBack at beginning should return false`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        val result = manager.navigateBack()

        assertFalse(result)
    }

    @Test
    fun `navigateForward at end should return false`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        manager.selectDirectory("src")
        capturedState?.let { manager.updateState(it) }

        val result = manager.navigateForward()

        assertFalse(result)
    }

    @Test
    fun `clearHistory should clear navigation history`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        manager.selectDirectory("src")
        capturedState?.let { manager.updateState(it) }

        manager.selectDirectory("src/main")
        capturedState?.let { manager.updateState(it) }

        manager.clearHistory()

        // Should not be able to navigate back after clearing history
        val result = manager.navigateBack()
        assertFalse(result)
    }

    @Test
    fun `updateState should update internal state`() {
        var capturedState: NavigationState? = null
        val manager = NavigationManager(
            navigationState = NavigationState(),
            onStateChanged = { capturedState = it },
        )

        val newState = NavigationState(focusPath = "custom/path", viewportResetToken = 42)
        manager.updateState(newState)

        // Verify state was updated by calling resetViewport
        manager.resetViewport()

        assertEquals(43, capturedState?.viewportResetToken)
    }
}
