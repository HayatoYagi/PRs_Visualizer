package io.github.hayatoyagi.prvisualizer.repository.store

import io.github.hayatoyagi.prvisualizer.storage.FileLocalStorage
import io.github.hayatoyagi.prvisualizer.storage.LocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PersistedFavoriteReposStore(
    private val localStorage: LocalStorage = FileLocalStorage(appName = "PRsVisualizerForGitHub"),
) : FavoriteReposStore {
    private val mutableFavorites = MutableStateFlow(loadFavorites())

    override val favorites: StateFlow<Set<String>> = mutableFavorites.asStateFlow()

    override fun toggleFavorite(fullName: String) {
        val current = mutableFavorites.value
        val updated = if (current.contains(fullName)) current - fullName else current + fullName
        mutableFavorites.value = updated
        persist(updated)
    }

    private fun loadFavorites(): Set<String> {
        val stored = localStorage.getString(FAVORITES_KEY) ?: return emptySet()
        return stored.split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun persist(favorites: Set<String>) {
        if (favorites.isEmpty()) {
            localStorage.remove(FAVORITES_KEY)
        } else {
            localStorage.putString(FAVORITES_KEY, favorites.sorted().joinToString(","))
        }
    }

    private companion object {
        const val FAVORITES_KEY = "favorite_repositories"
    }
}
