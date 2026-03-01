package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.github.GitHubApi

interface RepoSelectionService {
    /**
     * Fetches the list of repositories accessible to the user.
     *
     * @param token The OAuth token
     * @return Result containing the list of repository names
     */
    suspend fun fetchRepositoryOptions(token: String): Result<List<String>>
}

class RepoSelectionServiceImpl(
    private val apiFactory: (String) -> GitHubApi = ::GitHubApi,
) : RepoSelectionService {
    override suspend fun fetchRepositoryOptions(token: String): Result<List<String>> = runCatching {
        apiFactory(token.trim()).fetchAccessibleRepositoryNames()
    }
}
