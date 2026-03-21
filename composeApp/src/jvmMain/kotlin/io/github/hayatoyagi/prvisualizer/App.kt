@file:Suppress("MatchingDeclarationName") // VisualizerUiState is used by App() composable; keeping them together for cohesion

package io.github.hayatoyagi.prvisualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.repository.store.PersistedSelectedRepositoryStore
import io.github.hayatoyagi.prvisualizer.state.AuthState
import io.github.hayatoyagi.prvisualizer.state.SnapshotFetchState
import io.github.hayatoyagi.prvisualizer.ui.dialog.DialogHost
import io.github.hayatoyagi.prvisualizer.ui.explorer.ExplorerPane
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListActions
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListPane
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListUiState
import io.github.hayatoyagi.prvisualizer.ui.prlist.rememberPrListUiState
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
    val prList: PrListUiState,
    val focusRoot: FileNode.Directory,
    val fileOverlayByPath: Map<String, FileOverlay>,
    val directoryOverlayByPath: Map<String, DirectoryOverlay>,
)

@Composable
private fun rememberVisualizerUiState(ready: SnapshotFetchState.Ready): VisualizerUiState {
    val root = ready.snapshot.rootNode
    val allPrs = ready.snapshot.pullRequests
    val currentUser = ready.snapshot.viewerLogin.orEmpty()
    val prListUiState = rememberPrListUiState(
        allPrs = allPrs,
        filterState = ready.filterState,
        currentUser = currentUser,
        selectedPath = ready.navigationState.selectedPath,
        prColorMap = ready.colorState.prColorMap,
    )
    val focusRoot = remember(root, ready.navigationState.focusPath) {
        findDirectory(root, ready.navigationState.focusPath) ?: root
    }
    val allFiles = remember(root) { collectAllFiles(root) }
    val allDirectories = remember(root) { collectAllDirectories(root) }
    val fileOverlayByPath = remember(prListUiState.visiblePrs, allFiles) {
        computeFileOverlayByPath(prListUiState.visiblePrs, allFiles)
    }
    val directoryOverlayByPath = remember(prListUiState.visiblePrs, allDirectories) {
        computeDirectoryOverlayByPath(prListUiState.visiblePrs, allDirectories)
    }
    return VisualizerUiState(
        allPrs = allPrs,
        prList = prListUiState,
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

    AppEffects(vm = vm, authState = authState, snapshotFetchState = snapshotFetchState)

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
            val ready = snapshotFetchState as? SnapshotFetchState.Ready
            val uiState = ready?.let { rememberVisualizerUiState(it) }
            DialogHost(
                dialogState = vm.state.dialogState,
                selectedRepo = selectedRepo,
                uiState = uiState,
                prColorMap = ready?.colorState?.prColorMap ?: emptyMap(),
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
            when {
                ready != null && uiState != null -> AppMainRow(
                    vm = vm,
                    uiState = uiState,
                    ready = ready,
                )
                snapshotFetchState is SnapshotFetchState.Fetching -> AppLoadingState()
                else -> Unit
            }
        }
    }
}

@Composable
private fun AppEffects(
    vm: VisualizerViewModel,
    authState: AuthState,
    snapshotFetchState: SnapshotFetchState,
) {
    RegisterShortcuts(vm)
    LaunchedEffect(Unit) {
        vm.initializeSession()
    }
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) vm.ensureRepositoryOptions()
    }
    LaunchedEffect(snapshotFetchState) {
        if (snapshotFetchState is SnapshotFetchState.Ready) {
            vm.ensurePrColors(snapshotFetchState.snapshot.pullRequests)
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

@Composable
private fun AppMainRow(
    vm: VisualizerViewModel,
    uiState: VisualizerUiState,
    ready: SnapshotFetchState.Ready,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        ExplorerPane(
            root = ready.snapshot.rootNode,
            fileOverlayByPath = uiState.fileOverlayByPath,
            directoryOverlayByPath = uiState.directoryOverlayByPath,
            focusPath = ready.navigationState.focusPath,
            selectedPath = ready.navigationState.selectedPath,
            expandedPaths = ready.navigationState.explorerState.expandedPaths,
            onSelectDirectory = { vm.selectDirectory(it) },
            onSelectFile = { vm.selectFile(it) },
            onToggleExpanded = { vm.toggleDirectoryExpanded(it) },
        )
        TreemapPane(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            focusPath = ready.navigationState.focusPath,
            focusRoot = uiState.focusRoot,
            selectedPath = ready.navigationState.selectedPath,
            fileOverlayByPath = uiState.fileOverlayByPath,
            directoryOverlayByPath = uiState.directoryOverlayByPath,
            prColorMap = ready.colorState.prColorMap,
            viewportResetToken = ready.navigationState.viewportResetToken,
            onFocusPathChange = { vm.changeFocusPath(it) },
            onSelectedPathChange = { vm.updateSelectedPath(it) },
            onFileDoubleClick = { vm.openFileDetailsDialog(it) },
        )
        PrListPane(
            uiState = uiState.prList,
            actions = PrListActions(
                onShowDraftsChange = { vm.updateShowDrafts(it) },
                onOnlyMineChange = { vm.updateOnlyMine(it) },
                onTogglePr = { prId, checked -> vm.togglePr(prId, checked) },
                onOpenPr = { pr -> vm.openPrDetailsDialog(pr) },
                onCyclePrColor = { vm.cyclePrColor(it) },
                onShuffleColors = { vm.shufflePrColors() },
                onToggleSelectAll = { vm.toggleSelectAll() },
            ),
        )
    }
}

@Composable
private fun AppLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AppColors.textPrimary)
            Text(
                text = "Loading snapshot...",
                color = AppColors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
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
