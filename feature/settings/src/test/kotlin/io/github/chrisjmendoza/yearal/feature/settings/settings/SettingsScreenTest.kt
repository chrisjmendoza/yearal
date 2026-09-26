package io.github.chrisjmendoza.yearal.feature.settings.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.core.domain.settings.WeekdayDisplay
import io.github.chrisjmendoza.yearal.core.domain.settings.WidgetTheme
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [SettingsScreen] under Robolectric: each control reflects the state it is given, has the semantics
 * TalkBack needs (`selected`, on/off, disabled), and reports a change through its callback with the
 * right value (FEATURES W1, W2, H5; docs/ARCHITECTURE.md §4 "Accessibility"). Browsing and toggling
 * holiday sets is `feature:holidays`' own `HolidaysScreenTest` now (ROADMAP M6 T2); this screen only
 * needs to prove its "Holiday sets" row opens that screen.
 *
 * Tests of the "Delete all data" dialogs drive the two-step flow by reassigning [loadedState]
 * directly (as `feature:events`'s `EventEditorScreenTest` does), since the Compose test rule refuses a
 * second `setContent` call.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val weekdaySelections = mutableListOf<WeekdayDisplay>()
    private val themeSelections = mutableListOf<ThemeMode>()
    private val colorSourceSelections = mutableListOf<ColorSource>()
    private val paletteSelections = mutableListOf<ColorPalette>()
    private val pureBlackChanges = mutableListOf<Boolean>()
    private val todayWidgetThemeSelections = mutableListOf<WidgetTheme>()
    private val monthWidgetThemeSelections = mutableListOf<WidgetTheme>()
    private val widgetBackgroundOpacityChanges = mutableListOf<Int>()
    private var holidaysOpened = 0
    private var backPresses = 0
    private var deleteAllDataRequested = 0
    private var deleteAllDataContinued = 0
    private var deleteAllDataCancelled = 0
    private var deleteAllDataConfirmed = 0
    private var deleteAllDataDoneDismissed = 0

    private var loadedState: SettingsUiState.Loaded by
        mutableStateOf(SettingsUiState.Loaded(UserSettings.DEFAULT, dynamicColorSupported = true))

    private fun show(
        settings: UserSettings = UserSettings.DEFAULT,
        dynamicColorSupported: Boolean = true,
        deleteAllDataStep: DeleteAllDataStep = DeleteAllDataStep.NONE,
        fontScale: Float = 1f,
    ) {
        loadedState = SettingsUiState.Loaded(settings, dynamicColorSupported, deleteAllDataStep)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                IfcTheme(dynamicColor = false) {
                    SettingsScreen(
                        state = loadedState,
                        onBack = { backPresses++ },
                        onWeekdayDisplaySelected = { weekdaySelections += it },
                        onThemeModeSelected = { themeSelections += it },
                        onColorSourceSelected = { colorSourceSelections += it },
                        onPaletteSelected = { paletteSelections += it },
                        onPureBlackChanged = { pureBlackChanges += it },
                        onTodayWidgetThemeSelected = { todayWidgetThemeSelections += it },
                        onMonthWidgetThemeSelected = { monthWidgetThemeSelections += it },
                        onWidgetBackgroundOpacityChanged = { widgetBackgroundOpacityChanges += it },
                        onOpenHolidays = { holidaysOpened++ },
                        onRequestDeleteAllData = { deleteAllDataRequested++ },
                        onContinueDeleteAllData = { deleteAllDataContinued++ },
                        onCancelDeleteAllData = { deleteAllDataCancelled++ },
                        onConfirmDeleteAllData = { deleteAllDataConfirmed++ },
                        onDismissDeleteAllDataDone = { deleteAllDataDoneDismissed++ },
                    )
                }
            }
        }
    }

    private fun heading(text: String) =
        compose.onNode(hasText(text).and(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)))

    @Test
    fun `weekday options render with their explanations and the stored one is selected`() {
        show(UserSettings(weekdayDisplay = WeekdayDisplay.BOTH))

        compose.onNodeWithText("IFC weekdays are not the real weekdays", substring = true).assertIsDisplayed()
        compose
            .onNodeWithText("Both")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsSelected()
        compose.onNodeWithText("IFC weekdays with the real weekdays underneath").assertIsDisplayed()
        compose
            .onNodeWithText("Actual only")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotSelected()
        compose.onNodeWithText("Only the real weekdays of each month").assertIsDisplayed()
        compose
            .onNodeWithText("IFC only")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotSelected()
        compose.onNodeWithText("Only the perpetual IFC weekday names, identical every month").assertIsDisplayed()
    }

    @Test
    fun `the selected weekday option follows the state`() {
        show(UserSettings(weekdayDisplay = WeekdayDisplay.NOMINAL))

        compose.onNodeWithText("IFC only").assertIsSelected()
        compose.onNodeWithText("Both").assertIsNotSelected()
        compose.onNodeWithText("Actual only").assertIsNotSelected()
    }

    @Test
    fun `clicking a weekday option reports it and does not change the screen by itself`() {
        show()

        compose.onNodeWithText("Actual only").performScrollTo().performClick()

        weekdaySelections shouldContainExactly listOf(WeekdayDisplay.ACTUAL)
        // Stateless: the selection only moves once the caller passes a new state.
        compose.onNodeWithText("Both").assertIsSelected()
    }

    @Test
    fun `theme options form a radio group that reports the chosen mode`() {
        show(UserSettings(themeMode = ThemeMode.SYSTEM))

        val systemTag = SettingsTestTags.THEME_MODE_PREFIX + ThemeMode.SYSTEM.name
        val lightTag = SettingsTestTags.THEME_MODE_PREFIX + ThemeMode.LIGHT.name
        val darkTag = SettingsTestTags.THEME_MODE_PREFIX + ThemeMode.DARK.name
        compose.onNodeWithTag(systemTag).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(lightTag).performScrollTo().assertIsNotSelected()
        compose.onNodeWithTag(darkTag).performScrollTo().assertIsNotSelected()

        compose.onNodeWithTag(darkTag).performScrollTo().performClick()
        compose.onNodeWithTag(lightTag).performScrollTo().performClick()

        themeSelections shouldContainExactly listOf(ThemeMode.DARK, ThemeMode.LIGHT)
    }

    @Test
    fun `colour source segments reflect the state and report the other option on click`() {
        show(UserSettings(colorSource = ColorSource.DYNAMIC), dynamicColorSupported = true)

        compose.onNodeWithText("Yearal palette").performScrollTo().assertIsNotSelected()
        val dynamic = compose.onNodeWithText("Material You").performScrollTo()
        dynamic.assertIsSelected().assertIsEnabled()

        compose.onNodeWithText("Yearal palette").performClick()

        colorSourceSelections shouldContainExactly listOf(ColorSource.BRAND)
    }

    @Test
    fun `Material You is disabled with its reason when the device cannot support it`() {
        show(UserSettings(colorSource = ColorSource.BRAND), dynamicColorSupported = false)

        compose.onNodeWithText("Material You").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Not available on this device").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Material You").performClick()

        colorSourceSelections shouldBe emptyList()
    }

    @Test
    fun `palette swatches show the stored selection and report a click`() {
        show(UserSettings(colorSource = ColorSource.BRAND, palette = ColorPalette.SOL))

        compose.onNodeWithText("Sol").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Teal").performScrollTo().assertIsNotSelected()

        compose.onNodeWithText("Night").performScrollTo().performClick()

        paletteSelections shouldContainExactly listOf(ColorPalette.NIGHT)
    }

    // docs/design-plan.md §4.8 (ROADMAP wave 3 J3): a live preview strip beneath the swatches, in
    // whichever palette is currently selected — even while the row above is disabled.
    @Test
    fun `the palette preview strip names the selected palette and follows it`() {
        show(UserSettings(colorSource = ColorSource.BRAND, palette = ColorPalette.MOSS))

        compose.onNodeWithContentDescription("Preview of the Moss palette").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the palette preview strip still reflects the palette while Material You is selected`() {
        show(UserSettings(colorSource = ColorSource.DYNAMIC, palette = ColorPalette.INK))

        compose.onNodeWithContentDescription("Preview of the Ink palette").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the palette row is disabled and explained while Material You is the colour source`() {
        show(UserSettings(colorSource = ColorSource.DYNAMIC))

        compose
            .onNodeWithText("Palette applies when Yearal palette is the colour source", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Teal").performScrollTo().assertIsNotEnabled()

        compose.onNodeWithText("Teal").performClick()

        paletteSelections shouldBe emptyList()
    }

    // Review finding: below API 31 the theme falls back to the palette even while ColorSource.DYNAMIC is
    // stored (e.g. restored from a backup made on a newer device), so a palette choice still has a
    // visible effect and the row must not read as disabled with "no visible effect".
    @Test
    fun `the palette row stays enabled while Material You is stored but unsupported on this device`() {
        show(UserSettings(colorSource = ColorSource.DYNAMIC), dynamicColorSupported = false)

        compose.onNodeWithText("Teal").performScrollTo().assertIsEnabled()

        compose.onNodeWithText("Night").performScrollTo().performClick()

        paletteSelections shouldContainExactly listOf(ColorPalette.NIGHT)
    }

    @Test
    fun `pure black switch reflects the state and reports the opposite value on click`() {
        show(UserSettings(pureBlack = false))

        val row = compose.onNodeWithText("Pure black in dark mode").performScrollTo()
        row.assertIsEnabled()

        row.performClick()

        pureBlackChanges shouldContainExactly listOf(true)
    }

    @Test
    fun `each widget theme row reflects its own stored setting and reports a click independently`() {
        show(UserSettings(todayWidgetTheme = WidgetTheme.LIGHT, monthWidgetTheme = WidgetTheme.FOLLOW_APP))

        val todayDark = SettingsTestTags.TODAY_WIDGET_THEME_PREFIX + WidgetTheme.DARK.name
        val monthDark = SettingsTestTags.MONTH_WIDGET_THEME_PREFIX + WidgetTheme.DARK.name
        compose
            .onNodeWithTag(SettingsTestTags.TODAY_WIDGET_THEME_PREFIX + WidgetTheme.LIGHT.name)
            .performScrollTo()
            .assertIsSelected()
        compose
            .onNodeWithTag(SettingsTestTags.MONTH_WIDGET_THEME_PREFIX + WidgetTheme.FOLLOW_APP.name)
            .performScrollTo()
            .assertIsSelected()

        compose.onNodeWithTag(todayDark).performScrollTo().performClick()
        compose.onNodeWithTag(monthDark).performScrollTo().performClick()

        todayWidgetThemeSelections shouldContainExactly listOf(WidgetTheme.DARK)
        monthWidgetThemeSelections shouldContainExactly listOf(WidgetTheme.DARK)
    }

    @Test
    fun `the widget background slider shows the stored percentage and reports a new one`() {
        show(UserSettings(widgetBackgroundOpacity = 60))

        compose.onNodeWithText("Widget background: 60%", substring = true).performScrollTo().assertIsDisplayed()

        compose
            .onNodeWithTag(SettingsTestTags.WIDGET_BACKGROUND_SLIDER)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(35f) }

        widgetBackgroundOpacityChanges shouldContainExactly listOf(35)
    }

    // Review finding: the slider used to bind `value` straight to the stored percentage and persist on
    // every `onValueChange`, so one drag from 0 to 100 could write to DataStore up to 20 times and the
    // thumb lagged a step behind the finger. `SetProgress` is the accessibility action Robolectric can
    // reliably exercise for a Slider (a real multi-frame touch drag isn't simulated by this test host);
    // it drives the same onValueChange/onValueChangeFinished pair a drag's last frame and release would,
    // so it stands in for "the drag has ended" — the point being that exactly one value is reported, not
    // one per intermediate position, for a value that never equalled the stored one before this action.
    @Test
    fun `setting the widget background slider away from its stored value persists exactly one update`() {
        show(UserSettings(widgetBackgroundOpacity = 0))

        compose
            .onNodeWithTag(SettingsTestTags.WIDGET_BACKGROUND_SLIDER)
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(80f) }

        widgetBackgroundOpacityChanges shouldContainExactly listOf(80)
    }

    @Test
    fun `the widget background slider carries an accessible label`() {
        show(UserSettings(widgetBackgroundOpacity = 60))

        compose
            .onNodeWithContentDescription("Background opacity")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `the holiday sets row is reachable and opens the Holidays screen`() {
        show()

        compose
            .onNodeWithText("Holiday sets")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        holidaysOpened shouldBe 1
    }

    // A11y audit finding #25: GroupHeading ("Colour source", "Palette", "Widgets") must expose the same
    // TalkBack heading semantics as SectionHeading, or the heading-navigation gesture skips them.
    @Test
    fun `group headings are exposed as TalkBack headings`() {
        show()

        heading("Colour source").performScrollTo().assertIsDisplayed()
        heading("Palette").performScrollTo().assertIsDisplayed()
        heading("Widgets").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `back arrow calls onBack`() {
        show()

        compose.onNodeWithContentDescription("Back").assertHasClickAction().performClick()

        backPresses shouldBe 1
    }

    // ----- "Delete all data" (FEATURES W6): reachable by TalkBack, unmistakably destructive, a
    // two-step confirmation with cancel at each step. -----

    @Test
    fun `the delete-all-data row is reachable and opens the first confirmation`() {
        show()

        compose
            .onNodeWithText("Delete all data")
            .performScrollTo()
            .assertHasClickAction()
            .performClick()

        deleteAllDataRequested shouldBe 1
    }

    @Test
    fun `the first confirmation explains what will be erased and can be cancelled`() {
        show(deleteAllDataStep = DeleteAllDataStep.CONFIRM_FIRST)

        compose.onNodeWithText("Delete all data?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()

        deleteAllDataCancelled shouldBe 1
        deleteAllDataContinued shouldBe 0
    }

    @Test
    fun `continuing the first confirmation opens the final, unmistakably destructive one`() {
        show(deleteAllDataStep = DeleteAllDataStep.CONFIRM_FIRST)

        compose.onNodeWithText("Continue").performClick()

        deleteAllDataContinued shouldBe 1
    }

    @Test
    fun `the final confirmation can be cancelled without erasing anything`() {
        show(deleteAllDataStep = DeleteAllDataStep.CONFIRM_SECOND)

        compose.onNodeWithText("This can't be undone").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()

        deleteAllDataCancelled shouldBe 1
        deleteAllDataConfirmed shouldBe 0
    }

    @Test
    fun `confirming the final dialog erases everything`() {
        show(deleteAllDataStep = DeleteAllDataStep.CONFIRM_SECOND)

        // The row behind the dialog shares its text with the dialog's own confirm button.
        compose.onAllNodesWithText("Delete all data").onLast().performClick()

        deleteAllDataConfirmed shouldBe 1
    }

    @Test
    fun `the completion notice can be dismissed`() {
        show(deleteAllDataStep = DeleteAllDataStep.DONE)

        compose.onNodeWithText("All data deleted").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()

        deleteAllDataDoneDismissed shouldBe 1
    }

    @Test
    fun `no confirmation dialog shows while the step is NONE`() {
        show(deleteAllDataStep = DeleteAllDataStep.NONE)

        compose.onAllNodesWithText("Delete all data?").assertCountEquals(0)
        compose.onAllNodesWithText("This can't be undone").assertCountEquals(0)
        compose.onAllNodesWithText("All data deleted").assertCountEquals(0)
    }

    // A11y audit finding #28 (docs/ARCHITECTURE.md §4 "Accessibility"): the 48dp touch-target floor,
    // proven on the rows/controls whose sizing does not depend on Modifier.minimumInteractiveComponentSize()
    // (which this Robolectric harness cannot measure) -- the explicit heightIn/ListItem-default ones.
    @Test
    fun `key rows and segmented controls meet the 48dp touch-target floor`() {
        show()

        compose.onNodeWithText("Holiday sets").performScrollTo().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Delete all data").performScrollTo().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Yearal palette").performScrollTo().assertHeightIsAtLeast(48.dp)
        compose
            .onNodeWithTag(SettingsTestTags.TODAY_WIDGET_THEME_PREFIX + WidgetTheme.LIGHT.name)
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
    }

    // docs/ARCHITECTURE.md §4 "Accessibility": 200% font scale never clips.
    @Test
    fun `at 200 percent font scale the section headings and a control stay displayed`() {
        show(fontScale = 2f)

        compose.onNodeWithText("Appearance").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Delete all data").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `loading state shows the spinner and the app bar only`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                SettingsScreen(
                    state = SettingsUiState.Loading,
                    onBack = {},
                    onWeekdayDisplaySelected = {},
                    onThemeModeSelected = {},
                    onColorSourceSelected = {},
                    onPaletteSelected = {},
                    onPureBlackChanged = {},
                    onTodayWidgetThemeSelected = {},
                    onMonthWidgetThemeSelected = {},
                    onWidgetBackgroundOpacityChanged = {},
                    onOpenHolidays = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Loading settings").assertIsDisplayed()
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }
}
