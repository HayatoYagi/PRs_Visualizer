@file:Suppress("MatchingDeclarationName") // VisualizerUiState is used by App() composable; keeping them together for cohesion

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
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.repository.store.PersistedSelectedRepositoryStore
import io.github.hayatoyagi.prvisualizer.state.AuthState
import io.github.hayatoyagi.prvisualizer.state.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.ui.dialog.DialogHost
import io.github.hayatoyagi.prvisualizer.ui.explorer.ExplorerPane
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListPane
import io.github.hayatoyagi.prvisualizer.ui.prlist.filterPrs
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.computeDirectoryOverlayByPath
import io.github.hayatoyagi.prvisualizer.ui.shared.computeFileOverlayByPath
import io.github.hayatoyagi.prvisualizer.ui.shared.findDirectory
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.shortcut.RegisterShortcuts
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.toolbar.ToolbarRow
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapPane

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
    val visibleIds = remember(filteredPrs) {
        filteredPrs.map { it.id }.toSet()
    }
    val effectiveSelectedIds = remember(vm.state.filterState.prSelection, visibleIds) {
        vm.state.filterState.prSelection.resolve(visibleIds)
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

    AppEffects(vm = vm, authState = authState, allPrs = uiState.allPrs)

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
            DialogHost(
                dialogState = vm.state.dialogState,
                selectedRepo = selectedRepo,
                uiState = uiState,
                prColorMap = vm.state.colorState.prColorMap,
                repoSelectionState = vm.state.repoSelectionState,
                onReloadRepoOptions = { vm.loadRepositoryOptions() },
                onDismissRepoDialog = { vm.closeRepoDialog() },
                onSelectRepo = { vm.selectRepo(it) },
                onRefresh = { vm.refresh() },
                onRetryLoadCommits = { vm.reloadFileDetailsCommits() },
                onDismissDialog = { vm.closeDialog() },
                onDismissErrorDialog = { vm.dismissErrorDialog() },
                onOpenPrInBrowser = { url ->
                    openUrl(url)
                    vm.closeDialog()
                },
                onSelectFile = { filePath ->
                    vm.selectFile(filePath)
                    vm.closeDialog()
                },
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
) {
    RegisterShortcuts(vm)
    LaunchedEffect(Unit) {
        vm.initializeSession()
    }
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) vm.ensureRepositoryOptions()
    }
    LaunchedEffect(allPrs) {
        vm.ensurePrColors(allPrs)
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
            focusRoot = uiState.focusRoot,
            selectedPath = vm.state.navigationState.selectedPath,
            fileOverlayByPath = uiState.fileOverlayByPath,
            directoryOverlayByPath = uiState.directoryOverlayByPath,
            prColorMap = vm.state.colorState.prColorMap,
            viewportResetToken = vm.state.navigationState.viewportResetToken,
            onFocusPathChange = { vm.changeFocusPath(it) },
            onSelectedPathChange = { vm.updateSelectedPath(it) },
            onFileDoubleClick = { vm.openFileDetailsDialog(it) },
            isLoading = isConnecting,
        )
        PrListPane(
            filteredPrs = uiState.filteredPrs,
            selectedPrIds = uiState.effectiveSelectedIds,
            selectedPath = vm.state.navigationState.selectedPath,
            prColorMap = vm.state.colorState.prColorMap,
            showDrafts = vm.state.filterState.showDrafts,
            onlyMine = vm.state.filterState.onlyMine,
            onShowDraftsChange = { vm.updateShowDrafts(it) },
            onOnlyMineChange = { vm.updateOnlyMine(it) },
            onTogglePr = { prId, checked ->
                vm.togglePr(
                    prId = prId,
                    checked = checked,
                    visibleIds = uiState.filteredPrs.map { it.id }.toSet(),
                )
            },
            onOpenPr = { pr -> vm.openPrDetailsDialog(pr) },
            onCyclePrColor = { vm.cyclePrColor(it) },
            onShuffleColors = { vm.shufflePrColors(uiState.allPrs) },
            onSelectAllPrs = { vm.selectAllPrs() },
            onDeselectAllPrs = { vm.clearPrSelection() },
            isLoading = isConnecting,
        )
    }
}

private fun snapshotOrNull(fetchState: SnapshotFetchState) = when (fetchState) {
    is SnapshotFetchState.Ready -> fetchState.snapshot
    SnapshotFetchState.Fetching, SnapshotFetchState.Idle, is SnapshotFetchState.Failed -> null
}

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
