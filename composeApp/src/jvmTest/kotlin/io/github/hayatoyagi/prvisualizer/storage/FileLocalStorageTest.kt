package io.github.hayatoyagi.prvisualizer.storage

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileLocalStorageTest {
    private val storage = FileLocalStorage(appName = "GitHubPRsVisualizerTest")
    private lateinit var key: String

    @BeforeTest
    fun setup() {
        key = "local_storage_test_${System.nanoTime()}"
        storage.remove(key)
    }

    @AfterTest
    fun cleanup() {
        storage.remove(key)
    }

    @Test
    fun `getString returns null when no data exists`() {
        val result = storage.getString(key)
        assertNull(result)
    }

    @Test
    fun `putString and getString should persist and restore value`() {
        storage.putString(key, "owner/repo")
        val loaded = storage.getString(key)

        assertEquals("owner/repo", loaded)
    }

    @Test
    fun `putString should overwrite existing data`() {
        storage.putString(key, "value1")
        storage.putString(key, "value2")

        val loaded = storage.getString(key)
        assertEquals("value2", loaded)
    }

    @Test
    fun `remove should delete stored data`() {
        storage.putString(key, "value")
        storage.remove(key)

        val loaded = storage.getString(key)
        assertNull(loaded)
    }

    @Test
    fun `putString should ignore blank values`() {
        storage.putString(key, " ")
        val loaded = storage.getString(key)
        assertNull(loaded)
    }

    @Test
    fun `getString should trim value`() {
        storage.putString(key, "  value  ")
        assertEquals("value", storage.getString(key))
    }
}
