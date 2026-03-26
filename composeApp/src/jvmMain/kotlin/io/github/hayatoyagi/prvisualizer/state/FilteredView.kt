package io.github.hayatoyagi.prvisualizer.state

import io.github.hayatoyagi.prvisualizer.PullRequest
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot

/**
 * Derived state that depends only on [GitHubSnapshot] and [FilterState].
 *
 * By isolating the filter-dependent computation here, operations that only change
 * [SnapshotFetchState.Ready.prSelection] (e.g. toggling a single PR checkbox) can reuse
 * the same [FilteredView] instance without re-running the filter logic.
 */
data class FilteredView(
    val filteredPrs: List<PullRequest>,
    val filteredPrIds: Set<String>,
) {
    companion object {
        fun create(snapshot: GitHubSnapshot, filterState: FilterState): FilteredView {
            val filtered = filterPrs(
                allPrs = snapshot.pullRequests,
                showDrafts = filterState.showDrafts,
                onlyMine = filterState.onlyMine,
                currentUser = snapshot.viewerLogin.orEmpty(),
            )
            return FilteredView(
                filteredPrs = filtered,
                filteredPrIds = filtered.map { it.id }.toSet(),
            )
        }
    }
}
