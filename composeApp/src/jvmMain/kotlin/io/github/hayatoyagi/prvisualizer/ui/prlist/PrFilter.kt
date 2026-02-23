package io.github.hayatoyagi.prvisualizer.ui.prlist

import io.github.hayatoyagi.prvisualizer.PullRequest

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
