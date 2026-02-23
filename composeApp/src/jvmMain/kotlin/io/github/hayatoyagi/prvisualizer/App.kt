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
import io.github.hayatoyagi.prvisualizer.github.EnvConfig
import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import io.github.hayatoyagi.prvisualizer.repository.RepoState
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
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.findDirectory
import io.github.hayatoyagi.prvisualizer.ui.shared.findFileNode
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.toolbar.AuthRow
import io.github.hayatoyagi.prvisualizer.ui.toolbar.ToolbarRow
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapPane

data class VisualizerUiState(
    val filteredPrs: List<PullRequest>,
    val effectiveSelectedIds: Set<String>,
    val visiblePrs: List<PullRequest>,
    val focusRoot: FileNode.Directory,
    val fileOverlayByPath: Map<String, FileOverlay>,
    val directoryOverlayByPath: Map<String, DirectoryOverlay>,
)

@Composable
private fun rememberVisualizerUiState(vm: VisualizerViewModel): VisualizerUiState {
    val snapshot = vm.state.sessionState.snapshotFetchState.snapshot
    val emptyRoot = remember { FileNode.Directory(path = "", name = "repo", children = emptyList(), weight = 1.0) }
    val root = snapshot?.rootNode ?: emptyRoot
    val allPrs = snapshot?.pullRequests ?: emptyList()
    val currentUser = vm.state.sessionState.currentUser

    val filteredPrs = remember(
        vm.state.filterState.showDrafts,
        vm.state.filterState.onlyMine,
        vm.state.filterState.query,
        allPrs,
        currentUser,
    ) {
        filterPrs(allPrs, vm.state.filterState.showDrafts, vm.state.filterState.onlyMine, vm.state.filterState.query, currentUser)
    }
    // Treat emptySet as "uninitialized / all selected" to avoid a flash on first load.
    // After the user explicitly toggles a PR, selectedPrIds becomes non-empty.
    val effectiveSelectedIds = remember(vm.state.filterState.selectedPrIds, filteredPrs) {
        if (vm.state.filterState.selectedPrIds.isEmpty()) {
            filteredPrs.map { it.id }.toSet()
        } else {
            vm.state.filterState.selectedPrIds
        }
    }
    // Only reset to all when a filter change leaves the current selection with no overlap.
    LaunchedEffect(filteredPrs) {
        val available = filteredPrs.map { it.id }.toSet()
        if (vm.state.filterState.selectedPrIds.isNotEmpty() &&
            vm.state.filterState.selectedPrIds.none { available.contains(it) }
        ) {
            vm.selectAllPrs(available)
        }
    }
    val visiblePrs = remember(filteredPrs, effectiveSelectedIds) {
        filteredPrs.filter { effectiveSelectedIds.contains(it.id) }
    }
    val focusRoot = remember(root, vm.state.navigationState.focusPath) {
        findDirectory(root, vm.state.navigationState.focusPath) ?: root
    }
    val allFiles = remember(root) {
        buildList {
            fun collectFiles(node: FileNode) {
                when (node) {
                    is FileNode.File -> add(node)
                    is FileNode.Directory -> node.children.forEach(::collectFiles)
                }
            }
            collectFiles(root)
        }
    }
    val allDirectories = remember(root) {
        buildList {
            fun collectDirectories(dir: FileNode.Directory) {
                add(dir)
                dir.children.forEach { child ->
                    if (child is FileNode.Directory) collectDirectories(child)
                }
            }
            collectDirectories(root)
        }
    }
    val fileOverlayByPath = remember(visiblePrs, allFiles) {
        computeFileOverlayByPath(visiblePrs, allFiles)
    }
    val directoryOverlayByPath = remember(visiblePrs, allDirectories) {
        computeDirectoryOverlayByPath(visiblePrs, allDirectories)
    }
    return VisualizerUiState(
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
    val vm = viewModel { VisualizerViewModel() }
    val oauthClientId = remember { EnvConfig.get("GITHUB_CLIENT_ID")?.trim().orEmpty() }
    val authState = vm.state.sessionState.authState
    val snapshotFetchState = vm.state.sessionState.snapshotFetchState
    val selectedRepo = vm.repoState.collectAsState().value as? RepoState.Selected
    val allPrs = snapshotFetchState.snapshot?.pullRequests ?: emptyList()

    val uiState = rememberVisualizerUiState(vm)

    LaunchedEffect(Unit) {
        vm.initializeSession()
    }
    LaunchedEffect(authState.oauthToken) {
        if (authState.isLoggedIn) {
            vm.ensureRepositoryOptions()
        }
    }
    // Ensure all PRs have colors assigned
    LaunchedEffect(allPrs) {
        vm.ensurePrColors(allPrs)
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.backgroundMain)
                .onPointerEvent(PointerEventType.Release) { event ->
                    when (event.button) {
                        PointerButton.Back -> vm.navigateBack()
                        PointerButton.Forward -> vm.navigateForward()
                        else -> {}
                    }
                }.onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !event.isMetaPressed) {
                        return@onPreviewKeyEvent false
                    }
                    when (event.key) {
                        Key.R -> {
                            vm.resetViewport()
                            true
                        }
                        Key.F -> {
                            vm.clearQuery()
                            true
                        }
                        else -> false
                    }
                },
        ) {
            ToolbarRow(
                owner = selectedRepo?.owner.orEmpty(),
                repo = selectedRepo?.repo.orEmpty(),
                isLoggedIn = authState.isLoggedIn,
                onOpenRepoDialog = { vm.openRepoDialog() },
                onShuffleColors = { vm.shufflePrColors(allPrs) },
            )
            AuthRow(
                oauthClientId = oauthClientId,
                isAuthorizing = authState.isAuthorizing,
                isConnecting = snapshotFetchState.isFetching,
                isLoggedIn = authState.isLoggedIn,
                currentUser = vm.state.sessionState.currentUser,
                deviceUserCode = authState.deviceUserCode,
                deviceVerificationUrl = authState.deviceVerificationUrl,
                // Auth errors take priority; if both exist, the snapshot error is suppressed.
                connectionError = authState.error ?: snapshotFetchState.error,
                hasSnapshot = snapshotFetchState.snapshot != null,
                onLogin = { vm.loginAndConnect(oauthClientId) },
                onRefresh = { vm.refresh() },
                onCopyDeviceCode = { copyToClipboard(authState.deviceUserCode.orEmpty()) },
                onOpenVerifyPage = { openUrl(authState.deviceVerificationUrl.orEmpty()) },
            )
            when (val dialogState = vm.state.dialogState) {
                is DialogState.RepoPicker -> {
                    RepoPickerDialog(
                        initialQuery = "${selectedRepo?.owner.orEmpty()}/${selectedRepo?.repo.orEmpty()}".trim().trim('/'),
                        options = vm.state.repoSelectionState.options,
                        isLoading = vm.state.repoSelectionState.isLoading,
                        onReload = { vm.loadRepositoryOptions() },
                        onDismiss = { vm.closeRepoDialog() },
                        onSelect = { fullName ->
                            vm.selectRepo(fullName)
                            vm.refresh()
                        },
                    )
                }
                is DialogState.FileDetails -> {
                    val filePath = dialogState.filePath
                    val fileName = filePath.substringAfterLast('/')
                    val fileNode = remember(uiState.focusRoot, filePath) {
                        findFileNode(uiState.focusRoot, filePath)
                    }
                    val fileOverlay = uiState.fileOverlayByPath[filePath]
                    val oauthToken = authState.oauthToken

                    if (fileNode != null && oauthToken.isNotBlank()) {
                        val githubApi = remember(oauthToken) { GitHubApi(oauthToken) }
                        FileDetailsDialog(
                            filePath = filePath,
                            fileName = fileName,
                            totalLines = fileNode.totalLines,
                            fileOverlay = fileOverlay,
                            repoFullName = "${selectedRepo?.owner.orEmpty().trim()}/${selectedRepo?.repo.orEmpty().trim()}",
                            defaultBranch = snapshotFetchState.snapshot?.defaultBranch ?: "main",
                            prColorMap = vm.state.colorState.prColorMap,
                            githubApi = githubApi,
                            onDismiss = { vm.closeDialog() },
                        )
                    }
                }
                is DialogState.PrDetails -> {
                    PrDetailsDialog(
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
                }
                is DialogState.None -> Unit
            }

            Row(modifier = Modifier.fillMaxSize()) {
                ExplorerPane(
                    root = snapshotFetchState.snapshot?.rootNode,
                    fileOverlayByPath = uiState.fileOverlayByPath,
                    directoryOverlayByPath = uiState.directoryOverlayByPath,
                    focusPath = vm.state.navigationState.focusPath,
                    selectedPath = vm.state.navigationState.selectedPath,
                    expandedPaths = vm.state.navigationState.explorerState.expandedPaths,
                    onSelectDirectory = { vm.selectDirectory(it) },
                    onSelectFile = { vm.selectFile(it) },
                    onToggleExpanded = { vm.toggleDirectoryExpanded(it) },
                    isLoading = snapshotFetchState.isFetching,
                )
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
                    isLoading = snapshotFetchState.isFetching,
                )
                PrListPane(
                    filteredPrs = uiState.filteredPrs,
                    selectedPrIds = uiState.effectiveSelectedIds,
                    selectedPath = vm.state.navigationState.selectedPath,
                    prColorMap = vm.state.colorState.prColorMap,
                    query = vm.state.filterState.query,
                    showDrafts = vm.state.filterState.showDrafts,
                    onlyMine = vm.state.filterState.onlyMine,
                    onQueryChange = { vm.updateQuery(it) },
                    onShowDraftsChange = { vm.updateShowDrafts(it) },
                    onOnlyMineChange = { vm.updateOnlyMine(it) },
                    onTogglePr = { prId, checked ->
                        // Initialize from effectiveSelectedIds on first interaction (selectedPrIds is empty = all)
                        if (vm.state.filterState.selectedPrIds.isEmpty()) {
                            vm.selectAllPrs(uiState.effectiveSelectedIds)
                        }
                        vm.togglePr(prId, checked)
                    },
                    onOpenPr = { pr -> vm.openPrDetailsDialog(pr) },
                    onCyclePrColor = { vm.cyclePrColor(it) },
                    isLoading = snapshotFetchState.isFetching,
                )
            }
        }
    }
}
