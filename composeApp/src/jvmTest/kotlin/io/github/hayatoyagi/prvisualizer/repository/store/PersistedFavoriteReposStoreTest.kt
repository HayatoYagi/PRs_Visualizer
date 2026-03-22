package io.github.hayatoyagi.prvisualizer.repository.store

import io.github.hayatoyagi.prvisualizer.storage.LocalStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistedFavoriteReposStoreTest {
    @Test
    fun `store should start empty when no persisted value exists`() {
        val localStorage = FakeLocalStorage()

        val store = PersistedFavoriteReposStore(localStorage)

        assertTrue(store.favorites.value.isEmpty())
    }

    @Test
    fun `store should initialize from persisted favorites`() {
        val localStorage = FakeLocalStorage(
            initial = mapOf("favorite_repositories" to "owner1/repo1,owner2/repo2"),
        )

        val store = PersistedFavoriteReposStore(localStorage)

        assertEquals(setOf("owner1/repo1", "owner2/repo2"), store.favorites.value)
    }

    @Test
    fun `toggleFavorite should add a new favorite and persist it`() {
        val localStorage = FakeLocalStorage()
        val store = PersistedFavoriteReposStore(localStorage)

        store.toggleFavorite("owner/repo")

        assertTrue(store.favorites.value.contains("owner/repo"))
        val persisted = localStorage.values["favorite_repositories"] ?: ""
        assertTrue(persisted.contains("owner/repo"))
    }

    @Test
    fun `toggleFavorite should remove an existing favorite and persist`() {
        val localStorage = FakeLocalStorage(
            initial = mapOf("favorite_repositories" to "owner/repo"),
        )
        val store = PersistedFavoriteReposStore(localStorage)

        store.toggleFavorite("owner/repo")

        assertFalse(store.favorites.value.contains("owner/repo"))
        assertEquals(null, localStorage.values["favorite_repositories"])
    }

    @Test
    fun `toggleFavorite should remove storage key when last favorite is removed`() {
        val localStorage = FakeLocalStorage(
            initial = mapOf("favorite_repositories" to "owner/repo"),
        )
        val store = PersistedFavoriteReposStore(localStorage)

        store.toggleFavorite("owner/repo")

        assertFalse(localStorage.values.containsKey("favorite_repositories"))
    }

    @Test
    fun `store should handle multiple favorites independently`() {
        val localStorage = FakeLocalStorage()
        val store = PersistedFavoriteReposStore(localStorage)

        store.toggleFavorite("owner/repo1")
        store.toggleFavorite("owner/repo2")
        store.toggleFavorite("owner/repo1")

        assertEquals(setOf("owner/repo2"), store.favorites.value)
    }

    private class FakeLocalStorage(
        initial: Map<String, String> = emptyMap(),
    ) : LocalStorage {
        val values: MutableMap<String, String> = initial.toMutableMap()

        override fun getString(key: String): String? = values[key]

        override fun putString(key: String, value: String) {
            values[key] = value
        }

        override fun remove(key: String) {
            values.remove(key)
        }
    }
}
