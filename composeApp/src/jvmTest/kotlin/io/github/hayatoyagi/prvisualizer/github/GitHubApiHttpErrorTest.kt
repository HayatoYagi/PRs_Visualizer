package io.github.hayatoyagi.prvisualizer.github

import com.sun.net.httpserver.HttpServer
import io.github.hayatoyagi.prvisualizer.FileCommit
import kotlinx.coroutines.test.runTest
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Comprehensive tests for GitHubApi HTTP error paths, including:
 * - 4xx responses (401 Unauthorized, 404 Not Found, 422 Unprocessable)
 * - 5xx / network errors
 * - Malformed or unexpected JSON responses
 * - Empty lists (zero PRs, zero files)
 * - Pagination edge cases
 *
 * Note: Uses com.sun.net.httpserver.HttpServer (from JDK) to avoid adding external test dependencies.
 * While this ties tests to internal JDK APIs, it allows comprehensive testing without new dependencies.
 * If portability becomes an issue, consider migrating to a dedicated library like MockWebServer.
 */
class GitHubApiHttpErrorTest {
    @Test
    fun `fetchAccessibleRepositoryNames throws GitHubAuthExpiredException on 401 response`() = runTest {
        val server = startTestServer { exchange ->
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1)
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            val exception = assertFailsWith<GitHubAuthExpiredException> {
                api.fetchAccessibleRepositoryNames()
            }

            assertTrue(exception.message?.contains("expired or revoked") == true)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchAccessibleRepositoryNames throws GitHubApiException on 404 response`() = runTest {
        val server = startTestServer { exchange ->
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, 0)
            exchange.responseBody.use { it.write("Not Found".toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            val exception = assertFailsWith<GitHubApiException> {
                api.fetchAccessibleRepositoryNames()
            }

            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, exception.statusCode)
            assertTrue(exception.message?.contains("404") == true)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchAccessibleRepositoryNames throws GitHubApiException on 422 response`() = runTest {
        val server = startTestServer { exchange ->
            val errorJson = """{"message":"Validation Failed","errors":[]}"""
            exchange.sendResponseHeaders(422, errorJson.length.toLong())
            exchange.responseBody.use { it.write(errorJson.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            val exception = assertFailsWith<GitHubApiException> {
                api.fetchAccessibleRepositoryNames()
            }

            assertEquals(422, exception.statusCode)
            assertTrue(exception.message?.contains("422") == true)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchAccessibleRepositoryNames throws GitHubApiException on 500 response`() = runTest {
        val server = startTestServer { exchange ->
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, 0)
            exchange.responseBody.use { it.write("Internal Server Error".toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            val exception = assertFailsWith<GitHubApiException> {
                api.fetchAccessibleRepositoryNames()
            }

            assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, exception.statusCode)
            assertTrue(exception.message?.contains("500") == true)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchAccessibleRepositoryNames throws GitHubApiException on 503 response`() = runTest {
        val server = startTestServer { exchange ->
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAVAILABLE, 0)
            exchange.responseBody.use { it.write("Service Unavailable".toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            val exception = assertFailsWith<GitHubApiException> {
                api.fetchAccessibleRepositoryNames()
            }

            assertEquals(HttpURLConnection.HTTP_UNAVAILABLE, exception.statusCode)
            assertTrue(exception.message?.contains("503") == true)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchAccessibleRepositoryNames handles malformed JSON response`() = runTest {
        val server = startTestServer { exchange ->
            val malformedJson = """{"invalid": json syntax"""
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, malformedJson.length.toLong())
            exchange.responseBody.use { it.write(malformedJson.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            assertFailsWith<Exception> {
                api.fetchAccessibleRepositoryNames()
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchAccessibleRepositoryNames handles empty repository list`() = runTest {
        val server = startTestServer { exchange ->
            val emptyList = "[]"
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, emptyList.length.toLong())
            exchange.responseBody.use { it.write(emptyList.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")
            val repos = api.fetchAccessibleRepositoryNames()

            assertEquals(emptyList(), repos)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchAccessibleRepositoryNames handles single page pagination`() = runTest {
        var requestCount = 0
        val server = startTestServer { exchange ->
            requestCount++
            val repos = """[{"full_name":"owner/repo1"},{"full_name":"owner/repo2"}]"""
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, repos.length.toLong())
            exchange.responseBody.use { it.write(repos.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")
            val repos = api.fetchAccessibleRepositoryNames()

            assertEquals(2, repos.size)
            assertTrue(repos.contains("owner/repo1"))
            assertTrue(repos.contains("owner/repo2"))
            assertEquals(1, requestCount) // Should only make one request
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchSnapshot throws GitHubAuthExpiredException on 401 response`() = runTest {
        val server = startTestServer { exchange ->
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1)
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            val exception = assertFailsWith<GitHubAuthExpiredException> {
                api.fetchSnapshot("owner", "repo")
            }

            assertTrue(exception.message?.contains("expired or revoked") == true)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchSnapshot throws GitHubApiException on 404 repository not found`() = runTest {
        val server = startTestServer { exchange ->
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, 0)
            exchange.responseBody.use { it.write("Not Found".toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            val exception = assertFailsWith<GitHubApiException> {
                api.fetchSnapshot("owner", "repo")
            }

            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, exception.statusCode)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchSnapshot handles repository with zero open pull requests`() = runTest {
        val server = startTestServer { exchange ->
            val path = exchange.requestURI.path
            val response = when {
                path.contains("/user") -> """{"login":"testuser"}"""
                path.contains("/repos/") && !path.contains("/pulls") && !path.contains("/git/trees") ->
                    """{"default_branch":"main"}"""
                path.contains("/pulls") -> "[]"
                path.contains("/git/trees") -> """{"tree":[]}"""
                else -> "{}"
            }
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")
            val snapshot = api.fetchSnapshot("owner", "repo")

            assertEquals(0, snapshot.pullRequests.size)
            assertEquals("main", snapshot.defaultBranch)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchSnapshot handles repository with zero files`() = runTest {
        val server = startTestServer { exchange ->
            val path = exchange.requestURI.path
            val response = when {
                path.contains("/user") -> """{"login":"testuser"}"""
                path.contains("/repos/") && !path.contains("/pulls") && !path.contains("/git/trees") ->
                    """{"default_branch":"main"}"""
                path.contains("/pulls") -> "[]"
                path.contains("/git/trees") -> """{"tree":[]}"""
                else -> "{}"
            }
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")
            val snapshot = api.fetchSnapshot("owner", "repo")

            assertEquals(0, snapshot.rootNode.children.size)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchFileCommits throws GitHubAuthExpiredException on 401 response`() = runTest {
        val server = startTestServer { exchange ->
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1)
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            val exception = assertFailsWith<GitHubAuthExpiredException> {
                api.fetchFileCommits("owner", "repo", "path/to/file.kt")
            }

            assertTrue(exception.message?.contains("expired or revoked") == true)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchFileCommits throws GitHubApiException on 404 file not found`() = runTest {
        val server = startTestServer { exchange ->
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, 0)
            exchange.responseBody.use { it.write("Not Found".toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            val exception = assertFailsWith<GitHubApiException> {
                api.fetchFileCommits("owner", "repo", "nonexistent.kt")
            }

            assertEquals(HttpURLConnection.HTTP_NOT_FOUND, exception.statusCode)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchFileCommits handles empty commit history`() = runTest {
        val server = startTestServer { exchange ->
            val emptyList = "[]"
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, emptyList.length.toLong())
            exchange.responseBody.use { it.write(emptyList.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")
            val commits = api.fetchFileCommits("owner", "repo", "path/to/file.kt")

            assertEquals(emptyList<FileCommit>(), commits)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchFileCommits handles malformed JSON in commit response`() = runTest {
        val server = startTestServer { exchange ->
            val malformedJson = """[{"sha":"abc123", "commit":"""
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, malformedJson.length.toLong())
            exchange.responseBody.use { it.write(malformedJson.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")

            assertFailsWith<Exception> {
                api.fetchFileCommits("owner", "repo", "path/to/file.kt")
            }
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchSnapshot handles unexpected JSON structure in user response`() = runTest {
        val server = startTestServer { exchange ->
            val path = exchange.requestURI.path
            val response = when {
                path.contains("/user") -> """{"unexpected":"field"}"""
                path.contains("/repos/") && !path.contains("/pulls") && !path.contains("/git/trees") ->
                    """{"default_branch":"main"}"""
                path.contains("/pulls") -> "[]"
                path.contains("/git/trees") -> """{"tree":[]}"""
                else -> "{}"
            }
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")
            val snapshot = api.fetchSnapshot("owner", "repo")

            // Should handle missing login field gracefully due to ignoreUnknownKeys
            assertEquals(null, snapshot.viewerLogin)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `fetchSnapshot handles unexpected JSON structure in repository response`() = runTest {
        val server = startTestServer { exchange ->
            val path = exchange.requestURI.path
            val response = when {
                path.contains("/user") -> """{"login":"testuser"}"""
                path.contains("/repos/") && !path.contains("/pulls") && !path.contains("/git/trees") ->
                    """{"unexpected":"field"}"""
                path.contains("/pulls") -> "[]"
                path.contains("/git/trees") -> """{"tree":[]}"""
                else -> "{}"
            }
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
            exchange.close()
        }

        try {
            val api = GitHubApi("test-token", "http://localhost:${server.address.port}")
            val snapshot = api.fetchSnapshot("owner", "repo")

            // Should use default "main" when default_branch is missing
            assertEquals("main", snapshot.defaultBranch)
        } finally {
            server.stop(0)
        }
    }

    private fun startTestServer(handler: (com.sun.net.httpserver.HttpExchange) -> Unit): HttpServer {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/") { exchange ->
            try {
                handler(exchange)
            } catch (e: Exception) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1)
                exchange.close()
            }
        }
        server.start()
        return server
    }
}
