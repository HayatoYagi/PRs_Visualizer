package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VisualizerStateTest {
    @Test
    fun `DialogState defaults should be correct`() {
        assertIs<DialogState.None>(DialogState.None)
    }

    @Test
    fun `FilterState defaults should be correct`() {
        val filterState = FilterState()
        assertTrue(filterState.showDrafts)
        assertFalse(filterState.onlyMine)
        assertTrue(filterState.selectedPrIds.isEmpty())
    }

    @Test
    fun `NavigationState reset helpers should keep expected values`() {
        val navState = NavigationState(
            focusPath = "some/path",
            selectedPath = "some/file.kt",
            viewportResetToken = 3,
        )

        val resetNavigation = navState.resetNavigation()
        assertEquals("", resetNavigation.focusPath)
        assertNull(resetNavigation.selectedPath)
        assertEquals(3, resetNavigation.viewportResetToken)

        val resetViewport = navState.resetViewport()
        assertEquals("some/path", resetViewport.focusPath)
        assertEquals("some/file.kt", resetViewport.selectedPath)
        assertEquals(4, resetViewport.viewportResetToken)
    }

    @Test
    fun `VisualizerState defaults should separate auth and snapshot state`() {
        val state = VisualizerState()
        assertIs<AuthState.Unauthenticated>(state.authState)
        assertIs<SnapshotFetchState.Idle>(state.snapshotFetchState)
    }

    @Test
    fun `VisualizerState defaults should be correct`() {
        val state = VisualizerState()
        assertIs<DialogState.None>(state.dialogState)
        assertTrue(state.filterState.showDrafts)
        assertFalse(state.filterState.onlyMine)
        assertEquals("", state.navigationState.focusPath)
        assertTrue(state.colorState.prColorMap.isEmpty())
    }

    @Test
    fun `VisualizerState resetForRepositoryChange should preserve toggles and clear selection state`() {
        val state = VisualizerState(
            dialogState = DialogState.FileDetails(filePath = "test.kt", defaultBranch = "main"),
            filterState = FilterState(
                showDrafts = false,
                onlyMine = true,
                selectedPrIds = setOf("pr1", "pr2"),
            ),
            navigationState = NavigationState(
                focusPath = "old/path",
                selectedPath = "old/file.kt",
                viewportResetToken = 10,
            ),
            colorState = ColorState(
                prColorMap = mapOf("pr1" to Color.Red, "pr2" to Color.Blue),
            ),
            authState = AuthState.Failed(AppError.Network("error")),
            snapshotFetchState = SnapshotFetchState.Failed(AppError.Unknown("snapshot error")),
        )

        val reset = state.resetForRepositoryChange()
        assertIs<DialogState.None>(reset.dialogState)
        assertFalse(reset.filterState.showDrafts)
        assertTrue(reset.filterState.onlyMine)
        assertTrue(reset.filterState.selectedPrIds.isEmpty())
        assertEquals("", reset.navigationState.focusPath)
        assertNull(reset.navigationState.selectedPath)
        assertEquals(0, reset.navigationState.viewportResetToken)
        assertTrue(reset.colorState.prColorMap.isEmpty())
        assertIs<AuthState.Unauthenticated>(reset.authState)
        assertIs<SnapshotFetchState.Idle>(reset.snapshotFetchState)
    }
}
