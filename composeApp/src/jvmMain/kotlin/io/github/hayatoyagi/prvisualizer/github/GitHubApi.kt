package io.github.hayatoyagi.prvisualizer.github

import io.github.hayatoyagi.prvisualizer.FileNode
import io.github.hayatoyagi.prvisualizer.PrFileChange
import io.github.hayatoyagi.prvisualizer.PullRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

private data class FileSeed(
    val path: String,
    val estimatedLines: Int,
)

data class GitHubSnapshot(
    val rootNode: FileNode.Directory,
    val pullRequests: List<PullRequest>,
    val viewerLogin: String?,
)

class GitHubApi(
    private val token: String,
) {
    private val client = HttpClient.newHttpClient()

    companion object {
        private val BINARY_EXTENSIONS = setOf(
            // Images
            "png", "jpg", "jpeg", "gif", "bmp", "ico", "webp", "tiff", "tif", "avif",
            // Archives
            "zip", "tar", "gz", "bz2", "7z", "rar", "xz",
            // Executables and libraries
            "exe", "dll", "so", "dylib", "bin", "app",
            // Documents and fonts
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "ttf", "otf", "woff", "woff2",
            // Media
            "mp3", "mp4", "avi", "mov", "wav", "flac", "ogg", "webm",
            // Disk images
            "dmg", "iso",
            // Databases
            "db", "sqlite",
            // Other binary formats
            "class", "jar", "war", "pyc", "o", "a", "lib",
        )
    }

    suspend fun fetchAccessibleRepositoryNames(): List<String> = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "token is required" }
        val repos = loadRepositoryNamesByPage { page ->
            requestArray("https://api.github.com/user/repos?per_page=100&page=$page&sort=updated")
        }
        repos.distinct().sortedBy { it.lowercase() }
    }

    suspend fun fetchSnapshot(owner: String, repo: String): GitHubSnapshot = withContext(Dispatchers.IO) {
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

        val rootNode = buildTree(fileSeeds, activePaths)
        GitHubSnapshot(rootNode = rootNode, pullRequests = pullRequests, viewerLogin = viewerLogin)
    }

    private fun fetchViewerLogin(): String? {
        val response = requestJson("https://api.github.com/user")
        return response.optString("login").ifBlank { null }
    }

    private fun fetchDefaultBranch(owner: String, repo: String): String {
        val response = requestJson("https://api.github.com/repos/${enc(owner)}/${enc(repo)}")
        return response.optString("default_branch").ifBlank { "main" }
    }

    private fun fetchOpenPullRequests(owner: String, repo: String): List<PullRequest> {
        val pulls = mutableListOf<PullRequest>()
        var page = 1
        while (true) {
            val response = requestArray(
                "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/pulls?state=open&per_page=100&page=$page",
            )
            if (response.length() == 0) break

            repeat(response.length()) { idx ->
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
            if (response.length() < 100) break
            page += 1
        }
        return pulls
    }

    private fun fetchPullRequestFiles(owner: String, repo: String, number: Int): List<PrFileChange> {
        val files = mutableListOf<PrFileChange>()
        var page = 1
        while (true) {
            val response = requestArray(
                "https://api.github.com/repos/${enc(owner)}/${enc(repo)}/pulls/$number/files?per_page=100&page=$page",
            )
            if (response.length() == 0) break

            repeat(response.length()) { idx ->
                val file = response.getJSONObject(idx)
                val path = file.optString("filename")
                if (isBinaryFile(path)) return@repeat
                files += PrFileChange(
                    path = path,
                    additions = file.optInt("additions"),
                    deletions = file.optInt("deletions"),
                )
            }
            if (response.length() < 100) break
            page += 1
        }
        return files
    }

    private fun isBinaryFile(path: String): Boolean {
        val lastDotIndex = path.lastIndexOf('.')
        if (lastDotIndex == -1 || lastDotIndex >= path.length - 1) return false
        val extension = path.substring(lastDotIndex + 1).lowercase()
        return extension in BINARY_EXTENSIONS
    }

    private fun fetchRepositoryFiles(owner: String, repo: String, branch: String): List<FileSeed> {
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
            val estimatedLines = maxOf(1, size / 40)
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
        while (true) {
            val response = requestPage(page) ?: break
            if (response.length() == 0) break
            repeat(response.length()) { index ->
                val repo = response.getJSONObject(index)
                val repoName = repo.optString("full_name")
                if (repoName.isNotBlank()) repos += repoName
            }
            if (response.length() < 100) break
            page += 1
        }
        return repos
    }

    private fun requestBody(url: String): String {
        val request = HttpRequest.newBuilder(URI(url))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $token")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 401) {
            throw GitHubAuthExpiredException("GitHub token expired or revoked. Please login again.")
        }
        if (response.statusCode() !in 200..299) {
            throw GitHubApiException(response.statusCode(), "GitHub API error ${response.statusCode()} for $url: ${response.body()}")
        }
        return response.body()
    }

    private fun buildTree(allFiles: List<FileSeed>, activePaths: Set<String>): FileNode.Directory {
        data class MutableDir(val path: String, val name: String, val children: MutableList<Any> = mutableListOf())

        val root = MutableDir(path = "", name = "repo")
        val dirsByPath = mutableMapOf("" to root)

        fun ensureDir(path: String): MutableDir {
            return dirsByPath.getOrPut(path) {
                val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "")
                val dirName = path.substringAfterLast('/')
                val parent = ensureDir(parentPath)
                val newDir = MutableDir(path = path, name = dirName)
                parent.children += newDir
                newDir
            }
        }

        allFiles.forEach { file ->
            val parentPath = file.path.substringBeforeLast('/', missingDelimiterValue = "")
            val dir = ensureDir(parentPath)
            dir.children += file
        }

        fun freeze(dir: MutableDir): FileNode.Directory {
            val frozenChildren = dir.children.map { child ->
                when (child) {
                    is MutableDir -> freeze(child)
                    is FileSeed -> {
                        val extension = child.path.substringAfterLast('.', missingDelimiterValue = "")
                        FileNode.File(
                            path = child.path,
                            name = child.path.substringAfterLast('/'),
                            extension = extension,
                            totalLines = child.estimatedLines,
                            hasActivePr = activePaths.contains(child.path),
                            weight = maxOf(
                                child.estimatedLines.toDouble(),
                                if (activePaths.contains(child.path)) 8.0 else 1.0,
                            ),
                        )
                    }
                    else -> error("Unexpected node type")
                }
            }

            return FileNode.Directory(
                path = dir.path,
                name = if (dir.path.isEmpty()) "repo" else dir.name,
                children = frozenChildren.sortedByDescending { it.weight },
                weight = frozenChildren.sumOf { it.weight }.coerceAtLeast(1.0),
            )
        }

        return freeze(root)
    }

    private fun enc(raw: String): String = URLEncoder.encode(raw, StandardCharsets.UTF_8)
}
