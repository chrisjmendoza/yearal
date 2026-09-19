package io.github.chrisjmendoza.yearal.core.designsystem.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex

/** [TwoPaneLayout]'s default share of the width given to [TwoPaneLayout]'s `list` slot. */
private const val DEFAULT_LIST_WEIGHT = 0.38f

/**
 * The list-detail Scene for expanded widths (docs/ARCHITECTURE.md §4 "Adaptive layouts"): [list] on
 * the left, a divider, [detail] on the right. Used by both `:feature:calendar` (Month + Day detail)
 * and `:feature:events` (the events list + editor), so the two features agree on the same split and
 * accessibility ordering without depending on each other (CLAUDE.md rule 10).
 *
 * **Why this instead of Navigation 3's own list-detail Scene:** at the versions pinned in
 * `gradle/libs.versions.toml` (nav3 = "1.1.7"; no `adaptive-navigation3` artifact is resolvable —
 * checked against the local Gradle module cache for this task), there is no published Nav3 `SceneStrategy`
 * that renders two back stack entries side by side. This is the "small custom two-pane Scene"
 * docs/ARCHITECTURE.md §4 already names as the fallback: a plain two-`Box` `Row`, not a `SceneStrategy`
 * — the list and detail content are composed directly inside the existing single-entry screen
 * (`MonthRoute`, `EventListRoute`), so no change to `:app`'s `NavDisplay` wiring is needed.
 *
 * Each pane is its own semantics traversal group ([isTraversalGroup]), [list] ordered before [detail]
 * ([traversalIndex]), so TalkBack reads the whole list, then the whole detail pane, rather than
 * interleaving rows across the divider.
 *
 * @param list the master pane's content, e.g. the Month grid or the events list.
 * @param detail the detail pane's content, e.g. the Day detail or the event editor, or an empty-state
 * message before anything is selected.
 * @param modifier applied to the root [Row].
 * @param listWeight [list]'s share of the available width, `0f..1f`; [detail] takes the rest.
 */
@Composable
fun TwoPaneLayout(
    list: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    listWeight: Float = DEFAULT_LIST_WEIGHT,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .weight(listWeight)
                    .fillMaxHeight()
                    .semantics {
                        isTraversalGroup = true
                        traversalIndex = 0f
                    },
        ) {
            list()
        }
        VerticalDivider()
        Box(
            modifier =
                Modifier
                    .weight(1f - listWeight)
                    .fillMaxHeight()
                    .semantics {
                        isTraversalGroup = true
                        traversalIndex = 1f
                    },
        ) {
            detail()
        }
    }
}
