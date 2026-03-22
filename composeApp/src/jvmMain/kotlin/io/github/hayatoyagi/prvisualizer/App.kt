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
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListPane
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.shortcut.RegisterShortcuts
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.toolbar.ToolbarRow
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapPane

@Composable
@Preview
fun App() {
    val vm = viewModel {
        VisualizerViewModel(
            selectedRepositoryStore = PersistedSelectedRepositoryStore(),
        )
    }
    val snapshotFetchState = vm.state.snapshotFetchState
    val selectedRepo = vm.repoState.collectAsState().value as? RepoState.Selected

    AppEffects(vm = vm)

    MaterialTheme {
        Column(
            modifier = appRootModifier(vm),
        ) {
            ToolbarRow(
                owner = selectedRepo?.owner.orEmpty(),
                repo = selectedRepo?.repo.orEmpty(),
                authState = vm.state.authState,
                snapshotFetchState = snapshotFetchState,
                currentUser = vm.state.currentUser,
                onLogin = { vm.loginAndConnect() },
                onLogout = { vm.logout() },
                onRefresh = { vm.refresh() },
                onOpenRepoDialog = { vm.openRepoDialog() },
            )
            val ready = snapshotFetchState as? SnapshotFetchState.Ready
            DialogHost(
                dialogState = vm.state.dialogState,
                selectedRepo = selectedRepo,
                ready = ready,
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
                ready != null -> AppMainRow(
                    vm = vm,
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
) {
    val authState = vm.state.authState
    val snapshotFetchState = vm.state.snapshotFetchState
    val ready = snapshotFetchState as? SnapshotFetchState.Ready
    RegisterShortcuts(vm)
    LaunchedEffect(Unit) {
        vm.initializeSession()
    }
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) vm.ensureRepositoryOptions()
    }
    LaunchedEffect(ready?.snapshot) {
        ready?.let {
            vm.ensurePrColors(it.snapshot.pullRequests)
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
    ready: SnapshotFetchState.Ready,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        ExplorerPane(
            root = ready.snapshot.rootNode,
            fileOverlayByPath = ready.fileOverlayByPath,
            directoryOverlayByPath = ready.directoryOverlayByPath,
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
            focusRoot = ready.focusRoot,
            selectedPath = ready.navigationState.selectedPath,
            fileOverlayByPath = ready.fileOverlayByPath,
            directoryOverlayByPath = ready.directoryOverlayByPath,
            prColorMap = ready.colorState.prColorMap,
            viewportResetToken = ready.navigationState.viewportResetToken,
            onFocusPathChange = { vm.changeFocusPath(it) },
            onSelectedPathChange = { vm.updateSelectedPath(it) },
            onFileDoubleClick = { vm.openFileDetailsDialog(it) },
        )
        PrListPane(
            filteredPrs = ready.filteredPrs,
            selectedPrIds = ready.selectedPrIds,
            selectedPath = ready.navigationState.selectedPath,
            prColorMap = ready.colorState.prColorMap,
            showDrafts = ready.filterState.showDrafts,
            onlyMine = ready.filterState.onlyMine,
            selectAllState = ready.selectAllState,
            onShowDraftsChange = { vm.updateShowDrafts(it) },
            onOnlyMineChange = { vm.updateOnlyMine(it) },
            onTogglePr = { prId, checked -> vm.togglePr(prId, checked) },
            onOpenPr = { pr -> vm.openPrDetailsDialog(pr) },
            onCyclePrColor = { vm.cyclePrColor(it) },
            onShuffleColors = { vm.shufflePrColors() },
            onToggleSelectAll = { vm.toggleSelectAll() },
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
