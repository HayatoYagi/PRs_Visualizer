package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot

interface SnapshotFetchService {
    suspend fun fetchSnapshot(
        token: String,
        owner: String,
        repo: String,
    ): Result<GitHubSnapshot>
}

class SnapshotFetchServiceImpl(
    private val apiFactory: (String) -> GitHubApi = ::GitHubApi,
) : SnapshotFetchService {
    override suspend fun fetchSnapshot(
        token: String,
        owner: String,
        repo: String,
    ): Result<GitHubSnapshot> = runCatching {
        apiFactory(token).fetchSnapshot(
            owner = owner,
            repo = repo,
        )
    }
}
