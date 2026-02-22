package io.github.hayatoyagi.prvisualizer.ui.prlist

import io.github.hayatoyagi.prvisualizer.PullRequest

fun filterPrs(
    allPrs: List<PullRequest>,
    showDrafts: Boolean,
    onlyMine: Boolean,
    query: String,
    currentUser: String,
): List<PullRequest> =
    allPrs.filter { pr ->
        (showDrafts || !pr.isDraft) &&
            (!onlyMine || pr.author == currentUser) &&
            (query.isBlank() || pr.title.contains(query, ignoreCase = true) || "#${pr.number}".contains(query))
    }
