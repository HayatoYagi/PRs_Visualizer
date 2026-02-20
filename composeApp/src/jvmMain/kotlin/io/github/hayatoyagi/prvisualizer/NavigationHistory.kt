package io.github.hayatoyagi.prvisualizer

/**
 * Tracks navigation history for back/forward functionality.
 * History is reset when the repository changes or the application restarts.
 */
class NavigationHistory {
    private val history = mutableListOf<String>()
    private var currentIndex = -1

    /**
     * Records a new focus path in the history.
     * When a new path is recorded, any forward history is cleared.
     */
    fun recordFocusPath(path: String) {
        // Don't record if it's the same as the current path
        if (currentIndex >= 0 && currentIndex < history.size && history[currentIndex] == path) {
            return
        }

        // Remove any forward history
        if (currentIndex < history.size - 1) {
            history.subList(currentIndex + 1, history.size).clear()
        }

        // Add the new path
        history.add(path)
        currentIndex = history.size - 1
    }

    /**
     * Navigates back in history and returns the previous focus path.
     * Returns null if there's no previous history.
     */
    fun navigateBack(): String? {
        if (!canNavigateBack()) return null
        currentIndex--
        return history[currentIndex]
    }

    /**
     * Navigates forward in history and returns the next focus path.
     * Returns null if there's no forward history.
     */
    fun navigateForward(): String? {
        if (!canNavigateForward()) return null
        currentIndex++
        return history[currentIndex]
    }

    /**
     * Returns true if there's previous history to navigate back to.
     */
    fun canNavigateBack(): Boolean = currentIndex > 0

    /**
     * Returns true if there's forward history to navigate to.
     */
    fun canNavigateForward(): Boolean = currentIndex < history.size - 1

    /**
     * Clears all history. Called when the repository changes.
     */
    fun clear() {
        history.clear()
        currentIndex = -1
    }

    /**
     * Returns the current focus path, or null if history is empty.
     */
    fun currentPath(): String? = if (currentIndex >= 0 && currentIndex < history.size) {
        history[currentIndex]
    } else {
        null
    }
}
