package io.github.chrisjmendoza.yearal.widget.today

import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.domain.ZoneProvider
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.widget.R
import io.github.chrisjmendoza.yearal.widget.di.WidgetEntryPoint
import io.github.chrisjmendoza.yearal.widget.theme.applyWidgetBackgroundOpacity
import io.github.chrisjmendoza.yearal.widget.theme.resolveWidgetColors
import io.github.chrisjmendoza.yearal.widget.theme.shouldShowLowOpacityChip
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/**
 * The "Today" home-screen widget (FEATURES S1, S3, S4, S5; docs/ARCHITECTURE.md §5). Resizable from a
 * 2x1 "date only" cell up to a 3x2 cell that also shows the Gregorian equivalent and the actual
 * weekday.
 *
 * Every [provideGlance] call — the initial placement,
 * [androidx.glance.appwidget.GlanceAppWidget.updateAll] from
 * [io.github.chrisjmendoza.yearal.widget.WidgetRolloverListener], and the `updatePeriodMillis`
 * backstop in `today_widget_info.xml` — reads [Clock] and [ZoneProvider] through [WidgetEntryPoint] and
 * computes "today" inside the composable content itself (CLAUDE.md rule 2), so a stale render is not
 * possible: there is no cached date anywhere in this class.
 */
class TodayGlanceWidget : GlanceAppWidget() {
    /**
     * Three breakpoints (docs/ARCHITECTURE.md §5: "All widgets use `SizeMode.Responsive` with three
     * sizes"): [SMALL] shows the IFC date alone, [MEDIUM] adds the Gregorian line, [LARGE] adds the
     * labelled actual weekday too.
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val clock = entryPoint.clock()
        val zoneProvider = entryPoint.zoneProvider()
        // Read here, not inside the @Composable content below: androidx.compose.ui's NonObservableLocale
        // lint check is right that Locale.getDefault() inside a composable does not recompose on a
        // locale change, but Glance content never does either way -- a LOCALE_CHANGED broadcast reaches
        // this widget through WidgetRolloverListener, which calls updateAll and re-invokes
        // provideGlance from scratch, so reading it once per call already self-corrects (CLAUDE.md rule 9).
        val locale = Locale.getDefault()
        val formatter = IfcDateFormatter(context.resources, locale)
        val tapHint = context.getString(R.string.today_widget_tap_hint)
        // The first (current) value only, matching holidaySetProvider/observeAgendaUseCase's own
        // single-snapshot-per-render treatment above: DebouncedWidgetUpdater is what makes a *later*
        // settings change reach this render, by requesting a fresh provideGlance call, not a collection
        // kept open here (docs/design-plan.md §4.9, §5.6).
        val settings = entryPoint.settingsRepository().settings.first()
        provideContent {
            TodayWidgetContent(clock, zoneProvider, formatter, tapHint, settings, context.resources, locale)
        }
    }

    /**
     * The API 35+ system widget-picker preview (ROADMAP M5 T5; docs/ARCHITECTURE.md §5 "Picker
     * previews"), rendered through [io.github.chrisjmendoza.yearal.widget.preview.WidgetPreviewUpdater]'s
     * `setWidgetPreview` call. Reuses [TodayWidgetContent] unchanged -- the same code that renders the
     * real widget -- fed a [PREVIEW_CLOCK]/[PREVIEW_ZONE_PROVIDER] pair fixed on the same illustrative
     * sample date as `res/layout/today_widget_preview.xml` (IFC Sol 13, 2026), so the dynamic and static
     * previews can never drift apart the way two separately hand-built mock-ups could.
     */
    override suspend fun providePreview(
        context: Context,
        widgetCategory: Int,
    ) {
        val locale = Locale.getDefault()
        val formatter = IfcDateFormatter(context.resources, locale)
        val tapHint = context.getString(R.string.today_widget_tap_hint)
        provideContent {
            // A fixed UserSettings.DEFAULT, not a live SettingsRepository read: a picker preview must
            // not depend on live data (see this class's own KDoc for PREVIEW_CLOCK/PREVIEW_ZONE_PROVIDER).
            TodayWidgetContent(
                PREVIEW_CLOCK,
                PREVIEW_ZONE_PROVIDER,
                formatter,
                tapHint,
                UserSettings.DEFAULT,
                context.resources,
                locale,
            )
        }
    }

    /** Breakpoints shared with `today_widget_info.xml`'s min/max resize attributes. */
    companion object {
        /**
         * 2 cells wide, 1 cell tall: the IFC date only. Height is 48dp, not the nominal-cell 40dp
         * (`70dp * 1 cell - 30dp`), because that is also `today_widget_info.xml`'s `minHeight` --
         * raised to the 48dp touch-target floor for the whole widget's single tap target (ROADMAP M8
         * T1, accessibility audit finding #15). Keeping this constant in step with that floor matters
         * because [TodayWidgetContent] compares `LocalSize.current` to this exact value while composing
         * at [SMALL] size (`size.height > SMALL.height`, always false there since the two are equal):
         * if this stayed at 40dp
         * while the true minimum height became 48dp, the "date only" appearance below would still be
         * correct in code (that comparison is self-referential either way), but the constant itself
         * would no longer describe the smallest size a user can actually resize to.
         */
        val SMALL: DpSize = DpSize(110.dp, 48.dp)

        /** 3 cells wide, 1 cell tall: adds the Gregorian date line. Height matches [SMALL]'s for the
         * same reason (ROADMAP M8 T1, accessibility audit finding #15). */
        val MEDIUM: DpSize = DpSize(180.dp, 48.dp)

        /** 3 cells wide, 2 cells tall: adds the labelled actual weekday. */
        val LARGE: DpSize = DpSize(180.dp, 110.dp)

        /**
         * A fixed instant resolving, in [PREVIEW_ZONE_PROVIDER]'s zone, to Gregorian June 30, 2026 --
         * IFC Sol 13, 2026, the same sample date named in `res/layout/today_widget_preview.xml`'s KDoc.
         * [Clock.fixed] and a literal [ZoneProvider] are ordinary `java.time`/domain values, not a
         * fake `Clock.systemUTC()` call (CLAUDE.md rule 2 is about *live* dates; a picker preview is
         * deliberately never live).
         */
        internal val PREVIEW_CLOCK: Clock = Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneOffset.UTC)

        /** Paired with [PREVIEW_CLOCK]; UTC keeps the sample deterministic regardless of test/device zone. */
        internal val PREVIEW_ZONE_PROVIDER = ZoneProvider { ZoneOffset.UTC }
    }
}

/**
 * The widget's content. Recomputes "today" itself on every composition, from [clock] and
 * [zoneProvider] — no parameter here is a cached date — and reads [LocalSize] to decide which lines
 * fit, matching [TodayGlanceWidget]'s responsive breakpoints. [formatter], [tapHint], [settings],
 * [resources] and [locale] are read once per [TodayGlanceWidget.provideGlance] call, since none of them
 * depend on the date.
 *
 * **Colours** follow the app's appearance (`docs/design-plan.md` §4.9, §5.6):
 * [io.github.chrisjmendoza.yearal.widget.theme.resolveWidgetColors] resolves [settings] and
 * [UserSettings.todayWidgetTheme] to a forced or system-following [androidx.glance.material3.ColorProviders],
 * or `null` when Material You dynamic colour applies, in which case `GlanceTheme.colors` is used exactly
 * as before this widget followed appearance settings at all.
 *
 * **Background opacity** ([UserSettings.widgetBackgroundOpacity]) is applied only to the widget's own
 * outer background -- never to the text itself -- via
 * [io.github.chrisjmendoza.yearal.widget.theme.applyWidgetBackgroundOpacity]. When
 * [io.github.chrisjmendoza.yearal.widget.theme.shouldShowLowOpacityChip] says so, the text block sits on
 * its own solid chip (the *opaque* widget-background colour, unaffected by the opacity setting) so it
 * stays legible however transparent the widget itself is -- `:widget` has no `surfaceContainer` role to
 * draw this chip from (`androidx.glance.color.ColorProviders` only exposes the pre-tonal-surface
 * Material 3 roles), so the opaque widget-background colour is the nearest available one, the same
 * substitution [io.github.chrisjmendoza.yearal.widget.month.MonthGlanceWidget] makes for its day cells
 * and, since ROADMAP M8 T1 (accessibility audit finding #16), for its own title/span/header block.
 */
@Composable
private fun TodayWidgetContent(
    clock: Clock,
    zoneProvider: ZoneProvider,
    formatter: IfcDateFormatter,
    tapHint: String,
    settings: UserSettings,
    resources: Resources,
    locale: Locale,
) {
    val colors = resolveWidgetColors(settings, settings.todayWidgetTheme, Build.VERSION.SDK_INT) ?: GlanceTheme.colors
    GlanceTheme(colors = colors) {
        val context = LocalContext.current
        // Read fresh on every composition, per CLAUDE.md rule 2 -- never `remember`, never a value
        // computed once outside this function and passed down.
        val date = todayDate(clock, zoneProvider)
        val state = buildTodayWidgetState(date, formatter, tapHint, resources, locale)
        val size = LocalSize.current

        val opaqueBackground = GlanceTheme.colors.widgetBackground.getColor(context)
        val translucentBackground =
            ColorProvider(applyWidgetBackgroundOpacity(opaqueBackground, settings.widgetBackgroundOpacity))

        var modifier =
            GlanceModifier
                .fillMaxSize()
                .background(translucentBackground)
                .padding(12.dp)
                .semantics { contentDescription = state.contentDescription }
        // todayLaunchIntent is null only if the platform cannot resolve this app's own launcher activity
        // (docs/security-and-privacy.md §6.4 requires an explicit intent, so there is nothing safe to
        // launch in that case); the widget still shows the date, just without a tap action.
        todayLaunchIntent(context)?.let { intent ->
            modifier = modifier.clickable(actionStartActivity(intent))
        }

        val lowOpacity = shouldShowLowOpacityChip(settings.widgetBackgroundOpacity)
        val textBlockModifier =
            if (lowOpacity) {
                GlanceModifier.background(ColorProvider(opaqueBackground)).cornerRadius(8.dp).padding(6.dp)
            } else {
                GlanceModifier
            }

        Column(
            modifier = modifier,
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(modifier = textBlockModifier) {
                Text(
                    text = state.primaryLabel,
                    style =
                        TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface,
                        ),
                )
                if (size.height > TodayGlanceWidget.SMALL.height || size.width > TodayGlanceWidget.SMALL.width) {
                    Text(
                        text = state.gregorianLabel,
                        style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    )
                }
                if (size.height >= TodayGlanceWidget.LARGE.height) {
                    Text(
                        text = state.actualWeekdayLabel,
                        style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    )
                    // The LARGE-size extras (design-plan §4.9, "large widgets should show more"): the
                    // year-progress line always has a value; the countdown is null only past
                    // IfcDate.MAX_YEAR (CLAUDE.md rule 6 -- handled, not silently dropped).
                    Text(
                        text = state.yearProgressLabel,
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    )
                    state.countdownLabel?.let { countdown ->
                        Text(
                            text = countdown,
                            style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.tertiary),
                        )
                    }
                }
            }
        }
    }
}
