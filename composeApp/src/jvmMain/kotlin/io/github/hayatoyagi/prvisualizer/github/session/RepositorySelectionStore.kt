package io.github.hayatoyagi.prvisualizer.github.session

interface RepositorySelectionStore {
    fun load(): Pair<String, String>?

    fun save(owner: String, repo: String)
}
