package io.github.hayatoyagi.prvisualizer.ui.repo

private const val MAX_REPO_OPTIONS = 200

/**
 * Filters repository options based on a search query.
 *
 * @param repositoryOptions The list of all repository names
 * @param query The search query to filter by
 * @return Filtered list of repository names matching the query, limited to 200 results
 */
fun filterRepoOptions(
    repositoryOptions: List<String>,
    query: String,
): List<String> {
    val q = query.trim()
    return if (q.isBlank()) {
        repositoryOptions.take(MAX_REPO_OPTIONS)
    } else {
        repositoryOptions
            .filter { it.contains(q, ignoreCase = true) }
            .take(MAX_REPO_OPTIONS)
    }
}
