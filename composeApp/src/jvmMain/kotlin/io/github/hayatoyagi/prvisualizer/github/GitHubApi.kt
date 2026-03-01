package io.github.hayatoyagi.prvisualizer.github

import io.github.hayatoyagi.prvisualizer.ChangeType
import io.github.hayatoyagi.prvisualizer.FileCommit
import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PrFileChange
import io.github.hayatoyagi.prvisualizer.PullRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

data class GitHubSnapshot(
    val rootNode: FileNode.Directory,
    val pullRequests: List<PullRequest>,
    val viewerLogin: String?,
    val defaultBranch: String,
)

private data class RawResponseWithHeaders(
    val body: String,
    val headers: HttpHeaders,
)

class GitHubApi(
    private val token: String,
) {
    private val client = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val apiSemaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

    suspend fun fetchAccessibleRepositoryNames(): List<String> = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { TOKEN_REQUIRED_MESSAGE }
        val repos = mutableListOf<String>()
        var nextUrl: String? = "https://api.github.com/user/repos?per_page=100&sort=updated"

        while (nextUrl != null) {
            val response = requestWithHeaders<List<GitHubRepository>>(nextUrl)
            response.data.forEach { repo ->
                val repoName = repo.fullName
                if (repoName.isNotBlank()) repos += repoName
            }
            nextUrl = extractNextPageUrl(response.headers)
        }

        repos.distinct().sortedBy { it.lowercase() }
    }

    suspend fun fetchSnapshot(
        owner: String,
        repo: String,
    ): GitHubSnapshot = withContext(Dispatchers.IO) {
        require(owner.isNotBlank()) { "owner is required" }
        require(repo.isNotBlank()) { "repo is required" }
        require(token.isNotBlank()) { TOKEN_REQUIRED_MESSAGE }

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
        val response = request<GitHubUser>("https://api.github.com/user")
        return response.login.ifBlank { null }
    }

    private fun fetchDefaultBranch(
        owner: String,
        repo: String,
    ): String {
        val response = request<GitHubRepository>("https://api.github.com/repos/${enc(owner)}/${enc(repo)}")
        return response.defaultBranch?.ifBlank { null } ?: "main"
    }

    private suspend fun fetchOpenPullRequests(
        owner: String,
        repo: String,
    ): List<PullRequest> = coroutineScope {
        val pulls = mutableListOf<PullRequest>()
        var nextUrl: String? = "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/pulls?state=open&per_page=$GITHUB_PAGE_SIZE"

        while (nextUrl != null) {
            val response = requestListWithHeaders<GitHubPullRequest>(nextUrl)
            
            // Fetch files for all PRs in this page concurrently
            val prFilesDeferred = response.data.map { pr ->
                async {
                    val files = fetchPullRequestFiles(owner, repo, pr.number)
                    PullRequest(
                        id = pr.nodeId.ifBlank { "pr-${pr.number}" },
                        number = pr.number,
                        title = pr.title,
                        author = pr.user?.login.orEmpty(),
                        isDraft = pr.draft,
                        url = pr.htmlUrl,
                        files = files,
                    )
                }
            }
            
            pulls.addAll(prFilesDeferred.awaitAll())
            nextUrl = extractNextPageUrl(response.headers)
        }

        return@coroutineScope pulls
    }

    private suspend fun fetchPullRequestFiles(
        owner: String,
        repo: String,
        number: Int,
    ): List<PrFileChange> = apiSemaphore.withPermit {
        val files = mutableListOf<PrFileChange>()
        var nextUrl: String? = "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/pulls/$number/files?per_page=$GITHUB_PAGE_SIZE"

        while (nextUrl != null) {
            val response = requestListWithHeaders<GitHubPullRequestFile>(nextUrl)
            response.data.forEach { file ->
                val path = file.filename
                if (isBinaryFile(path)) return@forEach
                val additions = file.additions
                val deletions = file.deletions
                val status = file.status
                files += PrFileChange(
                    path = path,
                    additions = normalizedAdditionsForStatus(status, additions, deletions),
                    deletions = deletions,
                )
            }
            nextUrl = extractNextPageUrl(response.headers)
        }

        return@withPermit files
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
        require(token.isNotBlank()) { TOKEN_REQUIRED_MESSAGE }

        val response = requestList<GitHubCommit>(
            "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/commits?path=${enc(path)}&per_page=$limit",
        )

        val commits = response.map { commitObj ->
            val commit = commitObj.commit
            val author = commit?.author
            val committer = commit?.committer

            FileCommit(
                sha = commitObj.sha.take(SHORT_SHA_LENGTH),
                message = commit?.message?.lines()?.firstOrNull() ?: "",
                author = author?.name?.takeIf { it.isNotBlank() }
                    ?: committer?.name?.takeIf { it.isNotBlank() }
                    ?: "Unknown",
                date = author?.date ?: committer?.date ?: "",
                url = commitObj.htmlUrl,
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
        val response = request<GitHubTree>(
            "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/git/trees/${enc(branch)}?recursive=1",
        )
        val files = mutableListOf<FileSeed>()
        response.tree.forEach { node ->
            if (node.type != "blob") return@forEach
            val path = node.path
            if (path.isBlank()) return@forEach
            if (isBinaryFile(path)) return@forEach
            val size = node.size
            val estimatedLines = maxOf(MIN_ESTIMATED_LINES, size / ESTIMATED_LINES_DIVISOR)
            files += FileSeed(path = path, estimatedLines = estimatedLines)
        }
        return files
    }

    private inline fun <reified T> request(url: String): T {
        val body = requestBody(url)
        return json.decodeFromString<T>(body)
    }

    private inline fun <reified T> requestList(url: String): List<T> {
        val body = requestBody(url)
        return json.decodeFromString<List<T>>(body)
    }

    private data class TypedResponseWithHeaders<T>(
        val data: T,
        val headers: HttpHeaders,
    )

    private inline fun <reified T> requestWithHeaders(url: String): TypedResponseWithHeaders<T> {
        val response = requestBodyWithHeaders(url)
        val data = json.decodeFromString<T>(response.body)
        return TypedResponseWithHeaders(data, response.headers)
    }

    private inline fun <reified T> requestListWithHeaders(url: String): TypedResponseWithHeaders<List<T>> {
        val response = requestBodyWithHeaders(url)
        val data = json.decodeFromString<List<T>>(response.body)
        return TypedResponseWithHeaders(data, response.headers)
    }

    /**
     * Extracts the next page URL from GitHub Link header.
     * GitHub Link header format: <url>; rel="next", <url>; rel="last"
     * Returns null if there is no next page.
     */
    internal fun extractNextPageUrl(headers: HttpHeaders): String? {
        val linkHeader = headers.firstValue("Link").orElse(null) ?: return null

        // Parse Link header to find rel="next"
        // Example: <https://api.github.com/user/repos?page=2>; rel="next"
        // Uses regex to handle variations in whitespace and quoting
        val links = linkHeader.split(",")
        for (link in links) {
            val parts = link.trim().split(";")
            if (parts.size >= 2) {
                val url = parts[0].trim().removeSurrounding("<", ">")
                // Match rel="next" or rel='next' with optional whitespace around the =
                val relPart = parts[1].trim()
                val relPattern = """rel\s*=\s*['"](next)['"]""".toRegex(RegexOption.IGNORE_CASE)
                if (relPattern.containsMatchIn(relPart)) {
                    return url
                }
            }
        }
        return null
    }

    private fun requestBody(url: String): String = requestBodyWithHeaders(url).body

    private fun requestBodyWithHeaders(url: String): RawResponseWithHeaders {
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
        return RawResponseWithHeaders(response.body(), response.headers())
    }

    private fun enc(raw: String): String = URLEncoder.encode(raw, StandardCharsets.UTF_8)

    private companion object {
        const val TOKEN_REQUIRED_MESSAGE = "token is required"
        const val GITHUB_PAGE_SIZE = 100
        const val SHORT_SHA_LENGTH = 7
        const val MIN_ESTIMATED_LINES = 1
        const val ESTIMATED_LINES_DIVISOR = 40
        const val MAX_CONCURRENT_REQUESTS = 10
    }
}
