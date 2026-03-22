package io.github.hayatoyagi.prvisualizer.repository.store

import kotlinx.coroutines.flow.StateFlow

/**
 * Source of truth for the user's favorite (starred) repositories.
 *
 * Favorites appear at the top of the repository picker dialog.
 */
interface FavoriteReposStore {
    /** The current set of favorite repository full names (e.g. "owner/repo"). */
    val favorites: StateFlow<Set<String>>

    /** Adds [fullName] to favorites if not present, removes it if already present. */
    fun toggleFavorite(fullName: String)
}
