package io.github.hayatoyagi.prvisualizer

class Platform {
    val name: String = "Java ${System.getProperty("java.version")}"
}

/**
 * Returns the platform information.
 *
 * @return Platform instance with name and version
 */
fun getPlatform() = Platform()
