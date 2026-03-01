package io.github.hayatoyagi.prvisualizer.storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

interface LocalStorage {
    /**
     * Gets a string value from storage.
     *
     * @param key The storage key
     * @return The stored value, or null if not found
     */
    fun getString(key: String): String?

    /**
     * Stores a string value.
     *
     * @param key The storage key
     * @param value The value to store
     */
    fun putString(key: String, value: String)

    /**
     * Removes a value from storage.
     *
     * @param key The storage key to remove
     */
    fun remove(key: String)
}

class FileLocalStorage(
    private val appName: String,
) : LocalStorage {
    override fun getString(key: String): String? {
        val path = getStoragePath(key)
        if (!Files.exists(path)) return null
        return runCatching { Files.readString(path).trim() }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    override fun putString(key: String, value: String) {
        val trimmedValue = value.trim()
        if (trimmedValue.isBlank()) return

        runCatching {
            val path = getStoragePath(key)
            val dir = path.parent
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
            }
            Files.writeString(path, trimmedValue, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }.onFailure { exception ->
            System.err.println("Warning: Failed to write local storage '$key': ${exception.message}")
        }
    }

    override fun remove(key: String) {
        runCatching {
            Files.deleteIfExists(getStoragePath(key))
        }
    }

    private fun getStoragePath(key: String): Path {
        val userHome = System.getProperty("user.home")
        val baseDir = when {
            isMacOs() -> Path.of(userHome, "Library", "Application Support")
            isWindows() -> resolveWindowsAppData(userHome)
            else -> Path.of(userHome, ".local", "share")
        }
        return baseDir.resolve(appName).resolve("$key.txt")
    }

    private fun resolveWindowsAppData(userHome: String): Path {
        val appData = System.getenv("APPDATA")
        return if (appData.isNullOrBlank()) {
            Path.of(userHome, "AppData", "Roaming")
        } else {
            Path.of(appData)
        }
    }

    private fun isMacOs(): Boolean = System.getProperty("os.name").contains("mac", ignoreCase = true)

    private fun isWindows(): Boolean = System.getProperty("os.name").contains("windows", ignoreCase = true)
}
