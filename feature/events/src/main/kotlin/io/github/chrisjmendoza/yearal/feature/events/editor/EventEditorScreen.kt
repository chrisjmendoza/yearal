package io.github.chrisjmendoza.yearal.feature.events.editor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.picker.GregorianDatePickerDialog
import io.github.chrisjmendoza.yearal.core.designsystem.picker.IfcDatePicker
import io.github.chrisjmendoza.yearal.core.designsystem.picker.rememberIfcDatePickerState
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.designsystem.theme.yearalTopAppBarColors
import io.github.chrisjmendoza.yearal.core.domain.event.EventCategory
import io.github.chrisjmendoza.yearal.core.domain.event.LeapDayPolicy
import io.github.chrisjmendoza.yearal.core.navigation.EventEditorKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import io.github.chrisjmendoza.yearal.feature.events.R
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

private val ScreenPadding = 16.dp
private val SectionSpacing = 16.dp
private val FieldSpacing = 8.dp
private val MinTouchTarget = 48.dp
private val ChipSpacing = 8.dp

/**
 * Reminder chips per row. Two keeps every chip wide enough for the longest preset label
 * ("30 minutes before") at ordinary text sizes while still reading as a grid rather than a ragged
 * pile; the five presets fall 2 / 2 / 1, and the short last row is padded so its chip keeps the same
 * width as the others rather than stretching across.
 */
private const val REMINDER_CHIPS_PER_ROW = 2

/**
 * Lines a reminder chip's label may take. Two, not one: at large font scales a half-width chip cannot
 * fit "30 minutes before" on one line, and wrapping keeps the label readable where a single line would
 * ellipsise it (docs/ARCHITECTURE.md §4 "Accessibility", 200% font scale).
 */
private const val REMINDER_LABEL_MAX_LINES = 2
private val ProgressIndicatorSize = 24.dp
private val ProgressIndicatorStroke = 2.dp

/** A colour swatch's visible size and its touch target (`docs/design-plan.md` §5.4: 40dp shown, 48dp tappable). */
private val SwatchSize = 40.dp
private val SwatchTouchTarget = 48.dp
private val SwatchSpacing = 8.dp
private val SwatchBorderWidth = 1.dp
private val ErrorOutlineWidth = 1.dp

/**
 * The event editor (`EventEditorKey`, `docs/ROADMAP.md` M4 T4): collects
 * [EventEditorViewModel.uiState] and renders it through the stateless [EventEditorScreen]. Reacts to
 * [EventEditorViewModel.editorEvents] by calling [onLeave] on
 * [EventEditorEvent.Saved]/[EventEditorEvent.Deleted]/[EventEditorEvent.NavigatedAway], and by
 * launching the `POST_NOTIFICATIONS` Activity Result request on
 * [EventEditorEvent.RequestNotificationPermission] (FEATURES E4, P2) — the request itself lives here,
 * not in the ViewModel, so [EventEditorViewModel] needs no Android permission API. Reacts to the
 * system back gesture by asking the ViewModel first, so an unsaved draft can show its guard — the
 * unsaved-changes guard and the Save/Delete in-flight guard both live in the ViewModel, so they work
 * the same way whether this composes as its own `EventEditorKey` entry (compact widths) or inline in
 * the expanded-width list-detail pane (docs/ROADMAP.md M3 T4;
 * `io.github.chrisjmendoza.yearal.feature.events.list.EventListRoute`).
 *
 * @param key which event to edit, or none for a new one.
 * @param navigator receives the "leave the editor" action by default (see [onLeave]).
 * @param onLeave invoked instead of [navigator]'s own back action when the editor is done — on save,
 * delete, or a confirmed discard. Defaults to [Navigator.goBack] (popping `EventEditorKey`'s own Nav3
 * entry, compact widths' behaviour, unchanged from before this task); the expanded-width list-detail
 * pane passes a callback that clears the selection instead, since there is no entry to pop there.
 * @param viewModelKey a distinct key for [hiltViewModel] when more than one [EventEditorViewModel] can
 * exist in the same `ViewModelStore` at once — the expanded-width pane's own case, where switching the
 * selected event reuses the same composition rather than getting a fresh Nav3 entry. `null` (the
 * default) uses the call site's own key, which is enough for `EventEditorKey`'s own Nav3 entry, where
 * a different event always means a new entry and therefore a new `ViewModelStore` regardless.
 */
@Composable
fun EventEditorRoute(
    key: EventEditorKey,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    onLeave: () -> Unit = navigator::goBack,
    viewModelKey: String? = null,
    viewModel: EventEditorViewModel =
        hiltViewModel<EventEditorViewModel, EventEditorViewModel.Factory>(
            key = viewModelKey,
            creationCallback = { factory -> factory.create(key) },
        ),
) {
    val editor = rememberEventEditorState(viewModel, onLeave)
    BackHandler(onBack = viewModel::requestBack)
    EventEditorScreen(state = editor.uiState, callbacks = editor.callbacks, modifier = modifier)
}

/**
 * Bundles an [EventEditorViewModel]'s live [EventEditorUiState] with every [EventEditorCallbacks]
 * bound to it (docs/ARCHITECTURE.md §4 "State management").
 *
 * @property uiState the ViewModel's current state.
 * @property callbacks every intent [EventEditorScreen] can report, already bound to [viewModel].
 */
internal data class EventEditorState(
    val uiState: EventEditorUiState,
    val callbacks: EventEditorCallbacks,
)

/**
 * Collects [viewModel]'s [EventEditorViewModel.uiState], wires its one-shot
 * [EventEditorViewModel.editorEvents] — [onLeave] on
 * [EventEditorEvent.Saved]/[EventEditorEvent.Deleted]/[EventEditorEvent.NavigatedAway], the
 * `POST_NOTIFICATIONS` Activity Result request on
 * [EventEditorEvent.RequestNotificationPermission] (FEATURES E4, P2) — and binds every
 * [EventEditorCallbacks] to it. Shared by [EventEditorRoute] (its own Nav3 entry) and the
 * expanded-width list-detail pane
 * ([io.github.chrisjmendoza.yearal.feature.events.list.EventListRoute], docs/ROADMAP.md M3 T4), so
 * both wire the editor's unsaved-changes guard and its Save/Delete in-flight guard exactly the same
 * way. The caller still owns the system back gesture ([EventEditorRoute]'s own [BackHandler]); this
 * helper only reacts to the ViewModel's own one-shot events.
 */
@Composable
internal fun rememberEventEditorState(
    viewModel: EventEditorViewModel,
    onLeave: () -> Unit,
): EventEditorState {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onNotificationPermissionResult(granted)
        }
    LaunchedEffect(viewModel) {
        viewModel.editorEvents.collectLatest { event ->
            when (event) {
                EventEditorEvent.RequestNotificationPermission -> {
                    // The ViewModel only sends this event on API 33+ (NotificationPermissionGate); the
                    // redundant SDK_INT check keeps the inlined API 33 constant lint-safe on minSdk 26.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                EventEditorEvent.Saved, EventEditorEvent.Deleted, EventEditorEvent.NavigatedAway -> {
                    onLeave()
                }
            }
        }
    }
    val callbacks =
        EventEditorCallbacks(
            onTitleChange = viewModel::setTitle,
            onDescriptionChange = viewModel::setDescription,
            onLocationChange = viewModel::setLocation,
            onAllDayChange = viewModel::setAllDay,
            onStartDateChange = viewModel::setStartDate,
            onAllDayEndDateChange = viewModel::setAllDayEndDate,
            onStartMinuteChange = viewModel::setStartMinuteOfDay,
            onEndMinuteChange = viewModel::setEndMinuteOfDay,
            onZoneChoiceChange = viewModel::setZoneChoice,
            onRecurrenceKindChange = viewModel::setRecurrenceKind,
            onLeapDayPolicyChange = viewModel::setLeapDayPolicy,
            onColorChange = viewModel::setColor,
            onCategoryChange = viewModel::setCategory,
            onRecurrenceEndKindChange = viewModel::setRecurrenceEndKind,
            onUntilDateChange = viewModel::setUntilDate,
            onCountChange = viewModel::setCount,
            onToggleReminder = viewModel::toggleReminder,
            onSave = viewModel::save,
            onRequestDelete = viewModel::requestDelete,
            onConfirmDelete = viewModel::confirmDelete,
            onCancelDelete = viewModel::cancelDelete,
            onBack = viewModel::requestBack,
            onConfirmDiscard = viewModel::confirmDiscard,
            onCancelDiscard = viewModel::cancelDiscard,
            onRestoreAllOccurrences = viewModel::restoreAllOccurrences,
            onDismissNotificationPermissionNotice = viewModel::dismissNotificationPermissionNotice,
            onOpenNotificationSettings = { openNotificationSettings(context) },
            onDismissRecurrenceResetNotice = viewModel::dismissRecurrenceResetNotice,
        )
    return EventEditorState(state, callbacks)
}

/**
 * Opens the app's own notification settings screen (the "quiet, dismissible explanation" of FEATURES
 * P2 links here rather than re-requesting the permission, which would only loop a denial).
 */
private fun openNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.startActivity(intent)
}

/** Every intent the stateless [EventEditorScreen] reports (grouped to keep the composable's signature short). */
data class EventEditorCallbacks(
    val onTitleChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onLocationChange: (String) -> Unit,
    val onAllDayChange: (Boolean) -> Unit,
    val onStartDateChange: (LocalDate) -> Unit,
    val onAllDayEndDateChange: (LocalDate) -> Unit,
    val onStartMinuteChange: (Int) -> Unit,
    val onEndMinuteChange: (Int) -> Unit,
    val onZoneChoiceChange: (ZoneChoice) -> Unit,
    val onRecurrenceKindChange: (RecurrenceKind) -> Unit,
    val onLeapDayPolicyChange: (LeapDayPolicy) -> Unit,
    val onColorChange: (Int?) -> Unit,
    val onCategoryChange: (EventCategory) -> Unit,
    val onRecurrenceEndKindChange: (RecurrenceEndKind) -> Unit,
    val onUntilDateChange: (LocalDate) -> Unit,
    val onCountChange: (Int) -> Unit,
    val onToggleReminder: (Int) -> Unit,
    val onSave: () -> Unit,
    val onRequestDelete: () -> Unit,
    val onConfirmDelete: () -> Unit,
    val onCancelDelete: () -> Unit,
    val onBack: () -> Unit,
    val onConfirmDiscard: () -> Unit,
    val onCancelDiscard: () -> Unit,
    val onRestoreAllOccurrences: () -> Unit,
    val onDismissNotificationPermissionNotice: () -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val onDismissRecurrenceResetNotice: () -> Unit,
)

/** Which date field a picker dialog is currently editing. */
private enum class PickerTarget { START, ALL_DAY_END, UNTIL }

/**
 * The stateless event editor — the unit for previews and Compose tests. Title/notes/location, the
 * colour row and category control (`docs/design-plan.md` §5.4), all-day vs timed with start/end date
 * and time (either calendar — FEATURES E2), device vs fixed zone, the recurrence chooser with the Leap
 * Day policy and end condition, reminder chips, and save/delete. The selected recurrence/policy/end row
 * fills with `secondaryContainer`; validation errors show as an `errorContainer` banner with the
 * offending field or button outlined in `error`; Save is a filled button in the app bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    state: EventEditorUiState,
    callbacks: EventEditorCallbacks,
    modifier: Modifier = Modifier,
    formatter: IfcDateFormatter = rememberIfcDateFormatter(),
) {
    var chooserTarget by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    var gregorianPickerTarget by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    var ifcPickerTarget by rememberSaveable { mutableStateOf<PickerTarget?>(null) }
    var timePickerIsStart by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val loaded = state as? EventEditorUiState.Loaded

    fun dateFor(
        target: PickerTarget,
        current: EventEditorUiState.Loaded,
    ): LocalDate =
        when (target) {
            PickerTarget.START -> current.startDate
            PickerTarget.ALL_DAY_END -> current.allDayEndDate
            PickerTarget.UNTIL -> current.untilDate
        }

    fun onDatePicked(
        target: PickerTarget,
        date: LocalDate,
    ) {
        when (target) {
            PickerTarget.START -> callbacks.onStartDateChange(date)
            PickerTarget.ALL_DAY_END -> callbacks.onAllDayEndDateChange(date)
            PickerTarget.UNTIL -> callbacks.onUntilDateChange(date)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            EditorTopBar(state = state, callbacks = callbacks)
        },
    ) { padding ->
        when (state) {
            EventEditorUiState.Loading -> {
                Unit
            }

            EventEditorUiState.NotFound -> {
                Text(
                    text = stringResource(R.string.events_editor_not_found),
                    modifier = Modifier.padding(padding).padding(ScreenPadding),
                )
            }

            is EventEditorUiState.Loaded -> {
                EditorBody(
                    state = state,
                    callbacks = callbacks,
                    formatter = formatter,
                    modifier = Modifier.padding(padding),
                    onPickStart = { chooserTarget = PickerTarget.START },
                    onPickAllDayEnd = { chooserTarget = PickerTarget.ALL_DAY_END },
                    onPickUntil = { chooserTarget = PickerTarget.UNTIL },
                    onPickStartTime = { timePickerIsStart = true },
                    onPickEndTime = { timePickerIsStart = false },
                )
                if (state.showDeleteConfirm) {
                    DeleteConfirmDialog(onConfirm = callbacks.onConfirmDelete, onDismiss = callbacks.onCancelDelete)
                }
                if (state.showDiscardConfirm) {
                    DiscardConfirmDialog(onConfirm = callbacks.onConfirmDiscard, onDismiss = callbacks.onCancelDiscard)
                }
            }
        }
    }

    if (loaded != null) {
        chooserTarget?.let { target ->
            CalendarChooserDialog(
                onPickGregorian = {
                    chooserTarget = null
                    gregorianPickerTarget = target
                },
                onPickIfc = {
                    chooserTarget = null
                    ifcPickerTarget = target
                },
                onDismiss = { chooserTarget = null },
            )
        }
        gregorianPickerTarget?.let { target ->
            GregorianDatePickerDialog(
                initialDate = dateFor(target, loaded),
                onConfirm = { date ->
                    gregorianPickerTarget = null
                    onDatePicked(target, date)
                },
                onDismiss = { gregorianPickerTarget = null },
            )
        }
        ifcPickerTarget?.let { target ->
            IfcDateChooserDialog(
                initialDate = dateFor(target, loaded),
                onConfirm = { date ->
                    ifcPickerTarget = null
                    onDatePicked(target, date)
                },
                onDismiss = { ifcPickerTarget = null },
            )
        }
        timePickerIsStart?.let { isStart ->
            EditorTimePickerDialog(
                initialMinuteOfDay = if (isStart) loaded.startMinuteOfDay else loaded.endMinuteOfDay,
                onConfirm = { minute ->
                    timePickerIsStart = null
                    if (isStart) callbacks.onStartMinuteChange(minute) else callbacks.onEndMinuteChange(minute)
                },
                onDismiss = { timePickerIsStart = null },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    state: EventEditorUiState,
    callbacks: EventEditorCallbacks,
) {
    val loaded = state as? EventEditorUiState.Loaded
    TopAppBar(
        title = {
            Text(
                stringResource(
                    if (loaded?.isNew != false) R.string.events_editor_title_new else R.string.events_editor_title_edit,
                ),
            )
        },
        navigationIcon = {
            IconButton(onClick = callbacks.onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.events_editor_back),
                )
            }
        },
        actions = {
            if (loaded != null && !loaded.isNew) {
                IconButton(onClick = callbacks.onRequestDelete, enabled = !loaded.isSaving) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.events_editor_delete))
                }
            }
            // ROADMAP R2: a save or delete in flight shows progress and disables Save, so a second tap
            // before the first write lands cannot start a second one.
            if (loaded?.isSaving == true) {
                val savingDescription = stringResource(R.string.events_editor_saving)
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(ProgressIndicatorSize).padding(horizontal = FieldSpacing / 2).semantics {
                            contentDescription = savingDescription
                        },
                    strokeWidth = ProgressIndicatorStroke,
                )
            }
            // docs/design-plan.md §4.5: a filled button, not a bare check icon, so Save reads as the
            // screen's primary action.
            Button(
                onClick = callbacks.onSave,
                enabled = loaded != null && loaded.canSave && !loaded.isSaving,
                modifier = Modifier.padding(end = FieldSpacing),
            ) {
                Text(stringResource(R.string.events_editor_save))
            }
        },
        colors = yearalTopAppBarColors(),
    )
}

@Composable
private fun EditorBody(
    state: EventEditorUiState.Loaded,
    callbacks: EventEditorCallbacks,
    formatter: IfcDateFormatter,
    modifier: Modifier,
    onPickStart: () -> Unit,
    onPickAllDayEnd: () -> Unit,
    onPickUntil: () -> Unit,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing),
    ) {
        if (state.saveFailed) {
            ErrorBanner(stringResource(R.string.events_editor_save_failed))
        }
        OutlinedTextField(
            value = state.title,
            onValueChange = callbacks.onTitleChange,
            label = { Text(stringResource(R.string.events_editor_title_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = callbacks.onDescriptionChange,
            label = { Text(stringResource(R.string.events_editor_notes_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.location,
            onValueChange = callbacks.onLocationChange,
            label = { Text(stringResource(R.string.events_editor_location_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        ColorSection(selected = state.colorArgb, onChange = callbacks.onColorChange)
        CategorySection(selected = state.category, onChange = callbacks.onCategoryChange)

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.events_editor_all_day_label), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.isAllDay, onCheckedChange = callbacks.onAllDayChange)
        }

        DateField(
            label = stringResource(R.string.events_editor_start_label),
            ifcLabel = state.startIfcLabel,
            gregorianLabel = state.startGregorianLabel,
            onClick = onPickStart,
        )
        if (state.isAllDay) {
            DateField(
                label = stringResource(R.string.events_editor_end_date_label),
                ifcLabel = null,
                gregorianLabel = formatter.formatGregorianLong(state.allDayEndDate),
                onClick = onPickAllDayEnd,
                isError = state.allDayEndBeforeStart,
            )
            if (state.allDayEndBeforeStart) ErrorBanner(stringResource(R.string.events_editor_end_before_start))
        } else {
            TimeRow(
                startMinute = state.startMinuteOfDay,
                endMinute = state.endMinuteOfDay,
                onPickStartTime = onPickStartTime,
                onPickEndTime = onPickEndTime,
                endIsError = state.endBeforeStart,
            )
            if (state.endBeforeStart) ErrorBanner(stringResource(R.string.events_editor_end_before_start))
            ZoneChoiceRow(
                zoneChoice = state.zoneChoice,
                fixedZoneId = state.fixedZoneId.id,
                onChange = callbacks.onZoneChoiceChange,
            )
        }

        RecurrenceSection(state = state, callbacks = callbacks, formatter = formatter, onPickUntil = onPickUntil)

        if (state.recurrenceKind != RecurrenceKind.NONE) {
            SeriesNotice()
            if (state.exdateCount > 0) {
                RestoreOccurrencesRow(count = state.exdateCount, onRestoreAll = callbacks.onRestoreAllOccurrences)
            }
        }

        ReminderSection(reminders = state.reminders, onToggle = callbacks.onToggleReminder)

        if (state.showNotificationPermissionNotice) {
            NotificationPermissionNotice(
                onOpenSettings = callbacks.onOpenNotificationSettings,
                onDismiss = callbacks.onDismissNotificationPermissionNotice,
            )
        }
    }
}

/**
 * The colour row (`docs/design-plan.md` §5.4): the seven fixed [EventColorSwatches], plus
 * "Calendar colour" (resets [selected] to `null`, inheriting the calendar's own colour) last. Each
 * swatch is a 40dp circle with a 48dp touch target (`docs/ARCHITECTURE.md` §4 "Accessibility"); the
 * selected one shows a check mark whose tint follows the swatch's own luminance so it stays legible on
 * every hue — colour is never the only signal (design-plan §2), so the selection is also carried in the
 * semantics via [Role.RadioButton]'s own `selected` state (from `Modifier.selectable`). The content
 * description is just the swatch's name — never suffixed with ", selected" — because `selectable`
 * already exposes that state on its own; saying it in the description too made TalkBack announce it
 * twice (design-pass fix 4).
 */
@Composable
private fun ColorSection(
    selected: Int?,
    onChange: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing / 2)) {
        Text(stringResource(R.string.events_editor_color_heading), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(SwatchSpacing),
        ) {
            for (swatch in EventColorSwatches.ALL) {
                ColorSwatch(
                    colorArgb = swatch.colorArgb,
                    name = stringResource(swatch.nameRes),
                    isSelected = selected == swatch.colorArgb,
                    onClick = { onChange(swatch.colorArgb) },
                )
            }
            CalendarColorSwatch(isSelected = selected == null, onClick = { onChange(null) })
        }
    }
}

@Composable
private fun ColorSwatch(
    colorArgb: Int,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val color = Color(colorArgb)
    val checkTint = if (color.luminance() > SWATCH_LUMINANCE_THRESHOLD) Color.Black else Color.White
    Box(
        modifier =
            Modifier
                .size(SwatchTouchTarget)
                .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
                .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(SwatchSize).background(color = color, shape = CircleShape)) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = checkTint,
                    modifier = Modifier.fillMaxSize().padding(FieldSpacing / 2),
                )
            }
        }
    }
}

/**
 * The "Calendar colour" swatch: neutral, outlined, resets [EventDraft.colorArgb] to `null`. Same
 * "selected" semantics as [ColorSwatch] (design-pass fix 4): `selectable` already carries the selection state, so
 * the description is just the swatch's own name, never suffixed.
 */
@Composable
private fun CalendarColorSwatch(
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val name = stringResource(R.string.events_editor_color_calendar)
    Box(
        modifier =
            Modifier
                .size(SwatchTouchTarget)
                .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
                .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(SwatchSize)
                    .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                    .border(SwatchBorderWidth, MaterialTheme.colorScheme.outline, CircleShape),
        ) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize().padding(FieldSpacing / 2),
                )
            }
        }
    }
}

/** Above which [androidx.compose.ui.graphics.Color.luminance] a check mark is drawn black rather than white. */
private const val SWATCH_LUMINANCE_THRESHOLD = 0.5f

/**
 * The category control (`docs/design-plan.md` §5.4): a three-way segmented row, Event / Observance /
 * Birthday. `EventCategory.EVENT` gets no chip on the list row, but is still a real, selectable choice
 * here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySection(
    selected: EventCategory,
    onChange: (EventCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing / 2)) {
        Text(stringResource(R.string.events_editor_category_heading), style = MaterialTheme.typography.titleSmall)
        val options = EventCategory.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, category ->
                SegmentedButton(
                    selected = selected == category,
                    onClick = { onChange(category) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                ) { Text(categoryLabel(category)) }
            }
        }
    }
}

@Composable
private fun categoryLabel(category: EventCategory): String =
    when (category) {
        EventCategory.EVENT -> stringResource(R.string.events_category_event)
        EventCategory.OBSERVANCE -> stringResource(R.string.events_category_observance)
        EventCategory.BIRTHDAY -> stringResource(R.string.events_category_birthday)
    }

@Composable
private fun DateField(
    label: String,
    ifcLabel: String?,
    gregorianLabel: String,
    onClick: () -> Unit,
    isError: Boolean = false,
) {
    val description =
        if (ifcLabel != null) {
            stringResource(R.string.events_editor_date_description, label, ifcLabel, gregorianLabel)
        } else {
            stringResource(R.string.events_editor_date_description_gregorian_only, label, gregorianLabel)
        }
    Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing / 2)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = onClick,
            modifier =
                Modifier.fillMaxWidth().heightIn(min = MinTouchTarget).semantics {
                    contentDescription =
                        description
                },
            border = errorOutlinedBorder(isError),
            colors = errorOutlinedButtonColors(isError),
        ) {
            Text(
                if (ifcLabel != null) {
                    stringResource(R.string.events_editor_date_pair, ifcLabel, gregorianLabel)
                } else {
                    gregorianLabel
                },
            )
        }
    }
}

@Composable
private fun TimeRow(
    startMinute: Int,
    endMinute: Int,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
    endIsError: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FieldSpacing)) {
        OutlinedButton(onClick = onPickStartTime, modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget)) {
            Text(formatMinuteOfDayLabel(startMinute))
        }
        OutlinedButton(
            onClick = onPickEndTime,
            modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget),
            border = errorOutlinedBorder(endIsError),
            colors = errorOutlinedButtonColors(endIsError),
        ) {
            Text(formatMinuteOfDayLabel(endMinute))
        }
    }
}

/**
 * The outline an [OutlinedButton] shows when the field it represents fails validation
 * (`docs/design-plan.md` §4.5: "the offending field/button row shows an error outline"): the error
 * colour instead of the default outline, otherwise unchanged.
 */
@Composable
private fun errorOutlinedBorder(isError: Boolean): BorderStroke =
    BorderStroke(
        width = ErrorOutlineWidth,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
    )

/** The label colour paired with [errorOutlinedBorder], so an invalid field's text is red too. */
@Composable
private fun errorOutlinedButtonColors(isError: Boolean) =
    if (isError) {
        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    } else {
        ButtonDefaults.outlinedButtonColors()
    }

@Composable
private fun ZoneChoiceRow(
    zoneChoice: ZoneChoice,
    fixedZoneId: String,
    onChange: (ZoneChoice) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = zoneChoice == ZoneChoice.FLOATING,
            onClick = { onChange(ZoneChoice.FLOATING) },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
            modifier = Modifier.heightIn(min = MinTouchTarget),
        ) { Text(stringResource(R.string.events_editor_zone_floating)) }
        SegmentedButton(
            selected = zoneChoice == ZoneChoice.FIXED,
            onClick = { onChange(ZoneChoice.FIXED) },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
            modifier = Modifier.heightIn(min = MinTouchTarget),
        ) { Text(stringResource(R.string.events_editor_zone_fixed, fixedZoneId)) }
    }
}

@Composable
private fun RecurrenceSection(
    state: EventEditorUiState.Loaded,
    callbacks: EventEditorCallbacks,
    formatter: IfcDateFormatter,
    onPickUntil: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing)) {
        Text(stringResource(R.string.events_editor_recurrence_heading), style = MaterialTheme.typography.titleSmall)
        Column(modifier = Modifier.selectableGroup()) {
            RecurrenceOption(
                label = stringResource(R.string.events_recurrence_option_none),
                selected = state.recurrenceKind == RecurrenceKind.NONE,
                onClick = { callbacks.onRecurrenceKindChange(RecurrenceKind.NONE) },
            )
            RecurrenceOption(
                label = stringResource(R.string.events_recurrence_option_yearly_ifc, state.startIfcDayLabel),
                selected = state.recurrenceKind == RecurrenceKind.YEARLY_IFC,
                onClick = { callbacks.onRecurrenceKindChange(RecurrenceKind.YEARLY_IFC) },
                supportingText = yearlyIfcSupportingText(state),
            )
            RecurrenceOption(
                label =
                    stringResource(
                        R.string.events_recurrence_option_yearly_gregorian,
                        state.startGregorianDayLabel,
                    ),
                selected = state.recurrenceKind == RecurrenceKind.YEARLY_GREGORIAN,
                onClick = { callbacks.onRecurrenceKindChange(RecurrenceKind.YEARLY_GREGORIAN) },
                supportingText = yearlyGregorianSupportingText(state),
            )
            if (state.monthlyIfcAvailable) {
                RecurrenceOption(
                    label = stringResource(R.string.events_recurrence_option_monthly_ifc),
                    selected = state.recurrenceKind == RecurrenceKind.MONTHLY_IFC,
                    onClick = { callbacks.onRecurrenceKindChange(RecurrenceKind.MONTHLY_IFC) },
                    supportingText =
                        stringResource(
                            R.string.events_editor_recurrence_explainer_monthly_ifc,
                            IfcDate.from(state.startDate).dayOfMonth,
                        ),
                )
            }
            RecurrenceOption(
                label = stringResource(R.string.events_recurrence_option_weekly),
                selected = state.recurrenceKind == RecurrenceKind.WEEKLY,
                onClick = { callbacks.onRecurrenceKindChange(RecurrenceKind.WEEKLY) },
                supportingText = stringResource(R.string.events_editor_recurrence_explainer_weekly),
            )
        }
        if (state.recurrenceKind != RecurrenceKind.NONE) {
            if (state.recurrenceKind == RecurrenceKind.YEARLY_IFC && state.isLeapDayAnchor) {
                LeapDayPolicySection(policy = state.leapDayPolicy, onChange = callbacks.onLeapDayPolicyChange)
            }
            RecurrenceEndSection(state = state, callbacks = callbacks, formatter = formatter, onPickUntil = onPickUntil)
        }
        if (state.showRecurrenceResetNotice) {
            RecurrenceResetNotice(onDismiss = callbacks.onDismissRecurrenceResetNotice)
        }
    }
}

/**
 * The one-line explainer under "yearly on the IFC date" (ROADMAP R4): whether the Gregorian date this
 * position falls on shifts by a day in leap years ([EventEditorUiState.Loaded.yearlyIfcGregorianShifts],
 * `docs/calendar-spec.md` §7.7), or the fixed Gregorian date for Year Day. `null` on a Leap Day anchor,
 * whose common-year behaviour is explained by [LeapDayPolicySection] instead, right below.
 */
@Composable
private fun yearlyIfcSupportingText(state: EventEditorUiState.Loaded): String? =
    when (IfcDate.from(state.startDate)) {
        is IfcDate.Regular -> {
            if (state.yearlyIfcGregorianShifts) {
                stringResource(R.string.events_editor_recurrence_explainer_yearly_ifc_shift, state.startIfcDayLabel)
            } else {
                stringResource(R.string.events_editor_recurrence_explainer_yearly_ifc_stable, state.startIfcDayLabel)
            }
        }

        is IfcDate.YearDay -> {
            stringResource(R.string.events_editor_recurrence_explainer_yearly_ifc_year_day)
        }

        is IfcDate.LeapDay -> {
            null
        }
    }

/**
 * The one-line explainer under "yearly on the Gregorian date" (ROADMAP R4): whether the IFC date this
 * position falls on shifts in leap years ([EventEditorUiState.Loaded.yearlyGregorianIfcShifts]).
 */
@Composable
private fun yearlyGregorianSupportingText(state: EventEditorUiState.Loaded): String =
    if (state.yearlyGregorianIfcShifts) {
        stringResource(R.string.events_editor_recurrence_explainer_yearly_gregorian_shift, state.startGregorianDayLabel)
    } else {
        stringResource(
            R.string.events_editor_recurrence_explainer_yearly_gregorian_stable,
            state.startGregorianDayLabel,
        )
    }

/**
 * A dismissible, TalkBack-announced explanation shown after the editor reset [RecurrenceKind] from
 * [RecurrenceKind.MONTHLY_IFC] to [RecurrenceKind.NONE] on its own (ROADMAP R3), because the start moved
 * to Year Day or Leap Day — which belong to no month. A polite live region so a screen reader announces
 * the change without stealing focus from whatever the user was doing.
 */
@Composable
private fun RecurrenceResetNotice(onDismiss: () -> Unit) {
    // docs/design-plan.md §4.5: an advisory notice, not neutral — tertiaryContainer, not surfaceVariant.
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(FieldSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FieldSpacing),
        ) {
            Text(
                text = stringResource(R.string.events_editor_recurrence_reset_notice),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            IconButton(onClick = onDismiss, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.events_editor_recurrence_reset_dismiss),
                )
            }
        }
    }
}

@Composable
private fun LeapDayPolicySection(
    policy: LeapDayPolicy,
    onChange: (LeapDayPolicy) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing / 2)) {
        Text(
            stringResource(R.string.events_editor_leap_day_policy_heading),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            stringResource(R.string.events_editor_leap_day_policy_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.selectableGroup()) {
            RecurrenceOption(
                label = stringResource(R.string.events_editor_leap_day_policy_june28),
                selected = policy == LeapDayPolicy.JUNE_28,
                onClick = { onChange(LeapDayPolicy.JUNE_28) },
            )
            RecurrenceOption(
                label = stringResource(R.string.events_editor_leap_day_policy_skip),
                selected = policy == LeapDayPolicy.SKIP,
                onClick = { onChange(LeapDayPolicy.SKIP) },
            )
            RecurrenceOption(
                label = stringResource(R.string.events_editor_leap_day_policy_sol1),
                selected = policy == LeapDayPolicy.SOL_1,
                onClick = { onChange(LeapDayPolicy.SOL_1) },
            )
        }
    }
}

@Composable
private fun RecurrenceEndSection(
    state: EventEditorUiState.Loaded,
    callbacks: EventEditorCallbacks,
    formatter: IfcDateFormatter,
    onPickUntil: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing / 2)) {
        Text(stringResource(R.string.events_editor_end_heading), style = MaterialTheme.typography.labelLarge)
        Column(modifier = Modifier.selectableGroup()) {
            RecurrenceOption(
                label = stringResource(R.string.events_editor_end_never),
                selected = state.recurrenceEndKind == RecurrenceEndKind.NEVER,
                onClick = { callbacks.onRecurrenceEndKindChange(RecurrenceEndKind.NEVER) },
            )
            RecurrenceOption(
                label = stringResource(R.string.events_editor_end_until),
                selected = state.recurrenceEndKind == RecurrenceEndKind.UNTIL,
                onClick = { callbacks.onRecurrenceEndKindChange(RecurrenceEndKind.UNTIL) },
            )
            RecurrenceOption(
                label = stringResource(R.string.events_editor_end_count),
                selected = state.recurrenceEndKind == RecurrenceEndKind.COUNT,
                onClick = { callbacks.onRecurrenceEndKindChange(RecurrenceEndKind.COUNT) },
            )
        }
        when (state.recurrenceEndKind) {
            RecurrenceEndKind.NEVER -> {
                Unit
            }

            RecurrenceEndKind.UNTIL -> {
                OutlinedButton(
                    onClick = onPickUntil,
                    modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
                    border = errorOutlinedBorder(state.untilBeforeStart),
                    colors = errorOutlinedButtonColors(state.untilBeforeStart),
                ) { Text(formatter.formatGregorianLong(state.untilDate)) }
                if (state.untilBeforeStart) ErrorBanner(stringResource(R.string.events_editor_until_before_start))
            }

            RecurrenceEndKind.COUNT -> {
                OutlinedTextField(
                    value = state.count.toString(),
                    onValueChange = { text -> text.toIntOrNull()?.let(callbacks.onCountChange) },
                    label = { Text(stringResource(R.string.events_editor_count_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }
    }
}

/**
 * Reminder chips (FEATURES E4). The first chip added on API 33+ triggers a `POST_NOTIFICATIONS`
 * request ([EventEditorViewModel.toggleReminder]); a denial shows [NotificationPermissionNotice]
 * below, never blocking this section or saving.
 */
@Composable
private fun ReminderSection(
    reminders: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing / 2)) {
        Text(stringResource(R.string.events_editor_reminders_heading), style = MaterialTheme.typography.titleSmall)
        // A fixed grid of equal-width chips rather than a FlowRow. A FlowRow packs each row until the
        // next chip will not fit, so the chips came out ragged (five presets of five different label
        // lengths) and, with no verticalArrangement, the wrapped rows touched each other. Chunking into
        // REMINDER_CHIPS_PER_ROW and giving every chip weight(1f) makes each row's chips identical in
        // width; the Spacer pads the last, short row so its chips keep that width instead of stretching.
        Column(verticalArrangement = Arrangement.spacedBy(ChipSpacing)) {
            EventDraft.REMINDER_PRESETS.chunked(REMINDER_CHIPS_PER_ROW).forEach { rowPresets ->
                Row(horizontalArrangement = Arrangement.spacedBy(ChipSpacing)) {
                    for (minutes in rowPresets) {
                        FilterChip(
                            selected = minutes in reminders,
                            onClick = { onToggle(minutes) },
                            label = {
                                Text(
                                    text = reminderLabel(minutes),
                                    maxLines = REMINDER_LABEL_MAX_LINES,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget),
                        )
                    }
                    repeat(REMINDER_CHIPS_PER_ROW - rowPresets.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * A quiet, dismissible explanation shown after the user denies `POST_NOTIFICATIONS` (FEATURES E4, P2):
 * reminders are still saved, but nothing will notify until the permission is granted from Settings.
 * Never blocks saving.
 */
@Composable
private fun NotificationPermissionNotice(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    // docs/design-plan.md §4.5: an advisory notice, not neutral — tertiaryContainer, not surfaceVariant.
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(FieldSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FieldSpacing),
        ) {
            Text(
                text = stringResource(R.string.events_editor_notification_permission_notice),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.events_editor_notification_permission_settings))
            }
            IconButton(onClick = onDismiss, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.events_editor_notification_permission_dismiss),
                )
            }
        }
    }
}

/**
 * A supporting line, shown only for a recurring event (`state.recurrenceKind != NONE`), spelling out
 * that Save and Delete both act on every occurrence — per-occurrence edits are out of scope for 1.0
 * (`docs/ARCHITECTURE.md` §3.2 "Scope cuts").
 */
@Composable
private fun SeriesNotice() {
    Text(
        text = stringResource(R.string.events_editor_series_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * How many occurrences were individually deleted ("delete this occurrence" from Day detail), with a
 * "restore all" action that clears every exdate (FEATURES E1).
 */
@Composable
private fun RestoreOccurrencesRow(
    count: Int,
    onRestoreAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FieldSpacing),
    ) {
        Text(
            text =
                pluralStringResource(R.plurals.events_editor_exdate_count, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRestoreAll, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.events_editor_restore_all))
        }
    }
}

@Composable
private fun reminderLabel(minutes: Int): String =
    when (minutes) {
        0 -> stringResource(R.string.events_reminder_0)
        10 -> stringResource(R.string.events_reminder_10)
        30 -> stringResource(R.string.events_reminder_30)
        60 -> stringResource(R.string.events_reminder_60)
        1440 -> stringResource(R.string.events_reminder_1440)
        else -> pluralStringResource(R.plurals.events_reminder_minutes_before, minutes, minutes)
    }

@Composable
private fun RecurrenceOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    supportingText: String? = null,
) {
    // Selectable Surface (as IfcDatePicker's Option), not a plain Row + Modifier.selectable: Material3's
    // RadioButton drops its own 48dp touch target when its onClick is null (it hands that job to the
    // ancestor that owns the click), so the enclosing container must be the one that reserves 48dp.
    // docs/design-plan.md §4.5: the selected recurrence/policy/end row fills with secondaryContainer.
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget).semantics { role = Role.RadioButton },
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget).padding(vertical = FieldSpacing / 2),
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.padding(start = FieldSpacing)) {
                Text(label)
                // ROADMAP R4: a one-line, start-date-specific explainer under the option, e.g. "Every
                // Sol 13 — the Gregorian date shifts by a day in leap years."
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * A validation error, shown as an `errorContainer` banner with a warning icon
 * (`docs/design-plan.md` §4.5) — replaces a plain red caption. The offending field or button also gets
 * [errorOutlinedBorder]/[errorOutlinedButtonColors], so the error is legible without colour alone
 * (design-plan §2: colour is never the only signal — the icon and the outline are its twins).
 */
@Composable
private fun ErrorBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(FieldSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FieldSpacing),
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = stringResource(R.string.events_editor_error_icon_description),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.events_editor_delete_confirm_title)) },
        text = { Text(stringResource(R.string.events_editor_delete_confirm_text)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.events_editor_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.events_editor_cancel)) } },
    )
}

@Composable
private fun DiscardConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.events_editor_discard_confirm_title)) },
        text = { Text(stringResource(R.string.events_editor_discard_confirm_text)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.events_editor_discard)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.events_editor_cancel)) } },
    )
}

@Composable
private fun CalendarChooserDialog(
    onPickGregorian: () -> Unit,
    onPickIfc: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.events_editor_pick_calendar_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FieldSpacing)) {
                TextButton(
                    onClick = onPickGregorian,
                    modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
                ) {
                    Text(stringResource(R.string.events_editor_pick_gregorian))
                }
                TextButton(onClick = onPickIfc, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
                    Text(stringResource(R.string.events_editor_pick_ifc))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.events_editor_cancel)) } },
    )
}

@Composable
private fun IfcDateChooserDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberIfcDatePickerState(initialDate = IfcDate.from(initialDate))
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { IfcDatePicker(state = pickerState) },
        confirmButton = {
            TextButton(
                onClick = { pickerState.date?.let { onConfirm(it.toLocalDate()) } },
                enabled =
                    pickerState.date != null,
            ) {
                Text(stringResource(R.string.events_editor_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.events_editor_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTimePickerDialog(
    initialMinuteOfDay: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = initialMinuteOfDay / MINUTES_PER_HOUR,
            initialMinute =
                initialMinuteOfDay % MINUTES_PER_HOUR,
        )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * MINUTES_PER_HOUR + state.minute) }) {
                Text(stringResource(R.string.events_editor_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.events_editor_cancel)) } },
        title = { Text(stringResource(R.string.events_editor_pick_time_title)) },
    ) {
        TimePicker(state = state)
    }
}

private const val MINUTES_PER_HOUR = 60

private fun formatMinuteOfDayLabel(minuteOfDay: Int): String {
    val hour = minuteOfDay / MINUTES_PER_HOUR
    val minute = minuteOfDay % MINUTES_PER_HOUR
    return "%02d:%02d".format(hour, minute)
}

// Previews — a new all-day event and an existing yearly-IFC one on Leap Day, plus 200% font
// (docs/ARCHITECTURE.md §4 "Accessibility"). Dynamic colour is off for determinism.

private fun previewLoaded(
    isNew: Boolean,
    recurrenceKind: RecurrenceKind,
    startDate: LocalDate,
    isLeapDayAnchor: Boolean = false,
    colorArgb: Int? = null,
    category: EventCategory = EventCategory.EVENT,
): EventEditorUiState.Loaded =
    EventEditorUiState.Loaded(
        isNew = isNew,
        title = if (isNew) "" else "Leap Day birthday",
        description = "",
        location = "",
        isAllDay = true,
        startDate = startDate,
        startIfcLabel = if (isLeapDayAnchor) "Leap Day, 2024" else "Sol 13, 2026",
        startIfcDayLabel = if (isLeapDayAnchor) "Leap Day" else "Sol 13",
        startGregorianLabel = "Monday, June 17, 2024",
        startGregorianDayLabel = "Jun 17",
        allDayEndDate = startDate,
        allDayEndBeforeStart = false,
        startMinuteOfDay = EventDraft.DEFAULT_START_MINUTE,
        endMinuteOfDay = EventDraft.DEFAULT_START_MINUTE + EventDraft.DEFAULT_DURATION_MINUTES,
        endBeforeStart = false,
        zoneChoice = ZoneChoice.FLOATING,
        fixedZoneId = java.time.ZoneId.of("UTC"),
        recurrenceKind = recurrenceKind,
        monthlyIfcAvailable = !isLeapDayAnchor,
        isLeapDayAnchor = isLeapDayAnchor,
        leapDayPolicy = LeapDayPolicy.JUNE_28,
        recurrenceEndKind = RecurrenceEndKind.NEVER,
        untilDate = startDate.plusYears(1),
        untilBeforeStart = false,
        count = 1,
        reminders = setOf(0, 1440),
        canSave = true,
        isDirty = !isNew,
        saveFailed = false,
        showDeleteConfirm = false,
        showDiscardConfirm = false,
        colorArgb = colorArgb,
        category = category,
    )

private val previewCallbacks =
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
        onBack = {},
        onConfirmDiscard = {},
        onCancelDiscard = {},
        onRestoreAllOccurrences = {},
        onDismissNotificationPermissionNotice = {},
        onOpenNotificationSettings = {},
        onDismissRecurrenceResetNotice = {},
    )

@Preview(name = "New event", showBackground = true, heightDp = 1400)
@Preview(
    name = "New event, dark",
    showBackground = true,
    heightDp = 1400,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
internal fun EventEditorNewPreview() {
    IfcTheme(dynamicColor = false) {
        EventEditorScreen(
            state =
                previewLoaded(
                    isNew = true,
                    recurrenceKind = RecurrenceKind.NONE,
                    startDate = LocalDate.of(2026, 6, 30),
                ),
            callbacks = previewCallbacks,
        )
    }
}

@Preview(name = "Edit, yearly IFC on Leap Day", showBackground = true, heightDp = 1600)
@Preview(name = "Edit, font 2.0", showBackground = true, heightDp = 2200, fontScale = 2f)
@Composable
internal fun EventEditorLeapDayPreview() {
    IfcTheme(dynamicColor = false) {
        EventEditorScreen(
            state =
                previewLoaded(
                    isNew = false,
                    recurrenceKind = RecurrenceKind.YEARLY_IFC,
                    startDate = LocalDate.of(2024, 6, 17),
                    colorArgb =
                        EventColorSwatches.ALL
                            .first { it.nameRes == R.string.events_editor_color_plum }
                            .colorArgb,
                    category = EventCategory.BIRTHDAY,
                    isLeapDayAnchor = true,
                ),
            callbacks = previewCallbacks,
        )
    }
}
