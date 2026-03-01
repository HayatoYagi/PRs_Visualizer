package io.github.hayatoyagi.prvisualizer.ui.treemap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ui.shared.TooltipIconButton
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapCanvas
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapLegend
import io.github.hayatoyagi.prvisualizer.ui.treemap.TreemapOverlay
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.LEGEND_PADDING_DP
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.LOADING_OVERLAY_ALPHA
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.treemapMoveHandler
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.treemapReleaseHandler
import io.github.hayatoyagi.prvisualizer.ui.treemap.handlers.treemapScrollHandler
import io.github.hayatoyagi.prvisualizer.ui.treemap.models.TreemapViewportCallbacks
import io.github.hayatoyagi.prvisualizer.ui.treemap.models.TreemapViewportModel

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun TreemapViewport(
    model: TreemapViewportModel,
    isLoading: Boolean,
    canZoomOut: Boolean,
    canZoomIn: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    callbacks: TreemapViewportCallbacks,
) {
    var legendBounds by remember { mutableStateOf<Rect?>(null) }
    var zoomControlBounds by remember { mutableStateOf<Rect?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(AppColors.backgroundCanvas)
            .onSizeChanged(callbacks.onSizeChanged)
            .treemapMoveHandler(isLoading = isLoading, onMoveEvent = callbacks.onMoveEvent)
            .treemapScrollHandler(isLoading = isLoading, onScrollEvent = callbacks.onScrollEvent)
            .treemapReleaseHandler(
                isLoading = isLoading,
                onReleaseEvent = { position, uptimeMillis ->
                    if (legendBounds?.contains(position) == true) return@treemapReleaseHandler
                    if (zoomControlBounds?.contains(position) == true) return@treemapReleaseHandler
                    callbacks.onReleaseEvent(position, uptimeMillis)
                },
            ),
    ) {
        TreemapCanvas(
            visibleDirectories = model.visibleDirectories,
            visibleFiles = model.visibleFiles,
            directoryOverlayByPath = model.directoryOverlayByPath,
            fileOverlayByPath = model.fileOverlayByPath,
            prColorMap = model.prColorMap,
            hoveredNode = model.hoveredNode,
            selectedPath = model.selectedPath,
            zoom = model.zoom,
            pan = model.pan,
        )
        TreemapOverlay(
            visibleNodes = model.visibleNodes,
            visibleFiles = model.visibleFiles,
            fileOverlayByPath = model.fileOverlayByPath,
            hoveredNode = model.hoveredNode,
            hoveredOverlay = model.hoveredOverlay,
            hoveredDirOverlay = model.hoveredDirOverlay,
            zoom = model.zoom,
            pan = model.pan,
            pointerPos = model.pointerPos,
        )
        TreemapLegend(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(LEGEND_PADDING_DP.dp)
                .onGloballyPositioned { coordinates ->
                    legendBounds = coordinates.boundsInParent()
                },
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(LEGEND_PADDING_DP.dp)
                .padding(4.dp)
                .onGloballyPositioned { coordinates ->
                    zoomControlBounds = coordinates.boundsInParent()
                },
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = AppColors.backgroundHeader.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp),
                    ),
            ) {
                TooltipIconButton(
                    tooltip = "Zoom in",
                    enabled = canZoomIn,
                    onClick = onZoomIn,
                ) {
                    Text("+", color = AppColors.textPrimary)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = AppColors.backgroundHeader.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(8.dp),
                    ),
            ) {
                TooltipIconButton(
                    tooltip = "Zoom out",
                    enabled = canZoomOut,
                    onClick = onZoomOut,
                ) {
                    Text("−", color = AppColors.textPrimary)
                }
            }
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.backgroundCanvas.copy(alpha = LOADING_OVERLAY_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppColors.textPrimary)
            }
        }
    }
}
