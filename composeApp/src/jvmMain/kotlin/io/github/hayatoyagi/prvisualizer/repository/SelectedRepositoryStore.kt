package io.github.hayatoyagi.prvisualizer.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SelectedRepositoryStore {
    val repoState: StateFlow<RepoState>

    fun select(owner: String, repo: String)
}

class InMemorySelectedRepositoryStore(
    initial: RepoState = RepoState.Unselected,
) : SelectedRepositoryStore {
    private val mutableRepoState = MutableStateFlow(initial)

    override val repoState: StateFlow<RepoState> = mutableRepoState.asStateFlow()

    override fun select(owner: String, repo: String) {
        mutableRepoState.value = RepoState.from(owner = owner, repo = repo)
    }
}
