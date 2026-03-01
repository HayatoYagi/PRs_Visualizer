package io.github.hayatoyagi.prvisualizer.ui.repo

private const val MAX_REPO_OPTIONS = 200

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
