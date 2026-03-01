package io.github.hayatoyagi.prvisualizer.github

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
