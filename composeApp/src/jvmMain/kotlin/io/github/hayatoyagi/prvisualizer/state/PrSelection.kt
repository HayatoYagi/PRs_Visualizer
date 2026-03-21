package io.github.hayatoyagi.prvisualizer.state

import androidx.compose.ui.state.ToggleableState
import kotlin.ConsistentCopyVisibility

sealed interface PrSelection {
    fun resolve(visibleIds: Set<String>): Set<String> = when (this) {
        AllVisible -> visibleIds
        is Explicit -> ids.intersect(visibleIds)
    }

    fun toggle(
        prId: String,
        checked: Boolean,
        visibleIds: Set<String>,
    ): PrSelection {
        val baseSelection = when (this) {
            AllVisible -> visibleIds
            is Explicit -> ids
        }
        val updatedIds = if (checked) {
            baseSelection + prId
        } else {
            baseSelection - prId
        }
        return fromExplicit(updatedIds, visibleIds)
    }

    fun triState(visibleIds: Set<String>): ToggleableState {
        if (visibleIds.isEmpty()) return ToggleableState.Off

        val resolvedSelection = resolve(visibleIds)
        return when {
            resolvedSelection.isEmpty() -> ToggleableState.Off
            resolvedSelection.size == visibleIds.size -> ToggleableState.On
            else -> ToggleableState.Indeterminate
        }
    }

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

        fun fromExplicit(
            ids: Set<String>,
            visibleIds: Set<String>,
        ): PrSelection = if (ids == visibleIds) allVisible() else Explicit.create(ids)
    }
}
