package io.github.hayatoyagi.prvisualizer.github.session

import io.github.hayatoyagi.prvisualizer.repository.RepoState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PersistedSelectedRepositoryStoreTest {
    @Test
    fun `store should initialize from persisted repository`() {
        val backingStore = FakeRepositorySelectionStore(
            loaded = Pair("persisted-owner", "persisted-repo"),
        )

        val store = PersistedSelectedRepositoryStore(backingStore)

        val selected = assertIs<RepoState.Selected>(store.repoState.value)
        assertEquals("persisted-owner", selected.owner)
        assertEquals("persisted-repo", selected.repo)
    }

    @Test
    fun `store should persist selected repository`() {
        val backingStore = FakeRepositorySelectionStore()
        val store = PersistedSelectedRepositoryStore(backingStore)

        store.select(owner = "owner", repo = "repo")

        assertEquals(Pair("owner", "repo"), backingStore.saved)
    }

    private class FakeRepositorySelectionStore(
        private val loaded: Pair<String, String>? = null,
    ) : RepositorySelectionStore {
        var saved: Pair<String, String>? = null

        override fun load(): Pair<String, String>? = loaded

        override fun save(owner: String, repo: String) {
            saved = Pair(owner, repo)
        }
    }
}
