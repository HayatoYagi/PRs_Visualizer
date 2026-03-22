package io.github.hayatoyagi.prvisualizer.state

import kotlin.ConsistentCopyVisibility

sealed interface PrSelection {
    data object AllVisible : PrSelection

    @ConsistentCopyVisibility
    data class Explicit private constructor(
        val ids: Set<String>,
    ) : PrSelection {
        companion object {
            fun create(ids: Set<String>): Explicit = Explicit(ids = ids)
        }
    }

    companion object {
        fun allVisible(): PrSelection = AllVisible

        fun none(): PrSelection = Explicit.create(emptySet())
    }
}
