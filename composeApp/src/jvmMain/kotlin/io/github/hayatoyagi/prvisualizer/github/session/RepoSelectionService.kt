package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.github.GitHubApi

interface RepoSelectionService {
    suspend fun fetchRepositoryOptions(token: String): Result<List<String>>
}

class RepoSelectionServiceImpl(
    private val apiFactory: (String) -> GitHubApi = ::GitHubApi,
) : RepoSelectionService {
    override suspend fun fetchRepositoryOptions(token: String): Result<List<String>> = runCatching {
        apiFactory(token.trim()).fetchAccessibleRepositoryNames()
    }
}
