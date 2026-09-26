# 0006. Calendar view-mode switcher: Day / Month / Year

- Status: **Proposed — needs the owner's decision**
- Date: 2026-09-25
- Owning doc updated: none yet — pending acceptance. On acceptance: `docs/ARCHITECTURE.md` §4 (nav keys,
  Month/Year screen behaviors, adaptive layouts), `docs/FEATURES.md` C13, `docs/design-plan.md` (the
  switcher control), `docs/ROADMAP.md` ("Beyond 1.0").

## Decision needed

| # | Question (owner's words) | Recommendation |
|---|---|---|
| 1 | One `CalendarKey(mode, year, month, selectedEpochDay?)` replacing `MonthKey`+`YearKey`, or keep them separate with a mode chip that navigates between them? | **One `CalendarKey`.** |
| 2 | Does the mode persist across launches (`UserSettings`, like `weekdayDisplay`), reset every launch, or live only in the tab's `rememberSaveable` state? | **Persist in `UserSettings.calendarMode`.** |
| 3 | Does "Day mode" become a third, full-width view, or does the switcher stay Month/Year only, since Month already *is* the day view (grid + card)? | **Month/Year only — no separate Day mode.** |
| 4 | Where does the switcher live — a segmented row in the Calendar app bar, the title pill's dropdown, or a bottom chip row? | **`SingleChoiceSegmentedButtonRow` in the app bar.** |

## Context

`docs/FEATURES.md` C13 (owner-requested 2026-09-19) asks for Day/Month/Year to become one switchable
view of a single selected date, "not three destinations reached different ways." Two of C13's own three
open questions are still open; its third ("does Day mode replace the day-detail sheet") is moot since
2026-09-25 — the sheet is gone, and a "day" is the Month screen's `DayCard` below the grid
(`docs/ARCHITECTURE.md` §4 "Screen behaviors — Month", "Day detail"). Today, Month and Year are reached
differently: `MonthKey(year, month, selectedEpochDay?)` is the Calendar tab's only static-free root
(resolved from `DateTicker`); `YearKey(year)` is reached only by tapping the Month app bar's title pill
(`MonthTitlePill`, content-described "Show year" — the affordance M2 T13 flagged as missing); a Year
tile tap pushes `MonthKey`. Neither key carries a day selection that survives a trip through the other:
`YearKey` has no `selectedEpochDay`, so nothing today lets a user pick a date, look at the year, and come
back to the same day.

**The invariant (FEATURES C13):** switching mode never changes the selected date; picking a date in a
coarser mode drills into it. This ADR checks both navigation shapes against it.

## Decision

### 1. Navigation shape

**One `CalendarKey(mode: CalendarMode, year: Int, month: Int, selectedEpochDay: Long?)`** replaces
`MonthKey` and `YearKey`. `CalendarMode { MONTH, YEAR }` is a plain serializable enum declared in
`:core:navigation` itself (not `:core:domain` — see Consequences). `month` stays meaningful in `YEAR`
mode too: it is "the month to open if the user drills to Month without tapping a tile," so the selected
date's month, not a nullable field the invariant would otherwise have to special-case.

Walking open app → Calendar tab → switch to Year → tap a month → tap a day → back ×3:

| Step | One `CalendarKey` | Separate keys + mode chip |
|---|---|---|
| Open Calendar tab | `[CalendarKey(MONTH, y, m, null)]` | `[MonthKey(y, m, null)]` |
| Switch to Year | top entry becomes `CalendarKey(YEAR, y, m, null)` (**replaced**, not pushed — see below) | `[MonthKey, YearKey(y)]` (pushed) |
| Tap a month tile | push `CalendarKey(MONTH, y, m2, null)` | push `MonthKey(y, m2, null)` |
| Tap a day | `select()` only, no push: `CalendarKey(MONTH, y, m2, d)` replaces itself | `select()` only: `MonthKey(y, m2, d)` replaces itself |
| Back ×1 | pop → `CalendarKey(YEAR, y, m, null)` | pop → `YearKey(y)` |
| Back ×2 | pop → `CalendarKey(MONTH, y, m, null)` (the root) | pop → `MonthKey(y, m, null)` (the root) |
| Back ×3 | stack size 1, tab isn't Today → **switch to Today tab** | same |
| Widget tap (`AppRoute.Day`) | `open(CALENDAR, [CalendarKey(MONTH, y, m, day)])`, mode forced to `MONTH` regardless of stored preference | `open(CALENDAR, [MonthKey(y, m, day)])` — same shape today |

Both shapes produce an identical back-stack *depth*. The difference is what the invariant costs. With
one `CalendarKey`, the selected date is a field every mode reads, so "switch mode, keep the date" is
"copy the key, change `mode`" — the invariant is structurally true. With separate keys, `YearKey` has
nowhere to put `selectedEpochDay`; keeping the date across a Month→Year→Month round trip needs a side
channel above both `MonthViewModel` and `YearViewModel` (a hoisted selection holder in `:app`, or a
third field bolted onto `YearKey` anyway) — plumbing to reconstruct what one key already carries.
**Recommendation: one `CalendarKey`.** It also gives Month and Year one shared `CalendarViewModel`
instead of two that must stay in step, matching the "one ViewModel feeds every pane" pattern
`MonthListDetailScreen` already established for compact vs. expanded width.

**Mode switches replace the top entry; drilling to a specific tile pushes.** A segmented-control tap is
not "go somewhere new," it's "look at the same selection at a different zoom" — pushing would mean
several taps between Month and Year pile up back-stack entries a user then has to un-press one at a
time. A month-tile tap in Year *is* new navigation (a different month), so it pushes, exactly as
`YearScreen`'s tile tap does today.

**Process death:** `CalendarKey` is `@Serializable`, so it survives exactly as `MonthKey`/`YearKey` do
today (Nav3 serializes the back stack; `TabBackStacks`'s `rememberSaveable`/`rememberNavBackStack` need
no change). **Two-pane (`TwoPaneLayout`, expanded width):** `MODE == MONTH` keeps today's shape (grid +
`DayCard`, `MonthListDetailScreen`); `MODE == YEAR` stays single-pane for v1 — a Year+DayCard two-pane
view is a real option later but is out of scope here (Consequences). **Tab reselection:** `TabBackStacks.select`
pops a re-tapped tab to index 0, i.e. back to whatever `CalendarKey` was seeded as the tab's root — the
mode the tab opened in, not necessarily `MONTH`, once decision 2 lets that root read a stored preference.

### 2. Persistence of the mode

- **`UserSettings.calendarMode`** (persists, like `weekdayDisplay`/`themeMode`): one more `DataStore`
  field and setter, identical shape to the six that already exist. Matches what a user who always drills
  to Year expects — the app doesn't make them re-tap it every launch.
- **Per-session only:** no persistence code, but repeat friction for exactly the user this feature is
  for; nothing in `docs/competitive-analysis.md` names a default-view expectation directly, but its
  widget-freeze complaint (§"Widget reliability," a 3★ review: the incumbent's widget "stays on whatever
  date it was") reads as evidence that users notice and dislike an app that doesn't track where they left
  it.
- **Tab's own `rememberSaveable` state:** free (no store write) but not durable — a process kill after
  the OS reclaims saved instance state loses it silently, unlike every other display setting, which is
  an inconsistent promise to make about one specific preference.

**Recommendation: `UserSettings.calendarMode`.** The cost is one field; the alternative durability is
already deliberately provided for `weekdayDisplay` and `themeMode`, and this is the same kind of
preference. `AppRoute.Day` (widget/notification taps) ignores the stored mode and always opens `MONTH` —
a deep link to a specific day is pointless in `YEAR` mode.

### 3. Day mode

**No separate Day mode; the switcher is Month/Year only.** Month already renders the day view (grid +
`DayCard`), the Today tab already shows today's day full width, and a third mode showing `DayCard` alone
with prev/next paging would duplicate both without adding a capability neither has — new paging UI, a
new back-stack shape, and a third set of tests, for content that already exists twice. If the owner
still wants literal Day/Month/Year, options (b) (full-width `DayCard` with paging) and (c) (agenda-style
day) stay available as a later `CalendarMode.DAY` value — decision 1's `CalendarKey` shape does not
foreclose adding one — but this ADR does not build it.

### 4. The UI control

**`SingleChoiceSegmentedButtonRow`** (two segments, Month/Year) in the Calendar app bar, replacing
`MonthTitlePill`'s chevron. The title text stays (the visible month/year, or the year alone in `YEAR`
mode); the segmented row sits as a second app-bar element, mirroring how the Settings screen already
uses this same component for its three-way `WidgetTheme` and `ThemeMode` choices
(`docs/ARCHITECTURE.md` §4 "Settings"). This closes the M2 T13 note directly: a two-segment control with
a visibly pressed state is a stronger, more conventional affordance than a chevron pill nobody read as
"tap to zoom out" without being told. **A11y:** a preceding `Text` carries `Modifier.semantics { heading() }`
("Calendar view," mirroring the weekday-header explainer's own heading), and each `SegmentedButton`
reports its own selected state through Material 3's built-in selectable semantics — no extra work
beyond supplying `stringResource` labels (never icon-only, CLAUDE.md rule 9). The title-pill dropdown
and a bottom chip row were considered and dropped: a dropdown hides the second option behind a tap (worse
discoverability, the opposite of what this feature is for), and a bottom row competes for space with
`DayCard`'s own action buttons at compact width.

## Consequences

- **Modules touched:** `:core:navigation` (`CalendarKey`/`CalendarMode` added; `MonthKey`/`YearKey`
  removed or deprecated — see migration below); `:core:domain` (`UserSettings.calendarMode`,
  `SettingsRepository` setter); `:feature:calendar` (`MonthViewModel`+`YearViewModel` merge into one
  `CalendarViewModel`, or `YearScreen`/`MonthScreen` both read the same shared state — implementation
  detail for the follow-up task, not fixed here); `:app` (`IfcApp.kt`'s `entryProvider` collapses two
  `entry<XKey>` registrations into one, `TabBackStacks`/`applyRoute`, `IntentRouter`/`AppRoute.Day`);
  docs listed above.
- **`CalendarMode` lives in `:core:navigation`, not `:core:domain`,** to avoid a new module edge:
  `:core:navigation` currently depends on nothing but the Nav3 runtime and `kotlinx-serialization`. If
  decision 2 is accepted, `:core:domain`'s `UserSettings.calendarMode` needs its *own* small enum (or a
  `Boolean`/two-value choice) rather than importing `:core:navigation`'s — the same reasoning
  `docs/ARCHITECTURE.md` §4 already gives for `WidgetIntents`/`ReminderIntent` staying separate small
  objects instead of justifying a shared module for one enum. `:app`, which depends on both, maps one to
  the other.
- **What stays frozen:** `:core:calendar` (no new date computation — CLAUDE.md rule 1), the Events and
  Holidays contracts, `TwoPaneLayout`, and the invariant itself: no future change may let a mode switch
  alter `selectedEpochDay`.
- **Migration for saved back stacks.** The app has not shipped past internal/owner use (status: M0–M1
  done, M2–M5 in progress) — no external installs carry a serialized `MonthKey`/`YearKey` back stack that
  must forever keep deserializing. The recommended path is still a soft landing, not a hard cutover:
  `:app`'s tab back-stack restoration should be covered by a **process-death test** that seeds a saved
  state containing the old key shapes (or any unrecognized key) and asserts the tab falls back to its
  default root rather than crashing — the same "fails soft" precedent `MonthKey.selectedEpochDay`
  already sets for invalid input, applied to the key type itself. `MonthKey`/`YearKey` can then be
  deleted outright once that test is green.
- **Test list:** `CalendarViewModel` unit tests (mode switch preserves `selectedEpochDay`; a date pick in
  `YEAR` mode drills to `MONTH` with that date selected; a fake `Clock`, with a midnight-crossing case);
  a Compose test for the segmented control (correct segment shown selected; heading + selected semantics
  present); `TabBackStacksTest` additions for the exact back-stack walk-through in decision 1, plus tab
  reselection popping to the persisted-mode root; an `IntentRouterTest`/`TabBackStacksTest` case asserting
  `AppRoute.Day` forces `MONTH` regardless of the stored preference; the process-death fallback test above.
- **Size estimate**, matching how `docs/ROADMAP.md`'s M-series tasks are split: **T1** `CalendarKey`/
  `CalendarMode`, `IntentRouter`/`TabBackStacks` updates, back-stack + process-death tests (~1 agent-day);
  **T2** `UserSettings.calendarMode` + `SettingsRepository` plumbing (~0.5 agent-day); **T3** the segmented
  control, `CalendarViewModel` merge, Month/Year screen wiring, a11y (~1 agent-day); **T4** docs
  (`ARCHITECTURE.md` §4, `FEATURES.md` C13, `design-plan.md`, `ROADMAP.md`) (~0.5 agent-day).
