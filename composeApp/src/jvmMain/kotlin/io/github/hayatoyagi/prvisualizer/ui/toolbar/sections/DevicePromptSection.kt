package io.github.hayatoyagi.prvisualizer.ui.toolbar.sections

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.AuthState
import io.github.hayatoyagi.prvisualizer.ui.shared.TooltipIconButton
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun DevicePromptSection(
    devicePrompt: AuthState.Authorizing?,
    toolbarTextStyle: TextStyle,
) {
    val prompt = devicePrompt ?: return

    SelectionContainer {
        Text(
            text = "Code: ${prompt.deviceUserCode} @ ${prompt.deviceVerificationUrl}",
            color = AppColors.textDeviceCode,
            style = toolbarTextStyle,
        )
    }
    TooltipIconButton(
        tooltip = "Copy Code",
        onClick = { copyToClipboard(prompt.deviceUserCode.orEmpty()) },
    ) {
        Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = "Copy Code",
            tint = AppColors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
    TooltipIconButton(
        tooltip = "Open Verify Page",
        onClick = { openUrl(prompt.deviceVerificationUrl.orEmpty()) },
    ) {
        Icon(
            imageVector = Icons.Filled.OpenInBrowser,
            contentDescription = "Open Verify Page",
            tint = AppColors.textPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}
