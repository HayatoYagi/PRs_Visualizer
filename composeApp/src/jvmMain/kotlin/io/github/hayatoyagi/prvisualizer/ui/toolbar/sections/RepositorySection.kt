package io.github.hayatoyagi.prvisualizer.ui.toolbar.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ui.shared.TooltipIconButton
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun RepositorySection(
    isConnecting: Boolean,
    isLoggedIn: Boolean,
    repoFullName: String,
    toolbarTextStyle: TextStyle,
    onRefresh: () -> Unit,
    onOpenRepoDialog: () -> Unit,
) {
    val isRefreshEnabled = !isConnecting && isLoggedIn
    val refreshLabel = if (isConnecting) "Refreshing..." else "Refresh Repository"

    TooltipIconButton(
        tooltip = refreshLabel,
        enabled = isRefreshEnabled,
        onClick = onRefresh,
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = if (isConnecting) "Refreshing..." else "Refresh",
            tint = if (isRefreshEnabled) AppColors.textPrimary else AppColors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = repoFullName,
            color = AppColors.textPrimary,
            style = toolbarTextStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TooltipIconButton(
            tooltip = "Select Repository",
            enabled = isLoggedIn,
            onClick = onOpenRepoDialog,
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = "Select Repo",
                tint = if (isLoggedIn) AppColors.textPrimary else AppColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
