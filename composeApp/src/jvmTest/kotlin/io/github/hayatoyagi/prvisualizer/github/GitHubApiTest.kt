package io.github.hayatoyagi.prvisualizer.github

import java.net.http.HttpHeaders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GitHubApiTest {
    @Test
    fun `normalizedAdditionsForStatus treats added empty file as one line`() {
        val api = GitHubApi(token = "dummy")

        val normalized = api.normalizedAdditionsForStatus(
            status = "added",
            additions = 0,
            deletions = 0,
        )

        assertEquals(1, normalized)
    }

    @Test
    fun `normalizedAdditionsForStatus keeps original additions for non-empty added file`() {
        val api = GitHubApi(token = "dummy")

        val normalized = api.normalizedAdditionsForStatus(
            status = "added",
            additions = 7,
            deletions = 0,
        )

        assertEquals(7, normalized)
    }

    @Test
    fun `normalizedAdditionsForStatus keeps zero for non-added file`() {
        val api = GitHubApi(token = "dummy")

        val normalized = api.normalizedAdditionsForStatus(
            status = "modified",
            additions = 0,
            deletions = 0,
        )

        assertEquals(0, normalized)
    }

    @Test
    fun `extractNextPageUrl returns next URL from Link header with double quotes`() {
        val api = GitHubApi(token = "dummy")
        val headers = HttpHeaders.of(
            mapOf(
                "Link" to listOf(
                    "<https://api.github.com/user/repos?page=2>; rel=\"next\", <https://api.github.com/user/repos?page=5>; rel=\"last\"",
                ),
            ),
        ) { _, _ -> true }

        val nextUrl = api.extractNextPageUrl(headers)

        assertEquals("https://api.github.com/user/repos?page=2", nextUrl)
    }

    @Test
    fun `extractNextPageUrl returns next URL from Link header with single quotes`() {
        val api = GitHubApi(token = "dummy")
        val headers = HttpHeaders.of(
            mapOf(
                "Link" to listOf(
                    "<https://api.github.com/user/repos?page=3>; rel='next'",
                ),
            ),
        ) { _, _ -> true }

        val nextUrl = api.extractNextPageUrl(headers)

        assertEquals("https://api.github.com/user/repos?page=3", nextUrl)
    }

    @Test
    fun `extractNextPageUrl returns null when no Link header present`() {
        val api = GitHubApi(token = "dummy")
        val headers = HttpHeaders.of(
            mapOf(
                "Content-Type" to listOf("application/json"),
            ),
        ) { _, _ -> true }

        val nextUrl = api.extractNextPageUrl(headers)

        assertNull(nextUrl)
    }

    @Test
    fun `extractNextPageUrl returns null when Link header has no next rel`() {
        val api = GitHubApi(token = "dummy")
        val headers = HttpHeaders.of(
            mapOf(
                "Link" to listOf(
                    "<https://api.github.com/user/repos?page=1>; rel=\"prev\", <https://api.github.com/user/repos?page=5>; rel=\"last\"",
                ),
            ),
        ) { _, _ -> true }

        val nextUrl = api.extractNextPageUrl(headers)

        assertNull(nextUrl)
    }

    @Test
    fun `extractNextPageUrl handles complex Link header with multiple rels`() {
        val api = GitHubApi(token = "dummy")
        val headers = HttpHeaders.of(
            mapOf(
                "Link" to listOf(
                    "<https://api.github.com/user/repos?page=1>; rel=\"prev\", <https://api.github.com/user/repos?page=3>; rel=\"next\", <https://api.github.com/user/repos?page=10>; rel=\"last\"",
                ),
            ),
        ) { _, _ -> true }

        val nextUrl = api.extractNextPageUrl(headers)

        assertEquals("https://api.github.com/user/repos?page=3", nextUrl)
    }
}
