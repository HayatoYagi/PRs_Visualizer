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
    ): Result<GitHubSnapshot> {
        if (token.isBlank() || owner.isBlank() || repo.isBlank()) {
            return Result.failure(IllegalArgumentException("token/owner/repo must be non-blank"))
        }
        return runCatching {
            apiFactory(token.trim()).fetchSnapshot(
                owner = owner.trim(),
                repo = repo.trim(),
            )
        }
    }
}
