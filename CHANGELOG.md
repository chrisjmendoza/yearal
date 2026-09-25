# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

User-visible features first, then the architecture and build work underneath them.

- The app is named **Yearal** (ROADMAP decision #1); applicationId and package base
  `io.github.chrisjmendoza.yearal` (decision #2). Name availability research is in
  [docs/competitive-analysis.md](docs/competitive-analysis.md) §8.
- Launcher icon: the "perfect month" glyph (28-day grid with Year Day beneath) as vector adaptive-icon
  layers with a themed (monochrome) variant; sources and the Play-listing PNG in [docs/brand/](docs/brand/README.md).
- `:core:designsystem` (M2 T1, T5): `IfcTheme` — dynamic colour on API 31+, user-switchable in Settings,
  with a non-dynamic fallback palette seeded from the brand icon (teal primary, cream surfaces, amber
  tertiary container for the intercalary band) — and `IfcDateFormatter` implementing calendar-spec §7.3–7.6
  (long/medium/numeric styles, labelled nominal vs actual weekday, day/week/quarter text, "Sol" and the
  intercalary names as resources).
- `MonthGrid`, `DayCell`, `IntercalaryBand`/`IntercalaryPlaceholder` and `WeekdayHeaders` in
  `:core:designsystem` (M2 T4; FEATURES C1–C4): the perpetual 4 × 7 grid with the IFC number large and the
  Gregorian day in the corner, nominal/actual/both weekday headers (Sunday first, always), a tappable Leap
  Day / Year Day band whose slot is reserved in every month, today ring, selection, event dots and holiday
  markers, merged TalkBack descriptions, and previews for the screenshot matrix.
- Reusable date pickers in `:core:designsystem`: `IfcDatePicker` (year, 13 months including Sol, days 1–28,
  Year Day always and Leap Day only in leap years; a selected Leap Day moves to June 28 with a notice when
  the year becomes a valid common year) and `GregorianDatePickerDialog` (Material 3, 1583–9999).
- `:feature:calendar` (M2 T6; FEATURES T1, T2, T3, T4, T6): the Today screen — hero IFC date, Gregorian
  equivalent, both weekdays clearly labelled, day/week/quarter, year progress and the countdown to the next
  Year Day / Leap Day, driven by `DateTicker` so it rolls over at midnight.
- Calendar tab (M2 T7, T8; FEATURES C1, C3, C5, C7): a swipeable month grid over IFC 1583–9999 with the
  Leap Day / Year Day bands, holiday marks from the enabled packs and a Today action. Tapping any day, the
  bands included, opens a day-detail sheet with both dates, both labelled weekdays, day/week/quarter and the
  day's holidays.
- `:feature:settings` (M2 T9): the Settings screen — weekday-header mode (Both / Actual only / IFC only),
  theme (System / Light / Dark), dynamic colour (Android 12+), one switch per bundled holiday pack — and the
  More hub with Settings and About rows. The app applies the theme settings live.
- Converter tab (M3 T1; FEATURES D1, D2, D4): convert a date between the Gregorian calendar and the
  International Fixed Calendar in either direction, for the years 1583–9999. Shows both dates in full, the
  numeric `IFC YYYY-MM-DD` form, the IFC weekday and the actual weekday on separately labelled lines ("no IFC
  weekday" on Leap Day and Year Day), the day of the year and week, and a proleptic-calendar note for early
  years. Copy or share the result as text that always carries the "IFC" marker and the Gregorian date.
- Year overview (M3 T2; FEATURES C6, C7): 13 mini-months plus Year Day, Leap Day marked in June in leap
  years, days with events marked, tap a month to open it, previous / next / Today. The Month screen's title
  now shows the month and opens the Year overview, a "jump to date" action accepts either calendar, Day
  detail has "Open in converter", and Today's agenda rows open the event.
- Learn / About and Privacy screens (M3 T3, M2 T12 in-app half; FEATURES L2, P5), reachable from More: the
  IFC rules, why the weekdays differ, how dates are calculated, a short history and an FAQ, with every
  example date computed by the conversion core; and a plain-language statement of what the app stores, what
  its two permissions are for, and what it never does.
- Events tab (M4 T4, T5; FEATURES E1, E2, E3, E5, E6, E7, E9): a searchable event list showing both dates on
  every row, and an editor with title, notes, location, all-day or timed, a date picked in either calendar,
  device or fixed time zone, yearly recurrence on the IFC or the Gregorian date, monthly on the IFC day,
  weekly, the Leap Day common-year choice, an end condition and reminder chips. Reminders are stored; they
  are delivered from M6, and until then the editor says so under the chips.
- Events on the calendar (M4 T6, T7; FEATURES C4, C5, T5): event dots on the month grid, the day's agenda
  in Day detail (tap to edit, "Add event"), and today's agenda, today's holidays and the next holiday on
  Today, all from the new `DefaultObserveAgendaUseCase` and the domain-level `HolidaySetProvider`.
- Today home-screen widget (M5 T1, `:widget`, Glance 1.2.0; FEATURES S1, S3, S4, S5): the IFC date, the
  Gregorian equivalent and the actual weekday, resizable, Material You colour, refreshed at midnight and
  after clock, zone and locale changes, reboot and app update; tap opens the app. WorkManager's
  `ACCESS_NETWORK_STATE` and `FOREGROUND_SERVICE` are removed from the merged manifest; `WAKE_LOCK` is added.
- Month-grid home-screen widget (M5 T3; FEATURES S2): the current IFC month with today highlighted and the
  Leap Day / Year Day band, refreshed through the same midnight-rollover path as the Today widget.

Architecture, tooling and CI:

- Planning baseline: calendar specification, feature catalog, architecture, roadmap, holiday and import
  strategy, security and privacy plan, competitive analysis.
- Workflow rules ([docs/WORKFLOW.md](docs/WORKFLOW.md)): definition of done, KDoc standard, anti-drift
  rules, rules for LLM agents, completion report, ADR template. Local work happens on a `local/<task>`
  branch and merges to `main` only once the owner says so; cloud/scheduled work happens on a `cloud/<task>`
  branch and opens a pull request instead.
- Gradle build skeleton: wrapper 9.7.1, version catalog, `build-logic` with the `ifc.jvm.library`
  convention (explicit API, warnings as errors, ktlint via Spotless, Dokka KDoc gate, JUnit 6 + Kotest).
- Gradle daemon pinned to JDK 21 (`gradle/gradle-daemon-jvm.properties`) and parallel IDE sync enabled,
  as generated by Android Studio on first import.
- `:core:calendar` — pure Kotlin/JVM IFC model: `IfcMonth`, `IfcDate` (`Regular`, `LeapDay`, `YearDay`),
  Gregorian ↔ IFC conversion, canonical numeric parse/format, and `IfcYearMonth` for month-grid layout. The
  frozen public API is documented in [docs/contracts/Calendar.md](docs/contracts/Calendar.md) (M1 T6).
- Spec-driven tests that read their vectors from `docs/calendar-spec.md` §6, plus an exhaustive
  definitional oracle over every day of years 1–9999.
- CI workflow (`ci.yml`) running the full gate and the doc link check on every push and pull request.
- `IfcDate` date arithmetic (M1 T6b, `docs/calendar-spec.md` §7.7): `plusDays`/`minusDays` and
  `plusWeeks`/`minusWeeks` (real Gregorian days/weeks), `plusMonths`/`minusMonths` and
  `plusYears`/`minusYears` (IFC pseudo-fields with `java.time`-style clamping of day 29 to 28). Arithmetic
  overflow and results outside years 1–9999 throw `DateTimeException`, never `ArithmeticException`
  (`docs/adr/0002-ifc-date-arithmetic.md`).
- `IfcDate.daysUntil` — the `ChronoUnit.DAYS`-based difference from calendar-spec §7.7.
- `:core:domain` (M1 T7, `docs/calendar-spec.md` §7.8): `ZoneProvider` (the current zone, read fresh
  rather than cached) and `DateTicker` (`Flow<LocalDate>` that re-emits at every local midnight,
  DST-safe), plus `RealDateTicker`. Neither calls `LocalDate.now()` directly (FEATURES Q2).
- `:core:testing` — hand-written fakes for the above: `MutableClock` (a settable/advanceable fake
  `java.time.Clock`), `FakeZoneProvider`, and `FakeDateTicker`.
- Toolchain (M0 T3, [docs/adr/0001-toolchain.md](docs/adr/0001-toolchain.md)): AGP 9.3.3 with built-in
  Kotlin 2.3.21, KSP 2.3.12, Hilt 2.60.1, Room 3.0.3, Compose BOM 2026.09.00, Navigation 3 1.1.7, Robolectric
  4.17 and Roborazzi 1.74.0, verified by a real build; the Android convention plugins
  (`ifc.android.library` / `.compose` / `.feature` / `.application`, `ifc.hilt`, `ifc.room`,
  `ifc.kotlin.serialization`) with lint as an error gate.
- `:app` (M0 T5, M2 T2): Hilt application, single edge-to-edge activity, the five top-level tabs
  (Today | Calendar | Events | Convert | More) in a `NavigationSuiteScaffold` with per-tab Navigation 3 back
  stacks, the real `Clock`/`ZoneProvider`/`DateTicker` bindings, encrypted-only Auto Backup rules, and an
  adaptive launcher icon. `:core:navigation` holds every `NavKey` and the `Navigator` interface.
- Holiday rule engine in `:core:domain` (M1 T8; FEATURES H1, H3; [docs/adr/0003-holiday-rule-model.md](docs/adr/0003-holiday-rule-model.md)):
  `HolidayEngine` and the sealed `HolidayRule` (`fixed`, `nthWeekday`, `weekdayRelative`, `offset`, `easter`
  western/orthodox, `table`, `ifc`) with the `since`/`until`/`yearFilter`/`observed`/`durationDays`
  modifiers and the `US_FEDERAL`, `NEXT_MONDAY`, `SUNDAY_TO_MONDAY` observed policies. Holidays are computed
  per year and memoised, never stored; range queries evaluate neighbouring years so New Year observed on
  Dec 31 and Kwanzaa's January tail are found. Verified against published tables (Easter 1900–2100, OPM
  federal holidays 2020–2030).
- `:core:holidays` (M1 T9; FEATURES H2, H3; [docs/adr/0004-holiday-pack-format.md](docs/adr/0004-holiday-pack-format.md)):
  the schema-1 JSON pack format, `HolidayPackLoader`, and the bundled `ifc`, `US` (federal + common
  observances) and `religious-christian` (Easter family) packs, checked against OPM 2024–2028.
- `:core:data` (M2 T3; FEATURES W1, W2, H5): `UserSettings` persisted as JSON in a typed DataStore
  (`filesDir/datastore/user_settings.json`, inside the Auto Backup include set) behind `SettingsRepository`,
  with a forward/backward-compatible serializer and corruption fallback to the defaults.
- CI builds and uploads the debug APK on every push.
- Events contract (M4 T1): `Event`, `EventTiming`, `EventCalendar`, `Reminder`, `Recurrence` / `IfcRecurrence`
  (with the Leap Day common-year policy `JUNE_28 | SKIP | SOL_1`), `Occurrence`, `DayAgenda`; the
  `EventRepository`, `RecurrenceExpander` and `ObserveAgendaUseCase` interfaces; the canonical IFC rule text
  (`IfcRuleText`: the `ifc_rule` column and `X-IFC-RRULE`); fakes and fixtures in `:core:testing`. Frozen in
  [docs/contracts/Events.md](docs/contracts/Events.md); decisions in
  [docs/adr/0005-events-contract.md](docs/adr/0005-events-contract.md), including a range-query zone-skew
  padding of two days instead of one.
- Day rollover scheduling (M5 T2, `:core:scheduling`; FEATURES S3 foundation): a once-a-day alarm for the
  next local midnight (DST-safe, windowed — no exact-alarm permission), re-armed at app start and after
  clock, time-zone and locale changes, reboot and app update through non-exported manifest receivers.
  Widgets and reminders plug in through the new `DayRolloverListener` (`:core:domain`). Adds the
  `RECEIVE_BOOT_COMPLETED` permission (normal, no prompt).
- CI: a merged-manifest permission allow-list gate (`scripts/check_manifest_permissions.py`) that reads the
  allow-list from `docs/security-and-privacy.md`, so a dependency cannot silently add a permission such as
  `INTERNET`; and `record-screenshots.yml`, a manual job that records Roborazzi goldens on Linux and uploads
  them as an artifact for the owner to commit with a signed commit.
- Recurrence expansion (M4 T3, `DefaultRecurrenceExpander` in `:core:domain`): IFC yearly, yearly-intercalary
  and monthly rules expand by direct construction in `:core:calendar`, with the `JUNE_28` / `SKIP` / `SOL_1`
  Leap Day policies, inclusive `UNTIL`, `COUNT` from the anchor, intervals, exdates and nothing after year
  9999. Gregorian `RRULE`s expand through `org.dmfs:lib-recur` 0.17.1 (Apache-2.0); a rule that cannot be
  evaluated shows the event once instead of failing. An independent brute-force oracle suite
  (`RecurrenceExpanderOracleTest`, written from the contract without reading the implementation) checks
  about 9,000 generated comparisons per run.
- Event storage (M4 T2, `:core:data`): Room 3 schema v1 (`calendars`, `events`, `event_exdates`,
  `reminders`) exported to `core/data/schemas/`, and `RoomEventRepository`, which passes the same 36-case
  contract suite as the fake. The month-range query measures about 1 ms with 1,000 events.
- Reminder notifications (M6 T1; FEATURES E4, P3). One alarm is scheduled at a time, for the earliest
  upcoming reminder across all events, and recomputed whenever an event changes, at local midnight, at app
  start, after a reboot, a clock or time-zone change and an app update. All-day events remind at 09:00
  local; timed events at their own start, daylight saving included. A reminder missed while the device was
  off still arrives if it is less than 15 minutes late. On the lock screen the notification shows only
  "Event reminder" and the time. No event text is ever put in an intent or a log.
- The app now declares `POST_NOTIFICATIONS`, `USE_EXACT_ALARM` and (Android 12 only) `SCHEDULE_EXACT_ALARM`
  (M6 T3), so reminders are punctual even in Doze; the editor asks for the notification permission the
  first time a reminder is added (Android 13+). Everything keeps working when notifications are denied or
  exact alarms are revoked.
- "Delete this occurrence" (with undo) and plain delete for events in Day detail (M4 T8); the editor says
  plainly that saving or deleting a repeating event acts on the whole series, shows how many occurrences
  were deleted individually and can restore them.
- "Delete all data" in Settings, behind a two-step confirmation (M4 T9; FEATURES W6); a test now pins the
  encrypted-only backup rules to the real database and settings files.
- Month widget event dots and a debounced widget refresh when events change (M5 T6), and widget-picker
  previews for every supported Android version (M5 T5).
- `docs/contracts/Calendar.md`, the frozen public API of `:core:calendar`; `README.md` rewritten to describe
  the app as it is; `scripts/check_docs.py` fails if the README regresses to known-stale statements.
- The event editor explains what each repeat option means for the chosen date — whether the Gregorian or the
  IFC date shifts in leap years, that a monthly IFC repeat happens 13 times a year, and that a weekly repeat
  does not stay on the same IFC weekday (R4).
- Screenshot-testing pipeline (R6, M2 T10): goldens live in a tracked `src/test/screenshots/` directory per
  module, `:core:designsystem` generates 56 captures from its previews, the record workflow uploads them with
  repo-relative paths, and CI verifies them automatically once the first goldens are committed
  ([docs/screenshots.md](docs/screenshots.md)).
- `docs/reviews/`: an external review of the project and our written response to it.
- A Holidays screen (M6 T2; FEATURES H5): browse every bundled holiday set with its region, size and
  sources, switch sets on or off, and read a chosen year's holidays grouped by IFC month with both dates
  on every row — tap one to open that day.
- Calendar and Events use both panes on a wide screen (M3 T4; FEATURES C11, Q7): a tablet, an unfolded
  foldable or a phone in landscape shows the month grid beside the day's detail, and the events list beside
  the editor, instead of a bottom sheet and a full-screen editor. Narrow screens are unchanged.
- Reminders and widgets open the right screen (M3 T5, M4 T10; FEATURES S5, E4): a reminder notification opens
  its event, a day on the Month widget opens that day, and the Today widget opens Today, instead of whichever
  tab happened to be showing. Everything arriving in an intent is validated and falls back to the normal start
  screen rather than trusting it; intents still carry nothing but IDs.
- A release build you can install (M2 T11, signing half): `:app`'s release type signs from a gitignored
  `keystore.properties` or `YEARAL_RELEASE_*` environment variables and falls back to the debug key with a
  warning when neither is set, so the build is always installable and no secret is ever needed to compile.
  It is also `profileable`, so Android Studio's profiler can attach to the exact build being judged.
  R8 stays off until its keep rules land (M8 T2). See [docs/release-builds.md](docs/release-builds.md).
- A skippable three-screen first-run intro (FEATURES L1): what the IFC is, why the nominal and the actual
  weekday differ, and where Year Day and Leap Day live, ending with a "find my IFC birthday" jump into the
  converter. Re-openable from Learn. Every example date is computed by the conversion core, so the text
  cannot drift from the calendar.
- Contextual explainers on the month grid (FEATURES L3): an info button beside the month heading explains
  why the IFC weekday and the real weekday disagree, and one beside the Leap Day / Year Day band explains
  why that row spans all seven columns. Both use `ExplainerInfoButton`, the reusable info-button and popup
  in `:core:designsystem`. Neither button sits inside the row it explains, so the weekday headers stay
  aligned with the day cells beneath them.
- The visual design pass (ROADMAP M2 T13; FEATURES W6; [docs/design-plan.md](docs/design-plan.md)):
  `:core:designsystem` gains a real design language — `YearalColors` semantic tokens (today ring, hero
  container, intercalary accent, marked and weekend grid cells) derived from whichever `ColorScheme` is
  active so they hold under any palette or Material You; `YearalShapes` and a named `PillShape`;
  `YearalTypography` (Roboto, tabular figures on every numeral-bearing style, a heavier `displayMedium`
  for hero dates); `Dimens` for spacing and mark sizes; and a reworked dark scheme whose surfaces read as
  teal-tinted rather than plain black. `ColorSchemeContrastTest` gates 4.5:1 text / 3:1 non-text contrast
  across every palette, both modes and pure black.
- Six curated palettes — Teal (default), Sol, Night, Moss, Rose, Ink — plus a colour-source switch
  (Yearal palette / Material You), a pure-black toggle for dark mode, per-widget (Today, Month) theme
  overrides, a widget background-opacity slider and a live miniature preview strip, all in a new Settings
  → Appearance section.
- Colour now reaches every screen instead of only state cues: a Today hero card with IFC/Gregorian
  eyebrows and an intercalary countdown chip; filled and marked cells on Month and Year; Year tiles as
  cards with the intercalary fill and holiday diamonds; an intercalary header on Day detail for Year Day
  and Leap Day; a converter result hero card; holiday-pack colour dots and holiday diamonds; and an Events
  list grouped under IFC month headers (Year Day and Leap Day get their own header) with a leading colour
  bar and small chips for recurrence and category instead of a fifth grey text line.
- Per-event colour and category in the event editor (FEATURES E8): a row of seven fixed swatches plus
  "Calendar colour" as the reset, and a three-way `EventCategory` control (Event / Observance / Birthday)
  shown as a chip in the list. Both were already stored, so `docs/contracts/Events.md` stays frozen.
- Grid illustrations (`GridIllustration`, `:feature:settings`) replace bare text on the first-run intro
  and as Learn section headers: three `Canvas`-drawn variants built from the same 13×28 grid the app
  already shows elsewhere, with no bitmap assets.
- Both home-screen widgets now follow the app's colour source, palette and pure-black setting, with their
  own theme override (follow app / light / dark) and a background-opacity setting; the Month widget's
  cells are filled and tinted, and the large Today widget adds a year-progress bar and a countdown to the
  next Year Day / Leap Day. **Known limitation:** a widget's own theme override has no effect while the
  colour source is Material You, because Glance's dynamic colour set follows the system UI mode and
  cannot be overridden per widget.

### Changed

- **Existing installs move from Material You to the Yearal palette.** `colorSource` replaces the old
  `dynamicColor` boolean and now defaults to `BRAND`, so a fresh or updated install shows the teal / cream
  / amber brand palette instead of a wallpaper-derived scheme without the user touching anything. Switch
  back any time in Settings → Appearance → "Colour source" → Material You (Android 12+ only).
- The Month screen anchors on the grid: one title (the app bar's, not a second heading inside the grid), a
  "Show year" pill with a chevron as the affordance for the Year view, and a selected-day summary filling
  the space below the grid instead of leaving it empty.
- The Year overview's Year Day tile was drawn like the mini-month tiles beside it — same container, same
  heading style, same today border — instead of the full-width filled pill the month grid uses, marked out
  only by the intercalary icon and its Gregorian date beneath. **Reversed by the design pass:** the tile
  now shares the intercalary fill with Month, Day detail and the widgets, now that the same accent is used
  consistently everywhere (design-plan.md §8 decision 4).
- The Events list groups rows under sticky IFC month headers (Year Day and Leap Day get their own header)
  instead of one flat list, with a single date line and small chips for recurrence and category instead of
  a fifth grey line.

- The month pager no longer re-queries the events of months that are already on screen: swiping one page
  now issues one query for the month that came into view, instead of three for the whole warm window.
  The month grid also stops rebuilding its 28 cell dates on every recomposition.

- Holiday sets are switched on and off in the new Holidays screen rather than in Settings; Settings and the
  More hub both link to it, so there is a single place that owns the setting.

- The Convert tab uses a swap-arrows icon instead of one that read as "refresh" (R5).

- The events list no longer prints "Default calendar" under every event. The calendar's name appears
  only once a second calendar exists to tell it apart from; the colour swatch and the hidden-calendar
  warning are unchanged.

- The event editor's reminder chips are laid out as an even grid — two per row, all the same width, with
  space between the rows — instead of a ragged flow whose wrapped rows touched each other. Long labels
  wrap to a second line rather than being cut off at large text sizes.

- The Month widget draws its grid as a grid: thin rule lines around every day cell, and a mark under any
  day that has something on it — a diamond for a holiday, a dot for an event, the same shapes and order
  the app's own month grid uses. Holidays come from the packs enabled in Settings; neither mark ever
  carries a name or a count. Day numbers are set larger at the bigger widget size so they fill their
  cells rather than floating in them.

- The Month widget fills the space it is given: the grid now stretches to the widget's full height instead
  of sitting in the top third of a tall one, and each day cell's tap target grows with it. At the larger
  size every cell also shows its Gregorian day under the IFC day, matching the app's own month grid — the
  widget's two weekday header rows name an IFC weekday and a real one, so a single number per cell was
  promising a second date it never showed. The month's Gregorian span moved from below the grid to
  directly under the month title.

- The daily midnight refresh now uses the same exact alarm as reminders, with the previous 10-minute
  windowed alarm as the fallback.

- The `IfcDateFormatter` Hilt binding moved from `:feature:calendar` to `:app`, because a second feature
  injects it and features never depend on each other.

- `DefaultObserveAgendaUseCase` takes its work dispatcher through an `internal` constructor, the same seam
  `HolidaysViewModel` has, so its tests run under deterministic virtual time instead of racing a real
  thread pool. An audit of the other twelve ViewModels found none that needs the seam (R9).

- The first-run intro's "shown once" guarantee (FEATURES L1) now has an end-to-end proof rather than only
  unit tests: `:app`'s first Compose test launches the real app twice against the same persisted settings
  and confirms the intro appears on a first launch and never again once dismissed (R10).

### Fixed

- The Year overview's mini-months looked broken after the design pass: the 28 day squares were painted in
  the same colour as the card they sit on, so each month was a blank card with a few loose diamonds on it.
  Every square now has a visible fill, its IFC day number, and its holiday diamond / event dot beneath the
  number, with today ringed and bold — a miniature of the Month grid (R11).
- `HolidaysViewModel`'s tests raced real threads: the view model evaluates packs on `Dispatchers.Default`,
  which a test's virtual clock cannot control, so they passed alone and failed under a loaded full-suite
  run. The dispatcher is now injectable for tests. Production behaviour is unchanged.
- An open Today, Month, Year or Day screen no longer shows a stale date, stale event times or events on the
  wrong day after the device clock or time zone changes, or after the app returns from the background (R1).
- Rapid taps on Save or Delete in the event editor could create a duplicate event; both are now disabled
  while a write is in flight, and a new event keeps one identity for its whole editor session (R2).
- Moving an event's start to Year Day or Leap Day while "monthly (IFC)" was selected silently saved it as
  non-repeating; the editor now resets the choice itself and says why (R3).
- The Privacy screen no longer says that nothing stored could leave the device while also describing Android's
  encrypted backup; it now says the app itself sends nothing and the backup is the one copy that can.
- Day detail no longer crashes on an out-of-range day key (for example from a malformed widget or
  notification intent); it shows a "date not available" state instead.
- `IfcDate.toNumericString()` / `toPrefixedString()` took their digits from the default locale, so a device
  set to Arabic, Persian or Hindi produced a numeric IFC date that `IfcDate.parse` rejects. The canonical
  form now always uses ASCII digits (`docs/calendar-spec.md` §7.6).
- System status and navigation bar icons followed the *system's* dark setting rather than the app's own
  `ThemeMode`, so a forced Light or Dark theme could show mismatched (for example white-on-cream) status
  bar icons; they now follow the theme the user actually chose (design-plan.md §3.2 finding D6).
- Both home-screen widgets ignored the app's theme entirely — always Material You dynamic colour, always
  the system dark setting — regardless of what the user had chosen in Settings; they now read
  `UserSettings` and match what the app itself shows (design-plan.md §4.9).
