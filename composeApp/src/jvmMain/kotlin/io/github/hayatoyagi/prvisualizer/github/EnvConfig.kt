package io.github.hayatoyagi.prvisualizer.github

import java.nio.file.Files
import java.nio.file.Path

object EnvConfig {
    private const val DOTENV_PARENT_SEARCH_DEPTH = 5

    private val values: Map<String, String> by lazy {
        val fromFile = loadDotEnvFromCandidates()
        val fromSystem = System.getenv()
        // Give priority to .env for local desktop usage.
        fromSystem + fromFile
    }

    fun get(key: String): String? = values[key]

    private fun loadDotEnvFromCandidates(): Map<String, String> {
        val userDir = Path.of(System.getProperty("user.dir"))
        val candidates = sequence {
            yield(userDir.resolve(".env"))
            var current: Path? = userDir
            repeat(DOTENV_PARENT_SEARCH_DEPTH) {
                current = current?.parent
                val path = current ?: return@repeat
                yield(path.resolve(".env"))
            }
        }.distinct()

        candidates.forEach { candidate ->
            val loaded = loadDotEnv(candidate)
            if (loaded.isNotEmpty()) return loaded
        }
        return emptyMap()
    }

    private fun loadDotEnv(path: Path): Map<String, String> {
        if (!Files.exists(path)) return emptyMap()
        val result = linkedMapOf<String, String>()
        Files.readAllLines(path).forEach { lineRaw ->
            val line = lineRaw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val index = line.indexOf('=')
            if (index <= 0) return@forEach
            val key = line.substring(0, index).trim()
            val value = line.substring(index + 1).trim().trim('"')
            if (key.isNotEmpty()) result[key] = value
        }
        return result
    }
}
