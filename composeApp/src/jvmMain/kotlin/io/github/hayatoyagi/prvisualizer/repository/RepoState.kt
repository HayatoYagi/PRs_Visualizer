package io.github.hayatoyagi.prvisualizer.repository

/**
 * Represents selected repository identity.
 */
sealed interface RepoState {
    data object Unselected : RepoState

    data class Selected(
        val owner: String,
        val repo: String,
    ) : RepoState

    companion object {
        fun from(owner: String, repo: String): RepoState {
            val normalizedOwner = owner.trim()
            val normalizedRepo = repo.trim()
            return if (normalizedOwner.isBlank() || normalizedRepo.isBlank()) {
                Unselected
            } else {
                Selected(owner = normalizedOwner, repo = normalizedRepo)
            }
        }
    }
}
