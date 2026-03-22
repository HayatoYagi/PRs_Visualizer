package io.github.hayatoyagi.prvisualizer.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.hayatoyagi.prvisualizer.ui.shared.copyToClipboard
import io.github.hayatoyagi.prvisualizer.ui.shared.openUrl
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun DeviceFlowDialog(
    userCode: String,
    verificationUrl: String,
    browserOpenedAutomatically: Boolean,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.backgroundPane,
        titleContentColor = AppColors.textPaneTitle,
        textContentColor = AppColors.textBody,
        title = { Text("Sign in with GitHub") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. Open the GitHub verification page.")
                OutlinedButton(
                    onClick = { openUrl(verificationUrl) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.textPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Sign-in Page")
                }
                Text("2. Enter this code on the GitHub page.")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = userCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = AppColors.textDeviceCode,
                    )
                    OutlinedButton(
                        onClick = { copyToClipboard(userCode) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.textPrimary,
                        ),
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy Code")
                    }
                }
                Text(
                    "The code has already been copied to your clipboard. " +
                        "If needed, use the button above to copy it again.",
                    color = AppColors.textBodyMuted,
                )
                Text(
                    "3. Authorize the app in your browser. " +
                        "This app will continue automatically after GitHub completes sign-in.",
                )
                if (!browserOpenedAutomatically) {
                    Text(
                        "Your browser could not be opened automatically in this environment.",
                        color = AppColors.textBodyMuted,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.textPrimary,
                ),
            ) {
                Text("Dismiss")
            }
        },
    )
}
