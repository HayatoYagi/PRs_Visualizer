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
    }

    @Test
    fun `PrSelection fromExplicit should canonicalize full visible selection to AllVisible`() {
        val ready = createReady(
            prSelection = PrSelection.Explicit.create(ids = setOf("pr1", "pr2")),
            pullRequestIds = setOf("pr1", "pr2"),
        )

        assertEquals(setOf("pr1", "pr2"), ready.selectedPrIds)
        assertEquals(ToggleableState.On, ready.selectAllState)
    }

    @Test
    fun `PrSelection toggle should canonicalize explicit full selection back to AllVisible`() {
        val ready = createReady(
            prSelection = PrSelection.Explicit.create(ids = setOf("pr1")),
            pullRequestIds = setOf("pr1", "pr2"),
        )

        val selection = ready.togglePrSelection(
            prId = "pr2",
            checked = true,
        )

        assertIs<PrSelection.AllVisible>(selection)
    }

    @Test
    fun `PrSelection resolve should intersect explicit selection with visible ids`() {
        val ready = createReady(
            prSelection = PrSelection.Explicit.create(ids = setOf("pr1", "pr3")),
            pullRequestIds = setOf("pr1", "pr2"),
        )

        assertEquals(setOf("pr1"), ready.resolveSelectedPrIds())
    }

    @Test
    fun `PrSelection fromExplicit should drop stale non-visible ids before canonicalizing`() {
        val ready = createReady(
            prSelection = PrSelection.Explicit.create(ids = setOf("pr1", "pr2", "stale")),
            pullRequestIds = setOf("pr1", "pr2"),
        )

        assertEquals(setOf("pr1", "pr2"), ready.selectedPrIds)
        assertEquals(ToggleableState.On, ready.selectAllState)
    }

    @Test
    fun `PrSelection triState should report On for AllVisible`() {
        val ready = createReady(
            prSelection = PrSelection.allVisible(),
            pullRequestIds = setOf("pr1", "pr2"),
        )

        val triState = ready.resolveSelectAllState()

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
        assertIs<PrSelection.AllVisible>(ready.prSelection)
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
                ),
                prSelection = PrSelection.Explicit.create(ids = setOf("pr1", "pr2")),
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

private fun createReady(
    prSelection: PrSelection,
    pullRequestIds: Set<String>,
): SnapshotFetchState.Ready = SnapshotFetchState.Ready(
    snapshot = GitHubSnapshot(
        rootNode = FileNode.Directory(path = "", name = "repo", children = emptyList(), weight = 1.0),
        pullRequests = pullRequestIds.map { prId ->
            io.github.hayatoyagi.prvisualizer.PullRequest(
                id = prId,
                number = prId.removePrefix("pr").toIntOrNull() ?: 1,
                title = prId,
                author = "author",
                isDraft = false,
                url = "https://example.com/$prId",
                files = emptyList(),
            )
        },
        viewerLogin = null,
        defaultBranch = "main",
    ),
    filterState = FilterState(),
    prSelection = prSelection,
)
