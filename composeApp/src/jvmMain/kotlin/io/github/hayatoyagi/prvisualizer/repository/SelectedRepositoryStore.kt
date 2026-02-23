package io.github.hayatoyagi.prvisualizer.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

class InMemorySelectedRepositoryStore(
    initial: RepoState = RepoState.Unselected,
) : SelectedRepositoryStore {
    private val mutableRepoState = MutableStateFlow(initial)

    override val repoState: StateFlow<RepoState> = mutableRepoState.asStateFlow()

    override fun select(owner: String, repo: String) {
        mutableRepoState.value = RepoState.from(owner = owner, repo = repo)
    }

    override fun unselect() {
        mutableRepoState.value = RepoState.Unselected
    }
}
