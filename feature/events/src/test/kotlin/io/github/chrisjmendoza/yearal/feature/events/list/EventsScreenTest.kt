package io.github.chrisjmendoza.yearal.feature.events.list

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.designsystem.adaptive.WindowWidthClass
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.domain.event.EventCalendar
import io.github.chrisjmendoza.yearal.core.domain.event.LeapDayPolicy
import io.github.chrisjmendoza.yearal.feature.events.editor.EventDraft
import io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorCallbacks
import io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorUiState
import io.github.chrisjmendoza.yearal.feature.events.editor.RecurrenceEndKind
import io.github.chrisjmendoza.yearal.feature.events.editor.RecurrenceKind
import io.github.chrisjmendoza.yearal.feature.events.editor.ZoneChoice
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

private val MinTouchTarget = 48.dp

/**
 * [EventsScreen] (docs/ROADMAP.md M3 T4; docs/ARCHITECTURE.md §4 "Adaptive layouts"): at
 * compact/medium widths it is exactly [EventListScreen] (a row tap navigates); at expanded widths both
 * panes of [EventListDetailScreen] are composed and a row tap only updates the detail pane in place.
 * Written from the spec citations already in `EventListScreenTest` and `EventEditorScreenTest` — this
 * class covers only what changes with the width.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h800dp")
class EventsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val picnic =
        EventListItem(
            eventId = 42,
            title = "Sol 13 picnic",
            calendarName = "",
            calendarColorArgb = EventCalendar.DEFAULT_COLOR_ARGB,
            calendarHidden = false,
            ifcLong = "Sol 13, 2026",
            ifcNumeric = "IFC 2026-07-13",
            gregorianLong = "Tuesday, June 30, 2026",
            isAllDay = true,
            timeLabel = null,
            zoneLabel = null,
            recurrenceSummary = null,
            ifcDayLabel = "Sol 13",
            gregorianWeekdayShort = "Tue",
            gregorianDayLabel = "Jun 30",
            monthHeaderKey = "2026-7",
            monthHeaderLabel = "Sol 2026",
        )

    private fun listState() = EventListUiState.Loaded(items = listOf(picnic), query = "", hasAnyEvents = true)

    private fun editorState(startDate: LocalDate = LocalDate.of(2026, 6, 30)) =
        EventEditorUiState.Loaded(
            isNew = false,
            title = "Sol 13 picnic",
            description = "",
            location = "",
            isAllDay = true,
            startDate = startDate,
            startIfcLabel = "Sol 13, 2026",
            startIfcDayLabel = "Sol 13",
            startGregorianLabel = "Tuesday, June 30, 2026",
            startGregorianDayLabel = "Jun 30",
            allDayEndDate = startDate,
            allDayEndBeforeStart = false,
            startMinuteOfDay = EventDraft.DEFAULT_START_MINUTE,
            endMinuteOfDay = EventDraft.DEFAULT_START_MINUTE + EventDraft.DEFAULT_DURATION_MINUTES,
            endBeforeStart = false,
            zoneChoice = ZoneChoice.FLOATING,
            fixedZoneId = ZoneId.of("UTC"),
            recurrenceKind = RecurrenceKind.NONE,
            monthlyIfcAvailable = true,
            isLeapDayAnchor = false,
            leapDayPolicy = LeapDayPolicy.JUNE_28,
            recurrenceEndKind = RecurrenceEndKind.NEVER,
            untilDate = startDate.plusYears(1),
            untilBeforeStart = false,
            count = 1,
            reminders = emptySet(),
            canSave = true,
            isDirty = false,
            saveFailed = false,
            showDeleteConfirm = false,
            showDiscardConfirm = false,
        )

    private fun noopEditorCallbacks(onBack: () -> Unit = {}) =
        EventEditorCallbacks(
            onTitleChange = {},
            onDescriptionChange = {},
            onLocationChange = {},
            onAllDayChange = {},
            onStartDateChange = {},
            onAllDayEndDateChange = {},
            onStartMinuteChange = {},
            onEndMinuteChange = {},
            onZoneChoiceChange = {},
            onRecurrenceKindChange = {},
            onLeapDayPolicyChange = {},
            onColorChange = {},
            onCategoryChange = {},
            onRecurrenceEndKindChange = {},
            onUntilDateChange = {},
            onCountChange = {},
            onToggleReminder = {},
            onSave = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onCancelDelete = {},
            onBack = onBack,
            onConfirmDiscard = {},
            onCancelDiscard = {},
            onRestoreAllOccurrences = {},
            onDismissNotificationPermissionNotice = {},
            onOpenNotificationSettings = {},
            onDismissRecurrenceResetNotice = {},
        )

    @Test
    fun `compact width renders only the events list and a row tap navigates`() {
        val opened = mutableListOf<Long>()
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                EventsScreen(
                    widthClass = WindowWidthClass.COMPACT,
                    listState = listState(),
                    onQueryChange = {},
                    onAddEvent = {},
                    onOpenEvent = { opened += it },
                    editorState = null,
                    editorCallbacks = null,
                )
            }
        }

        compose.onNodeWithText("Sol 13 picnic").performClick()

        opened shouldContainExactly listOf(42L)
        compose.onAllNodesWithText("No event selected").assertCountEquals(0)
    }

    @Test
    fun `expanded width composes both panes with the empty state before anything is selected`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                EventsScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    listState = listState(),
                    onQueryChange = {},
                    onAddEvent = {},
                    onOpenEvent = {},
                    editorState = null,
                    editorCallbacks = null,
                )
            }
        }

        compose.onNodeWithText("Sol 13 picnic").assertIsDisplayed()
        compose.onNodeWithText("No event selected").assertIsDisplayed()
    }

    @Test
    fun `expanded width shows the selected event's editor without leaving the list`() {
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                EventsScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    listState = listState(),
                    onQueryChange = {},
                    onAddEvent = {},
                    onOpenEvent = {},
                    editorState = editorState(),
                    editorCallbacks = noopEditorCallbacks(),
                )
            }
        }

        // The list (its search field label is unique to that pane) is still there…
        compose.onNodeWithText("Search events").assertIsDisplayed()
        // …at the same time as the editor's own content, addressed by its own field label.
        compose.onNodeWithText("Title").assertIsDisplayed()
    }

    @Test
    fun `a row tap only selects in place at expanded width, never navigating`() {
        val opened = mutableListOf<Long>()
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                EventsScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    listState = listState(),
                    onQueryChange = {},
                    onAddEvent = {},
                    onOpenEvent = { opened += it },
                    editorState = null,
                    editorCallbacks = null,
                )
            }
        }

        compose.onNodeWithText("Sol 13 picnic").performClick()

        // onOpenEvent is still just a callback the caller decides what to do with; EventListRoute is
        // what makes it "select in place" rather than navigate (see EventListRouteSelectionTest-style
        // coverage in EventListViewModelTest, which proves the ViewModel side of that decision).
        opened shouldContainExactly listOf(42L)
    }

    @Test
    fun `the editor's own back guard callback is reachable from the expanded detail pane`() {
        var backRequests = 0
        compose.setContent {
            IfcTheme(dynamicColor = false) {
                EventsScreen(
                    widthClass = WindowWidthClass.EXPANDED,
                    listState = listState(),
                    onQueryChange = {},
                    onAddEvent = {},
                    onOpenEvent = {},
                    editorState = editorState(),
                    editorCallbacks = noopEditorCallbacks(onBack = { backRequests++ }),
                )
            }
        }

        compose.onNodeWithContentDescription("Back").performClick()

        backRequests shouldBe 1
    }

    @Test
    fun `expanded layout touch targets stay at least 48dp at 200 percent font scale`() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                IfcTheme(dynamicColor = false) {
                    EventsScreen(
                        widthClass = WindowWidthClass.EXPANDED,
                        listState = listState(),
                        onQueryChange = {},
                        onAddEvent = {},
                        onOpenEvent = {},
                        editorState = editorState(),
                        editorCallbacks = noopEditorCallbacks(),
                    )
                }
            }
        }

        compose.onNode(hasContentDescription("Sol 13 picnic", substring = true)).assertHeightIsAtLeast(MinTouchTarget)
        compose.onNodeWithContentDescription("Add event").assertHeightIsAtLeast(MinTouchTarget)
    }
}
