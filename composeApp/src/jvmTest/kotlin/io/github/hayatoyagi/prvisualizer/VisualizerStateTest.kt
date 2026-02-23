package io.github.hayatoyagi.prvisualizer

import io.github.hayatoyagi.prvisualizer.repository.RepoState
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VisualizerStateTest {
    @Test
    fun `RepoState from should return selected for valid values`() {
        val repoState = RepoState.from(owner = "TestOwner", repo = "TestRepo")
        val selected = assertIs<RepoState.Selected>(repoState)
        assertEquals("TestOwner", selected.owner)
        assertEquals("TestRepo", selected.repo)
    }

    @Test
    fun `RepoState from should return unselected for blank values`() {
        assertIs<RepoState.Unselected>(RepoState.from(owner = "", repo = "Repo"))
        assertIs<RepoState.Unselected>(RepoState.from(owner = "Owner", repo = ""))
        assertIs<RepoState.Unselected>(RepoState.from(owner = "", repo = ""))
    }

    @Test
    fun `DialogState defaults should be correct`() {
        assertIs<DialogState.None>(DialogState.None)
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
    fun `SessionState defaults should separate auth and snapshot state`() {
        val session = SessionState()
        assertFalse(session.authState.isLoggedIn)
        assertNull(session.snapshotFetchState.snapshot)
        assertFalse(session.snapshotFetchState.isFetching)
        assertNull(session.authState.error)
        assertNull(session.snapshotFetchState.error)
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
    fun `VisualizerState resetForRepositoryChange should preserve toggles and clear query selection state`() {
        val state = VisualizerState(
            dialogState = DialogState.FileDetails(filePath = "test.kt"),
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
            sessionState = SessionState(
                authState = AuthState(
                    oauthToken = "token",
                    error = AppError.Network("error"),
                ),
                snapshotFetchState = SnapshotFetchState(
                    snapshot = null,
                    error = AppError.Unknown("snapshot error"),
                ),
            ),
        )

        val reset = state.resetForRepositoryChange()
        assertIs<DialogState.None>(reset.dialogState)
        assertFalse(reset.filterState.showDrafts)
        assertTrue(reset.filterState.onlyMine)
        assertEquals("", reset.filterState.query)
        assertTrue(reset.filterState.selectedPrIds.isEmpty())
        assertEquals("", reset.navigationState.focusPath)
        assertNull(reset.navigationState.selectedPath)
        assertEquals(0, reset.navigationState.viewportResetToken)
        assertTrue(reset.colorState.prColorMap.isEmpty())
        assertNull(reset.sessionState.authState.error)
        assertNull(reset.sessionState.snapshotFetchState.error)
        assertNull(reset.sessionState.snapshotFetchState.snapshot)
    }
}
