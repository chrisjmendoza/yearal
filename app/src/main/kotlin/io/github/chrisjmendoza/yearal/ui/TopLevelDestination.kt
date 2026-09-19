package io.github.chrisjmendoza.yearal.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.chrisjmendoza.yearal.R
import io.github.chrisjmendoza.yearal.core.navigation.ConverterKey
import io.github.chrisjmendoza.yearal.core.navigation.EventListKey
import io.github.chrisjmendoza.yearal.core.navigation.MoreKey
import io.github.chrisjmendoza.yearal.core.navigation.TodayKey

/**
 * How a [TopLevelDestination] draws its tab icon: either one of the small curated set in
 * `androidx.compose.material:material-icons-core` ([Vector]), or, when core has nothing suitable and
 * pulling in the much larger `material-icons-extended` is not warranted for one glyph
 * (`docs/ARCHITECTURE.md` §1 "Stack decisions"), a hand-drawn vector drawable resource ([Resource]).
 * [IfcApp] renders either with `Icon`, choosing `ImageVector.vectorResource` /
 * `androidx.compose.ui.res.painterResource` to match.
 */
sealed interface TabIcon {
    /** An [ImageVector] constant, such as one of `Icons.Filled.*`; needs no `@Composable` context. */
    data class Vector(
        val imageVector: ImageVector,
    ) : TabIcon

    /** A drawable resource, loaded with `painterResource` at the point of use. */
    data class Resource(
        @param:DrawableRes val id: Int,
    ) : TabIcon
}

/**
 * The five top-level tabs (docs/ARCHITECTURE.md §4 "Screens and navigation"): Today | Calendar |
 * Events | Convert | More. Each owns its own back stack whose root is [rootKey].
 *
 * The Calendar tab's root is the current month, which depends on the clock, so [CALENDAR] carries no
 * static root; [IfcApp] resolves it when the tab is first opened.
 */
enum class TopLevelDestination(
    @param:StringRes val labelRes: Int,
    val icon: TabIcon,
    val rootKey: NavKey?,
) {
    TODAY(R.string.tab_today, TabIcon.Vector(Icons.Filled.Home), TodayKey),
    CALENDAR(R.string.tab_calendar, TabIcon.Vector(Icons.Filled.DateRange), null),
    EVENTS(R.string.tab_events, TabIcon.Vector(Icons.AutoMirrored.Filled.List), EventListKey),

    // Icons.Filled.Refresh (a single circular arrow) read as "refresh", not "convert"
    // (docs/reviews/2026-09-19-astra-analysis-response.md; ROADMAP R5); material-icons-core has no
    // swap/compare glyph, so this is the hand-drawn ic_convert instead.
    CONVERT(R.string.tab_convert, TabIcon.Resource(R.drawable.ic_convert), ConverterKey()),
    MORE(R.string.tab_more, TabIcon.Vector(Icons.Filled.MoreVert), MoreKey),
}
