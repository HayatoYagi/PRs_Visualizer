package io.github.hayatoyagi.prvisualizer.ui.repo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.hayatoyagi.prvisualizer.RepoSelectionState
import io.github.hayatoyagi.prvisualizer.ui.theme.AppColors

/**
 * Displays a dialog for selecting a repository.
 *
 * @param initialQuery Initial search query text
 * @param repoSelectionState The current state of repository selection
 * @param onReload Callback to reload the repository list
 * @param onDismiss Callback when the dialog is dismissed
 * @param onSelect Callback when a repository is selected
 */
@Composable
fun RepoPickerDialog(
    initialQuery: String,
    repoSelectionState: RepoSelectionState,
    onReload: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val options = when (repoSelectionState) {
        RepoSelectionState.Idle,
        RepoSelectionState.Loading,
        -> emptyList()
        is RepoSelectionState.Ready -> repoSelectionState.options
        is RepoSelectionState.Error -> repoSelectionState.options
    }
    val isLoading = repoSelectionState is RepoSelectionState.Loading
    val loadingError = (repoSelectionState as? RepoSelectionState.Error)?.error

    var query by rememberSaveable { mutableStateOf(initialQuery) }
    val filteredOptions = remember(options, query) {
        filterRepoOptions(options, query)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Repository") },
        containerColor = AppColors.backgroundPane,
        titleContentColor = AppColors.textPaneTitle,
        textContentColor = AppColors.textBody,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search owner/repo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppColors.backgroundPaneList,
                        unfocusedContainerColor = AppColors.backgroundPaneList,
                        disabledContainerColor = AppColors.backgroundPaneList,
                        focusedTextColor = AppColors.textPrimary,
                        unfocusedTextColor = AppColors.textPrimary,
                        focusedLabelColor = AppColors.textSecondary,
                        unfocusedLabelColor = AppColors.textSecondary,
                        cursorColor = AppColors.textPrimary,
                    ),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onReload) {
                        Text(if (isLoading) "Loading..." else "Reload")
                    }
                    Text(
                        text = "${filteredOptions.size} results",
                        color = AppColors.textSecondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (loadingError != null) {
                    Text(
                        text = loadingError.message,
                        color = AppColors.textWarning,
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 360.dp)
                        .background(AppColors.backgroundPaneList, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    items(filteredOptions) { fullName ->
                        Text(
                            text = fullName,
                            color = AppColors.textRepoOption,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(fullName) }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = AppColors.textPrimary) }
        },
    )
}
