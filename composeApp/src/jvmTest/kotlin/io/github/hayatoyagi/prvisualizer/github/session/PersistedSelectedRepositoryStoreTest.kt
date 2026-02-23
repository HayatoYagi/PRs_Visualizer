package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.repository.RepoState
import io.github.hayatoyagi.prvisualizer.storage.LocalStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PersistedSelectedRepositoryStoreTest {
    @Test
    fun `store should initialize from persisted repository`() {
        val localStorage = FakeLocalStorage(
            initial = mapOf("last_repository" to "persisted-owner/persisted-repo"),
        )

        val store = PersistedSelectedRepositoryStore(localStorage)

        val selected = assertIs<RepoState.Selected>(store.repoState.value)
        assertEquals("persisted-owner", selected.owner)
        assertEquals("persisted-repo", selected.repo)
    }

    @Test
    fun `store should persist selected repository`() {
        val localStorage = FakeLocalStorage()
        val store = PersistedSelectedRepositoryStore(localStorage)

        store.select(owner = "owner", repo = "repo")

        assertEquals("owner/repo", localStorage.values["last_repository"])
    }

    @Test
    fun `store should clear persisted value when unselected`() {
        val localStorage = FakeLocalStorage(
            initial = mapOf("last_repository" to "owner/repo"),
        )
        val store = PersistedSelectedRepositoryStore(localStorage)

        store.unselect()

        assertEquals(null, localStorage.values["last_repository"])
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
