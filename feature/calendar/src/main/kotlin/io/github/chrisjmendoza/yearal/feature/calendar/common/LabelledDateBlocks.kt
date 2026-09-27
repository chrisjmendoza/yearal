package io.github.chrisjmendoza.yearal.feature.calendar.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens
import io.github.chrisjmendoza.yearal.feature.calendar.R

// The labelled-block rule (docs/design-plan.md §4.1, owner, 2026-09-27), shared by the Today hero and
// the Month day card so the two cannot drift: an eyebrow labels its whole block, so a weekday inside a
// block needs no label of its own, and every fact appears once — "IFC → weekday / date", "Gregorian →
// date with its real weekday", then one facts line.

/**
 * An eyebrow caption (`labelSmall`, uppercased) naming the block beneath it, e.g. "IFC" or "GREGORIAN"
 * (`docs/design-plan.md` §3.1 "Typography", §4.1).
 *
 * @param text the caption as written in resources; it is uppercased for display.
 * @param modifier applied to the text.
 */
@Composable
internal fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text = text.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = modifier)
}

/**
 * One [Eyebrow] over its value, e.g. "GREGORIAN" over "Thursday, September 17, 2026".
 *
 * @param eyebrow the caption naming the value's calendar.
 * @param value the value itself.
 * @param style the value's text style.
 */
@Composable
internal fun EyebrowValue(
    eyebrow: String,
    value: String,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs / 2)) {
        Eyebrow(eyebrow)
        Text(text = value, style = style)
    }
}

/**
 * The "IFC" block (`docs/design-plan.md` §4.1, §4.4): the "IFC" [Eyebrow], the IFC weekday bare under
 * it, then the IFC long date as the block's heading.
 *
 * **The weekday line is the IFC (nominal) weekday, never the real one** (CLAUDE.md rule 3). On Year Day
 * and Leap Day, which have none, the caller passes the "no IFC weekday" text instead (spec §4.1 item 5)
 * so the slot never stands empty. The line is spoken as [weekdaysDescription] — "IFC Sunday, actual
 * Thursday" (item 7) — so TalkBack hears both weekdays labelled on it and can never mistake the bare
 * IFC weekday for the real one.
 *
 * @param weekday the bare IFC weekday (`Sunday`), or "no IFC weekday" on an intercalary day.
 * @param weekdaysDescription both weekdays in spoken form, the weekday line's content description.
 * @param date the IFC long date (`September 8, 2026`, `Year Day, 2026`); marked as a heading.
 * @param weekdayStyle the weekday line's text style.
 * @param dateStyle the date's text style.
 * @param modifier applied to the block.
 * @param eyebrowLeading drawn before the eyebrow, e.g. the intercalary icon; `null` for none.
 * @param eyebrowTrailing drawn at the far end of the eyebrow row, e.g. the "Today" badge; `null` for none.
 */
@Composable
internal fun IfcDateBlock(
    weekday: String,
    weekdaysDescription: String,
    date: String,
    weekdayStyle: TextStyle,
    dateStyle: TextStyle,
    modifier: Modifier = Modifier,
    eyebrowLeading: (@Composable RowScope.() -> Unit)? = null,
    eyebrowTrailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        val eyebrow = stringResource(R.string.eyebrow_ifc)
        if (eyebrowLeading == null && eyebrowTrailing == null) {
            Eyebrow(eyebrow)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            ) {
                eyebrowLeading?.invoke(this)
                Eyebrow(eyebrow, modifier = Modifier.weight(1f))
                eyebrowTrailing?.invoke(this)
            }
        }
        Text(
            text = weekday,
            style = weekdayStyle,
            modifier = Modifier.semantics { contentDescription = weekdaysDescription },
        )
        Text(text = date, style = dateStyle, modifier = Modifier.semantics { heading() })
    }
}

/**
 * The "Gregorian" block (`docs/design-plan.md` §4.1, §4.4): the "Gregorian" [Eyebrow] over the real
 * date with its real weekday — the only place the real weekday is drawn.
 *
 * @param date the Gregorian long date with its weekday (`Thursday, September 17, 2026`).
 * @param style the date's text style.
 */
@Composable
internal fun GregorianDateBlock(
    date: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    EyebrowValue(eyebrow = stringResource(R.string.eyebrow_gregorian), value = date, style = style)
}

/**
 * The facts line (`docs/design-plan.md` §4.1, §4.4): the canonical numeric IFC form, the day of year
 * and week, and the quarter — `IFC 2026-10-08 · Day 260 · Week 38 of 52 · Q3`. The numeric form keeps
 * its `IFC` prefix (CLAUDE.md rule 5): here it is a fact among facts, not a second copy of the headline
 * date.
 *
 * @param numeric the numeric IFC date with its mandatory prefix (`IFC 2026-10-08`).
 * @param dayAndWeek `Day 260 · Week 38 of 52`, or `Day 169 · outside the weeks` (spec §7.4).
 * @param quarter `Q3` (spec §7.5).
 * @param style the line's text style.
 */
@Composable
internal fun FactsLine(
    numeric: String,
    dayAndWeek: String,
    quarter: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Text(text = stringResource(R.string.facts_line, numeric, dayAndWeek, quarter), style = style)
}
