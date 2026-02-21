package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisualizerStateTest {
    @Test
    fun `RepoState should have correct fullName`() {
        val repoState = RepoState(owner = "TestOwner", repo = "TestRepo")
        assertEquals("TestOwner/TestRepo", repoState.fullName)
    }

    @Test
    fun `RepoState defaults should be empty strings`() {
        val repoState = RepoState()
        assertEquals("", repoState.owner)
        assertEquals("", repoState.repo)
        assertEquals("/", repoState.fullName)
    }

    @Test
    fun `DialogState defaults should be correct`() {
        val dialogState = DialogState()
        assertFalse(dialogState.isRepoDialogOpen)
        assertEquals("", dialogState.repoPickerQuery)
    }

    @Test
    fun `FilterState defaults should be correct`() {
        val filterState = FilterState()
        assertTrue(filterState.showDrafts)
        assertFalse(filterState.onlyMine)
        assertEquals("", filterState.query)
        assertTrue(filterState.selectedPrIds.isEmpty())
    }

    @Test
    fun `NavigationState defaults should be correct`() {
        val navState = NavigationState()
        assertEquals("", navState.focusPath)
        assertEquals(null, navState.selectedPath)
        assertEquals(0, navState.viewportResetToken)
    }

    @Test
    fun `NavigationState resetNavigation should clear paths but not token`() {
        val navState = NavigationState(
            focusPath = "some/path",
            selectedPath = "some/file.kt",
            viewportResetToken = 5,
        )
        val reset = navState.resetNavigation()

        assertEquals("", reset.focusPath)
        assertEquals(null, reset.selectedPath)
        assertEquals(5, reset.viewportResetToken)
    }

    @Test
    fun `NavigationState resetViewport should increment token`() {
        val navState = NavigationState(
            focusPath = "some/path",
            selectedPath = "some/file.kt",
            viewportResetToken = 3,
        )
        val reset = navState.resetViewport()

        assertEquals("some/path", reset.focusPath)
        assertEquals("some/file.kt", reset.selectedPath)
        assertEquals(4, reset.viewportResetToken)
    }

    @Test
    fun `ColorState defaults should have empty map`() {
        val colorState = ColorState()
        assertTrue(colorState.prColorMap.isEmpty())
    }

    @Test
    fun `VisualizerState defaults should be correct`() {
        val state = VisualizerState()
        assertEquals("", state.repoState.owner)
        assertEquals("", state.repoState.repo)
        assertFalse(state.dialogState.isRepoDialogOpen)
        assertTrue(state.filterState.showDrafts)
        assertFalse(state.filterState.onlyMine)
        assertEquals("", state.navigationState.focusPath)
        assertTrue(state.colorState.prColorMap.isEmpty())
    }

    @Test
    fun `VisualizerState resetForNewRepo should preserve toggles and clear query selection state`() {
        val state = VisualizerState(
            repoState = RepoState(owner = "OldOwner", repo = "OldRepo"),
            dialogState = DialogState(isRepoDialogOpen = true, repoPickerQuery = "test"),
            filterState = FilterState(
                showDrafts = false,
                onlyMine = true,
                query = "search",
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
        )

        val reset = state.resetForNewRepo(owner = "NewOwner", repo = "NewRepo")

        // Repo should be updated
        assertEquals("NewOwner", reset.repoState.owner)
        assertEquals("NewRepo", reset.repoState.repo)

        // Dialog should be closed and cleared
        assertFalse(reset.dialogState.isRepoDialogOpen)
        assertEquals("", reset.dialogState.repoPickerQuery)

        // Toggle filters are preserved while query and selected IDs are cleared
        assertFalse(reset.filterState.showDrafts)
        assertTrue(reset.filterState.onlyMine)
        assertEquals("", reset.filterState.query)
        assertTrue(reset.filterState.selectedPrIds.isEmpty())

        // Navigation should be cleared
        assertEquals("", reset.navigationState.focusPath)
        assertEquals(null, reset.navigationState.selectedPath)
        assertEquals(0, reset.navigationState.viewportResetToken)

        // Colors should be cleared
        assertTrue(reset.colorState.prColorMap.isEmpty())
    }

    @Test
    fun `VisualizerState should support immutable updates`() {
        val state = VisualizerState(
            repoState = RepoState(owner = "Owner", repo = "Repo"),
        )

        val updated = state.copy(
            filterState = state.filterState.copy(query = "test"),
        )

        // Original should be unchanged
        assertEquals("", state.filterState.query)
        // Updated should have new value
        assertEquals("test", updated.filterState.query)
        // Other fields should remain the same
        assertEquals("Owner", updated.repoState.owner)
    }

    @Test
    fun `FilterState should support adding and removing PR IDs`() {
        val state = FilterState(selectedPrIds = setOf("pr1", "pr2"))

        val added = state.copy(selectedPrIds = state.selectedPrIds + "pr3")
        assertEquals(setOf("pr1", "pr2", "pr3"), added.selectedPrIds)

        val removed = state.copy(selectedPrIds = state.selectedPrIds - "pr1")
        assertEquals(setOf("pr2"), removed.selectedPrIds)
    }

    @Test
    fun `ColorState should support adding colors`() {
        val state = ColorState()

        val updated = state.copy(
            prColorMap = state.prColorMap + ("pr1" to Color.Red),
        )

        assertTrue(state.prColorMap.isEmpty())
        assertEquals(1, updated.prColorMap.size)
        assertEquals(Color.Red, updated.prColorMap["pr1"])
    }
}
