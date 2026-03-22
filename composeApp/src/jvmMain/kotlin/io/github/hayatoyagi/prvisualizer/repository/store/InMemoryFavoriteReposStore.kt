package io.github.hayatoyagi.prvisualizer.repository.store

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryFavoriteReposStore(
    initial: Set<String> = emptySet(),
) : FavoriteReposStore {
    private val mutableFavorites = MutableStateFlow(initial)

    override val favorites: StateFlow<Set<String>> = mutableFavorites.asStateFlow()

    override fun toggleFavorite(fullName: String) {
        val current = mutableFavorites.value
        mutableFavorites.value = if (current.contains(fullName)) current - fullName else current + fullName
    }
}
