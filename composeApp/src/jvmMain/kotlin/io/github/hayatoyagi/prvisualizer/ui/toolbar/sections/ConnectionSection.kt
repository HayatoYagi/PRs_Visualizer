package io.github.hayatoyagi.prvisualizer.ui.toolbar.sections

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun ConnectionSection(
    statusText: String,
    toolbarTextStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = statusText,
        color = AppColors.textSecondary,
        style = toolbarTextStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
