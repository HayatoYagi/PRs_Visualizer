package io.github.hayatoyagi.prvisualizer.github.session

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepositoryStoreTest {
    @BeforeTest
    fun setup() {
        // Clear any existing stored data before each test
        RepositoryStore.clearRepository()
    }

    @AfterTest
    fun cleanup() {
        // Clean up after each test
        RepositoryStore.clearRepository()
    }

    @Test
    fun `loadRepository returns null when no data exists`() {
        val result = RepositoryStore.loadRepository()
        assertNull(result)
    }

    @Test
    fun `saveRepository and loadRepository should persist and restore repository`() {
        val owner = "TestOwner"
        val repo = "TestRepo"

        RepositoryStore.saveRepository(owner, repo)
        val loaded = RepositoryStore.loadRepository()

        assertEquals(Pair(owner, repo), loaded)
    }

    @Test
    fun `saveRepository should overwrite existing data`() {
        RepositoryStore.saveRepository("Owner1", "Repo1")
        RepositoryStore.saveRepository("Owner2", "Repo2")

        val loaded = RepositoryStore.loadRepository()
        assertEquals(Pair("Owner2", "Repo2"), loaded)
    }

    @Test
    fun `clearRepository should remove stored data`() {
        RepositoryStore.saveRepository("Owner", "Repo")
        RepositoryStore.clearRepository()

        val loaded = RepositoryStore.loadRepository()
        assertNull(loaded)
    }

    @Test
    fun `saveRepository should handle empty strings gracefully`() {
        RepositoryStore.saveRepository("", "")
        val loaded = RepositoryStore.loadRepository()
        assertNull(loaded)
    }

    @Test
    fun `saveRepository should ignore blank owner`() {
        RepositoryStore.saveRepository("  ", "Repo")
        val loaded = RepositoryStore.loadRepository()
        assertNull(loaded)
    }

    @Test
    fun `saveRepository should ignore blank repo`() {
        RepositoryStore.saveRepository("Owner", "  ")
        val loaded = RepositoryStore.loadRepository()
        assertNull(loaded)
    }

    @Test
    fun `loadRepository should handle repository names with special characters`() {
        val owner = "owner-name"
        val repo = "repo.name-123"

        RepositoryStore.saveRepository(owner, repo)
        val loaded = RepositoryStore.loadRepository()

        assertEquals(Pair(owner, repo), loaded)
    }
}
