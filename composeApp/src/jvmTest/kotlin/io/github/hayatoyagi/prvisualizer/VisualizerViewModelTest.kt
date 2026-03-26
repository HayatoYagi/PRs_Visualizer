package io.github.hayatoyagi.prvisualizer

import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.repository.store.InMemorySelectedRepositoryStore
import io.github.hayatoyagi.prvisualizer.state.AuthState
import io.github.hayatoyagi.prvisualizer.state.DialogState
import io.github.hayatoyagi.prvisualizer.state.PrSelection
import io.github.hayatoyagi.prvisualizer.state.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.state.VisualizerState
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VisualizerViewModelTest {
    private val emptySnapshot = GitHubSnapshot(
        rootNode = FileNode.Directory(path = "", name = "repo", children = emptyList(), weight = 1.0),
        pullRequests = emptyList(),
        viewerLogin = null,
        defaultBranch = "main",
    )

    private fun readyState(
        snapshot: GitHubSnapshot = emptySnapshot,
    ) = VisualizerState(
        snapshotFetchState = SnapshotFetchState.Ready(snapshot = snapshot),
    )

    private fun snapshotWithPrs(vararg prs: PullRequest) = emptySnapshot.copy(
        pullRequests = prs.toList(),
    )

    private fun pr(id: String, author: String = "author", isDraft: Boolean = false) = PullRequest(
        id = id,
        number = id.hashCode(),
        title = "PR $id",
        author = author,
        isDraft = isDraft,
        url = "https://example.com/$id",
        files = emptyList(),
    )

    /**
     * Helper to access the Ready state from the ViewModel.
     */
    private val VisualizerViewModel.readyState: SnapshotFetchState.Ready
        get() = state.snapshotFetchState as SnapshotFetchState.Ready

    @Test
    fun `ViewModel should initialize repository from injected store`() {
        val store = InMemorySelectedRepositoryStore(
            initial = RepoState.Selected(owner = "InjectedOwner", repo = "InjectedRepo"),
        )

        val vm = VisualizerViewModel(selectedRepositoryStore = store)
        val selected = assertIs<RepoState.Selected>(vm.repoState.value)
        assertEquals("InjectedOwner", selected.owner)
        assertEquals("InjectedRepo", selected.repo)
    }

    @Test
    fun `ViewModel should default to Unselected repository state`() {
        val vm = VisualizerViewModel(selectedRepositoryStore = InMemorySelectedRepositoryStore())
        assertIs<RepoState.Unselected>(vm.repoState.value)
    }

    @Test
    fun `openRepoDialog should set dialog state correctly`() {
        val vm = VisualizerViewModel(selectedRepositoryStore = InMemorySelectedRepositoryStore())
        vm.openRepoDialog()

        assertIs<DialogState.RepoPicker>(vm.state.dialogState)
    }

    @Test
    fun `closeRepoDialog should close the dialog`() {
        val vm = VisualizerViewModel(selectedRepositoryStore = InMemorySelectedRepositoryStore())
        vm.openRepoDialog()
        assertIs<DialogState.RepoPicker>(vm.state.dialogState)

        vm.closeRepoDialog()
        assertIs<DialogState.None>(vm.state.dialogState)
    }

    @Test
    fun `openFileDetailsDialog should require ready snapshot`() {
        val vm = VisualizerViewModel(selectedRepositoryStore = InMemorySelectedRepositoryStore())
        assertFailsWith<IllegalStateException> {
            vm.openFileDetailsDialog("src/main/App.kt")
        }
    }

    @Test
    fun `openFileDetailsDialog should set dialog state correctly when snapshot is ready`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(emptySnapshot.copy(defaultBranch = "develop")),
        )

        vm.openFileDetailsDialog("src/main/App.kt")

        val dialog = assertIs<DialogState.FileDetails>(vm.state.dialogState)
        assertEquals("src/main/App.kt", dialog.filePath)
        assertEquals("develop", dialog.defaultBranch)
    }

    @Test
    fun `closeDialog should close the dialog`() {
        val vm = VisualizerViewModel(selectedRepositoryStore = InMemorySelectedRepositoryStore())
        vm.openRepoDialog()
        assertIs<DialogState.RepoPicker>(vm.state.dialogState)

        vm.closeDialog()
        assertIs<DialogState.None>(vm.state.dialogState)
    }

    @Test
    fun `closeDialog should reset auth state when dismissing device flow prompt`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = VisualizerState(
                authState = AuthState.Authorizing(
                    deviceUserCode = "ABCD-EFGH",
                    deviceVerificationUrl = "https://github.com/login/device",
                ),
                dialogState = DialogState.DeviceFlowPrompt(
                    userCode = "ABCD-EFGH",
                    verificationUrl = "https://github.com/login/device",
                ),
            ),
        )

        vm.closeDialog()

        assertEquals(AuthState.Unauthenticated, vm.state.authState)
        assertIs<DialogState.None>(vm.state.dialogState)
    }

    @Test
    fun `selectRepo should reset snapshot to Idle, clearing filter, navigation, and color state`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(
                initial = RepoState.Selected(owner = "Old", repo = "Repo"),
            ),
            initialState = readyState(),
        )

        // Modify some state inside Ready
        vm.openRepoDialog()
        vm.updateShowDrafts(false)
        vm.deselectAllPrs()

        // Select new repo — resets to Idle
        vm.selectRepo("New/Repository")

        val selected = assertIs<RepoState.Selected>(vm.repoState.value)
        assertEquals("New", selected.owner)
        assertEquals("Repository", selected.repo)
        assertIs<DialogState.None>(vm.state.dialogState)
        // Snapshot reverts to Idle: filter/nav/color no longer accessible
        assertIs<SnapshotFetchState.Idle>(vm.state.snapshotFetchState)
    }

    @Test
    fun `selectRepo same repo twice should apply reset only once`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        // Switch to a repo — triggers reset
        vm.updateShowDrafts(false)
        vm.selectRepo("Other/Repo")
        assertIs<SnapshotFetchState.Idle>(vm.state.snapshotFetchState)

        // Selecting the same repo again should not re-apply reset
        vm.selectRepo("Other/Repo")
        assertIs<SnapshotFetchState.Idle>(vm.state.snapshotFetchState)
    }

    @Test
    fun `updateShowDrafts should update filter in Ready state`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        assertTrue(vm.readyState.filterState.showDrafts)

        vm.updateShowDrafts(false)
        assertFalse(vm.readyState.filterState.showDrafts)
    }

    @Test
    fun `updateShowDrafts should be no-op when not Ready`() {
        val vm = VisualizerViewModel(selectedRepositoryStore = InMemorySelectedRepositoryStore())
        assertIs<SnapshotFetchState.Idle>(vm.state.snapshotFetchState)

        vm.updateShowDrafts(false) // Should not crash
        assertIs<SnapshotFetchState.Idle>(vm.state.snapshotFetchState)
    }

    @Test
    fun `updateOnlyMine should update filter in Ready state`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        assertFalse(vm.readyState.filterState.onlyMine)

        vm.updateOnlyMine(true)
        assertTrue(vm.readyState.filterState.onlyMine)
    }

    @Test
    fun `togglePr should add and remove PR IDs`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(snapshotWithPrs(pr("pr1"), pr("pr2"))),
        )
        assertIs<PrSelection.AllVisible>(vm.readyState.prSelection)

        vm.deselectAllPrs()
        vm.togglePr("pr1", checked = true)
        assertEquals(setOf("pr1"), assertIs<PrSelection.Explicit>(vm.readyState.prSelection).ids)

        vm.togglePr("pr2", checked = true)
        assertIs<PrSelection.AllVisible>(vm.readyState.prSelection)

        vm.togglePr("pr1", checked = false)
        assertEquals(setOf("pr2"), assertIs<PrSelection.Explicit>(vm.readyState.prSelection).ids)
    }

    @Test
    fun `toggleSelectAll should deselect when all selected and select when not all`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(snapshotWithPrs(pr("pr1"), pr("pr2"))),
        )

        // Initially AllVisible → toggleSelectAll should deselect all
        vm.toggleSelectAll()
        val explicit = assertIs<PrSelection.Explicit>(vm.readyState.prSelection)
        assertEquals(emptySet(), explicit.ids)

        // Now none selected → toggleSelectAll should select all
        vm.toggleSelectAll()
        assertIs<PrSelection.AllVisible>(vm.readyState.prSelection)
    }

    @Test
    fun `selectAllPrs should set selection to AllVisible`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        vm.deselectAllPrs()
        vm.selectAllPrs()
        assertIs<PrSelection.AllVisible>(vm.readyState.prSelection)
    }

    @Test
    fun `deselectAllPrs should clear all PR IDs`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        vm.selectAllPrs()

        vm.deselectAllPrs()
        assertEquals(emptySet(), assertIs<PrSelection.Explicit>(vm.readyState.prSelection).ids)
    }

    @Test
    fun `selectDirectory should update focusPath and reset token`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        val initialToken = vm.readyState.navigationState.viewportResetToken

        vm.selectDirectory("src/main")
        assertEquals("src/main", vm.readyState.navigationState.focusPath)
        assertEquals(initialToken + 1, vm.readyState.navigationState.viewportResetToken)
    }

    @Test
    fun `selectDirectory should expand ancestor chain`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        vm.selectDirectory("src")
        vm.toggleDirectoryExpanded("src")

        vm.selectDirectory("src/ui")

        assertTrue(vm.readyState.navigationState.explorerState.expandedPaths.contains("src"))
        assertTrue(vm.readyState.navigationState.explorerState.expandedPaths.contains("src/ui"))
    }

    @Test
    fun `selectFile should update both paths and reset token`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        val initialToken = vm.readyState.navigationState.viewportResetToken

        vm.selectFile("src/main/App.kt")
        assertEquals("src/main/App.kt", vm.readyState.navigationState.selectedPath)
        assertEquals("src/main", vm.readyState.navigationState.focusPath)
        assertEquals(initialToken + 1, vm.readyState.navigationState.viewportResetToken)
    }

    @Test
    fun `changeFocusPath should update focusPath and reset token`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        val initialToken = vm.readyState.navigationState.viewportResetToken

        vm.changeFocusPath("new/path")
        assertEquals("new/path", vm.readyState.navigationState.focusPath)
        assertEquals(initialToken + 1, vm.readyState.navigationState.viewportResetToken)
    }

    @Test
    fun `changeFocusPath should expand ancestor chain`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        vm.selectDirectory("src")
        vm.toggleDirectoryExpanded("src")

        vm.changeFocusPath("src/main")

        assertTrue(vm.readyState.navigationState.explorerState.expandedPaths.contains("src"))
        assertTrue(vm.readyState.navigationState.explorerState.expandedPaths.contains("src/main"))
    }

    @Test
    fun `updateSelectedPath should only update selectedPath`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        vm.updateSelectedPath("file.kt")
        assertEquals("file.kt", vm.readyState.navigationState.selectedPath)
    }

    @Test
    fun `resetNavigation should clear paths`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        vm.selectFile("src/App.kt")

        vm.resetNavigation()
        assertEquals("", vm.readyState.navigationState.focusPath)
        assertNull(vm.readyState.navigationState.selectedPath)
    }

    @Test
    fun `resetViewport should increment token`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        val initialToken = vm.readyState.navigationState.viewportResetToken

        vm.resetViewport()
        assertEquals(initialToken + 1, vm.readyState.navigationState.viewportResetToken)
    }

    @Test
    fun `ensurePrColors should assign colors to new PRs`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        val prs = listOf(pr("pr1"), pr("pr2"))

        vm.ensurePrColors(prs)

        assertEquals(2, vm.readyState.colorState.prColorMap.size)
        assertNotNull(vm.readyState.colorState.prColorMap["pr1"])
        assertNotNull(vm.readyState.colorState.prColorMap["pr2"])
    }

    @Test
    fun `ensurePrColors should not reassign existing colors`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        val pr1 = pr("pr1")

        vm.ensurePrColors(listOf(pr1))
        val color1 = vm.readyState.colorState.prColorMap["pr1"]

        // Call again with same PR
        vm.ensurePrColors(listOf(pr1))
        assertEquals(color1, vm.readyState.colorState.prColorMap["pr1"])
    }

    @Test
    fun `shufflePrColors should reassign all colors`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(snapshotWithPrs(pr("pr1"), pr("pr2"))),
        )

        vm.ensurePrColors(listOf(pr("pr1"), pr("pr2")))
        vm.shufflePrColors()

        assertEquals(2, vm.readyState.colorState.prColorMap.size)
        // Colors exist but may be different
        assertNotNull(vm.readyState.colorState.prColorMap["pr1"])
        assertNotNull(vm.readyState.colorState.prColorMap["pr2"])
    }

    @Test
    fun `cyclePrColor should cycle through palette`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        val p = pr("pr1")

        vm.ensurePrColors(listOf(p))
        val initialColor = vm.readyState.colorState.prColorMap["pr1"]
        val initialIndex = AppColors.authorPalette.indexOf(initialColor)

        vm.cyclePrColor("pr1")

        val newColor = vm.readyState.colorState.prColorMap["pr1"]
        val expectedIndex = (initialIndex + 1) % AppColors.authorPalette.size
        assertEquals(AppColors.authorPalette[expectedIndex], newColor)
    }

    @Test
    fun `cyclePrColor should handle PR without assigned color`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        vm.cyclePrColor("pr1")

        // Should assign the first color in the palette
        assertEquals(AppColors.authorPalette[0], vm.readyState.colorState.prColorMap["pr1"])
    }

    @Test
    fun `state operations should maintain immutability`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )
        val initialState = vm.state

        vm.updateShowDrafts(false)

        // Original state reference should be different
        assertTrue(vm.state !== initialState)
        assertTrue((initialState.snapshotFetchState as SnapshotFetchState.Ready).filterState.showDrafts)
        assertFalse(vm.readyState.filterState.showDrafts)
    }

    @Test
    fun navigateBackReturnsToRootAfterResetAndFirstNavigation() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        vm.resetNavigation()
        vm.selectDirectory("src")

        assertTrue(vm.navigateBack())
        assertEquals("", vm.readyState.navigationState.focusPath)
    }

    @Test
    fun `selectFile should record parent path in navigation history`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        vm.resetNavigation()
        vm.selectFile("src/main/App.kt")

        // navigateBack should return to the parent directory of the selected file
        assertTrue(vm.navigateBack())
        assertEquals("", vm.readyState.navigationState.focusPath)
    }

    @Test
    fun `navigateBack after selectFile should restore previous focusPath`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        vm.resetNavigation()
        vm.selectDirectory("src")
        vm.selectFile("src/main/App.kt")

        // Back should go to "src" (before the file selection pushed "src/main")
        assertTrue(vm.navigateBack())
        assertEquals("src", vm.readyState.navigationState.focusPath)
    }

    @Test
    fun `changeFocusPath should record path in navigation history`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        vm.resetNavigation()
        vm.changeFocusPath("src/main")

        // navigateBack should return to root (before changeFocusPath)
        assertTrue(vm.navigateBack())
        assertEquals("", vm.readyState.navigationState.focusPath)
    }

    @Test
    fun `navigateBack after changeFocusPath should restore previous focusPath`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        vm.resetNavigation()
        vm.selectDirectory("src")
        vm.changeFocusPath("src/main")

        // Back should go to "src" (before changeFocusPath pushed "src/main")
        assertTrue(vm.navigateBack())
        assertEquals("src", vm.readyState.navigationState.focusPath)
    }

    @Test
    fun `navigateBack should expand ancestor chain of restored focusPath`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        vm.resetNavigation()
        vm.selectDirectory("src/main")
        vm.selectDirectory("docs")
        vm.toggleDirectoryExpanded("src")

        assertTrue(vm.navigateBack())
        assertEquals("src/main", vm.readyState.navigationState.focusPath)
        assertTrue(vm.readyState.navigationState.explorerState.expandedPaths.contains("src"))
        assertTrue(vm.readyState.navigationState.explorerState.expandedPaths.contains("src/main"))
    }

    @Test
    fun `navigateForward should expand ancestor chain of restored focusPath`() {
        val vm = VisualizerViewModel(
            selectedRepositoryStore = InMemorySelectedRepositoryStore(),
            initialState = readyState(),
        )

        vm.resetNavigation()
        vm.selectDirectory("docs")
        vm.selectDirectory("src/main")
        assertTrue(vm.navigateBack())
        vm.toggleDirectoryExpanded("src")

        assertTrue(vm.navigateForward())
        assertEquals("src/main", vm.readyState.navigationState.focusPath)
        assertTrue(vm.readyState.navigationState.explorerState.expandedPaths.contains("src"))
        assertTrue(vm.readyState.navigationState.explorerState.expandedPaths.contains("src/main"))
    }
}
