package io.github.hayatoyagi.prvisualizer.ui.repo

fun filterRepoOptions(
    repositoryOptions: List<String>,
    query: String,
): List<String> {
    val q = query.trim()
    return if (q.isBlank()) {
        repositoryOptions.take(200)
    } else {
        repositoryOptions
            .filter { it.contains(q, ignoreCase = true) }
            .take(200)
    }
}
