package io.github.hayatoyagi.prvisualizer.repository.store

import io.github.hayatoyagi.prvisualizer.repository.RepoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
