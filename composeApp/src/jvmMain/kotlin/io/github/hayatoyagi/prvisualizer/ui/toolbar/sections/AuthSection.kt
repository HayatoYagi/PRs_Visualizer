package io.github.hayatoyagi.prvisualizer.ui.toolbar.sections

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.ui.shared.TooltipIconButton
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

@Composable
fun AuthSection(
    isLoggedIn: Boolean,
    isAuthorizing: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    if (!isLoggedIn) {
        val isEnabled = !isAuthorizing
        val label = if (isAuthorizing) "Authorizing..." else "Login with GitHub"
        TooltipIconButton(
            tooltip = label,
            enabled = isEnabled,
            onClick = onLogin,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = label,
                tint = if (isEnabled) AppColors.textPrimary else AppColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    } else {
        val label = "Logout"
        TooltipIconButton(
            tooltip = label,
            enabled = true,
            onClick = onLogout,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = label,
                tint = AppColors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
