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

    val filteredRepoOptions = remember(githubSession.repositoryOptions, vm.repoPickerQuery) {
        filterRepoOptions(githubSession.repositoryOptions, vm.repoPickerQuery)
    }

    val filteredPrs = remember(vm.showDrafts, vm.onlyMine, vm.query, allPrs, currentUser) {
        filterPrs(allPrs, vm.showDrafts, vm.onlyMine, vm.query, currentUser)
    }

    // Treat emptySet as "uninitialized / all selected" to avoid a flash on first load.
    // After the user explicitly toggles a PR, selectedPrIds becomes non-empty.
    val effectiveSelectedIds = remember(vm.selectedPrIds, filteredPrs) {
        if (vm.selectedPrIds.isEmpty()) filteredPrs.map { it.id }.toSet()
        else vm.selectedPrIds
    }

    // Only reset to all when a filter change leaves the current selection with no overlap.
    LaunchedEffect(filteredPrs) {
        val available = filteredPrs.map { it.id }.toSet()
        if (vm.selectedPrIds.isNotEmpty() && vm.selectedPrIds.none { available.contains(it) }) {
            vm.selectAllPrs(available)
        }
    }
    LaunchedEffect(Unit) {
        githubSession.restoreTokenAndConnectIfNeeded(owner = vm.owner, repo = vm.repo)
        if (githubSession.githubSnapshot != null) {
            vm.resetNavigation()
            vm.resetViewport()
        }
    }
    LaunchedEffect(githubSession.oauthToken) {
        if (githubSession.oauthToken.isNotBlank()) {
            githubSession.ensureRepositoryOptions()
            if (vm.repo.isBlank() && githubSession.repositoryOptions.isNotEmpty()) {
                val default = githubSession.repositoryOptions.first()
                vm.selectRepo(default)
            }
        }
    }

    val visiblePrs = remember(filteredPrs, effectiveSelectedIds) {
        filteredPrs.filter { effectiveSelectedIds.contains(it.id) }
    }

    val focusRoot = remember(root, vm.focusPath) {
        findDirectory(root, vm.focusPath) ?: root
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
        computeFileOverlayByPath(visiblePrs, visibleFiles)
    }

    val directoryOverlayByPath = remember(visiblePrs, visibleDirectories) {
        computeDirectoryOverlayByPath(visiblePrs, visibleDirectories)
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.backgroundMain)
                .onPreviewKeyEvent { event ->
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
                owner = vm.owner,
                repo = vm.repo,
                isLoggedIn = githubSession.oauthToken.isNotBlank(),
                onOpenRepoDialog = { vm.openRepoDialog() },
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
                            owner = vm.owner,
                            repo = vm.repo,
                        )
                        if (githubSession.githubSnapshot != null) {
                            vm.resetNavigation()
                            vm.resetViewport()
                        }
                    }
                },
                onRefresh = {
                    scope.launch {
                        githubSession.refresh(owner = vm.owner, repo = vm.repo)
                        if (githubSession.githubSnapshot != null) {
                            vm.resetNavigation()
                            vm.resetViewport()
                        }
                    }
                },
                onCopyDeviceCode = { copyToClipboard(githubSession.deviceUserCode.orEmpty()) },
                onOpenVerifyPage = { openUrl(githubSession.deviceVerificationUrl.orEmpty()) },
            )
            if (vm.isRepoDialogOpen) {
                RepoPickerDialog(
                    query = vm.repoPickerQuery,
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
                            githubSession.refresh(owner = vm.owner, repo = vm.repo)
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
                    focusPath = vm.focusPath,
                    selectedPath = vm.selectedPath,
                    onSelectDirectory = { vm.selectDirectory(it) },
                    onSelectFile = { vm.selectFile(it) },
                )

                TreemapPane(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    focusPath = vm.focusPath,
                    visiblePrs = visiblePrs,
                    focusRoot = focusRoot,
                    selectedPath = vm.selectedPath,
                    fileOverlayByPath = fileOverlayByPath,
                    directoryOverlayByPath = directoryOverlayByPath,
                    viewportResetToken = vm.viewportResetToken,
                    onFocusPathChange = { vm.changeFocusPath(it) },
                    onSelectedPathChange = { vm.updateSelectedPath(it) },
                    onRelatedPrsDetected = { vm.addRelatedPrs(it) },
                    repoFullName = "${vm.owner.trim()}/${vm.repo.trim()}",
                )

                PrListPane(
                    filteredPrs = filteredPrs,
                    selectedPrIds = effectiveSelectedIds,
                    selectedPath = vm.selectedPath,
                    query = vm.query,
                    showDrafts = vm.showDrafts,
                    onlyMine = vm.onlyMine,
                    onQueryChange = { vm.updateQuery(it) },
                    onShowDraftsChange = { vm.updateShowDrafts(it) },
                    onOnlyMineChange = { vm.updateOnlyMine(it) },
                    onTogglePr = { prId, checked ->
                        // Initialize from effectiveSelectedIds on first interaction (selectedPrIds is empty = all)
                        if (vm.selectedPrIds.isEmpty()) vm.selectAllPrs(effectiveSelectedIds)
                        vm.togglePr(prId, checked)
                    },
                    onOpenPr = { openUrl(it) },
                )
            }
        }
    }
}
