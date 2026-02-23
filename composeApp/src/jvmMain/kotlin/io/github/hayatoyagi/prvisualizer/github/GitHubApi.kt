package io.github.hayatoyagi.prvisualizer.github

import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.FileCommit
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PrFileChange
import io.github.hayatoyagi.prvisualizer.PullRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

data class GitHubSnapshot(
    val rootNode: FileNode.Directory,
    val pullRequests: List<PullRequest>,
    val viewerLogin: String?,
    val defaultBranch: String,
)

class GitHubApi(
    private val token: String,
) {
    private companion object {
        const val GITHUB_PAGE_SIZE = 100
        const val SHORT_SHA_LENGTH = 7
        const val MIN_ESTIMATED_LINES = 1
        const val ESTIMATED_LINES_DIVISOR = 40
    }

    private val client = HttpClient.newHttpClient()

    suspend fun fetchAccessibleRepositoryNames(): List<String> = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "token is required" }
        val repos = loadRepositoryNamesByPage { page ->
            requestArray("https://api.github.com/user/repos?per_page=100&page=$page&sort=updated")
        }
        repos.distinct().sortedBy { it.lowercase() }
    }

    suspend fun fetchSnapshot(
        owner: String,
        repo: String,
    ): GitHubSnapshot = withContext(Dispatchers.IO) {
        require(owner.isNotBlank()) { "owner is required" }
        require(repo.isNotBlank()) { "repo is required" }
        require(token.isNotBlank()) { "token is required" }

        val viewerLogin = fetchViewerLogin()
        val pullRequests = fetchOpenPullRequests(owner, repo)
        val defaultBranch = fetchDefaultBranch(owner, repo)
        val fileSeeds = fetchRepositoryFiles(owner, repo, defaultBranch)
        val activePaths = pullRequests
            .flatMap { it.files }
            .map { it.path }
            .toSet()

        // Include files that are newly added in PRs (not in default branch)
        val allPrFileChanges = pullRequests.flatMap { it.files }
        val newlyAddedFiles = allPrFileChanges
            .filter { it.changeType == ChangeType.Addition }
            .filter { change -> fileSeeds.none { seed -> seed.path == change.path } }
            .groupBy { it.path }
            .map { (path, changes) ->
                // Use max additions if multiple PRs add the same file
                val maxAdditions = changes.maxOf { it.additions }
                FileSeed(path = path, estimatedLines = maxOf(1, maxAdditions))
            }

        val allFileSeeds = fileSeeds + newlyAddedFiles
        val rootNode = buildTree(allFileSeeds, activePaths)
        GitHubSnapshot(
            rootNode = rootNode,
            pullRequests = pullRequests,
            viewerLogin = viewerLogin,
            defaultBranch = defaultBranch,
        )
    }

    private fun fetchViewerLogin(): String? {
        val response = requestJson("https://api.github.com/user")
        return response.optString("login").ifBlank { null }
    }

    private fun fetchDefaultBranch(
        owner: String,
        repo: String,
    ): String {
        val response = requestJson("https://api.github.com/repos/${enc(owner)}/${enc(repo)}")
        return response.optString("default_branch").ifBlank { "main" }
    }

    private fun fetchOpenPullRequests(
        owner: String,
        repo: String,
    ): List<PullRequest> {
        val pulls = mutableListOf<PullRequest>()
        var page = 1
        var hasNextPage: Boolean
        do {
            val response = requestArray(
                "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/pulls?state=open&per_page=$GITHUB_PAGE_SIZE&page=$page",
            )
            val responseSize = response.length()
            hasNextPage = responseSize == GITHUB_PAGE_SIZE

            repeat(responseSize) { idx ->
                val pr = response.getJSONObject(idx)
                val number = pr.optInt("number")
                val files = fetchPullRequestFiles(owner, repo, number)
                pulls += PullRequest(
                    id = pr.optString("node_id", "pr-$number"),
                    number = number,
                    title = pr.optString("title"),
                    author = pr.optJSONObject("user")?.optString("login").orEmpty(),
                    isDraft = pr.optBoolean("draft", false),
                    url = pr.optString("html_url"),
                    files = files,
                )
            }
            page += 1
        } while (hasNextPage)
        return pulls
    }

    private fun fetchPullRequestFiles(
        owner: String,
        repo: String,
        number: Int,
    ): List<PrFileChange> {
        val files = mutableListOf<PrFileChange>()
        var page = 1
        var hasNextPage: Boolean
        do {
            val response = requestArray(
                "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/pulls/$number/files?per_page=$GITHUB_PAGE_SIZE&page=$page",
            )
            val responseSize = response.length()
            hasNextPage = responseSize == GITHUB_PAGE_SIZE

            repeat(responseSize) { idx ->
                val file = response.getJSONObject(idx)
                val path = file.optString("filename")
                if (isBinaryFile(path)) return@repeat
                val additions = file.optInt("additions")
                val deletions = file.optInt("deletions")
                val status = file.optString("status")
                files += PrFileChange(
                    path = path,
                    additions = normalizedAdditionsForStatus(status, additions, deletions),
                    deletions = deletions,
                )
            }
            page += 1
        } while (hasNextPage)
        return files
    }

    suspend fun fetchFileCommits(
        owner: String,
        repo: String,
        path: String,
        limit: Int = 10,
    ): List<FileCommit> = withContext(Dispatchers.IO) {
        require(owner.isNotBlank()) { "owner is required" }
        require(repo.isNotBlank()) { "repo is required" }
        require(path.isNotBlank()) { "path is required" }
        require(token.isNotBlank()) { "token is required" }

        val response = requestArray(
            "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/commits?path=${enc(path)}&per_page=$limit",
        )

        val commits = mutableListOf<FileCommit>()
        repeat(response.length()) { idx ->
            val commitObj = response.getJSONObject(idx)
            val commit = commitObj.optJSONObject("commit")
            val author = commit?.optJSONObject("author")
            val committer = commit?.optJSONObject("committer")

            commits += FileCommit(
                sha = commitObj.optString("sha", "").take(SHORT_SHA_LENGTH),
                message = commit?.optString("message", "")?.lines()?.firstOrNull() ?: "",
                author = author?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: committer?.optString("name")?.takeIf { it.isNotBlank() }
                    ?: "Unknown",
                date = author?.optString("date") ?: committer?.optString("date") ?: "",
                url = commitObj.optString("html_url", ""),
            )
        }
        return@withContext commits
    }

    // GitHub can return added empty files as status=added with +0/-0.
    internal fun normalizedAdditionsForStatus(
        status: String,
        additions: Int,
        deletions: Int,
    ): Int = if (status == "added" && additions == 0 && deletions == 0) MIN_ESTIMATED_LINES else additions

    private fun fetchRepositoryFiles(
        owner: String,
        repo: String,
        branch: String,
    ): List<FileSeed> {
        val response = requestJson(
            "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/git/trees/${enc(branch)}?recursive=1",
        )
        val tree = response.optJSONArray("tree") ?: JSONArray()
        val files = mutableListOf<FileSeed>()
        repeat(tree.length()) { idx ->
            val node = tree.getJSONObject(idx)
            if (node.optString("type") != "blob") return@repeat
            val path = node.optString("path")
            if (path.isBlank()) return@repeat
            if (isBinaryFile(path)) return@repeat
            val size = node.optInt("size", 0)
            val estimatedLines = maxOf(MIN_ESTIMATED_LINES, size / ESTIMATED_LINES_DIVISOR)
            files += FileSeed(path = path, estimatedLines = estimatedLines)
        }
        return files
    }

    private fun requestJson(url: String): JSONObject {
        val body = requestBody(url)
        return JSONObject(body)
    }

    private fun requestArray(url: String): JSONArray {
        val body = requestBody(url)
        return JSONArray(body)
    }

    private fun loadRepositoryNamesByPage(requestPage: (Int) -> JSONArray?): List<String> {
        val repos = mutableListOf<String>()
        var page = 1
        var hasNextPage: Boolean
        do {
            val response = requestPage(page)
            val responseSize = response?.length() ?: 0
            hasNextPage = responseSize == GITHUB_PAGE_SIZE
            if (response == null) return repos
            repeat(responseSize) { index ->
                val repo = response.getJSONObject(index)
                val repoName = repo.optString("full_name")
                if (repoName.isNotBlank()) repos += repoName
            }
            page += 1
        } while (hasNextPage)
        return repos
    }

    private fun requestBody(url: String): String {
        val request = HttpRequest
            .newBuilder(URI(url))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $token")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw GitHubAuthExpiredException("GitHub token expired or revoked. Please login again.")
        }
        if (response.statusCode() !in HttpURLConnection.HTTP_OK until HttpURLConnection.HTTP_MULT_CHOICE) {
            throw GitHubApiException(response.statusCode(), "GitHub API error ${response.statusCode()} for $url: ${response.body()}")
        }
        return response.body()
    }

    private fun enc(raw: String): String = URLEncoder.encode(raw, StandardCharsets.UTF_8)
}
