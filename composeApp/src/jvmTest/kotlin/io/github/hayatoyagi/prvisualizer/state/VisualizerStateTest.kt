package io.github.hayatoyagi.prvisualizer.state

import androidx.compose.ui.state.ToggleableState
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot
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
        assertIs<PrSelection.AllVisible>(filterState.prSelection)
    }

    @Test
    fun `PrSelection fromExplicit should canonicalize full visible selection to AllVisible`() {
        val visibleIds = setOf("pr1", "pr2")

        val selection = PrSelection.fromExplicit(ids = visibleIds, visibleIds = visibleIds)

        assertIs<PrSelection.AllVisible>(selection)
    }

    @Test
    fun `PrSelection toggle should canonicalize explicit full selection back to AllVisible`() {
        val visibleIds = setOf("pr1", "pr2")
        val partiallySelected = PrSelection.fromExplicit(ids = setOf("pr1"), visibleIds = visibleIds)

        val selection = partiallySelected.toggle(prId = "pr2", checked = true, visibleIds = visibleIds)

        assertIs<PrSelection.AllVisible>(selection)
    }

    @Test
    fun `PrSelection resolve should intersect explicit selection with visible ids`() {
        val selection = PrSelection.fromExplicit(
            ids = setOf("pr1", "pr3"),
            visibleIds = setOf("pr1", "pr2"),
        )

        assertEquals(setOf("pr1"), selection.resolve(setOf("pr1", "pr2")))
    }

    @Test
    fun `PrSelection fromExplicit should drop stale non-visible ids before canonicalizing`() {
        val selection = PrSelection.fromExplicit(
            ids = setOf("pr1", "pr2", "stale"),
            visibleIds = setOf("pr1", "pr2"),
        )

        assertIs<PrSelection.AllVisible>(selection)
    }

    @Test
    fun `PrSelection triState should report On for AllVisible`() {
        val triState = PrSelection.allVisible().triState(setOf("pr1", "pr2"))

        assertEquals(ToggleableState.On, triState)
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
        assertIs<SnapshotFetchState.Idle>(state.snapshotFetchState)
    }

    @Test
    fun `SnapshotFetchState Ready should contain filter, navigation, and color state with defaults`() {
        val snapshot = GitHubSnapshot(
            rootNode = FileNode.Directory(path = "", name = "repo", children = emptyList(), weight = 1.0),
            pullRequests = emptyList(),
            viewerLogin = null,
            defaultBranch = "main",
        )
        val ready = SnapshotFetchState.Ready(snapshot = snapshot)

        assertTrue(ready.filterState.showDrafts)
        assertFalse(ready.filterState.onlyMine)
        assertIs<PrSelection.AllVisible>(ready.filterState.prSelection)
        assertEquals("", ready.navigationState.focusPath)
        assertNull(ready.navigationState.selectedPath)
        assertTrue(ready.colorState.prColorMap.isEmpty())
    }

    @Test
    fun `VisualizerState resetForRepositoryChange should clear snapshot and dialog`() {
        val snapshot = GitHubSnapshot(
            rootNode = FileNode.Directory(path = "", name = "repo", children = emptyList(), weight = 1.0),
            pullRequests = emptyList(),
            viewerLogin = null,
            defaultBranch = "main",
        )
        val state = VisualizerState(
            dialogState = DialogState.FileDetails(filePath = "test.kt", defaultBranch = "main"),
            snapshotFetchState = SnapshotFetchState.Ready(
                snapshot = snapshot,
                filterState = FilterState(
                    showDrafts = false,
                    onlyMine = true,
                    prSelection = PrSelection.fromExplicit(
                        ids = setOf("pr1", "pr2"),
                        visibleIds = setOf("other"),
                    ),
                ),
                navigationState = NavigationState(
                    focusPath = "old/path",
                    selectedPath = "old/file.kt",
                    viewportResetToken = 10,
                ),
                colorState = ColorState(
                    prColorMap = mapOf("pr1" to androidx.compose.ui.graphics.Color.Red),
                ),
            ),
            authState = AuthState.Failed(AppError.Network("error")),
        )

        val reset = state.resetForRepositoryChange()
        assertIs<DialogState.None>(reset.dialogState)
        // Snapshot resets to Idle — filter, navigation, and color are gone
        assertIs<SnapshotFetchState.Idle>(reset.snapshotFetchState)
        // Auth error is cleared
        assertIs<AuthState.Unauthenticated>(reset.authState)
    }

    @Test
    fun `VisualizerState resetForRepositoryChange should preserve non-failed auth`() {
        val state = VisualizerState(
            authState = AuthState.Authenticated(oauthToken = "token"),
        )

        val reset = state.resetForRepositoryChange()
        assertIs<AuthState.Authenticated>(reset.authState)
    }
}
