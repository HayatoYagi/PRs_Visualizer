package io.github.hayatoyagi.prvisualizer.ui.treemap

import io.github.hayatoyagi.prvisualizer.TreemapNode

fun nodeKey(node: TreemapNode): String = if (node.isDirectory) "D:${node.path}" else "F:${node.path}"
