package io.github.hayatoyagi.prvisualizer.navigation

import io.github.hayatoyagi.prvisualizer.NavigationState
import io.github.hayatoyagi.prvisualizer.resetNavigation
import io.github.hayatoyagi.prvisualizer.resetViewport
import io.github.hayatoyagi.prvisualizer.ui.shared.parentPathOf

/**
 * Manages navigation history, viewport, and directory focus.
 * Extracts navigation-related responsibilities from VisualizerViewModel.
 */
class NavigationManager(
    private var navigationState: NavigationState = NavigationState(),
    private val onStateChanged: (NavigationState) -> Unit,
) {
    private val navigationHistory = NavigationHistory()

    init {
        // Initialize history with the current focus path
        navigationHistory.recordFocusPath(navigationState.focusPath)
    }

    /**
     * Selects a directory and records it in navigation history.
     * Auto-expands the selected directory and its ancestor chain.
     */
    fun selectDirectory(path: String) {
        navigationHistory.recordFocusPath(path)
        val explorerState = navigationState.explorerState
        val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, path)
        navigationState = navigationState.copy(
            focusPath = path,
            viewportResetToken = navigationState.viewportResetToken + 1,
            explorerState = explorerState.copy(expandedPaths = expandedPaths),
        )
        onStateChanged(navigationState)
    }

    /**
     * Selects a file and records its parent directory in navigation history.
     * Auto-expands parent directories so the file is visible.
     */
    fun selectFile(path: String) {
        val parentPath = parentPathOf(path)
        navigationHistory.recordFocusPath(parentPath)
        val explorerState = navigationState.explorerState
        val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, parentPath)
        navigationState = navigationState.copy(
            selectedPath = path,
            focusPath = parentPath,
            viewportResetToken = navigationState.viewportResetToken + 1,
            explorerState = explorerState.copy(expandedPaths = expandedPaths),
        )
        onStateChanged(navigationState)
    }

    /**
     * Changes the focus path and records it in navigation history.
     * Auto-expands the focused directory and its ancestor chain.
     */
    fun changeFocusPath(path: String) {
        navigationHistory.recordFocusPath(path)
        val explorerState = navigationState.explorerState
        val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, path)
        navigationState = navigationState.copy(
            focusPath = path,
            viewportResetToken = navigationState.viewportResetToken + 1,
            explorerState = explorerState.copy(expandedPaths = expandedPaths),
        )
        onStateChanged(navigationState)
    }

    /**
     * Updates only the selected path without affecting focus or history.
     */
    fun updateSelectedPath(path: String?) {
        navigationState = navigationState.copy(selectedPath = path)
        onStateChanged(navigationState)
    }

    /**
     * Resets navigation to the root, clearing paths and history.
     */
    fun resetNavigation() {
        navigationState = navigationState.resetNavigation()
        onStateChanged(navigationState)
        navigationHistory.clear()
        navigationHistory.recordFocusPath(navigationState.focusPath)
    }

    /**
     * Resets the viewport by incrementing the reset token.
     */
    fun resetViewport() {
        navigationState = navigationState.resetViewport()
        onStateChanged(navigationState)
    }

    /**
     * Toggles the expanded state of a directory.
     */
    fun toggleDirectoryExpanded(path: String) {
        val explorerState = navigationState.explorerState
        val expandedPaths = explorerState.expandedPaths
        val newExpandedPaths = if (expandedPaths.contains(path)) {
            expandedPaths - path
        } else {
            expandedPaths + path
        }
        navigationState = navigationState.copy(
            explorerState = explorerState.copy(expandedPaths = newExpandedPaths),
        )
        onStateChanged(navigationState)
    }

    /**
     * Navigates back in history. Returns true if navigation occurred.
     */
    fun navigateBack(): Boolean {
        val previousPath = navigationHistory.navigateBack()
        return if (previousPath != null) {
            val explorerState = navigationState.explorerState
            val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, previousPath)
            navigationState = navigationState.copy(
                focusPath = previousPath,
                viewportResetToken = navigationState.viewportResetToken + 1,
                explorerState = explorerState.copy(expandedPaths = expandedPaths),
            )
            onStateChanged(navigationState)
            true
        } else {
            false
        }
    }

    /**
     * Navigates forward in history. Returns true if navigation occurred.
     */
    fun navigateForward(): Boolean {
        val nextPath = navigationHistory.navigateForward()
        return if (nextPath != null) {
            val explorerState = navigationState.explorerState
            val expandedPaths = expandPathAndAncestors(explorerState.expandedPaths, nextPath)
            navigationState = navigationState.copy(
                focusPath = nextPath,
                viewportResetToken = navigationState.viewportResetToken + 1,
                explorerState = explorerState.copy(expandedPaths = expandedPaths),
            )
            onStateChanged(navigationState)
            true
        } else {
            false
        }
    }

    /**
     * Clears navigation history. Called when the repository changes.
     */
    fun clearHistory() {
        navigationHistory.clear()
        navigationHistory.recordFocusPath(navigationState.focusPath)
    }

    /**
     * Expands a path and all its ancestors in the explorer.
     */
    private fun expandPathAndAncestors(
        expandedPaths: Set<String>,
        path: String,
    ): Set<String> {
        if (path.isBlank()) return expandedPaths

        var currentPath = ""
        var newExpandedPaths = expandedPaths
        for (segment in path.split('/')) {
            if (segment.isBlank()) continue
            currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"
            if (!newExpandedPaths.contains(currentPath)) {
                newExpandedPaths = newExpandedPaths + currentPath
            }
        }
        return newExpandedPaths
    }

    /**
     * Updates the internal navigation state. Called by ViewModel when state changes externally.
     */
    fun updateState(newNavigationState: NavigationState) {
        navigationState = newNavigationState
    }
}
