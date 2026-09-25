# Yearal — the International Fixed Calendar for Android

An Android app for the [International Fixed Calendar](https://en.wikipedia.org/wiki/International_Fixed_Calendar)
(IFC): 13 months of exactly 28 days, a month called **Sol** between June and July, and two "floating"
days — **Year Day** and **Leap Day** — that belong to no week.

> **Status: pre-release, not on Google Play yet.** The app is real and runs — `main` already has a working
> calendar, converter, events and two home-screen widgets — but it hasn't been through a Play closed test.
> See [What works today](#what-works-today) below and [docs/ROADMAP.md](docs/ROADMAP.md) for the full
> milestone ledger.

## The calendar in 30 seconds

- Every month has 28 days, starts on a Sunday, and ends on a Saturday. The 13th is always a Friday.
- Months: January, February, March, April, May, June, **Sol**, July, August, September, October,
  November, December.
- **Year Day** follows December 28 (Gregorian December 31). In leap years **Leap Day** follows June 28
  (Gregorian June 17). Neither has a weekday.
- The year number and January 1 match the Gregorian calendar.

Example: Gregorian Thursday, 17 September 2026 is IFC **September 8, 2026**. Its IFC weekday is
Sunday — which is why the app always makes clear which weekday is which. (This example is one of the
machine-checked vectors in [docs/calendar-spec.md](docs/calendar-spec.md) §6.4.)

## What works today

Everything below is implemented and covered by tests on `main`. Nothing here needs a permission beyond
what's listed in [Privacy](#privacy).

- **Today, Month, Year and Day-detail views** — a swipeable month grid and a 13-mini-month year overview,
  both with the Leap Day / Year Day bands and both real and IFC weekdays shown side by side. Tap any
  day and the card below the grid shows its Gregorian equivalent, both weekdays, holidays and events, with
  "Add event" and "Open in converter". Today's card leads with the real weekday.
- **Converter** — convert any date between Gregorian and IFC in either direction, for the years
  1583–9999, with copy/share.
- **Events** — a list and editor with recurrence on IFC dates ("every Sol 13", "every Year Day", "every
  Leap Day"), Gregorian weekly/RRULE recurrence, all-day or timed events, and a device or fixed time zone.
  Reminder notifications fire even in Doze and hide the event's title on the lock screen. A single
  occurrence of a repeating event can be deleted (with undo).
- **Built-in holidays** — the IFC observances (Year Day, Leap Day, Sol 1) and a US federal + observances
  pack, shown on the grid, in the day card below it and in the Today agenda. The Holidays screen (More → Holidays)
  lists any year's holidays in both calendars and switches packs on or off.
- **Today and Month home-screen widgets**, refreshed at midnight and after a clock, time-zone or locale
  change, reboot, or app update — no per-minute polling. Both follow the app's own colour palette and
  theme (with a per-widget override and a background-opacity setting). The Month widget marks days that
  have events (never their titles) and updates within a second or so of an edit.
- **A first-run intro** — three skippable screens on what the IFC is, why the weekdays differ, and where
  Year Day and Leap Day live; re-openable any time from Learn. On the month grid itself, an info button
  beside the heading explains the two kinds of weekday, and one beside the Leap Day / Year Day band
  explains why that row belongs to no week.
- **Learn, Privacy and Settings screens** — an in-app explainer for the calendar's rules and quirks (with
  illustrations built from the app's own 13×28 grid), a plain-language privacy statement, and an
  Appearance section: weekday-header style, theme, six curated colour palettes (or Material You on
  Android 12+), a pure-black option for dark mode, per-widget theming, holiday packs, plus "Delete all
  data".
- **Colour throughout the UI, not just accents** — a coloured Today hero, filled and marked grid cells,
  intercalary (Year Day / Leap Day) accents shared across every screen and both widgets, per-event colour
  and category, and a live palette preview in Settings before you commit to one.

## What's not here yet

- **Snooze, notification actions and tapping a reminder straight into its event** — reminders currently
  open the app.
- **Widget configuration** (per-widget options) and an agenda widget — M5/M7a.
- **Device-calendar overlay, `.ics` import/export, and URL subscriptions** — releases 1.1–1.3
  ([docs/ROADMAP.md](docs/ROADMAP.md) release map).
- **A Play Store listing.** Release builds are signed and installable
  ([docs/release-builds.md](docs/release-builds.md)), but no upload key has been generated and nothing has
  been submitted; `main` is built feature by feature and a build is cut for testers when one is wanted.

## Documentation

| Doc | What it covers |
|---|---|
| [docs/FEATURES.md](docs/FEATURES.md) | What users are asking for, and every planned feature by priority |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Releases, milestones, task breakdown, open decisions |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Tech stack, modules, data model, UI and widget architecture, testing, CI/CD |
| [docs/calendar-spec.md](docs/calendar-spec.md) | The IFC rules, conversion algorithms, type model, and machine-verified test vectors |
| [docs/contracts/](docs/contracts/Events.md) | Frozen public APIs: the calendar core and the events model |
| [docs/holidays-and-import.md](docs/holidays-and-import.md) | Holiday rule engine, data licensing, device-calendar overlay, `.ics` import/export |
| [docs/security-and-privacy.md](docs/security-and-privacy.md) | Threat model, permissions, backups, Play policy, repo hygiene |
| [docs/competitive-analysis.md](docs/competitive-analysis.md) | Existing IFC apps, what their users say, and the gaps |
| [docs/WORKFLOW.md](docs/WORKFLOW.md) | How work is done here: the gate, Definition of Done, documentation and anti-drift rules |
| [docs/adr/](docs/adr/README.md) | Architecture decision records — choices made after the planning baseline |

## Building and running

Requires JDK 21 (Android Studio's bundled JBR works). `java` is not assumed to be on `PATH`, so set
`JAVA_HOME` for the shell first. From the repo root, in PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat check                 # compile (warnings = errors), tests, ktlint, KDoc gate, lint — all modules
.\gradlew.bat :app:assembleDebug    # APK at app\build\outputs\apk\debug\app-debug.apk
python scripts\check_docs.py        # doc link check
```

`minSdk` 26, `targetSdk` 36, `compileSdk` 37 ([gradle/libs.versions.toml](gradle/libs.versions.toml) is the
source of truth for these and every other dependency version).

## Modules

Kotlin + Jetpack Compose (Material 3) on Navigation 3, Room 3, Hilt and Jetpack Glance widgets, plus three
pure-JVM modules — `:core:calendar`, `:core:domain`, `:core:holidays` — that hold the date/event/holiday
logic with zero Android dependencies. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#2-module-structure)
§2 for the full module tree and dependency rules; [docs/contracts/Calendar.md](docs/contracts/Calendar.md)
and [docs/contracts/Events.md](docs/contracts/Events.md) are the frozen APIs the modules build against.

## Privacy

No `INTERNET` permission and no server — the app itself cannot send your data anywhere. The one copy that
can leave the phone is Android's own encrypted device backup, which the user controls.
No ads, no analytics, no accounts, no crash SDKs. Full detail, including exactly which permissions are
declared and why, is in [docs/security-and-privacy.md](docs/security-and-privacy.md); the same statement
ships in-app as the Privacy screen (More → Privacy).

## License

**Not yet chosen.** The repository is public but unlicensed, so by default all rights are reserved; see
[docs/ROADMAP.md](docs/ROADMAP.md#open-decisions-for-the-owner) open decision #3 for the status. Don't
assume you can reuse this code until a license is added.
