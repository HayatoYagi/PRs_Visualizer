package io.github.hayatoyagi.prvisualizer.github.session

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepositorySelectionStoreImplTest {
    @BeforeTest
    fun setup() {
        // Clear any existing stored data before each test
        RepositorySelectionStoreImpl.clear()
    }

    @AfterTest
    fun cleanup() {
        // Clean up after each test
        RepositorySelectionStoreImpl.clear()
    }

    @Test
    fun `load returns null when no data exists`() {
        val result = RepositorySelectionStoreImpl.load()
        assertNull(result)
    }

    @Test
    fun `save and load should persist and restore repository`() {
        val owner = "TestOwner"
        val repo = "TestRepo"

        RepositorySelectionStoreImpl.save(owner, repo)
        val loaded = RepositorySelectionStoreImpl.load()

        assertEquals(Pair(owner, repo), loaded)
    }

    @Test
    fun `save should overwrite existing data`() {
        RepositorySelectionStoreImpl.save("Owner1", "Repo1")
        RepositorySelectionStoreImpl.save("Owner2", "Repo2")

        val loaded = RepositorySelectionStoreImpl.load()
        assertEquals(Pair("Owner2", "Repo2"), loaded)
    }

    @Test
    fun `clear should remove stored data`() {
        RepositorySelectionStoreImpl.save("Owner", "Repo")
        RepositorySelectionStoreImpl.clear()

        val loaded = RepositorySelectionStoreImpl.load()
        assertNull(loaded)
    }

    @Test
    fun `save should handle empty strings gracefully`() {
        RepositorySelectionStoreImpl.save("", "")
        val loaded = RepositorySelectionStoreImpl.load()
        assertNull(loaded)
    }

    @Test
    fun `save should ignore blank owner`() {
        RepositorySelectionStoreImpl.save("  ", "Repo")
        val loaded = RepositorySelectionStoreImpl.load()
        assertNull(loaded)
    }

    @Test
    fun `save should ignore blank repo`() {
        RepositorySelectionStoreImpl.save("Owner", "  ")
        val loaded = RepositorySelectionStoreImpl.load()
        assertNull(loaded)
    }

    @Test
    fun `load should handle repository names with special characters`() {
        val owner = "owner-name"
        val repo = "repo.name-123"

        RepositorySelectionStoreImpl.save(owner, repo)
        val loaded = RepositorySelectionStoreImpl.load()

        assertEquals(Pair(owner, repo), loaded)
    }
}
