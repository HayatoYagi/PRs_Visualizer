package io.github.hayatoyagi.prvisualizer.repository.store

import io.github.hayatoyagi.prvisualizer.repository.RepoState
import kotlinx.coroutines.flow.StateFlow

/**
 * Source of truth for the currently selected repository.
 *
 * All mutations must go through [io.github.hayatoyagi.prvisualizer.VisualizerViewModel]
 * so that dependent UI state (filter, navigation, snapshot, etc.) is reset consistently.
 * Do not mutate this store directly from outside the ViewModel.
 */
interface SelectedRepositoryStore {
    val repoState: StateFlow<RepoState>

    /**
     * Selects a repository by owner and repo name.
     * Blank values are normalized to [RepoState.Unselected].
     */
    fun select(owner: String, repo: String)

    /** Clears the current selection, transitioning to [RepoState.Unselected]. */
    fun unselect()
}
