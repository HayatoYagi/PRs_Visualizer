package io.github.hayatoyagi.prvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.github.hayatoyagi.prvisualizer.github.EnvConfig
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf

class VisualizerViewModel(
    initialOwner: String = EnvConfig.get("GITHUB_OWNER") ?: "HayatoYagi",
    initialRepo: String = EnvConfig.get("GITHUB_REPO") ?: "GitHub_PRs_Visualizer",
) : ViewModel() {

    // Repo identity
    var owner by mutableStateOf(initialOwner)
        private set
    var repo by mutableStateOf(initialRepo)
        private set

    // Dialog
    var isRepoDialogOpen by mutableStateOf(false)
        private set
    var repoPickerQuery by mutableStateOf("")
        private set

    // PR filters
    var showDrafts by mutableStateOf(true)
        private set
    var onlyMine by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    var selectedPrIds by mutableStateOf<Set<String>>(emptySet())
        private set

    // Navigation
    var focusPath by mutableStateOf("")
        private set
    var selectedPath by mutableStateOf<String?>(null)
        private set
    var viewportResetToken by mutableIntStateOf(0)
        private set
    
    // Navigation history for back/forward buttons
    private val navigationHistory = NavigationHistory()

    // Dialog intents
    fun openRepoDialog() {
        repoPickerQuery = "$owner/$repo".trim().trim('/')
        isRepoDialogOpen = true
    }

    fun closeRepoDialog() {
        isRepoDialogOpen = false
    }

    fun updateRepoPickerQuery(q: String) {
        repoPickerQuery = q
    }

    fun selectRepo(fullName: String) {
        owner = fullName.substringBefore('/', owner)
        repo = fullName.substringAfter('/', fullName)
        isRepoDialogOpen = false
        selectedPrIds = emptySet()
        resetNavigation()
    }

    // PR filter intents
    fun updateShowDrafts(value: Boolean) { showDrafts = value }
    fun updateOnlyMine(value: Boolean) { onlyMine = value }
    fun updateQuery(value: String) { query = value }
    fun clearQuery() { query = "" }

    fun togglePr(prId: String, checked: Boolean) {
        selectedPrIds = if (checked) selectedPrIds + prId else selectedPrIds - prId
    }

    fun selectAllPrs(available: Set<String>) {
        selectedPrIds = available
    }

    fun addRelatedPrs(related: Set<String>) {
        if (related.isNotEmpty()) selectedPrIds = selectedPrIds + related
    }

    // Navigation intents
    fun selectDirectory(path: String) {
        navigationHistory.recordFocusPath(path)
        focusPath = path
        viewportResetToken += 1
    }

    fun selectFile(path: String) {
        selectedPath = path
        val parentPath = parentPathOf(path)
        navigationHistory.recordFocusPath(parentPath)
        focusPath = parentPath
        viewportResetToken += 1
    }

    fun changeFocusPath(path: String) {
        navigationHistory.recordFocusPath(path)
        focusPath = path
        viewportResetToken += 1
    }

    fun updateSelectedPath(path: String?) {
        selectedPath = path
    }

    fun resetNavigation() {
        focusPath = ""
        selectedPath = null
        navigationHistory.clear()
    }

    fun resetViewport() {
        viewportResetToken += 1
    }

    /**
     * Navigates back in history. Returns true if navigation occurred.
     */
    fun navigateBack(): Boolean {
        val previousPath = navigationHistory.navigateBack()
        return if (previousPath != null) {
            focusPath = previousPath
            viewportResetToken += 1
            true
        } else {
            false
        }
    }

    /**
     * Navigates forward in history. Returns true if navigation occurred.
     */
    fun navigateForward(): Boolean {
        val nextPath = navigationHistory.navigateForward()
        return if (nextPath != null) {
            focusPath = nextPath
            viewportResetToken += 1
            true
        } else {
            false
        }
    }

    /**
     * Returns true if back navigation is possible.
     */
    fun canNavigateBack(): Boolean = navigationHistory.canNavigateBack()

    /**
     * Returns true if forward navigation is possible.
     */
    fun canNavigateForward(): Boolean = navigationHistory.canNavigateForward()
}
