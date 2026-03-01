package io.github.hayatoyagi.prvisualizer.ui.treemap.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
internal fun TreemapPaneHeader(
    focusPath: String,
    visiblePrCount: Int,
    canZoomOut: Boolean,
    canZoomIn: Boolean,
    onFocusPathChange: (String) -> Unit,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.backgroundHeader)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = { onFocusPathChange("") }) {
            Text("Root")
        }
        Button(
            onClick = { onFocusPathChange(parentPathOf(focusPath)) },
            enabled = focusPath.isNotBlank(),
        ) {
            Text("Up")
        }
        Text(
            text = "Focus: /${focusPath.ifBlank { "" }}",
            color = AppColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { copyToClipboard("/${focusPath.ifBlank { "" }}") }) {
            Text("Copy")
        }
        Button(onClick = onZoomOut, enabled = canZoomOut) {
            Text("-")
        }
        Button(onClick = onZoomIn, enabled = canZoomIn) {
            Text("+")
        }
        Text(
            text = "Visible PRs: $visiblePrCount",
            color = AppColors.textSecondary,
        )
    }
}
