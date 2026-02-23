package io.github.hayatoyagi.prvisualizer.github.session

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Stores and retrieves the last opened repository information.
 * Persists owner and repository name to local storage so it can be restored on app restart.
 */
object RepositoryStore {
    private const val FILE_NAME = "last_repository.txt"

    /**
     * Loads the last opened repository.
     * Returns null if no repository was previously saved.
     * @return Pair of (owner, repo) or null if not found or invalid
     */
    fun loadRepository(): Pair<String, String>? {
        val path = getStoragePath()
        if (!Files.exists(path)) return null

        return runCatching {
            val content = Files.readString(path).trim()
            if (content.isBlank()) return null

            val parts = content.split('/')
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                Pair(parts[0], parts[1])
            } else {
                null
            }
        }.getOrNull()
    }

    /**
     * Saves the current repository to persistent storage.
     * @param owner Repository owner
     * @param repo Repository name
     */
    fun saveRepository(owner: String, repo: String) {
        if (owner.isBlank() || repo.isBlank()) return

        runCatching {
            val path = getStoragePath()
            val dir = path.parent
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }
            val content = "$owner/$repo"
            Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }

    /**
     * Clears the saved repository information.
     */
    fun clearRepository() {
        runCatching {
            Files.deleteIfExists(getStoragePath())
        }
    }

    private fun getStoragePath(): Path {
        val userHome = System.getProperty("user.home")
        val baseDir = when {
            isMacOs() -> Path.of(userHome, "Library", "Application Support")
            isWindows() -> {
                val appData = System.getenv("APPDATA")
                if (appData.isNullOrBlank()) {
                    Path.of(userHome, "AppData", "Roaming")
                } else {
                    Path.of(appData)
                }
            }
            else -> Path.of(userHome, ".local", "share") // Linux/Unix
        }
        return baseDir.resolve("GitHubPRsVisualizer").resolve(FILE_NAME)
    }

    private fun isMacOs(): Boolean = System.getProperty("os.name").contains("mac", ignoreCase = true)

    private fun isWindows(): Boolean = System.getProperty("os.name").contains("windows", ignoreCase = true)
}
