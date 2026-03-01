package io.github.hayatoyagi.prvisualizer.github

internal val BINARY_EXTENSIONS = setOf(
    // Images
    "png",
    "jpg",
    "jpeg",
    "gif",
    "bmp",
    "ico",
    "webp",
    "tiff",
    "tif",
    "avif",
    // Archives
    "zip",
    "tar",
    "gz",
    "bz2",
    "7z",
    "rar",
    "xz",
    // Executables and libraries
    "exe",
    "dll",
    "so",
    "dylib",
    "bin",
    "app",
    // Documents and fonts
    "pdf",
    "doc",
    "docx",
    "xls",
    "xlsx",
    "ppt",
    "pptx",
    "ttf",
    "otf",
    "woff",
    "woff2",
    // Media
    "mp3",
    "mp4",
    "avi",
    "mov",
    "wav",
    "flac",
    "ogg",
    "webm",
    // Disk images
    "dmg",
    "iso",
    // Databases
    "db",
    "sqlite",
    // Other binary formats
    "class",
    "jar",
    "war",
    "pyc",
    "o",
    "a",
    "lib",
)

internal fun isBinaryFile(path: String): Boolean {
    val lastDotIndex = path.lastIndexOf('.')
    if (lastDotIndex == -1) return false
    val extension = path.substring(lastDotIndex + 1).lowercase()
    return extension.isNotEmpty() && extension in BINARY_EXTENSIONS
}
