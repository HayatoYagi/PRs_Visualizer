package io.github.hayatoyagi.prvisualizer

object SampleData {
    const val repoName: String = "example/repo"
    private const val basePrUrl: String = "https://github.com/example/repo/pull/"

    val pullRequests: List<PullRequest> = listOf(
        PullRequest(
            id = "pr-101",
            number = 101,
            title = "Refactor auth flow",
            author = "hayatoy",
            isDraft = false,
            url = "${basePrUrl}101",
            files = listOf(
                PrFileChange("app/src/main/kotlin/core/auth/login/LoginViewModel.kt", additions = 42, deletions = 18),
                PrFileChange("app/src/main/kotlin/core/auth/token/TokenStore.kt", additions = 20, deletions = 3),
                PrFileChange("app/src/main/kotlin/core/network/auth/AuthApi.kt", additions = 12, deletions = 8),
            ),
        ),
        PullRequest(
            id = "pr-102",
            number = 102,
            title = "Tree rendering performance improvements",
            author = "codex-bot",
            isDraft = false,
            url = "${basePrUrl}102",
            files = listOf(
                PrFileChange("app/src/main/kotlin/feature/visualizer/canvas/TreemapCanvas.kt", additions = 95, deletions = 32),
                PrFileChange("app/src/main/kotlin/feature/visualizer/layout/TreemapLayout.kt", additions = 63, deletions = 47),
                PrFileChange("app/src/main/kotlin/core/auth/login/LoginViewModel.kt", additions = 7, deletions = 1),
            ),
        ),
        PullRequest(
            id = "pr-103",
            number = 103,
            title = "Introduce cached repository snapshots",
            author = "alice",
            isDraft = true,
            url = "${basePrUrl}103",
            files = listOf(
                PrFileChange("app/src/main/kotlin/core/cache/tree/TreeCache.kt", additions = 80, deletions = 2),
                PrFileChange("app/src/main/kotlin/core/cache/diff/DiffCache.kt", additions = 66, deletions = 0),
                PrFileChange("app/src/main/kotlin/feature/visualizer/layout/TreemapLayout.kt", additions = 12, deletions = 10),
            ),
        ),
        PullRequest(
            id = "pr-104",
            number = 104,
            title = "Delete legacy parser",
            author = "bob",
            isDraft = false,
            url = "${basePrUrl}104",
            files = listOf(
                PrFileChange("app/src/main/kotlin/legacy/parser/LegacyPatchParser.kt", additions = 0, deletions = 140),
                PrFileChange("app/src/main/kotlin/legacy/path/LegacyPathMatcher.kt", additions = 0, deletions = 58),
            ),
        ),
    )

    val rootNode: FileNode.Directory = buildTree(
        allFiles = listOf(
            FileSeed("app/src/main/kotlin/core/auth/login/LoginViewModel.kt", 240),
            FileSeed("app/src/main/kotlin/core/auth/token/TokenStore.kt", 120),
            FileSeed("app/src/main/kotlin/core/network/auth/AuthApi.kt", 110),
            FileSeed("app/src/main/kotlin/core/cache/tree/TreeCache.kt", 180),
            FileSeed("app/src/main/kotlin/core/cache/diff/DiffCache.kt", 170),
            FileSeed("app/src/main/kotlin/feature/visualizer/canvas/TreemapCanvas.kt", 410),
            FileSeed("app/src/main/kotlin/feature/visualizer/layout/TreemapLayout.kt", 310),
            FileSeed("app/src/main/kotlin/feature/visualizer/interaction/PointerInspector.kt", 156),
            FileSeed("app/src/main/kotlin/feature/sidebar/components/PrListItem.kt", 144),
            FileSeed("app/src/main/kotlin/feature/sidebar/components/FilterToggle.kt", 98),
            FileSeed("app/src/main/kotlin/legacy/parser/LegacyPatchParser.kt", 145),
            FileSeed("app/src/main/kotlin/legacy/path/LegacyPathMatcher.kt", 72),
            FileSeed("app/src/test/kotlin/core/auth/login/LoginViewModelTest.kt", 130),
            FileSeed("app/src/test/kotlin/feature/visualizer/layout/TreemapLayoutTest.kt", 162),
            FileSeed("scripts/dev/fixtures/sample_prs.json", 80),
            FileSeed("scripts/dev/fixtures/sample_tree.json", 60),
            FileSeed("docs/specs/Architecture.md", 90),
            FileSeed("docs/specs/Interaction.md", 70),
            FileSeed("docs/notes/2026-02-18-session.md", 32),
            FileSeed("README.md", 60),
        ),
        activePaths = pullRequests
            .flatMap { it.files }
            .map { it.path }
            .toSet(),
    )

    private data class FileSeed(val path: String, val lines: Int)

    private fun buildTree(allFiles: List<FileSeed>, activePaths: Set<String>): FileNode.Directory {
        data class MutableDir(val path: String, val name: String, val children: MutableList<Any> = mutableListOf())

        val root = MutableDir(path = "", name = "repo")
        val dirsByPath = mutableMapOf("" to root)

        fun ensureDir(path: String): MutableDir {
            return dirsByPath.getOrPut(path) {
                val parentPath = path.substringBeforeLast('/', missingDelimiterValue = "")
                val dirName = path.substringAfterLast('/')
                val parent = ensureDir(parentPath)
                val newDir = MutableDir(path = path, name = dirName)
                parent.children += newDir
                newDir
            }
        }

        allFiles.forEach { file ->
            val parentPath = file.path.substringBeforeLast('/', missingDelimiterValue = "")
            val dir = ensureDir(parentPath)
            dir.children += file
        }

        fun freeze(dir: MutableDir): FileNode.Directory {
            val frozenChildren = dir.children.map { child ->
                when (child) {
                    is MutableDir -> freeze(child)
                    is FileSeed -> {
                        val extension = child.path.substringAfterLast('.', missingDelimiterValue = "")
                        FileNode.File(
                            path = child.path,
                            name = child.path.substringAfterLast('/'),
                            extension = extension,
                            totalLines = child.lines,
                            hasActivePr = activePaths.contains(child.path),
                            weight = maxOf(child.lines.toDouble(), if (activePaths.contains(child.path)) 8.0 else 1.0),
                        )
                    }
                    else -> error("Unexpected node type")
                }
            }

            val weight = frozenChildren.sumOf { it.weight }.coerceAtLeast(1.0)
            return FileNode.Directory(
                path = dir.path,
                name = if (dir.path.isEmpty()) "repo" else dir.name,
                children = frozenChildren.sortedByDescending { it.weight },
                weight = weight,
            )
        }

        return freeze(root)
    }
}
