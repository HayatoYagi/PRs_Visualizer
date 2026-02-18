package io.github.hayatoyagi.prvisualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import io.github.hayatoyagi.prvisualizer.ui.explorer.ExplorerPane
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListPane
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.buildExplorerRows
import io.github.hayatoyagi.prvisualizer.ui.shared.findDirectory
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf
import io.github.hayatoyagi.prvisualizer.ui.shared.totalLines
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapPane

@Composable
@Preview
@OptIn(ExperimentalComposeUiApi::class)
fun App() {
    val root = SampleData.rootNode
    val allPrs = SampleData.pullRequests
    // TODO: Replace with authenticated GitHub login (or user-configurable value) instead of hard-coded user.
    val currentUser = "hayatoy"

    var showDrafts by remember { mutableStateOf(true) }
    var onlyMine by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedPrIds by remember { mutableStateOf(allPrs.map { it.id }.toSet()) }
    var focusPath by remember { mutableStateOf("") }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var viewportResetToken by remember { mutableIntStateOf(0) }

    val filteredPrs = remember(showDrafts, onlyMine, query, allPrs) {
        allPrs.filter { pr ->
            (showDrafts || !pr.isDraft) &&
                (!onlyMine || pr.author == currentUser) &&
                (query.isBlank() || pr.title.contains(query, ignoreCase = true) || "#${pr.number}".contains(query))
        }
    }

    LaunchedEffect(filteredPrs) {
        val available = filteredPrs.map { it.id }.toSet()
        if (selectedPrIds.none { available.contains(it) }) {
            selectedPrIds = available
        }
    }

    val visiblePrs = remember(filteredPrs, selectedPrIds) {
        filteredPrs.filter { selectedPrIds.contains(it.id) }
    }

    val focusRoot = remember(root, focusPath) {
        findDirectory(root, focusPath) ?: root
    }
    val explorerRows = remember(root) {
        buildExplorerRows(root)
    }
    val visibleFiles = remember(focusRoot) {
        focusRoot.children.filterIsInstance<FileNode.File>()
    }
    val visibleDirectories = remember(focusRoot) {
        focusRoot.children.filterIsInstance<FileNode.Directory>()
    }

    val fileOverlayByPath = remember(visiblePrs, visibleFiles) {
        val fileLines = visibleFiles.associateBy({ it.path }, { it.totalLines.coerceAtLeast(1) })
        visiblePrs
            .flatMap { pr -> pr.files.map { change -> pr to change } }
            .filter { fileLines.containsKey(it.second.path) }
            .groupBy({ it.second.path }, { it })
            .mapValues { (_, items) ->
                val totalChanged = items.sumOf { it.second.changedLines }
                val dominant = items
                    .groupBy { it.second.changeType }
                    .maxByOrNull { it.value.sumOf { pair -> pair.second.changedLines } }
                    ?.key ?: ChangeType.Modification
                val prs = items.map { it.first }.distinctBy { it.id }
                val lines = fileLines[items.first().second.path] ?: 1
                val density = (totalChanged.toFloat() / lines.toFloat()).coerceIn(0f, 1f)
                FileOverlay(prs = prs, dominantType = dominant, density = density)
            }
    }

    val directoryOverlayByPath = remember(visiblePrs, visibleDirectories) {
        visibleDirectories.associate { dir ->
            val relatedChanges = visiblePrs
                .flatMap { pr -> pr.files.map { change -> pr to change } }
                .filter { (_, change) -> change.path.startsWith("${dir.path}/") }

            val relatedPrs = relatedChanges.map { it.first }.distinctBy { it.id }
            val dominantType = relatedChanges
                .groupBy { it.second.changeType }
                .maxByOrNull { (_, items) -> items.sumOf { it.second.changedLines } }
                ?.key
            val totalChanged = relatedChanges.sumOf { it.second.changedLines }
            val density = (totalChanged.toFloat() / totalLines(dir).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

            dir.path to DirectoryOverlay(
                prs = relatedPrs,
                dominantType = dominantType,
                density = density,
            )
        }
    }

    MaterialTheme {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101820))
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !event.isMetaPressed) {
                        return@onPreviewKeyEvent false
                    }
                    when (event.key) {
                        Key.R -> {
                            viewportResetToken += 1
                            true
                        }
                        Key.F -> {
                            query = ""
                            true
                        }
                        else -> false
                    }
                },
        ) {
            ExplorerPane(
                rows = explorerRows,
                focusPath = focusPath,
                selectedPath = selectedPath,
                onSelectDirectory = {
                    focusPath = it
                    viewportResetToken += 1
                },
                onSelectFile = {
                    selectedPath = it
                    focusPath = parentPathOf(it)
                    viewportResetToken += 1
                },
            )

            TreemapPane(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                focusPath = focusPath,
                visiblePrs = visiblePrs,
                focusRoot = focusRoot,
                selectedPath = selectedPath,
                fileOverlayByPath = fileOverlayByPath,
                directoryOverlayByPath = directoryOverlayByPath,
                viewportResetToken = viewportResetToken,
                onFocusPathChange = {
                    focusPath = it
                    viewportResetToken += 1
                },
                onSelectedPathChange = { selectedPath = it },
                onRelatedPrsDetected = { related ->
                    if (related.isNotEmpty()) selectedPrIds = selectedPrIds + related
                },
            )

            PrListPane(
                filteredPrs = filteredPrs,
                selectedPrIds = selectedPrIds,
                selectedPath = selectedPath,
                query = query,
                showDrafts = showDrafts,
                onlyMine = onlyMine,
                onQueryChange = { query = it },
                onShowDraftsChange = { showDrafts = it },
                onOnlyMineChange = { onlyMine = it },
                onTogglePr = { prId, checked ->
                    selectedPrIds = if (checked) selectedPrIds + prId else selectedPrIds - prId
                },
                onOpenPr = { io.github.hayatoyagi.prvisualizer.ui.shared.openUrl(it) },
            )
        }
    }
}
