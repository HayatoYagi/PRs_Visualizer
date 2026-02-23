package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.repository.SelectedRepositoryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PersistedSelectedRepositoryStore(
    private val repositorySelectionStore: RepositorySelectionStore,
) : SelectedRepositoryStore {
    private val mutableRepoState = MutableStateFlow(
        repositorySelectionStore.load()
            ?.let { RepoState.from(owner = it.first, repo = it.second) }
            ?: RepoState.Unselected,
    )

    override val repoState: StateFlow<RepoState> = mutableRepoState.asStateFlow()

    override fun select(owner: String, repo: String) {
        val nextState = RepoState.from(owner = owner, repo = repo)
        mutableRepoState.value = nextState
        if (nextState is RepoState.Selected) {
            repositorySelectionStore.save(owner = nextState.owner, repo = nextState.repo)
        }
    }

    override fun unselect() {
        mutableRepoState.value = RepoState.Unselected
    }
}
