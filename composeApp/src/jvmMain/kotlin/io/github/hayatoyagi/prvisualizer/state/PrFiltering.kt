package io.github.hayatoyagi.prvisualizer.state

import io.github.hayatoyagi.prvisualizer.PullRequest

internal fun filterPrs(
    allPrs: List<PullRequest>,
    showDrafts: Boolean,
    onlyMine: Boolean,
    currentUser: String,
): List<PullRequest> =
    allPrs.filter { pr ->
        (showDrafts || !pr.isDraft) &&
            (!onlyMine || pr.author == currentUser)
    }
