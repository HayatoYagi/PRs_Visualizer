package io.github.hayatoyagi.prvisualizer.ui.toolbar.sections

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun ConnectionSection(
    connectionError: AppError?,
    statusText: String,
    toolbarTextStyle: TextStyle,
) {
    if (connectionError != null) {
        val (color, text) = connectionErrorPresentation(connectionError)
        SelectionContainer {
            Text(
                text = text,
                color = color,
                style = toolbarTextStyle,
            )
        }
        return
    }

    Text(
        text = statusText,
        color = AppColors.textSecondary,
        style = toolbarTextStyle,
    )
}

private fun connectionErrorPresentation(error: AppError): Pair<Color, String> = when (error) {
    is AppError.AuthExpired -> AppColors.textWarning to error.message
    is AppError.Network -> AppColors.textError to "Network error: ${error.message}"
    is AppError.ApiError -> AppColors.textError to "GitHub error ${error.statusCode}: ${error.message}"
    is AppError.OAuthFailed -> AppColors.textError to "OAuth failed: ${error.message}"
    is AppError.Unknown -> AppColors.textError to "Error: ${error.message}"
}
