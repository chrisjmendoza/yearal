package io.github.chrisjmendoza.yearal.core.designsystem.adaptive

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

/**
 * The window-width buckets every adaptive layout in this app keys off (docs/ARCHITECTURE.md §4
 * "Adaptive layouts"). Breakpoints match the Material window size class
 * ([WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND] = 600dp,
 * [WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND] = 840dp), the same source
 * `:app`'s `NavigationSuiteScaffold` uses for its own bar-to-rail switch, so the nav chrome and the
 * screen content never disagree about how wide the window is.
 */
enum class WindowWidthClass {
    /** Narrower than 600dp: a phone in portrait. Single-pane screens, a bottom sheet, a bottom bar. */
    COMPACT,

    /** 600dp to 840dp: a small tablet or an unfolded small foldable. Still single-pane; a nav rail. */
    MEDIUM,

    /** 840dp and up: a large tablet, an unfolded foldable, or a tablet in landscape. List-detail. */
    EXPANDED,
}

/**
 * Reads the current window's width and buckets it into a [WindowWidthClass], recomposing whenever the
 * window is resized, rotated, or folded/unfolded. This is the single place that decision is made
 * (docs/ARCHITECTURE.md §4): `:feature:calendar` and `:feature:events` both call this rather than each
 * computing their own breakpoint, so a phone that is "expanded" for one is "expanded" for the other
 * (CLAUDE.md rule 10 — the two features still never depend on each other, only on this shared module).
 *
 * Backed by [currentWindowAdaptiveInfoV2], not [android.content.res.Configuration.screenWidthDp]:
 * the former updates through [androidx.compose.ui.platform.LocalWindowInfo] on every resize without
 * waiting for an activity recreation, which is what lets a fold/unfold or a drag-resize on a desktop
 * window update the layout live rather than only after a configuration change.
 */
@Composable
fun currentWindowWidthClass(): WindowWidthClass {
    val sizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return when {
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WindowWidthClass.EXPANDED
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.COMPACT
    }
}
