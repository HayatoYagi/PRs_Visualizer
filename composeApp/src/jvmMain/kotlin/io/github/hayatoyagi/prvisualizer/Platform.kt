package io.github.hayatoyagi.prvisualizer

class Platform {
    val name: String = "Java ${System.getProperty("java.version")}"
}

fun getPlatform() = Platform()
