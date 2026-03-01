package io.github.hayatoyagi.prvisualizer.repository.store

import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.storage.FileLocalStorage
import io.github.hayatoyagi.prvisualizer.storage.LocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PersistedSelectedRepositoryStore(
    private val localStorage: LocalStorage = FileLocalStorage(appName = "PRsVisualizerForGitHub"),
) : SelectedRepositoryStore {
    private val mutableRepoState = MutableStateFlow(
        localStorage.getString(LAST_REPOSITORY_KEY)
            ?.toRepoStateOrNull()
            ?: RepoState.Unselected,
    )

    override val repoState: StateFlow<RepoState> = mutableRepoState.asStateFlow()

    override fun select(owner: String, repo: String) {
        val nextState = RepoState.from(owner = owner, repo = repo)
        mutableRepoState.value = nextState
        if (nextState is RepoState.Selected) {
            localStorage.putString(LAST_REPOSITORY_KEY, "${nextState.owner}/${nextState.repo}")
        }
    }

    override fun unselect() {
        mutableRepoState.value = RepoState.Unselected
        localStorage.remove(LAST_REPOSITORY_KEY)
    }

    private fun String.toRepoStateOrNull(): RepoState? {
        val parts = split('/')
        if (parts.size != 2) return null
        return RepoState.from(owner = parts[0], repo = parts[1]).takeIf { it is RepoState.Selected }
    }

    private companion object {
        const val LAST_REPOSITORY_KEY = "last_repository"
    }
}
