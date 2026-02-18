package io.github.hayatoyagi.prvisualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

private data class FileOverlay(
    val prs: List<PullRequest>,
    val dominantType: ChangeType,
    val density: Float,
)

private data class DirectoryOverlay(
    val prs: List<PullRequest>,
    val dominantType: ChangeType?,
    val density: Float,
)

private data class ExplorerRow(
    val path: String,
    val name: String,
    val depth: Int,
    val isDirectory: Boolean,
)

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

    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var pointerPos by remember { mutableStateOf(Offset.Zero) }
    var dragPointerPos by remember { mutableStateOf<Offset?>(null) }
    var focusPath by remember { mutableStateOf("") }

    var hoveredNode by remember { mutableStateOf<TreemapNode?>(null) }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var lastClickKey by remember { mutableStateOf<String?>(null) }
    var lastClickAt by remember { mutableStateOf(0L) }

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

    val allLayoutNodes = remember(focusRoot, canvasSize) {
        computeTreemap(
            root = focusRoot,
            bounds = Rect(0f, 0f, canvasSize.width.toFloat(), canvasSize.height.toFloat()),
        )
    }

    val visibleNodes = remember(allLayoutNodes) {
        allLayoutNodes.filter { it.depth == 1 }
    }
    val visibleFiles = remember(visibleNodes) { visibleNodes.filter { !it.isDirectory } }
    val visibleDirectories = remember(visibleNodes) { visibleNodes.filter { it.isDirectory } }

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
            val density = (totalChanged.toFloat() / dir.totalLines.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

            dir.path to DirectoryOverlay(
                prs = relatedPrs,
                dominantType = dominantType,
                density = density,
            )
        }
    }

    val hoveredOverlay = hoveredNode?.takeIf { !it.isDirectory }?.let { fileOverlayByPath[it.path] }
    val hoveredDirOverlay = hoveredNode?.takeIf { it.isDirectory }?.let { directoryOverlayByPath[it.path] }

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
                            zoom = 1f
                            pan = Offset.Zero
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
            Column(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF18212B))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Explorer", color = Color(0xFFE8F1F8), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Current: /${focusPath.ifBlank { "" }}",
                    color = Color(0xFF9EC4DD),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxWidth()
                        .background(Color(0xFF13202B)),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(explorerRows, key = { if (it.isDirectory) "d:${it.path}" else "f:${it.path}" }) { row ->
                        val isCurrentDir = row.isDirectory && row.path == focusPath
                        val isAncestor = row.isDirectory && focusPath.startsWith("${row.path}/")
                        val isSelectedFile = !row.isDirectory && row.path == selectedPath
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    when {
                                        isCurrentDir -> Color(0xFF2A455B)
                                        isSelectedFile -> Color(0xFF2B3A4A)
                                        else -> Color.Transparent
                                    },
                                )
                                .clickable {
                                    if (row.isDirectory) {
                                        focusPath = row.path
                                        zoom = 1f
                                        pan = Offset.Zero
                                    } else {
                                        selectedPath = row.path
                                        focusPath = parentPathOf(row.path)
                                        zoom = 1f
                                        pan = Offset.Zero
                                    }
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.width((row.depth * 12).dp))
                            Text(
                                text = if (row.isDirectory) "[D]" else "[F]",
                                color = if (row.isDirectory) Color(0xFFFFD37A) else Color(0xFF96B2C8),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (row.isDirectory) "${row.name}/" else row.name,
                                color = when {
                                    isCurrentDir -> Color.White
                                    isAncestor -> Color(0xFFFFE4A5)
                                    else -> Color(0xFFD7E4EE)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F2832))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = {
                        focusPath = ""
                        zoom = 1f
                        pan = Offset.Zero
                    }) {
                        Text("Root")
                    }
                    Button(
                        onClick = {
                            focusPath = parentPathOf(focusPath)
                            zoom = 1f
                            pan = Offset.Zero
                        },
                        enabled = focusPath.isNotBlank(),
                    ) {
                        Text("Up")
                    }
                    Text(
                        text = "Focus: /${focusPath.ifBlank { "" }}",
                        color = Color(0xFFDCEAF5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { copyToClipboard("/${focusPath.ifBlank { "" }}") },
                    ) {
                        Text("Copy")
                    }
                    Text(
                        text = "Visible PRs: ${visiblePrs.size}",
                        color = Color(0xFF9EC4DD),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF10232D))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Legend", color = Color(0xFF9EC4DD), style = MaterialTheme.typography.labelSmall)
                    // TODO: Remove the 20-item cap when legend virtualization/collapsing is implemented.
                    visiblePrs.take(20).forEach { pr ->
                        Surface(
                            color = Color(0xFF132A38),
                            border = androidx.compose.foundation.BorderStroke(2.dp, prColor(pr)),
                        ) {
                            Text(
                                text = "#${pr.number}",
                                color = Color(0xFFEAF2F8),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0B1117))
                        .onSizeChanged { canvasSize = it }
                        .onPointerEvent(PointerEventType.Move) { event ->
                            val position = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                            pointerPos = position
                            val dragging = event.buttons.isSecondaryPressed
                            if (dragging) {
                                val prev = dragPointerPos
                                if (prev != null) {
                                    pan += position - prev
                                }
                                dragPointerPos = position
                            } else {
                                dragPointerPos = null
                            }

                            val world = (position - pan) / zoom
                            hoveredNode = visibleNodes.asReversed().firstOrNull { it.rect.contains(world) }
                        }
                        .onPointerEvent(PointerEventType.Scroll) { event ->
                            val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
                            val factor = if (scrollY > 0f) 0.9f else 1.1f
                            val newZoom = (zoom * factor).coerceIn(0.4f, 8f)
                            val cursor = pointerPos
                            val world = (cursor - pan) / zoom
                            pan = cursor - world * newZoom
                            zoom = newZoom
                        }
                        .onPointerEvent(PointerEventType.Release) { event ->
                            dragPointerPos = null
                            val change = event.changes.firstOrNull() ?: return@onPointerEvent
                            if (event.button != PointerButton.Primary) return@onPointerEvent
                            val world = (change.position - pan) / zoom
                            val node = visibleNodes.asReversed().firstOrNull { it.rect.contains(world) } ?: return@onPointerEvent

                            if (!node.isDirectory) {
                                selectedPath = node.path
                                val related = visiblePrs.filter { pr -> pr.files.any { it.path == node.path } }.map { it.id }.toSet()
                                if (related.isNotEmpty()) selectedPrIds = selectedPrIds + related
                            }

                            val key = nodeKey(node)
                            val isDoubleClick = key == lastClickKey && (change.uptimeMillis - lastClickAt) < 350
                            if (isDoubleClick) {
                                if (node.isDirectory) {
                                    focusPath = node.path
                                    zoom = 1f
                                    pan = Offset.Zero
                                } else {
                                    openUrl("https://github.com/${SampleData.repoName}/blob/main/${node.path}")
                                }
                            }
                            lastClickKey = key
                            lastClickAt = change.uptimeMillis
                        },
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        visibleDirectories.forEach { node ->
                            val widthPx = node.rect.width * zoom
                            val heightPx = node.rect.height * zoom
                            if (widthPx < 1f || heightPx < 1f) return@forEach
                            val overlay = directoryOverlayByPath[node.path]
                            val hasConflict = (overlay?.prs?.size ?: 0) > 1
                            val rawTopLeft = node.rect.topLeft * zoom + pan
                            val rawSize = node.rect.size * zoom
                            val borderWidth = 8f
                            val inset = 1.5f
                            val topLeft = Offset(rawTopLeft.x + inset, rawTopLeft.y + inset)
                            val size = Size(
                                width = (rawSize.width - inset * 2f).coerceAtLeast(0f),
                                height = (rawSize.height - inset * 2f).coerceAtLeast(0f),
                            )
                            if (size.width <= 0f || size.height <= 0f) return@forEach

                            val fill = when (overlay?.dominantType) {
                                ChangeType.Addition -> Color(0xFF3CA65F)
                                ChangeType.Modification -> Color(0xFFD2A43F)
                                ChangeType.Deletion -> Color(0xFFCB4A44)
                                null -> Color(0xFF264155)
                            }
                            val alpha = if (overlay?.dominantType == null) 0.20f
                            else (0.18f + overlay.density * 0.78f).coerceIn(0.18f, 0.96f)

                            drawRect(
                                color = fill.copy(alpha = alpha),
                                topLeft = topLeft,
                                size = size,
                            )
                            drawPrBorder(
                                topLeft = topLeft,
                                size = size,
                                prs = overlay?.prs.orEmpty(),
                                fallback = Color(0xFF2F4A5F),
                                borderWidth = borderWidth,
                            )
                            if (hoveredNode?.path == node.path) {
                                drawRect(
                                    color = Color.White,
                                    topLeft = topLeft,
                                    size = size,
                                    style = Stroke(width = 2.5f),
                                )
                            }

                            if (hasConflict) {
                                clipRect(
                                    left = topLeft.x,
                                    top = topLeft.y,
                                    right = topLeft.x + size.width,
                                    bottom = topLeft.y + size.height,
                                ) {
                                    val spacing = 8f
                                    var x = topLeft.x - size.height
                                    while (x < topLeft.x + size.width) {
                                        drawLine(
                                            color = Color(0xAAFFE082),
                                            start = Offset(x, topLeft.y + size.height),
                                            end = Offset(x + size.height, topLeft.y),
                                            strokeWidth = 1f,
                                            cap = StrokeCap.Square,
                                        )
                                        x += spacing
                                    }
                                }
                            }
                        }

                        visibleFiles.forEach { node ->
                            val widthPx = node.rect.width * zoom
                            val heightPx = node.rect.height * zoom
                            val overlay = fileOverlayByPath[node.path]
                            if (widthPx < 2f || heightPx < 2f) {
                                if (node.hasActivePr) {
                                    drawCircle(
                                        color = Color(0xFFFFB800),
                                        radius = 1.5f,
                                        center = node.rect.center * zoom + pan,
                                    )
                                }
                                return@forEach
                            }

                            val fill = when (overlay?.dominantType) {
                                ChangeType.Addition -> Color(0xFF3CA65F)
                                ChangeType.Modification -> Color(0xFFD2A43F)
                                ChangeType.Deletion -> Color(0xFFCB4A44)
                                null -> Color(0xFF1C2A36)
                            }
                            val alpha = (0.18f + (overlay?.density ?: 0f) * 0.78f).coerceIn(0.18f, 0.96f)
                            val prs = overlay?.prs.orEmpty()

                            val rawTopLeft = node.rect.topLeft * zoom + pan
                            val rawSize = node.rect.size * zoom
                            val borderWidth = 8f
                            val inset = 1.5f
                            val topLeft = Offset(rawTopLeft.x + inset, rawTopLeft.y + inset)
                            val size = Size(
                                width = (rawSize.width - inset * 2f).coerceAtLeast(0f),
                                height = (rawSize.height - inset * 2f).coerceAtLeast(0f),
                            )
                            if (size.width <= 0f || size.height <= 0f) return@forEach
                            drawRect(color = fill.copy(alpha = alpha), topLeft = topLeft, size = size)
                            drawPrBorder(
                                topLeft = topLeft,
                                size = size,
                                prs = prs,
                                fallback = Color(0xFF2A3D4E),
                                borderWidth = borderWidth,
                            )
                            if (node.path == hoveredNode?.path || node.path == selectedPath) {
                                drawRect(
                                    color = Color.White,
                                    topLeft = topLeft,
                                    size = size,
                                    style = Stroke(width = 2.5f),
                                )
                            }

                            if (prs.size > 1) {
                                clipRect(
                                    left = topLeft.x,
                                    top = topLeft.y,
                                    right = topLeft.x + size.width,
                                    bottom = topLeft.y + size.height,
                                ) {
                                    val spacing = 8f
                                    var x = topLeft.x - size.height
                                    while (x < topLeft.x + size.width) {
                                        drawLine(
                                            color = Color(0xAAFFE082),
                                            start = Offset(x, topLeft.y + size.height),
                                            end = Offset(x + size.height, topLeft.y),
                                            strokeWidth = 1f,
                                            cap = StrokeCap.Square,
                                        )
                                        x += spacing
                                    }
                                }
                            }
                        }
                    }

                    visibleNodes.forEach { node ->
                        val widthPx = node.rect.width * zoom
                        val heightPx = node.rect.height * zoom
                        if (widthPx <= 90f || heightPx <= 24f) return@forEach
                        val label = if (node.isDirectory) "${node.name}/" else node.name
                        Text(
                            text = label,
                            color = if (node.isDirectory) Color(0xFFFFD37A) else Color(0xFFDAE8F3),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset {
                                    IntOffset(
                                        x = (node.rect.left * zoom + pan.x + 6f).toInt(),
                                        y = (node.rect.top * zoom + pan.y + 4f).toInt(),
                                    )
                                },
                        )
                    }

                    visibleFiles.forEach { node ->
                        val overlay = fileOverlayByPath[node.path] ?: return@forEach
                        val widthPx = node.rect.width * zoom
                        val heightPx = node.rect.height * zoom
                        if (widthPx <= 120f || heightPx <= 40f) return@forEach
                        val prText = overlay.prs.take(3).joinToString(" ") { "#${it.number}" }
                        val suffix = if (overlay.prs.size > 3) " +" else ""
                        Text(
                            text = "$prText$suffix",
                            color = Color(0xFFEAF2F8),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset {
                                    IntOffset(
                                        x = (node.rect.left * zoom + pan.x + 6f).toInt(),
                                        y = (node.rect.top * zoom + pan.y + 18f).toInt(),
                                    )
                                },
                        )
                    }

                    if (hoveredNode != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset {
                                    IntOffset(
                                        x = (pointerPos.x + 12f).toInt(),
                                        y = (pointerPos.y + 12f).toInt(),
                                    )
                                }
                                .border(1.dp, Color(0xFF3E5A72)),
                            color = Color(0xFF12212F),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(hoveredNode!!.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (hoveredNode!!.isDirectory) "Type: Directory" else "Type: File",
                                    color = Color(0xFFC5D8E7),
                                )
                                Text("Path: ${hoveredNode!!.path}", color = Color(0xFFC5D8E7))
                                Text("LOC: ${hoveredNode!!.totalLines}", color = Color(0xFFC5D8E7))
                                val prs = if (hoveredNode!!.isDirectory) hoveredDirOverlay?.prs.orEmpty() else hoveredOverlay?.prs.orEmpty()
                                Text(
                                    text = if (prs.isEmpty()) "PR: none" else "PR: ${prs.joinToString { "#${it.number} ${it.author}" }}",
                                    color = if (prs.size > 1) Color(0xFFFFD37A) else Color(0xFFC5D8E7),
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF18212B))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Open PRs", color = Color(0xFFE8F1F8), style = MaterialTheme.typography.titleLarge)
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = showDrafts, onCheckedChange = { showDrafts = it })
                    Text("Show draft", color = Color(0xFFD1DEEB))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Switch(checked = onlyMine, onCheckedChange = { onlyMine = it })
                    Text("Only my PRs", color = Color(0xFFD1DEEB))
                }
                HorizontalDivider(color = Color(0xFF2C3D4E))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filteredPrs, key = { it.id }) { pr ->
                        val checked = selectedPrIds.contains(pr.id)
                        val relatedToSelection = selectedPath != null && pr.files.any { it.path == selectedPath }
                        val listBorderColor = if (checked) prColor(pr) else prColor(pr).copy(alpha = 0.45f)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (relatedToSelection) 3.dp else 2.dp,
                                    color = listBorderColor,
                                    shape = MaterialTheme.shapes.medium,
                                )
                                .padding(8.dp),
                            color = if (pr.isDraft) Color(0xFF253748) else Color(0xFF203041),
                            onClick = { openUrl(pr.url) },
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedPrIds = if (isChecked) selectedPrIds + pr.id else selectedPrIds - pr.id
                                    },
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp, end = 8.dp)
                                        .width(12.dp)
                                        .fillMaxHeight(0.2f)
                                        .background(prColor(pr)),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "#${pr.number} ${pr.title}",
                                        color = Color(0xFFEAF2F8),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (relatedToSelection) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    Text(
                                        text = "${pr.author}${if (pr.isDraft) " • draft" else ""}",
                                        color = Color(0xFFB5C8D8),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    text = "Cmd+R: reset view  /  Cmd+F: clear search",
                    color = Color(0xFF8FA8BC),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun authorColor(author: String): Color {
    val palette = listOf(
        Color(0xFF4FC3F7),
        Color(0xFF81C784),
        Color(0xFFFF8A65),
        Color(0xFFBA68C8),
        Color(0xFFFFD54F),
        Color(0xFF90A4AE),
        Color(0xFF64B5F6),
        Color(0xFFA5D6A7),
        Color(0xFFFFB74D),
        Color(0xFFE57373),
        Color(0xFF4DB6AC),
        Color(0xFFF06292),
        Color(0xFF7986CB),
        Color(0xFFFFA726),
        Color(0xFF26A69A),
        Color(0xFFDCE775),
        Color(0xFF9575CD),
        Color(0xFFFF7043),
        Color(0xFF29B6F6),
        Color(0xFF8D6E63),
        Color(0xFF9CCC65),
        Color(0xFFFFCA28),
        Color(0xFF66BB6A),
        Color(0xFF42A5F5),
    )
    return palette[(author.hashCode().ushr(1)) % palette.size]
}

private fun prColor(pr: PullRequest): Color {
    return authorColor("${pr.author}:${pr.number}")
}

private fun findDirectory(root: FileNode.Directory, path: String): FileNode.Directory? {
    if (path.isBlank()) return root
    if (root.path == path) return root
    root.children.forEach { child ->
        if (child is FileNode.Directory) {
            val found = findDirectory(child, path)
            if (found != null) return found
        }
    }
    return null
}

private fun parentPathOf(path: String): String {
    if (path.isBlank()) return ""
    return path.substringBeforeLast('/', missingDelimiterValue = "")
}

private fun nodeKey(node: TreemapNode): String {
    return if (node.isDirectory) "D:${node.path}" else "F:${node.path}"
}

private fun DrawScope.drawPrBorder(
    topLeft: Offset,
    size: Size,
    prs: List<PullRequest>,
    fallback: Color,
    borderWidth: Float,
) {
    val uniquePrs = prs.distinctBy { it.id }
    when {
        uniquePrs.isEmpty() -> {
            drawRect(
                color = fallback,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = borderWidth),
            )
        }
        uniquePrs.size == 1 -> {
            drawRect(
                color = prColor(uniquePrs.first()),
                topLeft = topLeft,
                size = size,
                style = Stroke(width = borderWidth),
            )
        }
        else -> {
            drawDashedMulticolorRectBorder(
                topLeft = topLeft,
                size = size,
                colors = uniquePrs.map(::prColor),
                strokeWidth = borderWidth,
                dashLength = 14f,
                gapLength = 8f,
            )
        }
    }
}

private fun DrawScope.drawDashedMulticolorRectBorder(
    topLeft: Offset,
    size: Size,
    colors: List<Color>,
    strokeWidth: Float,
    dashLength: Float,
    gapLength: Float,
) {
    if (colors.isEmpty()) return
    val perimeter = (size.width + size.height) * 2f
    if (perimeter <= 0f) return

    var cursor = 0f
    var dashIndex = 0
    while (cursor < perimeter) {
        val dashStart = cursor
        val dashEnd = (cursor + dashLength).coerceAtMost(perimeter)
        val color = colors[dashIndex % colors.size]
        drawPerimeterSegment(
            topLeft = topLeft,
            size = size,
            startDistance = dashStart,
            endDistance = dashEnd,
            color = color,
            strokeWidth = strokeWidth,
        )
        dashIndex += 1
        cursor += dashLength + gapLength
    }
}

private fun DrawScope.drawPerimeterSegment(
    topLeft: Offset,
    size: Size,
    startDistance: Float,
    endDistance: Float,
    color: Color,
    strokeWidth: Float,
) {
    val breaks = listOf(
        0f,
        size.width,
        size.width + size.height,
        size.width * 2f + size.height,
        size.width * 2f + size.height * 2f,
    )

    var segmentStart = startDistance
    while (segmentStart < endDistance) {
        val edgeEnd = breaks.first { it > segmentStart }
        val segmentEnd = minOf(endDistance, edgeEnd)
        val from = pointOnRectPerimeter(topLeft, size, segmentStart)
        val to = pointOnRectPerimeter(topLeft, size, segmentEnd)
        drawLine(
            color = color,
            start = from,
            end = to,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Butt,
        )
        segmentStart = segmentEnd
    }
}

private fun pointOnRectPerimeter(topLeft: Offset, size: Size, distance: Float): Offset {
    val w = size.width
    val h = size.height
    val p = (2f * (w + h)).coerceAtLeast(1f)
    val d = ((distance % p) + p) % p

    return when {
        d <= w -> Offset(topLeft.x + d, topLeft.y)
        d <= w + h -> Offset(topLeft.x + w, topLeft.y + (d - w))
        d <= 2f * w + h -> Offset(topLeft.x + (2f * w + h - d), topLeft.y + h)
        else -> Offset(topLeft.x, topLeft.y + (p - d))
    }
}

private fun buildExplorerRows(root: FileNode.Directory): List<ExplorerRow> {
    val rows = mutableListOf<ExplorerRow>()

    fun visit(node: FileNode, depth: Int) {
        rows += ExplorerRow(
            path = node.path,
            name = if (node.path.isBlank()) "repo" else node.name,
            depth = depth,
            isDirectory = node is FileNode.Directory,
        )
        if (node is FileNode.Directory) {
            node.children
                .sortedWith(
                    compareBy<FileNode> { it !is FileNode.Directory }
                        .thenBy { it.name.lowercase() },
                )
                .forEach { child ->
                    visit(child, depth + 1)
                }
        }
    }

    visit(root, depth = 0)
    return rows
}

private fun openUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}

private fun copyToClipboard(text: String) {
    runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }
}
