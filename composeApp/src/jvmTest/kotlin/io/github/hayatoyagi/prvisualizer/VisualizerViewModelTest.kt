package io.github.hayatoyagi.prvisualizer

import io.github.hayatoyagi.prvisualizer.repository.InMemorySelectedRepositoryStore
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VisualizerViewModelTest {
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
    fun `ViewModel should initialize with provided owner and repo`() {
        val vm = VisualizerViewModel(initialOwner = "TestOwner", initialRepo = "TestRepo")
        val selected = assertIs<RepoState.Selected>(vm.repoState.value)
        assertEquals("TestOwner", selected.owner)
        assertEquals("TestRepo", selected.repo)
    }

    @Test
    fun `openRepoDialog should set dialog state correctly`() {
        val vm = VisualizerViewModel(initialOwner = "Owner", initialRepo = "Repo")
        vm.openRepoDialog()

        assertIs<DialogState.RepoPicker>(vm.state.dialogState)
    }

    @Test
    fun `closeRepoDialog should close the dialog`() {
        val vm = VisualizerViewModel()
        vm.openRepoDialog()
        assertIs<DialogState.RepoPicker>(vm.state.dialogState)

        vm.closeRepoDialog()
        assertIs<DialogState.None>(vm.state.dialogState)
    }

    @Test
    fun `openFileDetailsDialog should set dialog state correctly`() {
        val vm = VisualizerViewModel()
        vm.openFileDetailsDialog("src/main/App.kt")

        val dialog = assertIs<DialogState.FileDetails>(vm.state.dialogState)
        assertEquals("src/main/App.kt", dialog.filePath)
    }

    @Test
    fun `closeFileDetailsDialog should close the dialog`() {
        val vm = VisualizerViewModel()
        vm.openFileDetailsDialog("src/main/App.kt")
        assertIs<DialogState.FileDetails>(vm.state.dialogState)

        vm.closeFileDetailsDialog()
        assertIs<DialogState.None>(vm.state.dialogState)
    }

    @Test
    fun `selectRepo should preserve toggles and clear query selection state`() {
        val vm = VisualizerViewModel(initialOwner = "Old", initialRepo = "Repo")

        // Set some state
        vm.openRepoDialog()
        vm.updateShowDrafts(false)
        vm.updateOnlyMine(true)
        vm.updateQuery("search")
        vm.selectAllPrs(setOf("pr1", "pr2"))

        // Select new repo
        vm.selectRepo("New/Repository")

        val selected = assertIs<RepoState.Selected>(vm.repoState.value)
        assertEquals("New", selected.owner)
        assertEquals("Repository", selected.repo)
        assertIs<DialogState.None>(vm.state.dialogState)
        assertFalse(vm.state.filterState.showDrafts)
        assertTrue(vm.state.filterState.onlyMine)
        assertEquals("", vm.state.filterState.query)
        assertTrue(
            vm.state.filterState.selectedPrIds
                .isEmpty(),
        )
        assertTrue(
            vm.state.colorState.prColorMap
                .isEmpty(),
        )
        assertEquals("", vm.state.navigationState.focusPath)
        assertNull(vm.state.navigationState.selectedPath)
    }

    @Test
    fun `updateShowDrafts should update filter`() {
        val vm = VisualizerViewModel()
        assertTrue(vm.state.filterState.showDrafts)

        vm.updateShowDrafts(false)
        assertFalse(vm.state.filterState.showDrafts)
    }

    @Test
    fun `updateOnlyMine should update filter`() {
        val vm = VisualizerViewModel()
        assertFalse(vm.state.filterState.onlyMine)

        vm.updateOnlyMine(true)
        assertTrue(vm.state.filterState.onlyMine)
    }

    @Test
    fun `updateQuery should update search query`() {
        val vm = VisualizerViewModel()
        vm.updateQuery("test query")
        assertEquals("test query", vm.state.filterState.query)
    }

    @Test
    fun `clearQuery should clear search query`() {
        val vm = VisualizerViewModel()
        vm.updateQuery("test")
        assertEquals("test", vm.state.filterState.query)

        vm.clearQuery()
        assertEquals("", vm.state.filterState.query)
    }

    @Test
    fun `togglePr should add and remove PR IDs`() {
        val vm = VisualizerViewModel()
        assertTrue(
            vm.state.filterState.selectedPrIds
                .isEmpty(),
        )

        vm.togglePr("pr1", checked = true)
        assertEquals(setOf("pr1"), vm.state.filterState.selectedPrIds)

        vm.togglePr("pr2", checked = true)
        assertEquals(setOf("pr1", "pr2"), vm.state.filterState.selectedPrIds)

        vm.togglePr("pr1", checked = false)
        assertEquals(setOf("pr2"), vm.state.filterState.selectedPrIds)
    }

    @Test
    fun `selectAllPrs should set all PR IDs`() {
        val vm = VisualizerViewModel()
        val prs = setOf("pr1", "pr2", "pr3")

        vm.selectAllPrs(prs)
        assertEquals(prs, vm.state.filterState.selectedPrIds)
    }

    @Test
    fun `addRelatedPrs should add to existing selection`() {
        val vm = VisualizerViewModel()
        vm.selectAllPrs(setOf("pr1", "pr2"))

        vm.addRelatedPrs(setOf("pr3", "pr4"))
        assertEquals(setOf("pr1", "pr2", "pr3", "pr4"), vm.state.filterState.selectedPrIds)
    }

    @Test
    fun `addRelatedPrs should not change state if empty set provided`() {
        val vm = VisualizerViewModel()
        vm.selectAllPrs(setOf("pr1"))

        vm.addRelatedPrs(emptySet())
        assertEquals(setOf("pr1"), vm.state.filterState.selectedPrIds)
    }

    @Test
    fun `selectDirectory should update focusPath and reset token`() {
        val vm = VisualizerViewModel()
        val initialToken = vm.state.navigationState.viewportResetToken

        vm.selectDirectory("src/main")
        assertEquals("src/main", vm.state.navigationState.focusPath)
        assertEquals(initialToken + 1, vm.state.navigationState.viewportResetToken)
    }

    @Test
    fun `selectDirectory should expand ancestor chain`() {
        val vm = VisualizerViewModel()
        vm.selectDirectory("src")
        vm.toggleDirectoryExpanded("src")

        vm.selectDirectory("src/ui")

        assertTrue(vm.state.navigationState.explorerState.expandedPaths.contains("src"))
        assertTrue(vm.state.navigationState.explorerState.expandedPaths.contains("src/ui"))
    }

    @Test
    fun `selectFile should update both paths and reset token`() {
        val vm = VisualizerViewModel()
        val initialToken = vm.state.navigationState.viewportResetToken

        vm.selectFile("src/main/App.kt")
        assertEquals("src/main/App.kt", vm.state.navigationState.selectedPath)
        assertEquals("src/main", vm.state.navigationState.focusPath)
        assertEquals(initialToken + 1, vm.state.navigationState.viewportResetToken)
    }

    @Test
    fun `changeFocusPath should update focusPath and reset token`() {
        val vm = VisualizerViewModel()
        val initialToken = vm.state.navigationState.viewportResetToken

        vm.changeFocusPath("new/path")
        assertEquals("new/path", vm.state.navigationState.focusPath)
        assertEquals(initialToken + 1, vm.state.navigationState.viewportResetToken)
    }

    @Test
    fun `changeFocusPath should expand ancestor chain`() {
        val vm = VisualizerViewModel()
        vm.selectDirectory("src")
        vm.toggleDirectoryExpanded("src")

        vm.changeFocusPath("src/main")

        assertTrue(vm.state.navigationState.explorerState.expandedPaths.contains("src"))
        assertTrue(vm.state.navigationState.explorerState.expandedPaths.contains("src/main"))
    }

    @Test
    fun `updateSelectedPath should only update selectedPath`() {
        val vm = VisualizerViewModel()
        vm.updateSelectedPath("file.kt")
        assertEquals("file.kt", vm.state.navigationState.selectedPath)
    }

    @Test
    fun `resetNavigation should clear paths`() {
        val vm = VisualizerViewModel()
        vm.selectFile("src/App.kt")

        vm.resetNavigation()
        assertEquals("", vm.state.navigationState.focusPath)
        assertNull(vm.state.navigationState.selectedPath)
    }

    @Test
    fun `resetViewport should increment token`() {
        val vm = VisualizerViewModel()
        val initialToken = vm.state.navigationState.viewportResetToken

        vm.resetViewport()
        assertEquals(initialToken + 1, vm.state.navigationState.viewportResetToken)
    }

    @Test
    fun `ensurePrColors should assign colors to new PRs`() {
        val vm = VisualizerViewModel()
        val prs = listOf(
            PullRequest("pr1", 1, "Title 1", "author1", false, "url1", emptyList()),
            PullRequest("pr2", 2, "Title 2", "author2", false, "url2", emptyList()),
        )

        vm.ensurePrColors(prs)

        assertEquals(2, vm.state.colorState.prColorMap.size)
        assertNotNull(vm.state.colorState.prColorMap["pr1"])
        assertNotNull(vm.state.colorState.prColorMap["pr2"])
    }

    @Test
    fun `ensurePrColors should not reassign existing colors`() {
        val vm = VisualizerViewModel()
        val pr1 = PullRequest("pr1", 1, "Title 1", "author1", false, "url1", emptyList())

        vm.ensurePrColors(listOf(pr1))
        val color1 = vm.state.colorState.prColorMap["pr1"]

        // Call again with same PR
        vm.ensurePrColors(listOf(pr1))
        assertEquals(color1, vm.state.colorState.prColorMap["pr1"])
    }

    @Test
    fun `shufflePrColors should reassign all colors`() {
        val vm = VisualizerViewModel()
        val prs = listOf(
            PullRequest("pr1", 1, "Title 1", "author1", false, "url1", emptyList()),
            PullRequest("pr2", 2, "Title 2", "author2", false, "url2", emptyList()),
        )

        vm.ensurePrColors(prs)
        val originalColors = vm.state.colorState.prColorMap
            .toMap()

        vm.shufflePrColors(prs)

        assertEquals(2, vm.state.colorState.prColorMap.size)
        // Colors exist but may be different
        assertNotNull(vm.state.colorState.prColorMap["pr1"])
        assertNotNull(vm.state.colorState.prColorMap["pr2"])
    }

    @Test
    fun `cyclePrColor should cycle through palette`() {
        val vm = VisualizerViewModel()
        val pr = PullRequest("pr1", 1, "Title", "author", false, "url", emptyList())

        vm.ensurePrColors(listOf(pr))
        val initialColor = vm.state.colorState.prColorMap["pr1"]
        val initialIndex = AppColors.authorPalette.indexOf(initialColor)

        vm.cyclePrColor("pr1")

        val newColor = vm.state.colorState.prColorMap["pr1"]
        val expectedIndex = (initialIndex + 1) % AppColors.authorPalette.size
        assertEquals(AppColors.authorPalette[expectedIndex], newColor)
    }

    @Test
    fun `cyclePrColor should handle PR without assigned color`() {
        val vm = VisualizerViewModel()

        vm.cyclePrColor("pr1")

        // Should assign the first color in the palette
        assertEquals(AppColors.authorPalette[0], vm.state.colorState.prColorMap["pr1"])
    }

    @Test
    fun `state operations should maintain immutability`() {
        val vm = VisualizerViewModel()
        val initialState = vm.state

        vm.updateQuery("test")

        // Original state reference should be different
        assertTrue(vm.state !== initialState)
        assertEquals("", initialState.filterState.query)
        assertEquals("test", vm.state.filterState.query)
    }

    @Test
    fun navigateBackReturnsToRootAfterResetAndFirstNavigation() {
        val viewModel = VisualizerViewModel()

        viewModel.resetNavigation()
        viewModel.selectDirectory("src")

        assertTrue(viewModel.navigateBack())
        assertEquals("", viewModel.state.navigationState.focusPath)
    }

    @Test
    fun `selectFile should record parent path in navigation history`() {
        val vm = VisualizerViewModel()

        vm.resetNavigation()
        vm.selectFile("src/main/App.kt")

        // navigateBack should return to the parent directory of the selected file
        assertTrue(vm.navigateBack())
        assertEquals("", vm.state.navigationState.focusPath)
    }

    @Test
    fun `navigateBack after selectFile should restore previous focusPath`() {
        val vm = VisualizerViewModel()

        vm.resetNavigation()
        vm.selectDirectory("src")
        vm.selectFile("src/main/App.kt")

        // Back should go to "src" (before the file selection pushed "src/main")
        assertTrue(vm.navigateBack())
        assertEquals("src", vm.state.navigationState.focusPath)
    }

    @Test
    fun `changeFocusPath should record path in navigation history`() {
        val vm = VisualizerViewModel()

        vm.resetNavigation()
        vm.changeFocusPath("src/main")

        // navigateBack should return to root (before changeFocusPath)
        assertTrue(vm.navigateBack())
        assertEquals("", vm.state.navigationState.focusPath)
    }

    @Test
    fun `navigateBack after changeFocusPath should restore previous focusPath`() {
        val vm = VisualizerViewModel()

        vm.resetNavigation()
        vm.selectDirectory("src")
        vm.changeFocusPath("src/main")

        // Back should go to "src" (before changeFocusPath pushed "src/main")
        assertTrue(vm.navigateBack())
        assertEquals("src", vm.state.navigationState.focusPath)
    }

    @Test
    fun `navigateBack should expand ancestor chain of restored focusPath`() {
        val vm = VisualizerViewModel()

        vm.resetNavigation()
        vm.selectDirectory("src/main")
        vm.selectDirectory("docs")
        vm.toggleDirectoryExpanded("src")

        assertTrue(vm.navigateBack())
        assertEquals("src/main", vm.state.navigationState.focusPath)
        assertTrue(vm.state.navigationState.explorerState.expandedPaths.contains("src"))
        assertTrue(vm.state.navigationState.explorerState.expandedPaths.contains("src/main"))
    }

    @Test
    fun `navigateForward should expand ancestor chain of restored focusPath`() {
        val vm = VisualizerViewModel()

        vm.resetNavigation()
        vm.selectDirectory("docs")
        vm.selectDirectory("src/main")
        assertTrue(vm.navigateBack())
        vm.toggleDirectoryExpanded("src")

        assertTrue(vm.navigateForward())
        assertEquals("src/main", vm.state.navigationState.focusPath)
        assertTrue(vm.state.navigationState.explorerState.expandedPaths.contains("src"))
        assertTrue(vm.state.navigationState.explorerState.expandedPaths.contains("src/main"))
    }
}
