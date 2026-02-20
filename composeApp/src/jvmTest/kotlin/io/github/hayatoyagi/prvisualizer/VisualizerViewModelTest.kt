package io.github.hayatoyagi.prvisualizer

import androidx.compose.ui.graphics.Color
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VisualizerViewModelTest {

    @Test
    fun `ViewModel should initialize with provided owner and repo`() {
        val vm = VisualizerViewModel(initialOwner = "TestOwner", initialRepo = "TestRepo")
        assertEquals("TestOwner", vm.owner)
        assertEquals("TestRepo", vm.repo)
    }

    @Test
    fun `openRepoDialog should set dialog state correctly`() {
        val vm = VisualizerViewModel(initialOwner = "Owner", initialRepo = "Repo")
        vm.openRepoDialog()
        
        assertTrue(vm.isRepoDialogOpen)
        assertEquals("Owner/Repo", vm.repoPickerQuery)
    }

    @Test
    fun `closeRepoDialog should close the dialog`() {
        val vm = VisualizerViewModel()
        vm.openRepoDialog()
        assertTrue(vm.isRepoDialogOpen)
        
        vm.closeRepoDialog()
        assertFalse(vm.isRepoDialogOpen)
    }

    @Test
    fun `updateRepoPickerQuery should update query`() {
        val vm = VisualizerViewModel()
        vm.updateRepoPickerQuery("test/query")
        assertEquals("test/query", vm.repoPickerQuery)
    }

    @Test
    fun `selectRepo should reset all states and update repo`() {
        val vm = VisualizerViewModel(initialOwner = "Old", initialRepo = "Repo")
        
        // Set some state
        vm.openRepoDialog()
        vm.updateQuery("search")
        vm.selectAllPrs(setOf("pr1", "pr2"))
        
        // Select new repo
        vm.selectRepo("New/Repository")
        
        assertEquals("New", vm.owner)
        assertEquals("Repository", vm.repo)
        assertFalse(vm.isRepoDialogOpen)
        assertEquals("", vm.query)
        assertTrue(vm.selectedPrIds.isEmpty())
        assertTrue(vm.prColorMap.isEmpty())
        assertEquals("", vm.focusPath)
        assertNull(vm.selectedPath)
    }

    @Test
    fun `updateShowDrafts should update filter`() {
        val vm = VisualizerViewModel()
        assertTrue(vm.showDrafts)
        
        vm.updateShowDrafts(false)
        assertFalse(vm.showDrafts)
    }

    @Test
    fun `updateOnlyMine should update filter`() {
        val vm = VisualizerViewModel()
        assertFalse(vm.onlyMine)
        
        vm.updateOnlyMine(true)
        assertTrue(vm.onlyMine)
    }

    @Test
    fun `updateQuery should update search query`() {
        val vm = VisualizerViewModel()
        vm.updateQuery("test query")
        assertEquals("test query", vm.query)
    }

    @Test
    fun `clearQuery should clear search query`() {
        val vm = VisualizerViewModel()
        vm.updateQuery("test")
        assertEquals("test", vm.query)
        
        vm.clearQuery()
        assertEquals("", vm.query)
    }

    @Test
    fun `togglePr should add and remove PR IDs`() {
        val vm = VisualizerViewModel()
        assertTrue(vm.selectedPrIds.isEmpty())
        
        vm.togglePr("pr1", checked = true)
        assertEquals(setOf("pr1"), vm.selectedPrIds)
        
        vm.togglePr("pr2", checked = true)
        assertEquals(setOf("pr1", "pr2"), vm.selectedPrIds)
        
        vm.togglePr("pr1", checked = false)
        assertEquals(setOf("pr2"), vm.selectedPrIds)
    }

    @Test
    fun `selectAllPrs should set all PR IDs`() {
        val vm = VisualizerViewModel()
        val prs = setOf("pr1", "pr2", "pr3")
        
        vm.selectAllPrs(prs)
        assertEquals(prs, vm.selectedPrIds)
    }

    @Test
    fun `addRelatedPrs should add to existing selection`() {
        val vm = VisualizerViewModel()
        vm.selectAllPrs(setOf("pr1", "pr2"))
        
        vm.addRelatedPrs(setOf("pr3", "pr4"))
        assertEquals(setOf("pr1", "pr2", "pr3", "pr4"), vm.selectedPrIds)
    }

    @Test
    fun `addRelatedPrs should not change state if empty set provided`() {
        val vm = VisualizerViewModel()
        vm.selectAllPrs(setOf("pr1"))
        
        vm.addRelatedPrs(emptySet())
        assertEquals(setOf("pr1"), vm.selectedPrIds)
    }

    @Test
    fun `selectDirectory should update focusPath and reset token`() {
        val vm = VisualizerViewModel()
        val initialToken = vm.viewportResetToken
        
        vm.selectDirectory("src/main")
        assertEquals("src/main", vm.focusPath)
        assertEquals(initialToken + 1, vm.viewportResetToken)
    }

    @Test
    fun `selectFile should update both paths and reset token`() {
        val vm = VisualizerViewModel()
        val initialToken = vm.viewportResetToken
        
        vm.selectFile("src/main/App.kt")
        assertEquals("src/main/App.kt", vm.selectedPath)
        assertEquals("src/main", vm.focusPath)
        assertEquals(initialToken + 1, vm.viewportResetToken)
    }

    @Test
    fun `changeFocusPath should update focusPath and reset token`() {
        val vm = VisualizerViewModel()
        val initialToken = vm.viewportResetToken
        
        vm.changeFocusPath("new/path")
        assertEquals("new/path", vm.focusPath)
        assertEquals(initialToken + 1, vm.viewportResetToken)
    }

    @Test
    fun `updateSelectedPath should only update selectedPath`() {
        val vm = VisualizerViewModel()
        vm.updateSelectedPath("file.kt")
        assertEquals("file.kt", vm.selectedPath)
    }

    @Test
    fun `resetNavigation should clear paths`() {
        val vm = VisualizerViewModel()
        vm.selectFile("src/App.kt")
        
        vm.resetNavigation()
        assertEquals("", vm.focusPath)
        assertNull(vm.selectedPath)
    }

    @Test
    fun `resetViewport should increment token`() {
        val vm = VisualizerViewModel()
        val initialToken = vm.viewportResetToken
        
        vm.resetViewport()
        assertEquals(initialToken + 1, vm.viewportResetToken)
    }

    @Test
    fun `ensurePrColors should assign colors to new PRs`() {
        val vm = VisualizerViewModel()
        val prs = listOf(
            PullRequest("pr1", 1, "Title 1", "author1", false, "url1", emptyList()),
            PullRequest("pr2", 2, "Title 2", "author2", false, "url2", emptyList())
        )
        
        vm.ensurePrColors(prs)
        
        assertEquals(2, vm.prColorMap.size)
        assertNotNull(vm.prColorMap["pr1"])
        assertNotNull(vm.prColorMap["pr2"])
    }

    @Test
    fun `ensurePrColors should not reassign existing colors`() {
        val vm = VisualizerViewModel()
        val pr1 = PullRequest("pr1", 1, "Title 1", "author1", false, "url1", emptyList())
        
        vm.ensurePrColors(listOf(pr1))
        val color1 = vm.prColorMap["pr1"]
        
        // Call again with same PR
        vm.ensurePrColors(listOf(pr1))
        assertEquals(color1, vm.prColorMap["pr1"])
    }

    @Test
    fun `shufflePrColors should reassign all colors`() {
        val vm = VisualizerViewModel()
        val prs = listOf(
            PullRequest("pr1", 1, "Title 1", "author1", false, "url1", emptyList()),
            PullRequest("pr2", 2, "Title 2", "author2", false, "url2", emptyList())
        )
        
        vm.ensurePrColors(prs)
        val originalColors = vm.prColorMap.toMap()
        
        vm.shufflePrColors(prs)
        
        assertEquals(2, vm.prColorMap.size)
        // Colors exist but may be different
        assertNotNull(vm.prColorMap["pr1"])
        assertNotNull(vm.prColorMap["pr2"])
    }

    @Test
    fun `cyclePrColor should cycle through palette`() {
        val vm = VisualizerViewModel()
        val pr = PullRequest("pr1", 1, "Title", "author", false, "url", emptyList())
        
        vm.ensurePrColors(listOf(pr))
        val initialColor = vm.prColorMap["pr1"]
        val initialIndex = AppColors.authorPalette.indexOf(initialColor)
        
        vm.cyclePrColor("pr1")
        
        val newColor = vm.prColorMap["pr1"]
        val expectedIndex = (initialIndex + 1) % AppColors.authorPalette.size
        assertEquals(AppColors.authorPalette[expectedIndex], newColor)
    }

    @Test
    fun `cyclePrColor should handle PR without assigned color`() {
        val vm = VisualizerViewModel()
        
        vm.cyclePrColor("pr1")
        
        // Should assign the first color in the palette
        assertEquals(AppColors.authorPalette[0], vm.prColorMap["pr1"])
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
        assertEquals("", viewModel.focusPath)
    }
}
