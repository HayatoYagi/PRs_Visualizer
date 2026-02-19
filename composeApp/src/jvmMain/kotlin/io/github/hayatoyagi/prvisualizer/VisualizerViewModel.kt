package io.github.hayatoyagi.prvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import io.github.hayatoyagi.prvisualizer.github.EnvConfig
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import kotlin.random.Random

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

    // PR color management
    var prColorMap by mutableStateOf<Map<String, Color>>(emptyMap())
        private set

    // Navigation
    var focusPath by mutableStateOf("")
        private set
    var selectedPath by mutableStateOf<String?>(null)
        private set
    var viewportResetToken by mutableIntStateOf(0)
        private set

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
        focusPath = path
        viewportResetToken += 1
    }

    fun selectFile(path: String) {
        selectedPath = path
        focusPath = parentPathOf(path)
        viewportResetToken += 1
    }

    fun changeFocusPath(path: String) {
        focusPath = path
        viewportResetToken += 1
    }

    fun updateSelectedPath(path: String?) {
        selectedPath = path
    }

    fun resetNavigation() {
        focusPath = ""
        selectedPath = null
    }

    fun resetViewport() {
        viewportResetToken += 1
    }

    // PR color management intents
    fun ensurePrColors(prs: List<PullRequest>) {
        val prsNeedingColors = prs.filter { !prColorMap.containsKey(it.id) }
        if (prsNeedingColors.isNotEmpty()) {
            val newMap = prColorMap.toMutableMap()
            prsNeedingColors.forEach { pr ->
                newMap[pr.id] = randomColor()
            }
            prColorMap = newMap
        }
    }

    fun shufflePrColors(prs: List<PullRequest>) {
        val newMap = mutableMapOf<String, Color>()
        prs.forEach { pr ->
            newMap[pr.id] = randomColor()
        }
        prColorMap = newMap
    }

    fun setPrColor(prId: String, color: Color) {
        prColorMap = prColorMap + (prId to color)
    }

    fun cyclePrColor(prId: String) {
        val currentColor = prColorMap[prId]
        val currentIndex = if (currentColor != null) {
            AppColors.authorPalette.indexOf(currentColor)
        } else {
            -1
        }
        val nextIndex = (currentIndex + 1) % AppColors.authorPalette.size
        prColorMap = prColorMap + (prId to AppColors.authorPalette[nextIndex])
    }

    private fun randomColor(): Color {
        // Try to avoid selecting the same color as recently used
        val recentColors = prColorMap.values.takeLast(5).toSet()
        val availableColors = AppColors.authorPalette.filter { it !in recentColors }
        
        return if (availableColors.isNotEmpty()) {
            availableColors[Random.nextInt(availableColors.size)]
        } else {
            // Fall back to any color if all are recently used
            AppColors.authorPalette[Random.nextInt(AppColors.authorPalette.size)]
        }
    }
}
