package io.github.hayatoyagi.prvisualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.github.EnvConfig
import io.github.hayatoyagi.prvisualizer.github.session.rememberGitHubSessionState
import io.github.hayatoyagi.prvisualizer.ui.explorer.ExplorerPane
import io.github.hayatoyagi.prvisualizer.ui.prlist.PrListPane
import io.github.hayatoyagi.prvisualizer.ui.repo.RepoPickerDialog
import io.github.hayatoyagi.prvisualizer.ui.shared.DirectoryOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.FileOverlay
import io.github.hayatoyagi.prvisualizer.ui.shared.buildExplorerRows
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.findDirectory
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf
import io.github.hayatoyagi.prvisualizer.ui.shared.totalLines
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapPane
import kotlinx.coroutines.launch

@Composable
@Preview
@OptIn(ExperimentalComposeUiApi::class)
fun App() {
    var owner by remember { mutableStateOf(EnvConfig.get("GITHUB_OWNER") ?: "HayatoYagi") }
    var repo by remember { mutableStateOf(EnvConfig.get("GITHUB_REPO") ?: "GitHub_PRs_Visualizer") }
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

    var showDrafts by remember { mutableStateOf(true) }
    var onlyMine by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var selectedPrIds by remember { mutableStateOf(allPrs.map { it.id }.toSet()) }
    var focusPath by remember { mutableStateOf("") }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var viewportResetToken by remember { mutableIntStateOf(0) }
    var isRepoDialogOpen by remember { mutableStateOf(false) }
    var repoPickerQuery by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val filteredRepoOptions = remember(githubSession.repositoryOptions, repoPickerQuery) {
        val query = repoPickerQuery.trim()
        if (query.isBlank()) {
            githubSession.repositoryOptions.take(200)
        } else {
            githubSession.repositoryOptions
                .filter { it.contains(query, ignoreCase = true) }
                .take(200)
        }
    }

    val filteredPrs = remember(showDrafts, onlyMine, query, allPrs, currentUser) {
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
    LaunchedEffect(Unit) {
        githubSession.restoreTokenAndConnectIfNeeded(owner = owner, repo = repo)
        if (githubSession.githubSnapshot != null) {
            focusPath = ""
            selectedPath = null
            viewportResetToken += 1
        }
    }
    LaunchedEffect(githubSession.oauthToken) {
        if (githubSession.oauthToken.isNotBlank()) {
            githubSession.ensureRepositoryOptions()
            if (repo.isBlank() && githubSession.repositoryOptions.isNotEmpty()) {
                val default = githubSession.repositoryOptions.first()
                owner = default.substringBefore('/', owner)
                repo = default.substringAfter('/', default)
            }
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
        Column(
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F2832))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Repository: ${owner.trim()}/${repo.trim()}",
                    color = Color(0xFFDCEAF5),
                    modifier = Modifier.weight(1f).padding(top = 12.dp),
                )
                Button(
                    enabled = githubSession.oauthToken.isNotBlank(),
                    onClick = {
                        repoPickerQuery = "${owner.trim()}/${repo.trim()}".trim().trim('/')
                        isRepoDialogOpen = true
                    },
                ) {
                    Text("Select Repo")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F2832))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (githubSession.oauthToken.isBlank()) {
                    Button(
                        enabled = !githubSession.isAuthorizing && oauthClientId.isNotBlank(),
                        onClick = {
                            scope.launch {
                                githubSession.loginAndConnect(
                                    clientId = oauthClientId,
                                    owner = owner,
                                    repo = repo,
                                )
                                if (githubSession.githubSnapshot != null) {
                                    focusPath = ""
                                    selectedPath = null
                                    viewportResetToken += 1
                                }
                            }
                        },
                    ) {
                        Text(if (githubSession.isAuthorizing) "Authorizing..." else "Login with GitHub")
                    }
                }
                Button(
                    enabled = !githubSession.isConnecting && githubSession.oauthToken.isNotBlank(),
                    onClick = {
                        scope.launch {
                            githubSession.refresh(owner = owner, repo = repo)
                            if (githubSession.githubSnapshot != null) {
                                focusPath = ""
                                selectedPath = null
                                viewportResetToken += 1
                            }
                        }
                    },
                ) {
                    Text(if (githubSession.isConnecting) "Refreshing..." else "Refresh")
                }
                if (oauthClientId.isBlank()) {
                    Text(
                        text = "Missing GITHUB_CLIENT_ID in .env",
                        color = Color(0xFFFFC107),
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
                if (githubSession.deviceUserCode != null && githubSession.deviceVerificationUrl != null) {
                    SelectionContainer {
                        Text(
                            text = "GitHub code: ${githubSession.deviceUserCode} @ ${githubSession.deviceVerificationUrl}",
                            color = Color(0xFFFFE082),
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                    Button(
                        onClick = { copyToClipboard(githubSession.deviceUserCode.orEmpty()) },
                    ) { Text("Copy Code") }
                    Button(
                        onClick = { openUrl(githubSession.deviceVerificationUrl.orEmpty()) },
                    ) { Text("Open Verify Page") }
                }
                Text(
                    text = if (githubSession.githubSnapshot == null) {
                        if (githubSession.oauthToken.isBlank()) {
                            "Not connected (not logged in)"
                        } else {
                            "Logged in as: ${currentUser} (not connected)"
                        }
                    } else {
                        "Logged in as: ${currentUser}"
                    },
                    color = Color(0xFF9EC4DD),
                    modifier = Modifier.padding(top = 14.dp),
                )
                if (githubSession.connectionError != null) {
                    SelectionContainer {
                        Text(
                            text = "Error: ${githubSession.connectionError}",
                            color = Color(0xFFFF8A80),
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                }
            }
            if (isRepoDialogOpen) {
                RepoPickerDialog(
                    query = repoPickerQuery,
                    onQueryChange = { repoPickerQuery = it },
                    options = filteredRepoOptions,
                    isLoading = githubSession.isLoadingRepositories,
                    onReload = {
                        scope.launch { githubSession.loadRepositoryOptions() }
                    },
                    onDismiss = { isRepoDialogOpen = false },
                    onSelect = { fullName ->
                        val selectedOwner = fullName.substringBefore('/', owner)
                        val selectedRepo = fullName.substringAfter('/', fullName)
                        owner = selectedOwner
                        repo = selectedRepo
                        isRepoDialogOpen = false
                        scope.launch {
                            githubSession.refresh(owner = selectedOwner, repo = selectedRepo)
                            if (githubSession.githubSnapshot != null) {
                                focusPath = ""
                                selectedPath = null
                                viewportResetToken += 1
                            }
                        }
                    },
                )
            }

            Row(modifier = Modifier.fillMaxSize()) {
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
                    repoFullName = "${owner.trim()}/${repo.trim()}",
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
                    onOpenPr = { openUrl(it) },
                )
            }
        }
    }
}
