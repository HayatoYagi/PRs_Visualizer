package io.github.hayatoyagi.prvisualizer.ui.treemap

import io.github.hayatoyagi.prvisualizer.TreemapNode

/**
 * Generates a unique key for a treemap node.
 *
 * @param node The treemap node to generate a key for
 * @return A unique string key prefixed with "D:" for directories or "F:" for files
 */
fun nodeKey(node: TreemapNode): String = if (node.isDirectory) "D:${node.path}" else "F:${node.path}"
