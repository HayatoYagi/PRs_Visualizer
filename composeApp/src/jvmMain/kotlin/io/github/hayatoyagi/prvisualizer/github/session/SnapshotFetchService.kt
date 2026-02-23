package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.AppError
import io.github.hayatoyagi.prvisualizer.github.GitHubApi
import io.github.hayatoyagi.prvisualizer.github.GitHubApiException
import io.github.hayatoyagi.prvisualizer.github.GitHubSnapshot

interface SnapshotFetchService {
    suspend fun fetchSnapshot(
        token: String,
        owner: String,
        repo: String,
    ): Result<GitHubSnapshot>

    fun toConnectionError(error: Throwable): AppError
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

    override fun toConnectionError(error: Throwable): AppError = when (error) {
        is java.net.ConnectException, is java.net.UnknownHostException ->
            AppError.Network(error.message ?: "Network error")
        is GitHubApiException ->
            AppError.ApiError(error.statusCode, error.message ?: "API error")
        else -> AppError.Unknown(error.message ?: "Unknown error")
    }
}
