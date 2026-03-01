package io.github.hayatoyagi.prvisualizer.ui.prlist

import io.github.hayatoyagi.prvisualizer.PullRequest

/**
 * Filters pull requests based on draft status and author.
 *
 * @param allPrs List of all pull requests
 * @param showDrafts Whether to include draft PRs
 * @param onlyMine Whether to show only the current user's PRs
 * @param currentUser The current user's login name
 * @return Filtered list of pull requests
 */
fun filterPrs(
    allPrs: List<PullRequest>,
    showDrafts: Boolean,
    onlyMine: Boolean,
    currentUser: String,
): List<PullRequest> =
    allPrs.filter { pr ->
        (showDrafts || !pr.isDraft) &&
            (!onlyMine || pr.author == currentUser)
    }
