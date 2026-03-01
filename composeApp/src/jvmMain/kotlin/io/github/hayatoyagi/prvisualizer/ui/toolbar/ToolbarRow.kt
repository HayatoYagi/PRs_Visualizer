package io.github.hayatoyagi.prvisualizer.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ui.icons.CustomIcons
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun ToolbarRow(
    owner: String,
    repo: String,
    isLoggedIn: Boolean,
    onOpenRepoDialog: () -> Unit,
    onShuffleColors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.backgroundHeader)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Repository: ${owner.trim()}/${repo.trim()}",
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            enabled = isLoggedIn,
            onClick = onShuffleColors,
        ) {
            Icon(
                imageVector = CustomIcons.Shuffle,
                contentDescription = "Shuffle Colors",
                tint = if (isLoggedIn) AppColors.textPrimary else AppColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
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
