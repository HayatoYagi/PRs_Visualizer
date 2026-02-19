package io.github.hayatoyagi.prvisualizer.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun AuthRow(
    oauthClientId: String,
    isAuthorizing: Boolean,
    isConnecting: Boolean,
    isLoggedIn: Boolean,
    currentUser: String,
    deviceUserCode: String?,
    deviceVerificationUrl: String?,
    connectionError: AppError?,
    hasSnapshot: Boolean,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onCopyDeviceCode: () -> Unit,
    onOpenVerifyPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.backgroundHeader)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!isLoggedIn) {
            Button(
                enabled = !isAuthorizing && oauthClientId.isNotBlank(),
                onClick = onLogin,
            ) {
                Text(if (isAuthorizing) "Authorizing..." else "Login with GitHub")
            }
        }
        Button(
            enabled = !isConnecting && isLoggedIn,
            onClick = onRefresh,
        ) {
            Text(if (isConnecting) "Refreshing..." else "Refresh")
        }
        if (oauthClientId.isBlank()) {
            Text(
                text = "Missing GITHUB_CLIENT_ID in .env",
                color = AppColors.textWarning,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
        if (deviceUserCode != null && deviceVerificationUrl != null) {
            SelectionContainer {
                Text(
                    text = "GitHub code: $deviceUserCode @ $deviceVerificationUrl",
                    color = AppColors.textDeviceCode,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            Button(onClick = onCopyDeviceCode) { Text("Copy Code") }
            Button(onClick = onOpenVerifyPage) { Text("Open Verify Page") }
        }
        Text(
            text = if (!hasSnapshot) {
                if (!isLoggedIn) {
                    "Not connected (not logged in)"
                } else {
                    "Logged in as: $currentUser (not connected)"
                }
            } else {
                "Logged in as: $currentUser"
            },
            color = AppColors.textSecondary,
            modifier = Modifier.padding(top = 14.dp),
        )
        if (connectionError != null) {
            val (color, text) = when (connectionError) {
                is AppError.AuthExpired -> AppColors.textWarning to connectionError.message
                is AppError.Network -> AppColors.textError to "Network error: ${connectionError.message}"
                is AppError.ApiError -> AppColors.textError to "GitHub error ${connectionError.statusCode}: ${connectionError.message}"
                is AppError.OAuthFailed -> AppColors.textError to "OAuth failed: ${connectionError.message}"
                is AppError.Unknown -> AppColors.textError to "Error: ${connectionError.message}"
            }
            SelectionContainer {
                Text(
                    text = text,
                    color = color,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}
