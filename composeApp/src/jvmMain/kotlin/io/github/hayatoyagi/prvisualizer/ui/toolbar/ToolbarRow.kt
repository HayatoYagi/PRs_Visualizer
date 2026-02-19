package io.github.hayatoyagi.prvisualizer.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun ToolbarRow(
    owner: String,
    repo: String,
    isLoggedIn: Boolean,
    onOpenRepoDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.backgroundHeader)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Repository: ${owner.trim()}/${repo.trim()}",
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f).padding(top = 12.dp),
        )
        Button(
            enabled = isLoggedIn,
            onClick = onOpenRepoDialog,
        ) {
            Text("Select Repo")
        }
    }
}
