package io.github.hayatoyagi.prvisualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.repository.store.PersistedSelectedRepositoryStore
import io.github.hayatoyagi.prvisualizer.ui.explorer.ExplorerPane
import io.github.hayatoyagi.prvisualizer.ui.file.FileDetailsDialog
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrDetailsDialog
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListPane
import io.github.hayatoyagi.prvisualizer.ui.prlist.filterPrs
import io.github.hayatoyagi.prvisualizer.ui.repo.RepoPickerDialog
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.computeDirectoryOverlayByPath
import io.github.hayatoyagi.prvisualizer.ui.shared.computeFileOverlayByPath
import io.github.hayatoyagi.prvisualizer.ui.shared.findDirectory
import io.github.hayatoyagi.prvisualizer.ui.shared.findFileNode
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.toolbar.ToolbarRow
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapPane

private const val DEFAULT_BRANCH = "main"

data class VisualizerUiState(
    val allPrs: List<PullRequest>,
    val filteredPrs: List<PullRequest>,
    val effectiveSelectedIds: Set<String>,
    val visiblePrs: List<PullRequest>,
    val focusRoot: FileNode.Directory,
    val fileOverlayByPath: Map<String, FileOverlay>,
    val directoryOverlayByPath: Map<String, DirectoryOverlay>,
)

@Composable
private fun rememberVisualizerUiState(vm: VisualizerViewModel): VisualizerUiState {
    val snapshot = snapshotOrNull(vm.state.snapshotFetchState)
    val emptyRoot = remember { FileNode.Directory(path = "", name = "repo", children = emptyList(), weight = 1.0) }
    val root = snapshot?.rootNode ?: emptyRoot
    val allPrs = snapshot?.pullRequests ?: emptyList()
    val currentUser = vm.state.currentUser

    val filteredPrs = remember(
        vm.state.filterState.showDrafts,
        vm.state.filterState.onlyMine,
        allPrs,
        currentUser,
    ) {
        filterPrs(allPrs, vm.state.filterState.showDrafts, vm.state.filterState.onlyMine, currentUser)
    }
    // Treat emptySet as "uninitialized / all selected" to avoid a flash on first load.
    // After the user explicitly toggles a PR, selectedPrIds becomes non-empty.
    val effectiveSelectedIds = remember(vm.state.filterState.selectedPrIds, filteredPrs) {
        computeEffectiveSelectedIds(vm.state.filterState.selectedPrIds, filteredPrs)
    }
    val visiblePrs = remember(filteredPrs, effectiveSelectedIds) {
        filteredPrs.filter { effectiveSelectedIds.contains(it.id) }
    }
    val focusRoot = remember(root, vm.state.navigationState.focusPath) {
        findDirectory(root, vm.state.navigationState.focusPath) ?: root
    }
    val allFiles = remember(root) { collectAllFiles(root) }
    val allDirectories = remember(root) { collectAllDirectories(root) }
    val fileOverlayByPath = remember(visiblePrs, allFiles) {
        computeFileOverlayByPath(visiblePrs, allFiles)
    }
    val directoryOverlayByPath = remember(visiblePrs, allDirectories) {
        computeDirectoryOverlayByPath(visiblePrs, allDirectories)
    }
    return VisualizerUiState(
        allPrs = allPrs,
        filteredPrs = filteredPrs,
        effectiveSelectedIds = effectiveSelectedIds,
        visiblePrs = visiblePrs,
        focusRoot = focusRoot,
        fileOverlayByPath = fileOverlayByPath,
        directoryOverlayByPath = directoryOverlayByPath,
    )
}

@Composable
@Preview
@OptIn(ExperimentalComposeUiApi::class)
fun App() {
    val vm = viewModel {
        VisualizerViewModel(
            selectedRepositoryStore = PersistedSelectedRepositoryStore(),
        )
    }
    val authState = vm.state.authState
    val snapshotFetchState = vm.state.snapshotFetchState
    val selectedRepo = vm.repoState.collectAsState().value as? RepoState.Selected
    val isConnecting = snapshotFetchState is SnapshotFetchState.Fetching

    val uiState = rememberVisualizerUiState(vm)

    AppEffects(vm = vm, authState = authState, allPrs = uiState.allPrs, filteredPrs = uiState.filteredPrs)

    MaterialTheme {
        Column(
            modifier = appRootModifier(vm),
        ) {
            ToolbarRow(
                owner = selectedRepo?.owner.orEmpty(),
                repo = selectedRepo?.repo.orEmpty(),
                authState = authState,
                snapshotFetchState = snapshotFetchState,
                currentUser = vm.state.currentUser,
                onLogin = { vm.loginAndConnect() },
                onLogout = { vm.logout() },
                onRefresh = { vm.refresh() },
                onOpenRepoDialog = { vm.openRepoDialog() },
            )
            AppDialogHost(
                vm = vm,
                selectedRepo = selectedRepo,
                uiState = uiState,
                snapshotFetchState = snapshotFetchState,
            )
            AppMainRow(
                vm = vm,
                uiState = uiState,
                snapshotFetchState = snapshotFetchState,
                isConnecting = isConnecting,
            )
        }
    }
}

@Composable
private fun AppEffects(
    vm: VisualizerViewModel,
    authState: AuthState,
    allPrs: List<PullRequest>,
    filteredPrs: List<PullRequest>,
) {
    LaunchedEffect(Unit) {
        vm.initializeSession()
    }
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) vm.ensureRepositoryOptions()
    }
    LaunchedEffect(allPrs) {
        vm.ensurePrColors(allPrs)
    }
    // Only reset to all when a filter change leaves the current selection with no overlap.
    LaunchedEffect(filteredPrs) {
        val available = filteredPrs.map { it.id }.toSet()
        val selected = vm.state.filterState.selectedPrIds
        if (selected.isNotEmpty() && selected.none(available::contains)) {
            vm.selectAllPrs(available)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun appRootModifier(vm: VisualizerViewModel): Modifier = Modifier
    .fillMaxSize()
    .background(AppColors.backgroundMain)
    .onPointerEvent(PointerEventType.Release) { event ->
        when (event.button) {
            PointerButton.Back -> vm.navigateBack()
            PointerButton.Forward -> vm.navigateForward()
            else -> Unit
        }
    }
    .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || !event.isMetaPressed) return@onPreviewKeyEvent false
        if (event.key != Key.R) return@onPreviewKeyEvent false
        vm.resetViewport()
        true
    }

@Composable
private fun AppDialogHost(
    vm: VisualizerViewModel,
    selectedRepo: RepoState.Selected?,
    uiState: VisualizerUiState,
    snapshotFetchState: SnapshotFetchState,
) {
    when (val dialogState = vm.state.dialogState) {
        is DialogState.RepoPicker -> RepoPickerDialog(
            initialQuery = "${selectedRepo?.owner.orEmpty()}/${selectedRepo?.repo.orEmpty()}".trim().trim('/'),
            repoSelectionState = vm.state.repoSelectionState,
            onReload = { vm.loadRepositoryOptions() },
            onDismiss = { vm.closeRepoDialog() },
            onSelect = { fullName ->
                vm.selectRepo(fullName)
                vm.refresh()
            },
        )
        is DialogState.FileDetails -> FileDetailsDialogHost(
            dialogState = dialogState,
            uiState = uiState,
            selectedRepo = selectedRepo,
            snapshotFetchState = snapshotFetchState,
            prColorMap = vm.state.colorState.prColorMap,
            onRetryLoadCommits = { vm.reloadFileDetailsCommits() },
            onDismiss = { vm.closeDialog() },
        )
        is DialogState.PrDetails -> PrDetailsDialog(
            pr = dialogState.pr,
            onDismiss = { vm.closeDialog() },
            onOpenInBrowser = { url ->
                openUrl(url)
                vm.closeDialog()
            },
            onSelectFile = { filePath ->
                vm.selectFile(filePath)
                vm.closeDialog()
            },
        )
        is DialogState.None -> Unit
    }
}

@Composable
private fun FileDetailsDialogHost(
    dialogState: DialogState.FileDetails,
    uiState: VisualizerUiState,
    selectedRepo: RepoState.Selected?,
    snapshotFetchState: SnapshotFetchState,
    prColorMap: Map<String, androidx.compose.ui.graphics.Color>,
    onRetryLoadCommits: () -> Unit,
    onDismiss: () -> Unit,
) {
    val filePath = dialogState.filePath
    val fileNode = remember(uiState.focusRoot, filePath) { findFileNode(uiState.focusRoot, filePath) } ?: return
    FileDetailsDialog(
        filePath = filePath,
        fileName = filePath.substringAfterLast('/'),
        totalLines = fileNode.totalLines,
        fileOverlay = uiState.fileOverlayByPath[filePath],
        repoFullName = "${selectedRepo?.owner.orEmpty().trim()}/${selectedRepo?.repo.orEmpty().trim()}",
        defaultBranch = defaultBranch(snapshotFetchState),
        prColorMap = prColorMap,
        commitsState = dialogState.commitsState,
        onRetryLoadCommits = onRetryLoadCommits,
        onDismiss = onDismiss,
    )
}

@Composable
private fun AppMainRow(
    vm: VisualizerViewModel,
    uiState: VisualizerUiState,
    snapshotFetchState: SnapshotFetchState,
    isConnecting: Boolean,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        if (snapshotFetchState is SnapshotFetchState.Ready) {
            ExplorerPane(
                root = snapshotFetchState.snapshot.rootNode,
                fileOverlayByPath = uiState.fileOverlayByPath,
                directoryOverlayByPath = uiState.directoryOverlayByPath,
                focusPath = vm.state.navigationState.focusPath,
                selectedPath = vm.state.navigationState.selectedPath,
                expandedPaths = vm.state.navigationState.explorerState.expandedPaths,
                onSelectDirectory = { vm.selectDirectory(it) },
                onSelectFile = { vm.selectFile(it) },
                onToggleExpanded = { vm.toggleDirectoryExpanded(it) },
                isLoading = isConnecting,
            )
        }
        TreemapPane(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            focusPath = vm.state.navigationState.focusPath,
            visiblePrs = uiState.visiblePrs,
            focusRoot = uiState.focusRoot,
            selectedPath = vm.state.navigationState.selectedPath,
            fileOverlayByPath = uiState.fileOverlayByPath,
            directoryOverlayByPath = uiState.directoryOverlayByPath,
            prColorMap = vm.state.colorState.prColorMap,
            viewportResetToken = vm.state.navigationState.viewportResetToken,
            onFocusPathChange = { vm.changeFocusPath(it) },
            onSelectedPathChange = { vm.updateSelectedPath(it) },
            onRelatedPrsDetected = { vm.addRelatedPrs(it) },
            onFileDoubleClick = { vm.openFileDetailsDialog(it) },
            isLoading = isConnecting,
        )
        PrListPane(
            filteredPrs = uiState.filteredPrs,
            visiblePrCount = uiState.visiblePrs.size,
            selectedPrIds = uiState.effectiveSelectedIds,
            selectedPath = vm.state.navigationState.selectedPath,
            prColorMap = vm.state.colorState.prColorMap,
            showDrafts = vm.state.filterState.showDrafts,
            onlyMine = vm.state.filterState.onlyMine,
            onShowDraftsChange = { vm.updateShowDrafts(it) },
            onOnlyMineChange = { vm.updateOnlyMine(it) },
            onTogglePr = { prId, checked ->
                onPrToggle(
                    vm = vm,
                    effectiveSelectedIds = uiState.effectiveSelectedIds,
                    prId = prId,
                    checked = checked,
                )
            },
            onOpenPr = { pr -> vm.openPrDetailsDialog(pr) },
            onCyclePrColor = { vm.cyclePrColor(it) },
            onShuffleColors = { vm.shufflePrColors(uiState.allPrs) },
            isLoading = isConnecting,
        )
    }
}

private fun onPrToggle(
    vm: VisualizerViewModel,
    effectiveSelectedIds: Set<String>,
    prId: String,
    checked: Boolean,
) {
    // Initialize from effectiveSelectedIds on first interaction (selectedPrIds is empty = all)
    if (vm.state.filterState.selectedPrIds.isEmpty()) {
        vm.selectAllPrs(effectiveSelectedIds)
    }
    vm.togglePr(prId, checked)
}

private fun snapshotOrNull(fetchState: SnapshotFetchState) = when (fetchState) {
    is SnapshotFetchState.Ready -> fetchState.snapshot
    SnapshotFetchState.Fetching, SnapshotFetchState.Idle, is SnapshotFetchState.Failed -> null
}

private fun computeEffectiveSelectedIds(
    selectedPrIds: Set<String>,
    filteredPrs: List<PullRequest>,
): Set<String> = if (selectedPrIds.isEmpty()) filteredPrs.map { it.id }.toSet() else selectedPrIds

private fun collectAllFiles(root: FileNode.Directory): List<FileNode.File> = buildList {
    fun collectFiles(node: FileNode) {
        when (node) {
            is FileNode.File -> add(node)
            is FileNode.Directory -> node.children.forEach(::collectFiles)
        }
    }
    collectFiles(root)
}

private fun collectAllDirectories(root: FileNode.Directory): List<FileNode.Directory> = buildList {
    fun collectDirectories(dir: FileNode.Directory) {
        add(dir)
        dir.children.forEach { child ->
            if (child is FileNode.Directory) collectDirectories(child)
        }
    }
    collectDirectories(root)
}

private fun defaultBranch(snapshotFetchState: SnapshotFetchState): String = when (snapshotFetchState) {
    is SnapshotFetchState.Ready -> snapshotFetchState.snapshot.defaultBranch
    SnapshotFetchState.Idle, SnapshotFetchState.Fetching, is SnapshotFetchState.Failed -> DEFAULT_BRANCH
}
