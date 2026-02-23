package io.github.hayatoyagi.prvisualizer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigationHistoryTest {
    @Test
    fun testRecordAndNavigateBack() {
        val history = NavigationHistory()

        // Record first path
        history.recordFocusPath("path1")
        assertEquals("path1", history.currentPath())
        assertFalse(history.canNavigateBack())

        // Record second path
        history.recordFocusPath("path2")
        assertEquals("path2", history.currentPath())
        assertTrue(history.canNavigateBack())

        // Navigate back
        val previousPath = history.navigateBack()
        assertEquals("path1", previousPath)
        assertEquals("path1", history.currentPath())
        assertFalse(history.canNavigateBack())
    }

    @Test
    fun testNavigateForward() {
        val history = NavigationHistory()

        history.recordFocusPath("path1")
        history.recordFocusPath("path2")
        history.recordFocusPath("path3")

        // Navigate back twice
        history.navigateBack()
        history.navigateBack()
        assertEquals("path1", history.currentPath())

        // Navigate forward
        assertTrue(history.canNavigateForward())
        val nextPath = history.navigateForward()
        assertEquals("path2", nextPath)
        assertEquals("path2", history.currentPath())
    }

    @Test
    fun testRecordClearsForwardHistory() {
        val history = NavigationHistory()

        history.recordFocusPath("path1")
        history.recordFocusPath("path2")
        history.recordFocusPath("path3")

        // Navigate back
        history.navigateBack()
        assertEquals("path2", history.currentPath())
        assertTrue(history.canNavigateForward())

        // Record new path, should clear forward history
        history.recordFocusPath("path4")
        assertEquals("path4", history.currentPath())
        assertFalse(history.canNavigateForward())

        // Navigate back should go to path2, not path3
        history.navigateBack()
        assertEquals("path2", history.currentPath())
    }

    @Test
    fun testRecordSamePathDoesNotDuplicate() {
        val history = NavigationHistory()

        history.recordFocusPath("path1")
        history.recordFocusPath("path1")
        history.recordFocusPath("path1")

        // Should still only have one entry
        assertFalse(history.canNavigateBack())
        assertEquals("path1", history.currentPath())
    }

    @Test
    fun testClear() {
        val history = NavigationHistory()

        history.recordFocusPath("path1")
        history.recordFocusPath("path2")
        history.recordFocusPath("path3")

        history.clear()

        assertNull(history.currentPath())
        assertFalse(history.canNavigateBack())
        assertFalse(history.canNavigateForward())
    }

    @Test
    fun testNavigateBackWhenEmpty() {
        val history = NavigationHistory()

        assertNull(history.navigateBack())
        assertFalse(history.canNavigateBack())
    }

    @Test
    fun testNavigateForwardWhenEmpty() {
        val history = NavigationHistory()

        assertNull(history.navigateForward())
        assertFalse(history.canNavigateForward())
    }

    @Test
    fun testNavigateForwardWhenAtEnd() {
        val history = NavigationHistory()

        history.recordFocusPath("path1")
        history.recordFocusPath("path2")

        // Already at end
        assertNull(history.navigateForward())
        assertFalse(history.canNavigateForward())
    }
}
