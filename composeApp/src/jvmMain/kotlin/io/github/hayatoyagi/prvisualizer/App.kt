package io.github.hayatoyagi.prvisualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.hayatoyagi.prvisualizer.github.session.rememberGitHubSessionState
import io.github.hayatoyagi.prvisualizer.ui.explorer.ExplorerPane
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListPane
import io.github.hayatoyagi.prvisualizer.ui.repo.RepoPickerDialog
import io.github.hayatoyagi.prvisualizer.ui.shared.buildExplorerRows
import io.github.hayatoyagi.prvisualizer.ui.shared.computeDirectoryOverlayByPath
import io.github.hayatoyagi.prvisualizer.ui.shared.computeFileOverlayByPath
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.filterPrs
import io.github.hayatoyagi.prvisualizer.ui.shared.filterRepoOptions
import io.github.hayatoyagi.prvisualizer.ui.shared.findDirectory
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.toolbar.AuthRow
import io.github.hayatoyagi.prvisualizer.ui.toolbar.ToolbarRow
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapPane
import kotlinx.coroutines.launch

@Composable
@Preview
@OptIn(ExperimentalComposeUiApi::class)
fun App() {
    val vm = viewModel { VisualizerViewModel() }
    val oauthClientId = remember { EnvConfig.get("GITHUB_CLIENT_ID")?.trim().orEmpty() }
    val githubSession = rememberGitHubSessionState(
        initialToken = EnvConfig.get("GITHUB_TOKEN") ?: "",
        // TODO: Replace with authenticated GitHub login (or user-configurable value) instead of hard-coded user.
        initialUser = EnvConfig.get("GITHUB_USER") ?: "hayatoy",
    )
    val emptyRoot = remember { FileNode.Directory(path = "", name = "repo", children = emptyList(), weight = 1.0) }

    val root = githubSession.githubSnapshot?.rootNode ?: emptyRoot
    val allPrs = githubSession.githubSnapshot?.pullRequests ?: emptyList()
    val currentUser = githubSession.currentUser

    val scope = rememberCoroutineScope()

    val filteredRepoOptions = remember(githubSession.repositoryOptions, vm.state.dialogState.repoPickerQuery) {
        filterRepoOptions(githubSession.repositoryOptions, vm.state.dialogState.repoPickerQuery)
    }

    val filteredPrs =
        remember(vm.state.filterState.showDrafts, vm.state.filterState.onlyMine, vm.state.filterState.query, allPrs, currentUser) {
            filterPrs(allPrs, vm.state.filterState.showDrafts, vm.state.filterState.onlyMine, vm.state.filterState.query, currentUser)
        }

    // Treat emptySet as "uninitialized / all selected" to avoid a flash on first load.
    // After the user explicitly toggles a PR, selectedPrIds becomes non-empty.
    val effectiveSelectedIds = remember(vm.state.filterState.selectedPrIds, filteredPrs) {
        if (vm.state.filterState.selectedPrIds
                .isEmpty()
        ) {
            filteredPrs.map { it.id }.toSet()
        } else {
            vm.state.filterState.selectedPrIds
        }
    }

    // Only reset to all when a filter change leaves the current selection with no overlap.
    LaunchedEffect(filteredPrs) {
        val available = filteredPrs.map { it.id }.toSet()
        if (vm.state.filterState.selectedPrIds
                .isNotEmpty() &&
            vm.state.filterState.selectedPrIds
                .none { available.contains(it) }
        ) {
            vm.selectAllPrs(available)
        }
    }
    LaunchedEffect(Unit) {
        githubSession.restoreTokenAndConnectIfNeeded(owner = vm.state.repoState.owner, repo = vm.state.repoState.repo)
        if (githubSession.githubSnapshot != null) {
            vm.resetNavigation()
            vm.resetViewport()
        }
    }
    LaunchedEffect(githubSession.oauthToken) {
        if (githubSession.oauthToken.isNotBlank()) {
            githubSession.ensureRepositoryOptions()
            if (vm.state.repoState.repo
                    .isBlank() &&
                githubSession.repositoryOptions.isNotEmpty()
            ) {
                val default = githubSession.repositoryOptions.first()
                vm.selectRepo(default)
            }
        }
    }

    val visiblePrs = remember(filteredPrs, effectiveSelectedIds) {
        filteredPrs.filter { effectiveSelectedIds.contains(it.id) }
    }

    // Ensure all PRs have colors assigned
    LaunchedEffect(allPrs) {
        vm.ensurePrColors(allPrs)
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

    val explorerRows = remember(root, fileOverlayByPath, directoryOverlayByPath) {
        buildExplorerRows(root, fileOverlayByPath, directoryOverlayByPath)
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.backgroundMain)
                .onPointerEvent(PointerEventType.Release) { event ->
                    when (event.button) {
                        PointerButton.Back -> {
                            vm.navigateBack()
                        }
                        PointerButton.Forward -> {
                            vm.navigateForward()
                        }
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
                owner = vm.state.repoState.owner,
                repo = vm.state.repoState.repo,
                isLoggedIn = githubSession.oauthToken.isNotBlank(),
                onOpenRepoDialog = { vm.openRepoDialog() },
                onShuffleColors = { vm.shufflePrColors(allPrs) },
            )
            AuthRow(
                oauthClientId = oauthClientId,
                isAuthorizing = githubSession.isAuthorizing,
                isConnecting = githubSession.isConnecting,
                isLoggedIn = githubSession.oauthToken.isNotBlank(),
                currentUser = currentUser,
                deviceUserCode = githubSession.deviceUserCode,
                deviceVerificationUrl = githubSession.deviceVerificationUrl,
                connectionError = githubSession.connectionError,
                hasSnapshot = githubSession.githubSnapshot != null,
                onLogin = {
                    scope.launch {
                        githubSession.loginAndConnect(
                            clientId = oauthClientId,
                            owner = vm.state.repoState.owner,
                            repo = vm.state.repoState.repo,
                        )
                        if (githubSession.githubSnapshot != null) {
                            vm.resetNavigation()
                            vm.resetViewport()
                        }
                    }
                },
                onRefresh = {
                    scope.launch {
                        githubSession.refresh(owner = vm.state.repoState.owner, repo = vm.state.repoState.repo)
                        if (githubSession.githubSnapshot != null) {
                            vm.resetNavigation()
                            vm.resetViewport()
                        }
                    }
                },
                onCopyDeviceCode = { copyToClipboard(githubSession.deviceUserCode.orEmpty()) },
                onOpenVerifyPage = { openUrl(githubSession.deviceVerificationUrl.orEmpty()) },
            )
            if (vm.state.dialogState.isRepoDialogOpen) {
                RepoPickerDialog(
                    query = vm.state.dialogState.repoPickerQuery,
                    onQueryChange = { vm.updateRepoPickerQuery(it) },
                    options = filteredRepoOptions,
                    isLoading = githubSession.isLoadingRepositories,
                    onReload = {
                        scope.launch { githubSession.loadRepositoryOptions() }
                    },
                    onDismiss = { vm.closeRepoDialog() },
                    onSelect = { fullName ->
                        vm.selectRepo(fullName)
                        scope.launch {
                            githubSession.refresh(owner = vm.state.repoState.owner, repo = vm.state.repoState.repo)
                            if (githubSession.githubSnapshot != null) {
                                vm.resetNavigation()
                                vm.resetViewport()
                            }
                        }
                    },
                )
            }

            Row(modifier = Modifier.fillMaxSize()) {
                ExplorerPane(
                    rows = explorerRows,
                    focusPath = vm.state.navigationState.focusPath,
                    selectedPath = vm.state.navigationState.selectedPath,
                    onSelectDirectory = { vm.selectDirectory(it) },
                    onSelectFile = { vm.selectFile(it) },
                )

                TreemapPane(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    focusPath = vm.state.navigationState.focusPath,
                    visiblePrs = visiblePrs,
                    focusRoot = focusRoot,
                    selectedPath = vm.state.navigationState.selectedPath,
                    fileOverlayByPath = fileOverlayByPath,
                    directoryOverlayByPath = directoryOverlayByPath,
                    prColorMap = vm.state.colorState.prColorMap,
                    viewportResetToken = vm.state.navigationState.viewportResetToken,
                    onFocusPathChange = { vm.changeFocusPath(it) },
                    onSelectedPathChange = { vm.updateSelectedPath(it) },
                    onRelatedPrsDetected = { vm.addRelatedPrs(it) },
                    repoFullName = "${vm.state.repoState.owner.trim()}/${vm.state.repoState.repo.trim()}",
                )

                PrListPane(
                    filteredPrs = filteredPrs,
                    selectedPrIds = effectiveSelectedIds,
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
                        if (vm.state.filterState.selectedPrIds
                                .isEmpty()
                        ) {
                            vm.selectAllPrs(effectiveSelectedIds)
                        }
                        vm.togglePr(prId, checked)
                    },
                    onOpenPr = { openUrl(it) },
                    onCyclePrColor = { vm.cyclePrColor(it) },
                )
            }
        }
    }
}
