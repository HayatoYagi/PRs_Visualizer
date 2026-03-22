package io.github.hayatoyagi.prvisualizer.ui.repo

private const val MAX_REPO_OPTIONS = 200

fun filterRepoOptions(
    repositoryOptions: List<String>,
    query: String,
    favoriteRepos: Set<String> = emptySet(),
): List<String> {
    val q = query.trim()
    val filtered = if (q.isBlank()) {
        repositoryOptions.take(MAX_REPO_OPTIONS)
    } else {
        repositoryOptions
            .filter { it.contains(q, ignoreCase = true) }
            .take(MAX_REPO_OPTIONS)
    }
    if (favoriteRepos.isEmpty()) return filtered
    val favorites = filtered.filter { it in favoriteRepos }
    val rest = filtered.filter { it !in favoriteRepos }
    return favorites + rest
}
