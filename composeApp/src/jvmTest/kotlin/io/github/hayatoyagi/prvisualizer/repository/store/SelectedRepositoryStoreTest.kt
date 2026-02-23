package io.github.hayatoyagi.prvisualizer.repository.store

import io.github.hayatoyagi.prvisualizer.repository.store.InMemorySelectedRepositoryStore
import io.github.hayatoyagi.prvisualizer.repository.RepoState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SelectedRepositoryStoreTest {
    @Test
    fun `in-memory store should expose initial state`() {
        val store = InMemorySelectedRepositoryStore(
            initial = RepoState.Selected(owner = "owner", repo = "repo"),
        )

        val selected = assertIs<RepoState.Selected>(store.repoState.value)
        assertEquals("owner", selected.owner)
        assertEquals("repo", selected.repo)
    }

    @Test
    fun `in-memory store should normalize blank values to unselected`() {
        val store = InMemorySelectedRepositoryStore()

        store.select(owner = "", repo = "repo")

        assertIs<RepoState.Unselected>(store.repoState.value)
    }
}
