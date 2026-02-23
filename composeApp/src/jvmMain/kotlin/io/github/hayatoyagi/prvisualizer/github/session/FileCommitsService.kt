package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.FileCommit
import io.github.hayatoyagi.prvisualizer.github.GitHubApi

interface FileCommitsService {
    suspend fun fetchFileCommits(
        token: String,
        owner: String,
        repo: String,
        path: String,
        limit: Int,
    ): Result<List<FileCommit>>
}

class FileCommitsServiceImpl(
    private val apiFactory: (String) -> GitHubApi = ::GitHubApi,
) : FileCommitsService {
    override suspend fun fetchFileCommits(
        token: String,
        owner: String,
        repo: String,
        path: String,
        limit: Int,
    ): Result<List<FileCommit>> = runCatching {
        apiFactory(token).fetchFileCommits(owner = owner, repo = repo, path = path, limit = limit)
    }
}
