# Architecture

Status: **current as of M0 and M1 complete; M2–M6 in progress, M2 T13 (visual design pass) done** (2026-09-23). The toolchain is settled by
[adr/0001-toolchain.md](adr/0001-toolchain.md); [`gradle/libs.versions.toml`](../gradle/libs.versions.toml)
is the authority for versions and §1 explains the choices. Progress per task is in [ROADMAP.md](ROADMAP.md).

## How the planning docs fit together

| Doc | Authoritative for |
|---|---|
| [calendar-spec.md](calendar-spec.md) | IFC rules, conversion algorithms, the `IfcDate` type model, formatting, date-arithmetic semantics, test vectors |
| [FEATURES.md](FEATURES.md) | What the app does and in which release tier |
| **ARCHITECTURE.md** (this file) | Tech stack, module boundaries, data model, UI/widget architecture, testing, CI/CD |
| [holidays-and-import.md](holidays-and-import.md) | Holiday rule engine and data format, holiday data licensing, device-calendar overlay, `.ics` import/export |
| [security-and-privacy.md](security-and-privacy.md) | Threat model, permissions, backup rules, Play policy, repo hygiene |
| [privacy-policy.md](privacy-policy.md), [play-data-safety.md](play-data-safety.md) | The published end-user policy text and the Play Data safety / content-rating answers, both derived from security-and-privacy.md |
| [competitive-analysis.md](competitive-analysis.md) | Evidence behind priorities; naming collisions |
| [ROADMAP.md](ROADMAP.md) | Milestones, task breakdown for parallel agents, open questions |
| [WORKFLOW.md](WORKFLOW.md) | How work is done: the gate, Definition of Done, KDoc standard, anti-drift rules, rules for LLM agents |
| `gradle/libs.versions.toml` | The dependency versions the build actually uses (§1 explains the choices) |
| [device-test-matrix.md](device-test-matrix.md) | The on-device checks the automated gate cannot make (ROADMAP M5 T8) |
| [screenshots.md](screenshots.md) | Recording, reviewing and committing Roborazzi goldens; reading a CI diff |
| [reviews/](reviews/2026-09-19-astra-analysis-response.md) | External reviews of the project and our written response to each |
| [contracts/](contracts/Events.md) | Frozen public APIs that parallel tasks build against: [Calendar.md](contracts/Calendar.md) (`:core:calendar`, frozen M1 T6) and [Events.md](contracts/Events.md) (events, frozen M4 T1) |
| [adr/](adr/) | Decisions made after this baseline |

When two docs disagree, the doc that is authoritative for that topic wins, and the other gets fixed.

## Decisions at a glance

| Topic | Decision |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 with dynamic colour, single activity |
| SDK levels | minSdk 26 (native `java.time`), targetSdk 36, compileSdk 37 |
| Correctness keystone | `:core:calendar` — pure Kotlin/JVM, zero Android dependencies, exhaustively tested; **no date is ever computed outside it** |
| Storage | Room 3 + bundled SQLite driver for events; DataStore for settings. Dates are stored Gregorian (epoch day); IFC is a view. The only IFC data at rest is IFC-anchored recurrence rules |
| Time | Injectable `Clock`; `LocalDate.now()` is banned outside the `Clock` binding; no epoch-millisecond arithmetic |
| DI / navigation | Hilt; Navigation 3 |
| Widgets | Jetpack Glance; one exact alarm per day at local midnight + system-broadcast receivers + `updatePeriodMillis` backstop |
| Holidays | Own pure-Kotlin rule engine, bundled JSON rule packs, computed per year, never stored |
| Network | None in 1.0 — no `INTERNET` permission. No analytics, ads, or accounts, ever |
| Tests | JVM-first: JUnit 6 + Kotest property tests for pure modules; Robolectric + Roborazzi for UI; emulator only for nightly smoke tests |
| CI | GitHub Actions: format check, lint, all JVM tests, screenshot verification, debug assemble on every branch push and pull request (local work pushes a branch and merges on the owner's word; cloud work opens a PR — see [WORKFLOW.md](WORKFLOW.md) §1) |

## Reconciled decisions

The planning docs were written in parallel and disagreed in a few places. These are the rulings.

1. **`IfcDate` type model** — the sketch in calendar-spec §4 is the one to implement: a sealed
   interface with `Regular`, `LeapDay`, and `YearDay`, the `monthNumber` / `dayOfMonth = 29`
   pseudo-fields, and **no bare `dayOfWeek`** (only `nominalDayOfWeek?` and `actualDayOfWeek`).
   §3.1 below adds `IfcYearMonth` on top of it for grid layout.
2. **Canonical numeric form** — `IFC YYYY-MM-DD` with months `01`–`13`, Leap Day `06-29`, Year Day
   `13-29` (calendar-spec §7.3). It sorts correctly and keeps a plain int triple. The `IFC ` prefix is
   mandatory in anything a user can see.
3. **Month arithmetic from an intercalary day** — defined, clamping like `java.time`
   (Leap Day + 1 month = Sol 28), per calendar-spec §7.7.
4. **Yearly recurrence on Leap Day in common years** — per-event policy `JUNE_28 | SKIP | SOL_1`.
   User-created events default to `JUNE_28` (IFC June 28 is Gregorian June 17 in common years, so the
   Gregorian date never moves). The built-in Leap Day holiday is simply absent in common years.
5. **Exhaustive test range** — every day of years 1–9999 (≈3.65 M days; seconds on the JVM), matching
   the reference script that produced the spec's vectors, rather than a narrower window.
6. **UI year range** — pickers and the converter accept 1583–9999 with a proleptic-calendar note at the
   low end; the library accepts 1–9999 (calendar-spec §7.1).
7. **Weekday display default** — `BOTH`: nominal IFC weekday headers with the actual weekdays in a
   second header row. Everything tied to real life (today highlight, events, reminders) uses the actual
   weekday. To be validated with testers during internal testing.
8. **Holiday rule model** — the rule types and JSON format in holidays-and-import.md supersede the
   shorter list in §3.3 below. Lunisolar holidays come from tables generated at development time, so
   `:core:holidays` stays pure JVM.
9. **`.ics` parsing** — not decided here. A hand-rolled VEVENT parser and the `biweekly` library are
   both on the table; parsing untrusted files favours a maintained library behind our own interface.
   Settled by an ADR when the import milestone starts. RRULE expansion uses `lib-recur` either way.
10. **Release scope** — 1.0 ships without any calendar permission and without network access.
    Device-calendar overlay is 1.1, `.ics` import/export and file backup 1.2, URL subscriptions 1.3.
    The agenda widget (the first widget to show event text) ships in 1.x together with the widget
    privacy mode.
11. **Exact alarms** — reminders are the justification for `USE_EXACT_ALARM`; the midnight widget
    rollover reuses it when present and must work without it (windowed alarm + system broadcasts +
    `updatePeriodMillis`). No Play build declares the permission before reminders ship. See §5.
12. **Release signing** — the upload key stays offline and the 1.0 release bundle is signed on the
    owner's machine; CI proves the tag builds but does not hold the key. CI signing is an optional
    later convenience. Play is the only binary distribution channel at 1.0. See §7.
13. **No database encryption** — SQLCipher is rejected (§8). Sensitive-data exposure is handled where
    it actually happens: widgets, lock-screen notifications, exports, and backups.
14. **Today hero weekday (owner ruling, 2026-09-25)** — the weekday shown bare above the Today hero
    date is the **IFC nominal** weekday ("that's the whole point of the app"), not the actual one tried
    first. A deliberate exception to calendar-spec §4.1 item 6 for this one line: TalkBack speaks it
    labelled ("IFC weekday: …"), the real weekday stays in the Gregorian line and the labelled weekday
    block just below, and on Year Day / Leap Day the line is omitted rather than filled with the real
    weekday. Decision 7 (real-life things use the actual weekday) is unchanged everywhere else.

## Development environment

Found on the development machine on 2026-09-17:

- Android Studio 2026.1.2 (Quail 2) with bundled JBR (OpenJDK 21.0.10) at
  `C:\Program Files\Android\Android Studio\jbr`. Quail 2 supports AGP up to 9.3.
- SDK at `%LOCALAPPDATA%\Android\Sdk` with platforms 35, 36 and 37 (37 was installed by AGP itself during
  the M0 spike — the licence is accepted on the machine, so a missing platform is fetched on first build).
  `cmdline-tools` are not installed and the build does not need them.
- `JAVA_HOME` and `ANDROID_HOME` are not set and `java` is not on `PATH`.

Command-line Gradle needs a JDK to launch the wrapper. Either set the variables once at user level:

```powershell
setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"
setx ANDROID_HOME "%LOCALAPPDATA%\Android\Sdk"
```

or per shell session (what agents should do):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

Gradle runs on JDK 21 and compiles to a Java 17 bytecode target in every module. The daemon JDK is
pinned by `gradle/gradle-daemon-jvm.properties` (Java 21, any vendor; generated by Android Studio via
`updateDaemonJvm`). Locally the JBR satisfies it and in CI Temurin 21 does, so nothing is downloaded in
practice; the foojay URLs in that file are only a fallback for a machine with no JDK 21. Modules do not
declare Java toolchains of their own, so compilation never triggers JDK provisioning.

## 1. Tech stack (version-catalog-ready)

### Build and platform

| Key | Artifact / plugin | Version | Rationale |
|---|---|---|---|
| gradle | Gradle wrapper | 9.7.1 | Latest stable (2026-08-19) and already cached locally. AGP 9.3 needs 9.5.0 or later. |
| agp | com.android.application / com.android.library | 9.3.3 | Latest patch of the newest line that the installed Studio Quail 2 supports. Move to 9.4.x only after updating Studio to Quail 4. |
| kotlin | Kotlin (jvm plugin, compose plugin, serialization plugin) | 2.3.21 | Pinned below the latest stable (2.4.20) on purpose: KSP has no verified Kotlin 2.4 line (issue google/ksp#2965 is open). Room and Hilt both depend on KSP. |
| ksp | com.google.devtools.ksp | 2.3.12 | Latest (2026-09-09). |
| compileSdk / targetSdk / minSdk | | 37 / 36 / 26 | Compose 1.12 forces compileSdk 37. Play has required target 36 for new apps since 2026-08-31. minSdk 26 gives `java.time` without desugaring. |

### UI and AndroidX

| Key | Artifact | Version | Rationale |
|---|---|---|---|
| composeBom | androidx.compose:compose-bom | 2026.09.00 | Resolves to Compose 1.12.1, material3 1.4.0 and adaptive 1.3.0. |
| material3 | androidx.compose.material3:material3 | 1.4.0 (via BOM) | Dynamic color on API 31 and later, brand fallback palette below that. |
| adaptive | material3.adaptive, adaptive-layout, adaptive-navigation, material3-adaptive-navigation-suite | 1.3.0 (via BOM) | NavigationSuiteScaffold and list-detail layouts. |
| glance | androidx.glance:glance-appwidget, glance-material3, glance-appwidget-testing, glance-preview, glance-appwidget-preview | 1.2.0 | Stable (2026-08-26). Adds `providePreview` and `setWidgetPreview`. |
| room3 | androidx.room3:room3-runtime, room3-compiler, plugin `androidx.room3` | 3.0.3 | See the decision below. |
| sqlite | androidx.sqlite:sqlite-bundled | 2.7.1 | The bundled driver gives the same SQLite on every device and allows plain JVM DAO tests. |
| datastore | androidx.datastore:datastore | 1.2.1 | Typed `DataStore<UserSettings>` with a kotlinx-serialization JSON serializer. No protobuf toolchain. `UserSettings` holds the grid, theme and colour prefs (`weekdayDisplay`, `themeMode`, `colorSource`, `palette`, `pureBlack`, `todayWidgetTheme`, `monthWidgetTheme`, `widgetBackgroundOpacity` — `docs/design-plan.md` §5), plus `enabledHolidaySets` and `hasSeenIntro`. `colorSource` (`ColorSource.BRAND` / `DYNAMIC`) replaced a `dynamicColor` boolean pre-1.0; an old file's `dynamicColor` key is simply ignored and reads as `BRAND`, since `ignoreUnknownKeys` is on. |
| nav3 | androidx.navigation3:navigation3-runtime, navigation3-ui | 1.1.7 | See the decision below. |
| lifecycle | lifecycle-runtime-compose, lifecycle-viewmodel-compose, lifecycle-viewmodel-navigation3 | 2.11.0 | Needs compileSdk 37, which is fine here. |
| activity | activity-compose | 1.13.0 | |
| coreKtx / splashscreen / window / profileinstaller | | 1.19.0 / 1.2.0 / 1.5.1 / 1.4.1 | |
| work | androidx.work:work-runtime | 2.11.2 | Transitive through Glance. Used only lightly; see section 5. |

### Dependency injection, Kotlin libraries, tests and lint

| Key | Artifact | Version | Rationale |
|---|---|---|---|
| hilt | com.google.dagger:hilt-android, hilt-compiler (KSP), plugin | 2.60.1 | See the decision below. Needs AGP 9 and Gradle 9.1 or later, which are both met. |
| androidxHilt | androidx.hilt:hilt-lifecycle-viewmodel-compose, hilt-work | 1.4.0 | |
| coroutines | kotlinx-coroutines-core, -android, -test | 1.11.0 | |
| serialization | kotlinx-serialization-json | 1.11.0 | Nav keys, the settings serializer, and holiday-set JSON. |
| junit6 | org.junit:junit-bom (Jupiter) | 6.1.3 | Pure-JVM modules only. Parameterized and dynamic tests suit golden vectors. |
| junit4 | junit:junit | 4.13.2 | Android modules. Robolectric, the Compose test rule and Roborazzi are all JUnit4. |
| kotest | kotest-property, kotest-assertions-core | 6.2.5 | Used as libraries under JUnit, not as the Kotest runner. |
| turbine | app.cash.turbine:turbine | 1.2.1 | |
| robolectric | org.robolectric:robolectric | 4.17 | First release with SDK 37 support (2026-09-10). Very fresh. |
| roborazzi | io.github.takahirom.roborazzi (plugin, core, compose, junit-rule, compose-preview-scanner-support) | 1.74.0 | See the screenshot-testing decision below. |
| androidxTest | core / runner / rules / ext-junit / espresso | 1.7.0 / 1.7.0 / 1.7.0 / 1.3.0 / 3.7.0 | |
| spotless / ktlint | com.diffplug.spotless / ktlint | 8.10.2 / 1.8.0 | Formatter and style gate. |
| lint | Android Lint | built into AGP | Correctness gate. Set `warningsAsErrors = true` and check in a baseline. |
| recur | org.dmfs:lib-recur | 0.17.1 | RRULE expansion for `Recurrence.Gregorian`, behind `RecurrenceExpander.supports` (M4 T3). Apache-2.0; pure Java 8 bytecode with no `java.time` or Java 9+ API use, so it is safe at minSdk 26. About 165 KB, plus `org.dmfs:rfc5545-datetime` 0.3 (33 KB) and `org.dmfs:jems2` 2.23.1 (143 KB). Used by Etar and OpenTasks. |

### Stack decisions

- **Hilt over Koin or manual DI.** Compile-time graph validation catches agent mistakes at build time. Each feature module wires itself with `@HiltViewModel` and `@Module`, so `:app` does not become a merge hotspot. Koin fails at runtime, and manual DI funnels all wiring through `:app`.
- **Navigation 3 over Navigation Compose.** Navigation Compose 2.9.8 is in maintenance. Nav3 is stable, and its back stack as a plain list of `@Serializable` keys is trivial to unit-test and to synthesize from widget and notification intents. The 1.2.0-rc01 release adds a deep-link matcher and a `ResultEventBus`; adopt it once it is stable.
- **Room 3 over Room 2.8.5.**
  - Room 3 is KSP-only, coroutines-only and built on SQLiteDriver. Starting on it avoids a forced `androidx.room` to `androidx.room3` migration later.
  - Agents trained on Room 2 will reach for `androidx.room.*`, so state the Room 3 package in CLAUDE.md.
  - If Room 3 blocks M4, Room 2.8.5 plus the bundled driver is a mechanical downgrade.
- **Roborazzi over Paparazzi or Compose Preview Screenshot Testing.**
  - It is the only option that renders full Compose, activities and Glance on the JVM through Robolectric.
  - Compose Preview Screenshot Testing is still alpha (0.0.1-alpha16) and is being re-homed into AGP 9.5 test suites.
  - Paparazzi cannot host activities or Glance.
- **ktlint through Spotless plus Android Lint, no detekt.** detekt 2.0 is alpha only (alpha.6), and detekt 1.23.8 predates Kotlin 2.3. Revisit when 2.0 is stable.
- **No mocking library.** Use hand-written fakes in `:core:testing`. They are more deterministic for agents.
- **Deliberately absent:** Firebase, Play Services, analytics and network libraries. The app is offline-first with no accounts, and that also keeps it eligible for F-Droid.

### Flagged as unverified, to be settled by the M0 spike

**Settled on 2026-09-17 by [adr/0001-toolchain.md](adr/0001-toolchain.md)** — items 1–3 and 5 verified
by a real build (Kotlin 2.4.20 passed the Room/KSP spike but is not adopted yet); item 4's
`lifecycle-viewmodel-navigation3` exists at lifecycle 2.11.0; lib-recur was settled at 0.17.1 by M4 T3
(see the table above); `adaptive-navigation3` and the Android 17 behaviour review remain open for M3/M7.
The original list is kept for the record:

1. **Kotlin 2.4.20 with KSP 2.3.12, Room 3 and Hilt.** Evidence is mixed. KSP2 is decoupled from the compiler, but its own "Upgrade to Kotlin 2.4.0" issue is open and third parties report lock-out. The plan is to start on 2.3.21, try 2.4.20 on the scaffold, and bump only if it is green.
2. **Kotlin 2.3.21.** The exact patch was inferred from the Dagger 2.60 release notes. 2.3.20 is known good with AGP 9.3 on this machine, so fall back to it if 2.3.21 does not resolve.
3. **Hilt, Room 3 and Roborazzi Gradle plugins under AGP 9 defaults** (`android.newDsl=true`, built-in Kotlin).
   - Dagger 2.59 and later claim AGP 9 support.
   - Opting out (`android.newDsl=false`) sidesteps the question, but a greenfield project should not, because the opt-out is removed in AGP 10.
   - Under built-in Kotlin, Android modules do not apply `org.jetbrains.kotlin.android`. Raise KGP by declaring the `org.jetbrains.kotlin.jvm` plugin version at the root.
4. **Nav3 companion artifacts.**
   - `lifecycle-viewmodel-navigation3`: the docs sample shows 2.12.0-alpha03. Confirm that it ships at lifecycle 2.11.0 stable.
   - `adaptive-navigation3`: the docs sample shows 1.4.0-alpha02. Confirm whether it is in adaptive 1.3.0 stable. If it is not, hand-roll a two-pane Scene in M3 (about 100 lines).
5. **DataStore 1.2.1.** One Google page says 1.2.0 and another says 1.2.1; 1.2.1 is known to resolve.
6. **lib-recur version.**
7. **Release dates.** The summarizer garbled years on the GitHub release pages. Version numbers are reliable; treat dates as approximate.
8. **Android 17 (target 37) behavior changes.** Not reviewed. That is why targetSdk starts at 36 and the bump is a post-launch roadmap item.

## 2. Module structure

```
D:\Dev\yearal
├─ build-logic/convention        (included build; 6 small plugins)
├─ gradle/libs.versions.toml
├─ app                           Application, MainActivity, Hilt root, Nav3 back stacks + entryProvider assembly, IntentRouter
├─ core/
│   ├─ calendar      [JVM]       IFC model + conversion + month/year layout. Depends on nothing (JDK java.time only).
│   ├─ domain        [JVM]       Event/Reminder/Holiday models, IfcRecurrence + RecurrenceExpander, HolidayRule engine,
│   │                            repository interfaces, use cases (ObserveAgenda), Clock/Zone/DateTicker, DayRolloverListener
│   │                            (done), WidgetUpdater + ReminderScheduler interfaces. Depends on :core:calendar, coroutines-core, lib-recur
│   ├─ holidays      [JVM]       Bundled holiday-set JSON packs (schema 1, ADR 0004) + strict loader (HolidayPackLoader). Depends on :core:domain
│   ├─ data          [Android]   DataStore UserSettings + SettingsRepository (done); Room 3 YearalDatabase, DAOs, mappers, RoomEventRepository (done); ICS import/export (1.2)
│   ├─ devicecalendar[Android]   CalendarContract read-only overlay (M7), isolated because it owns a permission
│   ├─ scheduling    [Android]   AlarmManager day rollover (done: DayRolloverScheduler, alarm + system-event receivers, the
│   │                            DayRolloverListener multibinding; owns RECEIVE_BOOT_COMPLETED); reminder scheduling, notifications (M6)
│   ├─ navigation    [Android-light] all @Serializable NavKeys + Navigator interface (so features never depend on each other)
│   ├─ designsystem  [Android]   Theme + brand palette, MonthGrid, DayCell, IntercalaryBand, WeekdayHeaders, IfcDateFormatter (resources),
│   │                            date pickers (IfcDatePicker, GregorianDatePickerDialog; `picker` package). Depends on :core:domain (api, for WeekdayDisplay)
│   └─ testing       [JVM+Android split if needed] fakes, fixtures, golden vectors, MainDispatcherRule
├─ feature/
│   ├─ calendar                  Today (done), Month + day card, Year
│   ├─ settings                  Settings + More hub (done); Learn/About
│   ├─ converter                 Gregorian ↔ IFC converter (done): direction switch, both pickers, copy / share
│   ├─ events                    Event list + editor (done), against :core:domain interfaces only
│   └─ holidays                  Holidays screen (done, M6 T2): browse/toggle every bundled set, a per-year
│                                 list grouped by IFC month; Settings links here instead of duplicating switches
├─ widget                        Glance widgets, widget receivers, config activity, WidgetUpdater impl
└─ baselineprofile               (M8)
```

### Dependency direction

Dependencies are strictly one-way: `feature:*` and `widget` depend on `core:designsystem`, `core:navigation`, `core:domain` and the pure-JVM `core:holidays`; `core:designsystem` depends on `core:domain`; `core:domain` depends on `core:calendar`.

- Features depend on `:core:domain` interfaces and never on `:core:data`.
- Only `:app` depends on `:core:data`, `:core:scheduling`, `:core:devicecalendar` and `:widget`. It needs them to put the Hilt bindings on the classpath.
- Features never depend on other features. Cross-feature navigation goes through `:core:navigation` keys.
- **As built (ROADMAP M0 T4):** the Gradle check lives in `ifc.android.library` (§2 "build-logic" below),
  so it covers every Android library module, not only `:feature:*`, and fails the build if one depends
  on a `feature` or on `:core:data`.

Three pure-JVM modules hold all the logic that must be correct. They build and test in seconds with no Android toolchain, which makes them ideal for delegated agents.

### Package naming

Base: `io.github.chrisjmendoza.yearal` (the app is **Yearal**, ROADMAP.md decision #1, 2026-09-18). This is also the applicationId, which can never change after the first Play upload.

- A GitHub-derived reverse domain is legitimate, free, and matches a public repo.
- The namespace for each module is `<base>.core.calendar`, `<base>.feature.events`, and so on.
- The applicationId equals the base.

### build-logic

Yes, add it, but keep it minimal. With about 17 modules, copy-pasted `android {}` blocks are the top source of agent drift.

- Plugins, modelled on Now in Android and written against the AGP 9 new-DSL `CommonExtension`
  (shared code in `IfcAndroid.kt`; the API-shape rules are in [adr/0001-toolchain.md](adr/0001-toolchain.md)):
  - `ifc.jvm.library`: Kotlin JVM, Jupiter, Kotest, `explicitApi()`, Dokka KDoc gate, and the pure-JVM
    dependency rule check below.
  - `ifc.android.library`: SDK levels from the catalog, Java 17, lint as an error gate, JUnit4 +
    Robolectric, the generated Robolectric SDK pin, and the Android dependency rule check below.
  - `ifc.android.compose`: Compose compiler, BOM dependencies, Roborazzi.
  - `ifc.android.feature`: library, compose and hilt, plus the standard `:core:*` dependencies and
    Turbine. The dependency rule check is `ifc.android.library`'s own (below), inherited because this
    plugin applies it.
  - `ifc.android.application`: target SDK and the SemVer `versionCode`.
  - `ifc.hilt`: Hilt + KSP.
  - `ifc.kotlin.serialization` and `ifc.room`: the compiler plugins must be applied from `build-logic`'s classpath (ADR 0001, decision 3).
- Spotless is configured per module by the convention plugins (ktlint from the catalog).
- Do not write custom tasks beyond these, `generateRobolectricProperties` (below) excepted.
- **Module dependency rule check (ROADMAP M0 T4), `DependencyRules.kt`.** Two small functions, shared by
  the convention plugins that need them, walk every `implementation`/`api` configuration's declared
  project dependencies at `afterEvaluate` and fail the build with a message citing the broken CLAUDE.md
  rule:
  - `forbidProjectDependencies` (deny-list): every module that applies `ifc.android.library` — which
    `ifc.android.feature` also applies, so this covers `:feature:*` too — may not depend on a
    `:feature:*` module or on `:core:data`. This is what keeps `:core:designsystem`, `:core:navigation`,
    `:core:data` itself, `:core:scheduling` and `:widget` from depending on a feature or on `:core:data`
    (CLAUDE.md rule 10; only `:app` wires those together, per "Dependency direction" above — the
    `:widget` "may not depend on `:feature:calendar`" and "cannot depend on `:app`" notes in §5 are this
    same rule stated for `:widget` by name).
  - `restrictProjectDependenciesTo` (allow-list): every module that applies `ifc.jvm.library` may depend
    only on another pure-JVM module (`:core:calendar`, `:core:domain`, `:core:holidays`, `:core:testing`
    — never an Android module or `:core:data`, CLAUDE.md rule 11); `:core:calendar` gets an empty
    allow-list instead of that set, since it depends on nothing in the project at all (CLAUDE.md rule 1).
  - Both were verified empirically by temporarily adding a forbidden dependency to a module and
    confirming the build fails with the expected message, then reverting.
- **The Robolectric SDK pin (ROADMAP R7) is generated, not hand-copied.** `configureIfcAndroid`
  registers a `generateRobolectricProperties` task per Android module that writes
  `sdk=<catalog targetSdk>` to a `robolectric.properties` under `build/generated/robolectricProperties`,
  and prepends that directory onto every `Test` task's own `classpath` (`testOptions.unitTests.all`). A
  library module's test manifest has no `targetSdk` of its own, so without a pin Robolectric defaults to
  the newest SDK it ships, ahead of what the Compose test rule's Espresso input injection supports, and
  every Compose interaction test fails with a cryptic `InputManager.getInstance()`
  `NoSuchMethodException`. **Verified empirically that the fix must be a `robolectric.properties` file on
  the classpath, not a system property**: Robolectric only resolves its `sdk` config value through
  `Config.Implementation.fromProperties` via a classpath resource lookup; the `robolectric.<key>`
  system-property override its docs advertise wires up only the enum-valued configs (`LooperMode`,
  `GraphicsMode`, ...) through a different mechanism (`SingleValueConfigurer`) that `sdk` has no path
  into — `systemProperty("robolectric.sdk", ...)` is silently ignored. Declaring the generated directory
  as a `testImplementation` file dependency was tried first and also silently dropped: AGP's variant
  dependency model does not carry a plain file dependency on that bucket through to the resolved
  per-variant unit-test runtime classpath. Mutating the `Test` task's `classpath` directly is what
  worked, confirmed by deleting `:feature:calendar`'s hand-written copy and watching its Compose tests
  fail with the `InputManager` exception until the generated file was actually on that classpath.
  `:core:designsystem`'s `RobolectricSdkPinTest` pins the guarantee permanently (`Build.VERSION.SDK_INT`
  equals the catalog's `targetSdk`) in a module with no properties file of its own to hide behind.
- **Prefer a scrollable `Column` over `LazyColumn` for a bounded list in a screen that has Robolectric
  tests.** Robolectric's default window never composes off-screen lazy items and `performScrollTo()`
  cannot realise them, so assertions on anything past the fold fail with no hint at the cause. Every
  screen in the app follows this today.

## 3. Domain model

### 3.1 `:core:calendar`

The rules, algorithms, type model, formatting, and arithmetic semantics are specified in
[calendar-spec.md](calendar-spec.md) (§3, §4, §7). This module implements that spec and nothing else.
In brief: the IFC day-of-year equals the Gregorian day-of-year, so conversion is pure integer
arithmetic, and two invariants are worth remembering — Sol 1 is always Gregorian June 18, and Year Day
is always December 31.

`IfcDate` is the sealed interface from calendar-spec §4 (`Regular`, `LeapDay`, `YearDay`). On top of
it, this module provides the layout type the month grid and range queries are built on:

```kotlin
data class IfcYearMonth(val year: Int, val month: IfcMonth) {
    val firstDay: LocalDate
    val trailingIntercalary: IfcDate?             // LeapDay for June in leap years; YearDay for December; else null
    val gregorianRange: ClosedRange<LocalDate>    // 28 days, or 29 incl. trailing intercalary - ALWAYS contiguous
    fun actualDayOfWeek(column: Int): DayOfWeek   // constant down the column (28 = 4 whole weeks)
    fun plusMonths(n: Long): IfcYearMonth
}
```

**Design decisions:**

- The arithmetic core works on `(year, dayOfYear)` ints and an `epochDay` Long. `java.time` is a thin adapter layer, which keeps a later KMP port cheap.
- Validation happens at construction: day 1 to 28, Leap Day only in leap years, years 1 to 9999 (proleptic Gregorian, same as `java.time`).
- The canonical numeric text form is `IFC YYYY-MM-DD` with Leap Day `06-29` and Year Day `13-29` (calendar-spec §7.3). `parse` accepts it with or without the prefix; formatting for display always includes it.
- Day and week arithmetic delegates to `LocalDate`. Month and year arithmetic works on the IFC fields and clamps the way `java.time` does (calendar-spec §7.7).
- `explicitApi()` is on. The public API is frozen at the end of M1 and documented in [contracts/Calendar.md](contracts/Calendar.md).
- There is no Android code, no localization and no display formatting here. Names live in resources in `:core:designsystem`.

**Weekday insight for the UI.** Every month is 28 days, so the actual weekday of grid column k is constant within a month. It is constant across the whole year up to an intercalary day, and it shifts by one after Leap Day.

- In 2026, 1 January is a Thursday, so every IFC "Sunday" column in 2026 is an actual Thursday.
- Column headers can therefore show both weekdays cheaply, for example `Sun` with a small `Thu` under it.
- Code still obtains the actual weekday from `toLocalDate().dayOfWeek` and never derives one weekday from the other (calendar-spec §4.1).

**Golden vectors** live in calendar-spec §6 (105 machine-generated pairs plus negative vectors). They are copied into `:core:testing` as a CSV and are the acceptance test for this module. Spot check: Gregorian 2026-09-17 is IFC September 8, 2026 — nominal Sunday, actual Thursday.

### 3.2 Events

Events are anchored to Gregorian wall-clock time. Instants are derived and never stored.

The domain models, `EventRepository`, `RecurrenceExpander`, `ObserveAgendaUseCase`, the rule-text grammar
and the fakes are **frozen in [contracts/Events.md](contracts/Events.md)** (M4 T1); the points this section
left open are decided in [adr/0005-events-contract.md](adr/0005-events-contract.md).

```
calendars(id PK, name, color_argb, source /*LOCAL|ICS*/, visible)          -- an ICS import becomes its own calendar;
                                                                           -- row 1 is the built-in local calendar (name may be empty)
events(
  id PK AUTOINCREMENT, uid TEXT UNIQUE /*UUID, ICS round-trip*/, calendar_id FK,
  title, description, location, color_argb NULL, category /*EVENT|OBSERVANCE|BIRTHDAY*/,
  all_day INT,
  start_epoch_day INT NOT NULL,          -- local date in the event's zone
  start_minute_of_day INT NULL,          -- NULL iff all_day
  duration_minutes INT NOT NULL,         -- all-day: N*1440; timed: nominal wall-clock minutes
  end_epoch_day INT NOT NULL,            -- denormalised last local date of first occurrence (range index)
  zone_id TEXT NULL,                     -- NULL = floating (device zone); all-day is always floating
  recurrence_type INT NOT NULL,          -- 0 none | 1 Gregorian RRULE | 2 IFC rule
  rrule TEXT NULL,                       -- RFC 5545 string (lossless ICS)
  ifc_rule TEXT NULL,                    -- IfcRuleText, e.g. IFC;FREQ=YEARLY;MONTH=7;DAY=13 | IFC;FREQ=YEARLY;INTERCALARY=YEAR_DAY |
                                         --   IFC;FREQ=YEARLY;INTERCALARY=LEAP_DAY;COMMONYEAR=JUNE_28 | IFC;FREQ=MONTHLY;DAY=13;INTERVAL=2
  recurrence_until_epoch_day INT NULL,   -- RecurrenceExpander.recurrenceEndDate(event): last date of the last occurrence; NULL = unbounded
  created_at, updated_at)
  INDEX(start_epoch_day), INDEX(end_epoch_day), INDEX(recurrence_type, recurrence_until_epoch_day), INDEX(calendar_id)
event_exdates(event_id FK CASCADE, epoch_day, PK(event_id, epoch_day))     -- "delete this occurrence"; the occurrence's own start date
reminders(id PK, event_id FK CASCADE, minutes_before INT, UNIQUE(event_id, minutes_before))
```

Schema v1 above is frozen (ROADMAP M8 T5): `SchemaFreezeMigrationTest` and `LegacyDatabaseOpenTest`
(`:core:data`) validate the compiled entities against a byte-for-byte frozen copy of the exported schema
at `core/data/src/test/resources/frozen-schemas/` — never against the live, KSP-rewritten
`core/data/schemas/` directly, since Room's own export task keeps that file in sync with the current
entities whenever the version number is unchanged — and `SchemaExportGuardTest` keeps the live export
and the frozen copy from drifting apart; a real schema change is always version bump + a new exported
JSON + a `Migration` + a migration test here + (only then) a refreshed frozen copy, never an edit to
either `1.json` on its own.

- **IFC recurrence** is modelled as `sealed IfcRecurrence { YearlyOnDate(month, day), YearlyOnIntercalary(day), MonthlyOnDay(day) }`, plus `interval` and `until/count`.
  - `YearlyOnIntercalary(LeapDay)` carries a common-year policy, `JUNE_28 | SKIP | SOL_1`. User-created events default to `JUNE_28`, which keeps the Gregorian date at June 17 every year. The rule text always spells the policy out (`COMMONYEAR=`).
  - The event's start is occurrence 1 and must be a position of the rule; the text form (`ifc_rule`, `X-IFC-RRULE`) is implemented once, in `IfcRuleText` (grammar in [contracts/Events.md](contracts/Events.md) §3).
  - IFC rules expand in O(1) per year by direct construction, `IfcDate.Regular(y, m, d).toLocalDate()`, with no iteration.
  - Gregorian rules go through lib-recur with fast-forward.
  - There is no materialised occurrences table. `RecurrenceExpander` in `:core:domain` is pure and property-tested.
- **Scope cuts:**
  - Version 1 supports edit all, delete all, and delete one occurrence (EXDATE).
  - Per-occurrence overrides (an `event_overrides` table) come after 1.0.
  - User-defined holidays are yearly all-day events with `category=OBSERVANCE`, so they need no extra table.
  - ICS export writes `ifc_rule` as `X-IFC-RRULE` plus a Gregorian-approximate fallback.
- **Time zones:**
  - A non-null `zone_id` keeps the wall time fixed in that zone. A null `zone_id` follows the device.
  - Expansion yields `Occurrence(eventId, startLocal, endLocal, zone, allDay)` with **nominal** wall-clock values.
  - Bucketing by day converts each occurrence to the device zone (`Occurrence.dates(deviceZone)`); all-day occurrences are never shifted. A wall time in a DST gap happens later by the length of the gap, one in an overlap is the earlier instant; durations are nominal and ends exclusive (ADR 0005).
- **Reminders** use a single next-alarm pattern: only the earliest upcoming reminder is scheduled, as one PendingIntent.
  - When it fires, the app posts the notification, then recomputes and reschedules.
  - It also recomputes on event writes, boot, time or zone changes, and app update.
  - This pattern never approaches the 500-alarm cap.
  - All-day reminders fire at a configurable local time, 09:00 by default.
  - `POST_NOTIFICATIONS` is requested in context, the first time the user adds a reminder.

**As built (M6 T1, `:core:scheduling`, package `core.scheduling.reminder`).**

- `AlarmReminderScheduler` is the `ReminderScheduler` of [contracts/Events.md](contracts/Events.md) §5 and
  a `DayRolloverListener` (§5 below) in one `@Singleton`: `reschedule()` is the only entry point and the
  rollover hook just calls it. It is bound in `:core:scheduling`'s own `SchedulingModule` (`@Binds` plus
  `@Binds @IntoSet`), replacing the documented no-op `@Provides` that stood in `:app`'s `EventsModule`. It
  takes `EventRepository` as a `javax.inject.Provider`, because the production repository injects a
  `ReminderScheduler` to call after each write and taking it directly would be a Dagger cycle.
- One call does everything: read the clock and `ZoneProvider`, ask
  `EventRepository.getReminderCandidates(today)` (visible calendars, events with reminders), turn each
  candidate into trigger instants through `RecurrenceExpander.nextOccurrence` — that is `ReminderPlanner`,
  a pure function with its own oracle tests — post what has come due, and arm **one** alarm for the
  earliest instant still ahead, or cancel the alarm when there is none. Nothing is cached between calls
  except how far delivery has got, so a late delivery, a backwards clock change and a flight across zones
  all come out right.
- **Reference instants.** Timed: `Occurrence.start(deviceZone)`, so DST gaps and overlaps resolve by the
  frozen rule on `Occurrence`. All-day: 09:00 in the device zone on the occurrence's first date. The
  "configurable" part of the bullet above is **not built**: `UserSettings` has no reminder-time field
  ([contracts/Events.md](contracts/Events.md) §6 puts one out of 1.0 scope), so 09:00 lives as
  `ReminderPlanner.ALL_DAY_REMINDER_TIME`, which is the single definition of it.
- **Late reminders** — one whose instant passed while the device was off, in Doze, or before the process
  existed — are delivered late if they are no more than 15 minutes old, and dropped otherwise. Late
  delivery is what makes a reminder survive a reboot at all, since a reboot cancels every alarm and the
  app is only called again at `BOOT_COMPLETED`; the bound keeps a phone that was off for a week from
  emptying a fortnight of reminders into the shade. A high-water mark inside the singleton stops the same
  reminder being posted twice, which is what makes recomputing on every repository write free. The mark is
  in memory, so a fresh process may re-post a reminder from the last 15 minutes once; ids are stable, so
  that replaces the same notification rather than adding a second one.
- **Work is bounded.** At most one occurrence per calendar date and at most
  `ReminderPlanner.MAX_OCCURRENCES_PER_EVENT` (64) occurrences per event are examined; the walk normally
  stops after two, as soon as no later occurrence of that event could produce an earlier instant.
- **Notifications** are [security-and-privacy.md](security-and-privacy.md) §3.3's: one channel, title and
  time only, `VISIBILITY_PRIVATE` with a redacted public version, no full-screen intent, stable ids
  derived from ids, and a tap that opens the app through an explicit `PendingIntent` carrying the event
  id (`ReminderIntent.EXTRA_EVENT_ID`, ROADMAP M4 T10 — §4 "Intent routing" has the detail). On API
  33+ the permission is checked immediately before posting; denied means nothing is posted and the alarm
  is still armed, so the app works in full without notifications (FEATURES P2).
- **Around it:** the in-context `POST_NOTIFICATIONS` request is the event editor's (`:feature:events`), and
  `IfcApplication.onCreate` calls `reschedule()` off the main thread through `AppStartup` on every process
  start, so a reminder alarm lost to a force-stop or an OEM task killer is back as soon as anything starts
  the app. **Not built:** snooze.

### 3.3 Holidays

Holidays are computed, never stored.

- The rule types, modifiers, and the bundled JSON format are specified in [holidays-and-import.md](holidays-and-import.md): `fixed`, `nthWeekday` (negative n = last), `offset`, `easter` (western and orthodox), `table` (pre-generated lunisolar dates), and `ifc`, with `observed` policies and `since/until` bounds. In code this is a `sealed HolidayRule` hierarchy in `:core:domain`.
- A set is `HolidaySet(id, region, name, sources, holidays)`, loaded from JSON resources in `:core:holidays`
  by `HolidayPackLoader`; the engine is `HolidayEngine` in `:core:domain`. Decisions the spec left open
  (rule year vs anchor year, observed entries, memoisation key, the `SUNDAY_TO_MONDAY` policy) are in
  [adr/0003-holiday-rule-model.md](adr/0003-holiday-rule-model.md); the schema decisions in
  [adr/0004-holiday-pack-format.md](adr/0004-holiday-pack-format.md).
- Evaluation per year takes microseconds and is memoised.
- Enabled set IDs live in DataStore.
- `HolidayEngine` is bound in `:feature:calendar` (`di/HolidayModule`) and `HolidayPackLoader` in `:feature:settings`; both move to `:app` if a third module needs them. The IFC observances set and the US pack are enabled by default; any set, the IFC one included, may be switched off in Settings (`UserSettings.enabledHolidaySets`). Version 1 ships the "IFC observances" set (Year Day, Leap Day, Sol 1) and the US pack (federal holidays plus common observances). Lunisolar tables are a 1.0 stretch goal that can slip to 1.1 without affecting the engine. More sets are data-only PRs.
- **`HolidaySetProvider`** (`:core:domain`, M4 T6) is the one seam "which sets are enabled" flows through: `enabledSets(): Flow<List<HolidaySet>>`, the bundled packs already filtered to `UserSettings.enabledHolidaySets`. `:core:domain` cannot depend on `:core:holidays` (dependency direction, §2), so the production implementation, `PackHolidaySetProvider`, lives in `:feature:calendar` next to `HolidayEngine` and is bound there (`di/HolidayModule`); `ObserveAgendaUseCase`'s own binding lives in `:app` (`di/AgendaModule`) and receives it across the Hilt component the same way `HolidayCatalog` already receives `HolidayPackLoader` from `:feature:settings`. `HolidayCatalog` (grid and Day-detail label formatting) is unchanged and keeps evaluating `HolidayEngine` itself from an explicit `enabledSetIds` argument, since it needs arbitrary set ids for previews and per-call flexibility that a `Flow`-shaped provider does not give for free; the provider's job is only to give `ObserveAgendaUseCase` the same "enabled" definition without loading `:core:holidays` types into `:core:domain`.
- Device calendars (M7) are a read-only overlay through `CalendarContract.Instances`, queried on the same Gregorian range. The feature is opt-in and the `READ_CALENDAR` prompt appears in context.

### 3.4 Month-grid query

1. `IfcYearMonth.gregorianRange` gives a contiguous `[start, end]` of 28 or 29 days.
2. `ObserveAgendaUseCase(range): Flow<Map<LocalDate, DayAgenda>>` combines four sources:
   - **Non-recurring events:** `WHERE recurrence_type=0 AND start_epoch_day <= :end+2 AND end_epoch_day >= :start-2`. The two-day padding (`EventRepository.ZONE_SKEW_DAYS`) covers zone skew: offsets span UTC−12..UTC+14, so a zoned event can show two dates away from its own. Filter exactly in Kotlin.
   - **Recurring candidates:** `WHERE recurrence_type!=0 AND start_epoch_day <= :end+2 AND (recurrence_until_epoch_day IS NULL OR >= :start-2)`. This is a small set. Expand it in memory and subtract exdates.
   - Both come from `EventRepository.observeAgendaCandidates(range)`, restricted to visible calendars.
   - **Holidays:** `holidays(year, enabledSets)` filtered to the range.
   - **Device instances:** optional.
3. Bucket the results by local date. The UI maps dates to cells through `IfcDate.from`. The map holds only dates that have something on them.

The pager keeps three months warm with `beyondViewportPageCount = 1`: `MonthViewModel` calls `ObserveAgendaUseCase.invoke` once per warm month (its own page plus one neighbour on each side) and reads `DayAgenda.entries.size` for the grid's event-dot counts (FEATURES C4); the Month day card and Today query it for exactly one day each. The Year view issues one range query for the whole year and returns only a presence bitmap (`ObserveAgendaUseCase.presence(range): Flow<Set<LocalDate>>`, event occurrences only).

**Per-month subscriptions survive a single-page swipe (compose-perf pass).** "Once per warm month" is a
standing subscription, not a per-page-change one: `MonthViewModel.agendaCountsFor` caches each warm
month's `observeAgenda(range).shareIn(...)` flow, keyed by `IfcYearMonth`, and only trims the cache to
the current three-month window after building the next combine. Moving the pager by one page keeps two
of the three months warm, so only the month that newly enters the window issues a fresh
`ObserveAgendaUseCase.invoke` (and, in the production implementation, a fresh Room query) — the naive
`page.flatMapLatest { eventCountsAround(it) }` shape this replaced cancelled and re-issued all three on
every page change, including the two that had not actually changed. A future simplification back to that
shape would reintroduce that cost; `MonthViewModelTest`'s "paging keeps the agenda subscription for
months that stay warm" pins the query count.

## 4. UI architecture

### Screens and navigation

- **Bottom bar / rail:** the top-level destinations, via `NavigationSuiteScaffold`, are **Today | Calendar | Events | Convert | More**. "More" holds Holidays, Settings, Learn/About and Privacy.
- **Nav keys** live in `:core:navigation`: `TodayKey`, `IntroKey`, `MonthKey(year, month, selectedEpochDay?)` (a "day" destination is a `MonthKey` with the day selected — there is no separate day key since the Day detail popup was merged into the Month day card, 2026-09-25), `YearKey(year)`, `ConverterKey(prefillEpochDay?)`, `EventListKey`, `EventEditorKey(eventId?, prefillEpochDay?)`, `MoreKey` (the hub tab), `HolidaysKey`, `SettingsKey`, `LearnKey`, `PrivacyKey`.
- **Per-tab back stacks** are `TabBackStacks` in `:app`: one `NavBackStack` per tab, the Today root prefixed when another tab is shown so that back from a tab root returns to Today; the Calendar tab's root `MonthKey` is resolved from `DateTicker` when the tab is first opened.
- **Entry providers:** `:app` owns the per-tab back stacks (the Nav3 "top-level back stack" recipe), the single `NavDisplay`, and its `entryProvider` block, which registers one `entry<XKey> { XRoute(...) }` per nav key directly in [`ui/IfcApp.kt`](../app/src/main/kotlin/io/github/chrisjmendoza/yearal/ui/IfcApp.kt) — features do not expose their own `EntryProviderScope` extension; each just exports its `XRoute` composable(s) for `:app` to wire up.
- **Intent routing — as built (ROADMAP M3 T5, M4 T10).** Widget and notification taps send explicit
  intents with typed extras; `IntentRouter` (`:app`, package `intent`) turns a validated one into an
  [`AppRoute`](../app/src/main/kotlin/io/github/chrisjmendoza/yearal/intent/AppRoute.kt) and
  [`TabBackStacks.applyRoute`](../app/src/main/kotlin/io/github/chrisjmendoza/yearal/ui/navigation/TabBackStacks.kt)
  turns that into a back stack — a day opens `[MonthKey(of that day, selectedEpochDay)]` on the
  Calendar tab, an event opens `[EventListKey, EventEditorKey(eventId)]` on the Events tab, "today"
  selects the Today tab, and the Month widget's whole-widget tap opens `[MonthKey(current month)]`. No
  URI deep links are needed until Nav3 1.2 is stable.
  - **Where the extra-name contract lives.** `:widget` and `:core:scheduling` each own a small, plain
    object of action-string and extra-name constants
    ([`WidgetIntents`](../widget/src/main/kotlin/io/github/chrisjmendoza/yearal/widget/WidgetIntents.kt),
    [`ReminderIntent`](../core/scheduling/src/main/kotlin/io/github/chrisjmendoza/yearal/core/scheduling/reminder/ReminderIntent.kt))
    rather than a shared module. `:core:navigation`'s `NavKey` types need
    `androidx.navigation3:navigation3-runtime` as an `api` dependency, and that artifact's own POM pulls
    in `androidx.compose.runtime` and `runtime-saveable` — a UI back-stack dependency this task judged
    **not acceptable** to add to either a headless Glance widget module or a headless alarm/notification
    module that has never needed Compose. `:core:domain` is the module both producers already share, but
    it is off limits to a module boundary change here; a new module was judged unwarranted for two small
    constant objects. `:app` already depends on both `:widget` and `:core:scheduling`
    (`docs/ARCHITECTURE.md` §2), so `IntentRouter` references their constants directly, and NavKey
    construction stays entirely in `:app` — one source of truth per producer, checked by
    `IntentRouterTest`, `ReminderNotifierTest` and this module's own launch-intent tests, rather than a
    shared contract module.
  - **Validation is the point** (`docs/security-and-privacy.md` §6.3): an unrecognized or missing
    action, or a `Long` extra that is absent or stored under its name as any other type (a `Uri`, a
    nested `Intent`, a class name — `IntentRouter.longExtraOrNull` requires an exact type match, never
    `Bundle`'s type-mismatch-to-default coercion), resolves to `AppRoute.Default` and the app opens on
    its normal start destination. An epoch day that parses but falls outside `:core:calendar`'s
    supported years (`IfcDate.MIN_YEAR..MAX_YEAR`) fails the same way. An event id that is structurally
    valid but no longer exists is still routed to the editor, which already has its own "not found"
    state (`EventEditorViewModel`'s `notFound` flag) — `IntentRouter` cannot check existence itself
    without a database read (CLAUDE.md rule 8: ids only).
  - **Exactly-once delivery.** `MainActivity` keeps `android:launchMode="singleTask"` (unchanged) so a
    tap that finds the activity already running delivers through `onNewIntent`, which always routes —
    it is inherently a new, deliberate action. `onCreate` routes through
    `MainViewModel.routeFromCreate`, which is a no-op if `SavedStateHandle` already recorded a route
    for this activity instance's lifetime: unchanged across a configuration change (the same
    `MainViewModel` survives it) and, thanks to the `SavedStateHandle`'s restored `Bundle`, also unchanged
    across a process restart where Android re-delivers the original launch `Intent`.
    `IfcApp` applies the pending route (`TabBackStacks.applyRoute`) once the tab back stacks — and, for
    `AppRoute.CurrentMonth`, `DateTicker`'s "today" — are available, then consumes it.
  - **Producers.** The reminder notification's tap (`:core:scheduling`, `ReminderNotifier`) carries
    `ReminderIntent.ACTION_OPEN_EVENT` and the event id, with a request code derived from the event id
    so two different events' notifications never share one `PendingIntent`. The Today widget's tap
    carries `WidgetIntents.ACTION_OPEN_TODAY`. The Month widget's 28 day cells and its trailing Leap
    Day / Year Day band are each individually clickable (`GlanceModifier.clickable` per cell, resolved
    to its own explicit intent with `ACTION_OPEN_DAY` and that cell's epoch day) — Glance/RemoteViews
    resolves a tap to the most specific clickable view under it, so a cell's own target overrides the
    whole-widget `ACTION_OPEN_MONTH` fallback (the title, header rows and Gregorian-span line) inside its
    own bounds. Every `PendingIntent` stays explicit, immutable, and carries only ids or epoch days.
- **Screen behaviors:**
  - **Today** (`docs/design-plan.md` §4.1): a hero `Card` on `YearalTheme.colors.heroContainer`/`onHero` — today's **IFC (nominal)** weekday in `titleLarge` (bare, spoken as "IFC weekday: …"; omitted on Year Day and Leap Day — `TodayUiState.Loaded.heroWeekday`; owner ruling 2026-09-25, see design-plan §4.1), then the IFC date in `displayMedium`, an "IFC"/"Gregorian" eyebrow pair (the numeric IFC form and the Gregorian long date), the weekday block (unchanged sage `secondaryContainer`), the day/week/quarter line and a tinted year-progress bar. Below it: the intercalary countdown as an amber `PillShape` chip (`intercalaryContainer`, the `ic_intercalary` icon); a Holidays card and an Events card (`cardContainer`), each with a `labelSmall` eyebrow heading, holiday rows carrying a leading diamond mark (`HolidayDiamondMark`, `:feature:calendar`'s own `common` package, shared with the Month summary and Day detail) and agenda rows keeping their colour swatch with a hairline between them; both cards show one quiet line rather than disappearing when there is nothing to show.
  - **Month** (`docs/design-plan.md` §4.2): a `HorizontalPager` of months, one page per month, **anchored**: the app bar (`surfaceContainerLow`) → the `MonthGrid` at the top (`showTitle = false`, since the app bar itself is the page's one title) → a **selected-day summary** filling the rest of the page. The app bar's title is a tappable pill (`MonthTitlePill`, a `PillShape` chip with a trailing chevron, content-described "Show year") showing the visible page's month and year — tapping it zooms out to **Year** (docs/ROADMAP.md M3 T2), done this way rather than making the grid's own heading tappable because the grid is a shared `:core:designsystem` component and the zoom-out is `:feature:calendar` behaviour. Because hiding the grid's own heading also hides its weekday `ExplainerInfoButton` (a trap `MonthGrid`'s own KDoc calls out), the app bar carries an equivalent one as its first action, followed by the jump-to-date action (FEATURES C7) and the "Today" action. The **day card** (`DayCard`, `feature/calendar/month/DayCard.kt`) is the whole day detail — there is no popup (owner, 2026-09-25: the sheet only repeated the card, so the card absorbed it). It shows `MonthUiState.dayDetail`, built by `MonthViewModel` for the selected day — or today when nothing is selected — with `buildDayDetailUi` (every string from `IfcDateFormatter`): the IFC long date as its heading, the `IFC`-prefixed numeric date, the Gregorian long date, a static "Today" badge, the labelled weekday block, day/week/quarter, holidays (diamond rows) and events (colour-swatch rows, each tappable into the editor), then "Add event" and "Open in converter". Its holidays and agenda are evaluated for that one day (`HolidayCatalog.labels` and a single-day `ObserveAgendaUseCase` subscription, separate from the per-month `holidaysByMonth`/`eventCountsByMonth` the grid itself reads, with a stale-snapshot guard so a new day never shows the previous day's rows for a frame), and at compact/medium widths it renders only on the page the pager is actually settled on, since `beyondViewportPageCount` keeps neighbouring pages composed too. A grid tap only selects (`MonthViewModel.select`); nothing navigates. A `MonthKey` with `selectedEpochDay` opens the pager on that day already selected — `MonthPages.selectedDateOf` validates it and fails soft to no selection outside 1583–9999 — which is how the Holidays list, the converter's "Open day" and widget/notification taps open a day. Holidays for the grid come from `HolidayCatalog`, which evaluates the enabled packs with `HolidayEngine` for the visible page ±1. The jump-to-date action opens a small calendar chooser (Gregorian or IFC, the event editor's own pattern) and pushes the chosen date's `MonthKey`.
  - **Year** (`docs/design-plan.md` §4.3): 13 mini-months in a `LazyVerticalGrid(Adaptive(160.dp))`, each drawn as a single `Canvas` rather than 28 real day cells (`YearMiniMonthTile`, `:core:designsystem`) — 364 real cells visibly cost frames while scrolling the grid. Each of the 28 squares is a `DayCell` in miniature: the IFC day number (a `TextMeasurer` layout per number, remembered per tile size and only blitted in `onDraw`, sized to the square and capped at `labelSmall` so it still fits at font scale 2.0), the holiday diamond / event dot beneath it, and the `todayRing` ring plus a bold `todayText` number on today. The squares fill with `miniGridCell` / `miniGridCellMarked` — two tiers above the tile's `cardContainer` — because `gridCell` is the card's own Material role and made every square invisible (fix R11). On a two-column phone, Year Day takes the 14th slot. The app bar uses the same `surfaceContainerLow` colour as Month's and a `titleLarge` year number; the grid's `contentPadding` adds the Scaffold's own top inset plus a fixed gap so the first row is never clipped under the app bar. `YearUiState.holidays` (from `HolidayCatalog`, the same range `ObserveAgendaUseCase.presence` already covers) is evaluated and passed to `YearMiniMonthTile` (`YearScreen.kt`'s `holidays = state.holidays.keys`), which draws it alongside `eventDates`. A mini-grid day carrying both a holiday and an event draws both marks rather than picking one: the `.holidayMark` diamond stays centred in the cell and a smaller `.eventMark` dot sits in the cell's lower-right corner, the corner the diamond does not reach.
  - **Converter:** one chosen day and a direction switch (done). Gregorian → IFC picks the date in the Material 3
    `DatePickerDialog`; IFC → Gregorian uses `IfcDatePicker`. Both live in `:core:designsystem` (`picker` package) so the
    M4 event editor can reuse them, both are limited to `DatePickerRange` (1583–9999, reconciled decision 6), and the
    Material picker's UTC-millisecond API is adapted to `LocalDate` inside the wrapper, with no millisecond arithmetic.
    `IfcDatePicker` is stateless over an immutable `IfcDatePickerValue` (year as typed + selected day): Year Day is always
    offered, Leap Day only in leap years, and a selected Leap Day moves to June 28 — with a visible, announced notice —
    when the year becomes a valid common year (calendar-spec §7.10). The default input is today from `DateTicker`;
    `ConverterKey.prefillEpochDay` overrides it when it lies in range and is ignored otherwise. Input lives in the
    ViewModel's `SavedStateHandle` as primitives and is re-validated on restore. The result shows both dates, the numeric
    `IFC` form, both labelled weekdays, and the proleptic note for years up to 1923 (the latest adoption calendar-spec
    §7.1 names); copy and `ACTION_SEND` text always carry the `IFC` marker and the Gregorian date. **The design pass
    (done, docs/design-plan.md section 4.6, ROADMAP F2):** the result is a card on `YearalTheme.colors.heroContainer`
    (`MaterialTheme.shapes.large`), the same component the Today hero uses, with the IFC and Gregorian dates as
    eyebrow-labelled ("IFC" / "Gregorian", uppercase `labelSmall`) value pairs — the direction's answer in
    `headlineSmall`, the restated input in `titleMedium` — and the intercalary icon next to the IFC value on Year Day
    and Leap Day; the weekday block keeps a container, now `YearalTheme.colors.weekdayNominalContainer`, and its actual-
    weekday line uses `weekdayActualText` instead of a plain grey. A swap `IconButton` (`ic_swap.xml`, this module's own
    res — material-icons-core has no swap glyph, the same reasoning as `:app`'s `ic_convert.xml`) sits beside the
    segmented direction control and flips it. Copy, Share and Open day are `Button`/`OutlinedButton`s with a leading
    icon: `ic_copy.xml` and `ic_open.xml` (hand-drawn, same reasoning) and `Icons.Filled.Share` (material-icons-core
    has it). The app bar uses `TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)`.
  - **Day detail** (`docs/design-plan.md` §4.4) lives in the Month screen's day card (see **Month** above); the compact-width bottom sheet (`DayRoute`/`DayScreen`/`DayViewModel`, behind a `DayKey`) was removed on 2026-09-25. On Year Day and Leap Day the card's usual heading is replaced by an intercalary header (`IntercalaryHeader`): an `intercalaryContainer` surface with the `ic_intercalary` icon tinted `intercalary`, the date, and the weekday block's "no IFC weekday" explanation nested inside it. Holiday rows carry a leading diamond mark (`HolidayDiamondMark`), matching Today. An agenda row's long-press (also a TalkBack custom action) opens a delete confirmation owned by `MonthViewModel` (`requestDelete`/`confirmDelete`/`cancelDelete`): "delete this occurrence" for a recurring event — an `EventRepository.addExdate` on the occurrence's own start date, never the day the card is showing (`docs/contracts/Events.md` §4) — with an undo snackbar (`MonthEvent.OccurrenceDeleted`, hosted by `MonthScreen`'s scaffold or the expanded detail pane), or a plain, permanent delete for a one-off event (M4 T8). A pending confirmation is dropped when the card's day changes. "Add event" is a `FilledTonalButton`, "Open in converter" an `OutlinedButton`.
  - **Events** (done, `:feature:events`; visual design pass, `docs/design-plan.md` §4.5, §5.4). The list
    (behind `EventListKey`) groups its rows under **IFC month headers** (`docs/design-plan.md` §8
    decision 5 — the app's own calendar, not Gregorian: an IFC month can span two Gregorian ones, and
    Year Day and Leap Day each get their own header rather than joining the month they follow), built
    as plain `LazyColumn` header items rather than `stickyHeader` (which needs
    `ExperimentalFoundationApi` in this Compose Foundation version). Each row sits on `cardContainer`
    with a 4dp leading bar in the event's resolved colour, one combined date line ("Sol 12 · Tue 23
    Jun"; the numeric `IFC` form moves into the row's accessibility description, CLAUDE.md rule 5), the
    time or "All day", and up to two `AssistChip`s — the recurrence summary, and the category
    (`EventCategory.OBSERVANCE`/`BIRTHDAY`; `EVENT` gets none). The editor adds a **colour row** (eight
    40dp swatches with 48dp touch targets — seven fixed absolute hues plus "Calendar colour", which
    resets `Event.colorArgb` to `null`) and a three-way category `SingleChoiceSegmentedButtonRow`, both
    threaded through `EventDraft`/`EventEditorUiState`/`EventEditorViewModel` exactly like every other
    field (process-death survival included); neither needed a contract change since `Event.colorArgb`
    and `Event.category` already existed. The selected recurrence/policy/end row fills with
    `secondaryContainer`; the two advisory notices (the R3 recurrence-reset notice and the
    `POST_NOTIFICATIONS`-denied notice) sit on `tertiaryContainer`; a validation error is an
    `errorContainer` banner with a warning icon, and the offending field or button also gets an `error`
    outline — colour is never the only signal (design-plan §2). Save is a filled `Button` with the text
    "Save" in the app bar actions, not a bare check icon, disabled and showing progress while a save or
    delete is in flight (the R2 double-tap guard, unchanged).
  - **Holidays** (done, M6 T2, `:feature:holidays`): two sections behind `HolidaysKey`. "Holiday sets" lists every bundled pack (`BundledHolidayPacks.all` via `HolidayPackLoader`) with its name, region, how many holidays it defines, its sources when the pack carries them, and a switch that writes through `SettingsRepository.enabledHolidaySets` — the only place that toggle lives now; Settings' own screen links here instead of duplicating it (`docs/FEATURES.md` H5). "Holidays this year" lists every occurrence of the enabled sets for a chosen IFC year (`HolidayEngine.occurrences`), grouped by IFC month — an intercalary day groups under the month it follows, so Leap Day needs no special case — each row showing the holiday's name, its IFC date in long and numeric (`IFC`-prefixed) form, and its Gregorian date with its real weekday; tapping a row pushes that day's `MonthKey` with the day selected. The year defaults to and follows `DateTicker` (including across a December 31 → January 1 rollover) until the user pages away with the previous/next actions, which clamp to `DatePickerRange` (1583–9999). Every set disabled shows an explanatory empty state rather than nothing, now with a muted `Icons.Filled.DateRange` glyph above the message. **The design pass (done, docs/design-plan.md section 4.7, ROADMAP F2):** each pack row gets a leading 12dp colour dot from a static map keyed by `HolidaySet.id` in this module (the IFC-native pack → `YearalTheme.colors.intercalary`, `us` → `primary`, `religious-christian` → `secondary`, an unknown pack → `outline`, until packs carry their own colour, design-plan section 5.5); an enabled row sits on `YearalTheme.colors.cardContainer`, a disabled one on the plain page surface — colour plus the switch's own on/off state, never colour alone. Each occurrence row gets a leading 8dp rotated-square `holidayMark` diamond; a Year Day or Leap Day row (`HolidayOccurrenceRow.isIntercalary`, from `IfcDate.isIntercalary`) gets the intercalary icon instead and sits in an `intercalaryContainer` chip. The per-month sub-headings in the year list are a `labelSmall` eyebrow in `primary` (`MonthEyebrow`, distinct from the page's own `titleSmall` `SectionHeading`); the year navigator's year number is `titleLarge` in `primary`. The app bar uses the same `surfaceContainerLow` colours as the converter's. `HolidaysTestTags` (this module, mirroring `:core:designsystem`'s `MonthGridTestTags`) tags the diamond and the intercalary mark for tests, since a screen reader gets the row's own merged content description instead.
  - **Settings** (`:feature:settings`; FEATURES W1, W2, W6): the weekday-header radio group, then an
    **Appearance** section (design-plan §5, ROADMAP wave 2 H2) — the `ThemeMode` radio group with a
    detail line per option; a colour-source `SingleChoiceSegmentedButtonRow` ("Yearal palette" /
    "Material You", the latter disabled below API 31); six `ColorPalette` swatch chips in a `FlowRow`,
    each a 48dp circle of that palette's light `primary` with a smaller `tertiary` dot and a check mark
    when selected, the whole row disabled while the colour source is `DYNAMIC`; a pure-black switch; and
    two more three-way segmented rows (Today widget, Month widget — `WidgetTheme.FOLLOW_APP`/`LIGHT`/
    `DARK`) plus a 0–100% `Slider` (5% steps) for the widgets' background opacity — then the "Holiday
    sets" link and "Delete all data". Every control is a read-modify-write through
    `SettingsRepository.update` via a `SettingsViewModel` setter (`setPalette`, `setPureBlack`,
    `setTodayWidgetTheme`, `setMonthWidgetTheme`, `setWidgetBackgroundOpacity`, alongside the existing
    `setThemeMode`/`setColorSource`); nothing is held only in Compose state. **`PalettePreviewStrip`**
    (done, ROADMAP wave 3 J3, design-plan §4.8) replaces the marked `Spacer` under the palette row: a
    miniature 7-day row of 28dp `gridCell` squares (day 4 ringed `todayRing` and bold, day 6 a
    `holidayMark` diamond, day 2 an `eventMark` dot) plus an `intercalaryContainer` pill, wrapped in its
    own nested `IfcTheme` resolved from the *current* `UserSettings` (`darkTheme` from `themeMode`,
    falling back to `isSystemInDarkTheme()` for `SYSTEM`; `dynamicColor` from `colorSource == DYNAMIC`;
    `palette` and `pureBlack` passed straight through) — so it re-themes live as a user taps a swatch or
    a mode radio, before leaving the screen, independent of whatever theme the host `Activity` actually
    resolved. Its content description names the selected palette (`settings_palette_preview_description`,
    "Preview of the %s palette"). The More hub's four navigation rows (Holidays, Settings, Learn,
    Privacy) each get a 40dp `secondaryContainer` circle around their leading icon, tinted
    `onSecondaryContainer`; the non-interactive About row is unchanged. **"Send feedback"** (done,
    ROADMAP M8 T6): a fifth, identically styled row between Privacy and About opens an `ACTION_SENDTO`
    `mailto:` chooser (`docs/security-and-privacy.md` §6.3) prefilled with a version-stamped subject and
    a body assembled by the pure `buildFeedbackBody` from device, app and settings diagnostics only —
    never event or holiday-pack content (CLAUDE.md rule 8) — falling back to plain, selectable address
    text when no email app resolves.
  - **Learn** (done, M3 T3, `:feature:settings`): static sections (what the IFC is, the floating days, nominal-vs-actual weekdays, how dates are calculated, a brief history, an FAQ) plus an expandable-FAQ list. Every worked-example date is computed through `:core:calendar` (`LearnFacts`) and rendered with `IfcDateFormatter`, never typed as a literal. Its first row, "Watch the intro again," pushes `IntroKey` — the re-open path FEATURES L1 requires for a user who skipped it. **Grid illustrations** (done, ROADMAP wave 3 J3, design-plan §4.8): the "What is the IFC", "Why the weekdays differ" and "The two days outside the week" sections each open with a `GridIllustration` (package `art`) — the same three the first-run intro shows, reused as section headers so the two never disagree — built purely from `Canvas` and the design tokens (no bitmap assets), each with a caller-supplied `contentDescription` and its own visible text hidden from TalkBack behind it (`Modifier.clearAndSetSemantics`).
  - **Privacy** (done, the in-app half of M2 T12, `:feature:settings`): a static, truthful statement of what the app stores and what its two declared permissions (`RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`) are for, sourced from `docs/security-and-privacy.md`'s allow-list; the hosted-policy URL is left blank until one exists.
  - **First-run intro** (done, `:feature:settings`, package `intro`; FEATURES L1): three screens behind `IntroKey` — what the IFC is (calendar-spec §2.2 R4: 13 months of 28 days, Sol between June and July); the nominal-vs-actual weekday distinction, with the spec's own worked example (§4.1); Year Day and Leap Day (§2.4 R8, R9), ending with a "find my IFC birthday" button. Every worked example comes from `IntroFacts`, which reuses `LearnFacts`'s values directly (both objects are `internal`, so same-module visibility is enough) rather than recomputing them, so the two screens can never disagree. Plain Back/Next buttons, not a swipeable pager — a new visual language is exactly what M2 T13 owns, not this task. **Each page opens with a `GridIllustration`** (done, ROADMAP wave 3 J3, design-plan §4.8, package `art`): screen 1 draws a 13-block month strip with Sol picked out in `intercalaryContainer`; screen 2 draws the 4×7 dot grid with one weekday column ringed `todayRing`, plus two caption rows naming that column's IFC and actual weekday from the same `IntroFacts` worked example the body text already uses; screen 3 draws the dot grid with a Year Day pill (the `ic_intercalary` icon) shown outside it. Colour is never the only signal: every highlight also differs in shape from the rest (a ring, a distinct block, a pill outside the grid). The birthday hook calls `navigator.navigate(ConverterKey())`, the same "push onto whatever tab is current" pattern day card's "Open in converter" action already uses; `ConverterKey`'s existing `prefillEpochDay = null` default ("follow today, but the user can pick any date") already is the "let me pick a date" behavior FEATURES D3 wants, so no new key shape was needed. `IfcApp` decides whether to show it: `IntroGateViewModel` (`:app`) maps `SettingsRepository.settings` to a `StateFlow<Boolean?>` of `hasSeenIntro` that starts `null` (deliberately distinct from the persisted `false` a fresh install also has) until the store has actually been read once, so a returning user is never shown a flash of the intro while `MainViewModel.settings` is still sitting at `UserSettings.DEFAULT`. The first time that flow resolves non-null and `false`, a `LaunchedEffect` in `IfcApp` pushes `IntroKey` onto the Today tab's stack — literally "over" `TodayKey`, the tab's static root — guarded by a `rememberSaveable` flag so it fires at most once per process. Skipping or finishing marks `UserSettings.hasSeenIntro = true` through `IntroViewModel.markSeen()` before popping back off; "Learn more" pushes `LearnKey` without marking it seen, so leaving the intro unfinished still shows it again on the next cold start. **Known edge case, not handled:** if the very first launch is also routed by a widget or notification intent (see "Intent routing" above), the intro's `LaunchedEffect` and the intent-routing `LaunchedEffect` both act on the tab back stacks independently; which one "wins" the Today tab's top slot is not deterministic. In practice a first launch cannot yet have an existing widget tap or event reminder (both require the app to have run once already), so this has not been given a specific ordering rule.
  - **Contextual explainers** (FEATURES L3): `ExplainerInfoButton` (`:core:designsystem`, package `explainer`) is the reusable "info button + short `AlertDialog`" component — a 48dp `IconButton` with a hand-drawn `ic_info` glyph (material-icons-core is not a dependency of this module, the same reasoning as `ic_convert`/`ic_intercalary`) whose title and explanation are caller-supplied strings, so the component itself carries no calendar copy. It belongs in `:core:designsystem` rather than `:feature:settings` because its two call sites — the month grid's weekday header rows and its intercalary band — are both rendered from `:core:designsystem`'s own `MonthGrid`/`calendar` package already (CLAUDE.md rule 10 forbids a `:feature:settings` → `:feature:calendar` dependency, and `:core:designsystem` is the module both would otherwise need).
    - **Wired into `MonthGrid`** (done, ROADMAP R8). Because both call sites are inside `MonthGrid`, the copy lives in `:core:designsystem`'s own `strings.xml` (`weekday_explainer_*`, `intercalary_explainer_*`) next to the grid's other user-visible strings, not in a feature module — an earlier note here said "feature-owned string resources", which was wrong: `:feature:calendar` cannot reach into this module's resources, and this module already owns the grid's whole calendar vocabulary (`weekday_header_nominal`, `intercalary_band_subtitle`, `IfcDateFormatter`'s patterns). `MonthGrid` takes no new parameter and no call site changed.
    - **Placement.** Neither button sits *inside* the row it explains. The seven weekday headers and the 28 day cells share one set of column widths, so a 48dp button among them would pull the headers out of alignment with the days beneath. The weekday explainer therefore goes at the end of the month-title row, immediately above the headers it describes, where the heading leaves the space free; the intercalary explainer goes beside the band, which spans all seven columns and so aligns to nothing. The weekday copy is worded to hold for all three `WeekdayDisplay` settings, since a user may have hidden either row.
    - **The band slot.** The band and its `IntercalaryPlaceholder` are now wrapped in one full-width row, `MonthGridTestTags.INTERCALARY_SLOT`. *That slot*, not the band, is what must be identical in every month for a pager of months never to jump; the band itself is narrower than the placeholder by the button's 48dp touch target. `MonthGridTest` asserts the slot equality (at font scale 1 and 2), that the placeholder fills its slot, and that the band gives up exactly that 48dp.

### Theme and design tokens

The visual design pass ([`docs/design-plan.md`](design-plan.md), phase 1 "Foundation") layers a token
system on top of Material 3 rather than styling screens off raw `ColorScheme`/`Typography` values, and
flips the app's default look from the wallpaper to the brand.

- **`IfcTheme`** (`:core:designsystem`, `theme` package) resolves a `ColorScheme` from, in order:
  dynamic (wallpaper) colour when `dynamicColor` is on and the device is API 31+; otherwise the active
  `ColorPalette`'s scheme (`ColorPalette.colorSchemes()`, design-plan §5.2) for the requested light or
  dark mode; then, if dark mode and `pureBlack` are both set, `ColorScheme.pureBlack()` overrides the
  surfaces with AMOLED black (design-plan §5.3) on top of *whichever* scheme was chosen — brand or
  dynamic. **`dynamicColor` now defaults to `false`** (design-plan §5.1/§8.1 decision 1): the curated
  `ColorPalette.TEAL` brand scheme is what a fresh install shows without the user touching Settings,
  reversing the previous default that made the wallpaper the accidental default look. **`MainActivity`
  wires all four theme fields through** (ROADMAP wave 2 H2): `themeMode`, `colorSource`, `palette` and
  `pureBlack` all come from `SettingsRepository.settings` and pass straight to `IfcTheme`; Settings'
  Appearance section (above) is where a user changes any of them. The same task fixed design-plan §3.2's
  D6: `enableEdgeToEdge`'s status/navigation bar icon style used to follow the *system* dark setting
  regardless of a forced `ThemeMode`. A `DisposableEffect(darkTheme)` inside `setContent` now calls
  `enableEdgeToEdge` again with an explicit `SystemBarStyle.light`/`.dark` chosen from the same resolved
  `darkTheme` flag the theme itself uses (`MainActivity.resolveDarkTheme`, a plain `ThemeMode` + system
  dark-mode mapping pulled out for a unit test) — never `SystemBarStyle.auto`, which would silently
  re-derive the icon colour from the system again.
- **Six curated palettes** (`ColorPalette` in `:core:domain`, the schemes in `:core:designsystem`'s
  `theme/Palette*.kt`): `TEAL` (default, the launcher-icon brand scheme, `Color.kt`), `SOL`, `NIGHT`,
  `MOSS`, `ROSE`, `INK` (design-plan §5.2). Each is a hand-tuned light/dark `ColorScheme` pair, not a
  generated seed, so every role pairing the app draws — every `on*` colour on its own colour or
  container, `onSurface` on every `surfaceContainer` tier, `primary`/`secondary`/`tertiary`/`error`/
  `onSurfaceVariant` on `surface`, `outline` on `surface` — is guaranteed rather than merely likely to
  meet WCAG AA (4.5:1 text, 3:1 non-text). `ColorSchemeContrastTest` (a separate task, written from
  `docs/design-plan.md` rather than from this code) turns that guarantee into a gate assertion.
- **`YearalColors`**, provided through `LocalYearalColors` and read via `YearalTheme.colors` (mirroring
  `MaterialTheme.colorScheme`), is a small set of semantic tokens — `todayRing`, `heroContainer`,
  `intercalaryContainer`, `gridCell`, `gridCellWeekend`, `gridCellMarked`, and the rest — each
  *derived* from a Material role (`yearalColorsFrom`, design-plan §3.1's token table) rather than a
  brand constant, so the same token names produce a coherent look under any palette or under dynamic
  colour. `YearalShapes` (`extraSmall`…`extraLarge`, 4–28dp) and `PillShape` (the intercalary band's
  fully rounded corner) replace the ad hoc `RoundedCornerShape` literals components used to declare for
  themselves; `YearalTypography` adds tabular figures to the numeral-bearing styles and a heavier
  `displayMedium` for the Today hero date, built on Roboto (the owner's choice, design-plan §8 decision
  3 — no bundled font). `Dimens` centralises the spacing and sizing scale, including the grid's own
  `CellGap`/`CellPadding`/`TodayRingWidth`/mark sizes, which used to be private constants duplicated
  per component. `Dimens.MarkSize` is 8dp (design-plan §4.2 "Marks grow"), up from the incumbent's 6dp.
  Every day cell now carries a fill — `.gridCell` plain, `.gridCellMarked` for a day with an event or
  holiday, `primaryContainer` when selected — but `MonthGrid`/`DayCell` deliberately never draws
  `.gridCellWeekend`: tinting the IFC week's nominal Saturday/Sunday columns would visually assert a
  "weekend" that is not the real one (CLAUDE.md rule 3). The token stays declared for a future caller
  that wants it under a real-weekday key instead.
- **`yearalTopAppBarColors()`** (`theme/AppBars.kt`) is the `TopAppBarColors` every feature screen's
  `TopAppBar` passes (design-plan §3.2 "Top app bars"): `surfaceContainerLow` container,
  `surfaceContainer` once scrolled. Not folded into `IfcTheme` itself because `TopAppBar` lives in each
  feature module, not `:core:designsystem` (CLAUDE.md rule 10) — every screen opts in explicitly instead
  of accepting Material's plain-`surface` default, so every app bar in the app reads as one system.
- **`MonthGrid`'s inner heading is optional** (`showTitle: Boolean = true`): the Month screen (phase 2)
  passes `false` once its own app bar shows the month and year, so the grid and the app bar stop
  showing the same title twice; every other caller is unaffected by the default.
- **Pure black** (`ColorScheme.pureBlack()`, design-plan §5.3) is a `ColorScheme` transform, not a
  seventh palette, because it applies on top of whichever dark scheme is active. It replaces
  `background`/`surface`/`surfaceDim`/`surfaceContainerLowest` with true black and steps the remaining
  container tiers up from it, keeping every `on*` colour — a darker background can only improve an
  already-passing contrast ratio.
- **Dark scheme rework** (design-plan §3.1, finding D3): the brand dark scheme's surfaces moved from a
  near-black that read as plain black (`#0E1615` down to `#090F0E`) to a visibly teal-tinted set
  (`#121C1B` down to `#0D1514`, `surfaceBright` `#334542`) with a real step between tiers;
  `DarkPrimaryContainer` stays the brand teal unchanged so the hero card is unmistakably teal at night.

### State management

Use plain unidirectional data flow with no MVI framework.

- An `@HiltViewModel` exposes one `StateFlow<XUiState>` built with `stateIn(WhileSubscribed(5_000))`, plus public intent functions.
- `XRoute(viewModel)` collects with `collectAsStateWithLifecycle`. It delegates to a stateless `XScreen(state, callbacks)`, which is the unit for previews, Roborazzi and Compose tests.
- "Today" comes from `DateTicker: Flow<LocalDate>` in `:core:domain`:
  - It emits the current date, then delays to the next midnight.
  - **As built (ROADMAP R1).** `RealDateTicker` takes an optional `TimeChangeSignal` (`:core:domain`;
    default `NoTimeChangeSignal`, a source that never fires, so every existing caller is unchanged): a
    `Flow<Unit>` of invalidation hints with no payload. On each cycle it races the midnight delay against
    the signal — whichever comes first — then always re-reads `Clock` and `ZoneProvider`, emits only if
    the recomputed date differs from the last one emitted, and arms a fresh delay to the new next
    midnight. A firing that changes nothing produces no duplicate emission; a clock moved backwards
    across a boundary emits the earlier date. `AndroidTimeChangeSignal` (`:app`,
    `io.github.chrisjmendoza.yearal.time`) is the Android implementation: it fires on a context-registered
    `RECEIVER_NOT_EXPORTED` receiver for `TIME_SET`, `TIMEZONE_CHANGED` and `DATE_CHANGED`
    (`docs/security-and-privacy.md` §6.3), and on every activity resume through
    `Application.ActivityLifecycleCallbacks` (not `ProcessLifecycleOwner`: `lifecycle-process` is not a
    dependency of this app, and every resume, not only the first foregrounding, is a safe moment to
    recompute). Construction does no registration; `IfcApplication.onCreate` calls its `start()` once,
    the same place it arms the day-rollover alarm, so a half-constructed instance is never handed to
    `registerReceiver`/`registerActivityLifecycleCallbacks`. `TimeModule` binds it and passes it to
    `RealDateTicker`.
  - The same `TimeChangeSignal` also reaches `DefaultObserveAgendaUseCase.invoke`/`presence`
    (`:app`'s `AgendaModule`, default parameter for source compatibility), so a zone change re-buckets
    an already-open Month/Day/Today/Year screen's agenda even though `ZoneProvider` itself is a plain
    synchronous read, not a flow, and none of the repository or holiday-set flows emit on their own
    (`docs/contracts/Events.md` §5).
- Never call `LocalDate.now()` outside the `Clock` binding.

### Intercalary days in a 7-column grid

- The grid is always exactly 4 rows by 7 columns.
- June in leap years and every December gain an **intercalary band** below row 4.
  - It is one full-width pill spanning all seven columns.
  - It shows a label ("Leap Day" or "Year Day"), the Gregorian date and the real weekday.
  - It fills with `YearalTheme.colors.intercalaryContainer` plus an icon, and it is tappable like any
    other day.
  - The slot height is measured from real text (`intercalarySlotHeight()`), because non-linear font
    scaling breaks any "line height × N" estimate; months without a band show the month's Gregorian
    span in a same-silhouette placeholder pill on `surfaceContainer` (design-plan §4.2), so all
    thirteen months share one shape.
- Spanning every column makes "belongs to no week" visible. No weekday header aligns with it.
- The Year view's Year Day tile (`YearDayTile`, the grid's 14th item) is a sibling of the mini-month
  tile rather than a reuse of the band component — the band is built to span a month grid's seven
  columns, which the year's tile grid does not have — but it shares the band's fill
  (`.intercalaryContainer`/`.onIntercalaryContainer`) and icon, so the two read as one accent
  (design-plan §8 decision 4). Leap Day, inside the Year view's compact June tile rather than its own
  grid item, uses a smaller non-interactive inline indicator instead: the same pill fill, an icon, the
  label, an event dot and a hollow today ring, never colour alone — so that tile stays one TalkBack
  node; tapping anywhere in the tile, the indicator included, opens June.
- A `WeekdayDisplay { NOMINAL, ACTUAL, BOTH }` setting drives the headers. `BOTH` is the default: nominal IFC weekdays with the actual weekdays in a second header row.

### Adaptive layouts

**As built (docs/ROADMAP.md M3 T4).**

- Navigation is a bar on compact widths and a rail on medium widths and up, via `:app`'s
  `NavigationSuiteScaffold` (unchanged by this task).
- The width decision lives in one place, `core/designsystem/adaptive/WindowWidthClass.kt`:
  `WindowWidthClass { COMPACT, MEDIUM, EXPANDED }` and the composable `currentWindowWidthClass()`, built
  on `androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2()` and the Material window size
  class breakpoints (600dp / 840dp) — the same source `NavigationSuiteScaffold` uses for its own
  bar-to-rail switch, so the nav chrome and the screen content agree. `:feature:calendar` and
  `:feature:events` both call it; neither depends on the other (CLAUDE.md rule 10).
- **No Nav3 `SceneStrategy` was written.** At the versions pinned in `gradle/libs.versions.toml`
  (`nav3 = "1.1.7"`), there is no `adaptive-navigation3` artifact in the resolved dependency graph — a
  `androidx.compose.material3.adaptive:adaptive` (`adaptive-android` 1.3.0) is present and used for
  `currentWindowAdaptiveInfoV2()`, but nothing provides a Nav3 list-detail `SceneStrategy` that would
  render two back stack entries side by side. Per this section's own fallback clause, the list-detail
  Scene is `core/designsystem/adaptive/TwoPaneLayout.kt`: a plain `Row` of two `Box`es (list, a
  `VerticalDivider`, detail), each its own semantics traversal group so TalkBack reads the whole list
  pane then the whole detail pane. It is not a `SceneStrategy` and needs none: the two-pane switch
  happens *inside* the existing single Nav3 entry (`MonthKey`, `EventListKey`), so **`:app`'s
  `IfcApp.kt` / `NavDisplay` wiring needed no change** for this task.
- **Calendar:** `MonthRoute` reads `currentWindowWidthClass()` and renders `CalendarScreen`
  (`feature/calendar/month/CalendarScreen.kt`), which is `MonthScreen` alone at compact/medium widths
  (the grid with the day card below it) or `MonthListDetailScreen` at expanded widths: `MonthScreen`
  with `showDayCard = false` as the list pane and the same `DayCard` as the detail pane. A day tap only
  calls `MonthViewModel.select` at every width, and one `MonthViewModel` feeds both panes, so there is
  no second ViewModel and nothing to keep in step. The detail pane always has content — the selected
  day, or today — so it has no empty state. `BackHandler` clears the selection
  (`MonthViewModel.clearSelection`, so the card returns to today) only at expanded widths while
  something is selected; at compact and medium widths back leaves the tab as usual. (Until 2026-09-25
  compact widths pushed a `DayKey` bottom sheet, and a widget or notification tap opened that sheet
  over the two panes at expanded widths; both went with the sheet.)
- **Events:** the same shape. `EventListRoute` reads `currentWindowWidthClass()` and renders
  `EventsScreen` (`feature/events/list/EventsScreen.kt`): `EventListScreen` alone at compact/medium
  widths (a row tap or the FAB pushes `EventEditorKey` full-screen, unchanged), or
  `EventListDetailScreen` at expanded widths: the list as the list pane (a row tap or the FAB only
  calls `EventListViewModel.selectEvent` / `selectNewEvent`, no navigation) and the selected event's
  `EventEditorScreen` — the same stateless content the full-screen editor renders — as the detail pane,
  or an empty state. `EventListViewModel.selection: StateFlow<EventListSelection>` (`None` / `New` /
  `Existing(eventId)`) is the pane's own selection, ignored at compact/medium widths. The detail pane's
  `EventEditorViewModel` is likewise created only while selected, keyed by the selection
  (`"event-list-editor-$eventId"` / `"event-list-editor-new"`); `EventEditorScreen.kt`'s
  `rememberEventEditorState` (state collection, the one-shot event wiring, and the
  `POST_NOTIFICATIONS` request) is shared by `EventEditorRoute` (its own Nav3 entry) and the pane, so
  the unsaved-changes guard and the Save/Delete in-flight guard — both live in
  `EventEditorViewModel` — work identically in both layouts. `EventEditorRoute` gained two optional
  parameters for this: `onLeave` (default `Navigator::goBack`; the pane passes
  `EventListViewModel::clearSelection` instead) and `viewModelKey` (default `null`, needed only when
  more than one `EventEditorViewModel` can exist in the same `ViewModelStore` at once — the pane's
  case). `BackHandler` at `EventListRoute` delegates to the open editor's own `requestBack` while a
  selection exists, so the discard-confirmation guard shows before the selection is cleared, exactly as
  it does today when leaving the full-screen editor.
- Rotation, a fold/unfold, and a desktop-window resize are not a reset: the selection
  (`MonthViewModel.selected`, `EventListViewModel.selection`) and the open editor's draft
  (`EventEditorViewModel`'s own `SavedStateHandle`-backed state) live in ViewModels retained across
  configuration changes, not in the two-pane composables themselves — proven in
  `WindowWidthClassTest` (a live resize recomposes into the new bucket) and `CalendarScreenTest` /
  `EventsScreenTest` (the caller-held selection survives the width class flipping back and forth).
- The app is edge-to-edge (enforced at target 36), supports predictive back, and does not lock orientation. Target 36 ignores orientation locks on sw600dp and up anyway.

**Dependency added:** `androidx.compose.material3.adaptive:adaptive` (BOM-managed, resolved to 1.3.0 /
`adaptive-android`), Apache License 2.0, ~40KB, `implementation` in `core/designsystem` only — the
catalog alias (`gradle/libs.versions.toml`) already existed, unused, in anticipation of this task;
`adaptive-layout` and `adaptive-navigation` remain unused. `androidx.window:window-core`'s
`WindowSizeClass` type comes along transitively (an `api` dependency of `adaptive-android`) and is used
directly rather than re-wrapped, since `WindowWidthClass` already hides it from callers.

### Accessibility

- Cells are at least 48dp. Seven columns at 360dp gives 51dp.
- Each cell has a merged description, for example "Sol 13, IFC Friday. Gregorian Tuesday, June 30, 2026. 2 events. Holiday: …", with "Today." appended on the current date; `mergeDescendants = true` on `DayCell`, `IntercalaryBand` and the Year overview's tiles enforces that the inner numbers, labels and marks never surface as separately focusable nodes, pinned by a zero-children assertion in `MonthGridTest`/`YearOverviewTilesTest`.
- Use `selected` and `Role.Button` semantics, a heading on the month title, and traversal groups on the grid.
- Today, holidays and intercalary days are never encoded by color alone. Each also has a shape or icon.
- Screenshot tests run at font scale 2.0. Respect the reduced-motion setting.

### Localization

- All strings are in resources from day one, including the 13 month names with "Sol".
- The IFC date pattern is a positional string resource, `%1$s %2$d, %3$d`, so locales can reorder it
  (`date_long_regular`, `date_long_intercalary`, `date_medium_*` in `:core:designsystem`; `IfcDateFormatter`
  formats them with its own `Locale` via `String.format`, not `Resources.getString(id, args)`, so the
  numerals follow the formatter's locale).
- Gregorian dates use `DateTimeFormatter.ofLocalizedDate`.
- Weekday names come from `DayOfWeek.getDisplayName`.
- Numerals are formatted with the locale.
- RTL comes for free from `Row`.
- Per-app language uses AGP `generateLocaleConfig`.
- Version 1 ships in English only.

## 5. Widget architecture (Glance, in `:widget`)

### Widget types

1. **Today:** sizes from 2x1 to 2x2. It shows the IFC date large, with an optional Gregorian line and weekdays.
2. **Month grid:** 4x3 and larger. It uses the same 4x7 grid plus band and marks today, event dots and holidays. Rows are nested containers, because Glance caps a container at 10 children. Seven columns and at most six rows fit.
3. **Agenda:** the next N occurrences. It ships in 1.x, together with the "hide event details" widget privacy mode, because it is the first widget that shows event text on the home screen.

All widgets use `SizeMode.Responsive` with three sizes and tap actions that open the app on a day (a `MonthKey` with that day selected, via `IntentRouter`).
Colour follows the app's own theme by default (palette, pure black, `ThemeMode`), with Material You as an
opt-in colour source and a per-widget-type override — see "Widgets follow the app's appearance" below.

### Data

- `provideGlance` pulls repositories through a Hilt `@EntryPoint`.
- It computes "today" from the injected `Clock` at composition time, so any update trigger self-corrects.
- It collects the agenda flow while the Glance session is alive.
- Repository writes call `WidgetUpdater.requestUpdate()`. The interface lives in domain, and the implementation is here as a debounced `updateAll`.

### Midnight rollover (layered)

1. **Primary:** one `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, nextLocalMidnight + about 1s)`. It fires `updateAll` and re-arms itself. One wakeup per day has negligible battery cost.
   - Permissions: declare `USE_EXACT_ALARM` (API 33 and later, auto-granted and not revocable) and `SCHEDULE_EXACT_ALARM` with `maxSdkVersion="32"` (granted by default on API 31 and 32). **Declared since M6 T3**; the Play Console text is [security-and-privacy.md](security-and-privacy.md) §5.4.
   - **Event reminders are what justify the exact-alarm permission** — Play explicitly allows it for calendar apps that show event notifications, and requires a Play Console declaration. The widget rollover merely reuses it. No build uploaded to Play declares `USE_EXACT_ALARM` before reminders ship (M6).
   - This route avoids the Android 14 denied-by-default `SCHEDULE_EXACT_ALARM` flow entirely.
   - Always guard with `canScheduleExactAlarms()` and fall back to `setWindow` with a 10-minute window. Pre-reminder builds, and any build where Play rejects the declaration, run on the windowed alarm plus layers 2 and 3, so the design must be correct without exact alarms.
2. **Manifest receivers**, all on the implicit-broadcast exemption list or package-targeted: `TIME_SET`, `TIMEZONE_CHANGED`, `LOCALE_CHANGED`, `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`. Each one updates the widgets and re-arms the rollover and reminder alarms.
   - `ACTION_DATE_CHANGED` is **not** exempt, so it cannot be registered in the manifest at target 26 or later. It is used only context-registered, inside `DateTicker`, while the app is alive.
3. **Self-healing backstop:** `updatePeriodMillis` of 4 hours in the provider XML. It needs no code, recovers from OEM alarm killing or force-stop, and re-arms the alarm.
   - Do not add a periodic WorkManager job. Glance already uses WorkManager internally for sessions.

**As built (M5 T2, `:core:scheduling`).** Layers 1 and 2 exist and are armed in every build; layer 3 arrives with the first widget (M5 T1).

- **The hook is `DayRolloverListener`** (`:core:domain`, package `core.domain.rollover`): `suspend fun onDayRollover(trigger: DayRolloverTrigger)`, with one trigger per source (`MIDNIGHT`, `TIME_CHANGED`, `ZONE_CHANGED`, `LOCALE_CHANGED`, `BOOT_COMPLETED`, `APP_UPDATED`). `:core:scheduling` declares the Hilt multibinding `Set<DayRolloverListener>` with `@Multibinds`, so the empty set is valid and the module is complete and wired into `:app` before any widget exists. `:widget` contributes a listener with `@Binds @IntoSet` that calls its `updateAll`; since M6 T1 `AlarmReminderScheduler` contributes another (itself) that recomputes its next alarm for every trigger. Wherever this section says a trigger "updates the widgets and re-arms the reminder alarms", it happens through these listeners; `:core:scheduling` never learns who listens. `WidgetUpdater.requestUpdate()` stays what "Data" above says it is — the repository-write path (M5 T6) — and is not the rollover hook. `RecordingDayRolloverListener` in `:core:testing` is the fake.
- **A call is a hint, not a fact.** Listeners recompute "today" from the injected `Clock` and `ZoneProvider` every time and never infer the date from the trigger or the alarm's nominal time; that is what keeps the rollover correct on the windowed alarm, after late delivery in Doze, and after the clock is set backwards.
- `DayRolloverScheduler.arm()` sets one `RTC_WAKEUP` alarm for `nextLocalMidnight + 1 s`. "Next local midnight" is `today.plusDays(1).atStartOfDay(zone)` from the `Clock` and the `ZoneProvider` (DST-safe, including zones where 00:00 does not exist); the only conversion to epoch milliseconds is the `AlarmManager` call. The `PendingIntent` is explicit, immutable, has a fixed request code and no extras, so re-arming replaces the alarm and never stacks one.
- ~~**Pre-reminder builds use the windowed alarm on every API level (26–36)**, not only where `canScheduleExactAlarms()` is false.~~ **Superseded by M6 T3**, which declared the permissions. Android Lint's `MissingPermission` rejects any `setExactAndAllowWhileIdle` call, guarded or not, while the manifest declares neither exact-alarm permission, which is why the `canScheduleExactAlarms()` branch of layer 1 had to land in the change that declares them.
- **As built (M6 T3).** Both alarms are set through one internal helper, `armWakeup` (`ExactAlarms.kt`), which is the single place the exact-versus-windowed decision is taken: `setExactAndAllowWhileIdle` when `Build.VERSION.SDK_INT < 31` (an exact alarm needs no permission there) or `canScheduleExactAlarms()` is true, and `setWindow` with the 10-minute window otherwise. The capability is read on **every** call, never cached, because an API 31–32 user can revoke it at any moment; `SystemEventReceiver` also listens for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (a sixth action, package-addressed like `MY_PACKAGE_REPLACED`) and re-arms both the rollover and the reminder alarm under the new capability, notifying no listener, since the date has not changed. The windowed branch is not an error path: nothing in the design treats it as degraded, and the scheduler tests cover both states on API 26, 30, 31, 33 and 36.
- `DayRolloverAlarmReceiver` and `reminder/ReminderAlarmReceiver` (neither with an intent filter) and `SystemEventReceiver` (the actions of layer 2, plus the exact-alarm one) are declared in the library manifest with `android:exported="false"`; all check `intent.action` and read nothing else from the intent. A rollover receiver re-arms **first**, synchronously, then notifies the listeners concurrently on a background scope under `goAsync()` with an 8-second budget. A listener that throws or hangs does not stop the others; the failure is rethrown after the broadcast is released, not swallowed. The reminder receiver has the same shape through its own `ReminderBroadcastHandler` and the same 8-second budget, but no listeners: it only asks `AlarmReminderScheduler` to recompute.
- `IfcApplication.onCreate` arms the alarm on every process start (one `AlarmManager` call, no listener is notified), which is how an alarm lost to force-stop or an OEM task killer comes back.

Never update a widget per minute. The widgets show dates, not clocks.

**As built (M5 T1, `:widget`).** The Today widget (`TodayGlanceWidget` + `TodayWidgetReceiver`) is the
first thing in `:widget`, on Glance 1.2.0.

- `TodayGlanceWidget.provideGlance` resolves `Clock` and `ZoneProvider` through the Hilt `WidgetEntryPoint`
  (`@EntryPoint`, resolved with `EntryPointAccessors.fromApplication`, exactly like `:core:scheduling`'s
  `SchedulingEntryPoint` — Glance instantiates the widget itself, not Hilt). The composable content calls
  `todayDate(clock, zoneProvider)` directly, every composition, with no `remember`: this is what "at
  composition time" above means in code. `IfcDateFormatter` and the tap-hint string are read once per
  `provideGlance` call instead, since they do not depend on the date and reading `Locale.getDefault()`
  inside the composable itself trips Compose lint's `NonObservableLocale` check for no benefit (Glance
  content is not recomposed by a locale change the way an Activity's is; the `LOCALE_CHANGED` trigger
  already forces a fresh `provideGlance` through `WidgetRolloverListener`).
- `WidgetRolloverListener` (`@Binds @IntoSet` into `:core:scheduling`'s `Set<DayRolloverListener>`; renamed
  from `TodayWidgetRolloverListener` in M5 T3, once it covered more than one widget) calls a small
  `WidgetRefresher` seam (`GlanceWidgetRefresher.refreshAll` updates every widget the module owns, one
  `updateAll` call each) so the listener is unit-testable without a real `AppWidgetManager`.
- Three `SizeMode.Responsive` breakpoints, `SMALL` (110x48dp, 2x1: date only), `MEDIUM` (180x48dp, adds
  the Gregorian line) and `LARGE` (180x110dp, adds the labelled actual weekday); `SMALL`/`MEDIUM`'s
  height moved from 40dp to 48dp in ROADMAP M8 T1 (accessibility audit finding #15), matching
  `today_widget_info.xml`'s `minHeight`, raised to the 48dp touch-target floor for the widget's single
  tap region. **As built (Wave 3 I3):**
  `LARGE` also adds the year-progress line (`Day 260 of 365 · 71%`) and the next-intercalary countdown
  (`Year Day in 105 days`), the review's "large widgets should show more" (design-plan §4.9). Both are
  computed from `IfcDate` alone (`widget/today/TodayLargeContent.kt`): `dayOfYear`/`toLocalDate().
  lengthOfYear()` for the progress line, and a from-scratch port of `TodayUiState`'s own
  `nextIntercalaryDay`/`IfcDate.daysUntil` for the countdown, since `:widget` may not depend on
  `:feature:calendar` (CLAUDE.md rule 10) to reuse that module's copy. Every word is a `:widget` string
  resource (CLAUDE.md rule 9), not `:core:designsystem`'s -- the app's own `%N%% of the year`/`N days
  until X` phrasing is deliberately not reused, matching this section's already-established stance that
  `:widget`'s copy is its own. Matching
  `res/xml/today_widget_info.xml`'s `minWidth`/`minHeight`/`minResizeWidth`/`minResizeHeight` and
  `res/xml-v31/today_widget_info.xml`'s additional `maxResizeWidth`/`maxResizeHeight`/`targetCellWidth`/
  `targetCellHeight` (introduced in API 31; Android Lint's `UnusedAttribute` rejects them below minSdk 26
  in a single file, hence the two files — a `-v31` resource replaces the base file wholesale on API 31+,
  it does not merge attribute-by-attribute). `widgetCategory="home_screen"` only, not `keyguard`
  (docs/security-and-privacy.md §3.2: a date-only widget stays lock-screen eligible on Android 16 QPR2+
  by default, so nothing extra needs declaring).
- The tap action opens the app through `launchAppIntent`, which resolves the launcher via
  `PackageManager.getLaunchIntentForPackage` rather than naming `MainActivity` — `:widget` cannot depend
  on `:app` (§2) — then `androidx.glance.appwidget.action.actionStartActivity(intent)`. **As built
  (ROADMAP M3 T5):** `todayLaunchIntent` adds `WidgetIntents.ACTION_OPEN_TODAY`, so `IntentRouter`
  selects the Today tab explicitly (see "Intent routing" above).
- `GlanceTheme` uses Material You dynamic colour on API 31+ (`GlanceTheme.colors`) and the brand palette
  (`BrandLightColorScheme`/`BrandDarkColorScheme` from `:core:designsystem`, wrapped by
  `androidx.glance.material3.ColorProviders`) below it. **As built (Wave 3 I3): both widgets follow the
  app's appearance** instead of always the brand palette -- see "Widgets follow the app's appearance"
  below, right after this list.
- Glance depends on WorkManager, which unconditionally declares `WAKE_LOCK`, `ACCESS_NETWORK_STATE` and
  `FOREGROUND_SERVICE` alongside `RECEIVE_BOOT_COMPLETED` (already declared by `:core:scheduling`).
  `:widget`'s manifest keeps `WAKE_LOCK` (WorkManager's own reliability mechanism for the widget's
  background render) and strips the other two with `tools:node="remove"`, verified safe from the actual
  work-runtime and glance-appwidget sources rather than assumed — see
  docs/security-and-privacy.md §5.1 for the detail and the allow-list entries.

**As built (M5 T3, `:widget`).** The Month-grid widget (`MonthGlanceWidget` + `MonthWidgetReceiver`,
package `widget.month`) is the second widget in `:widget`, built the same way as Today.

- `MonthGlanceWidget.provideGlance` reaches `Clock`/`ZoneProvider` through the same `WidgetEntryPoint` as
  Today. The composable calls `todayDate(clock, zoneProvider)` every composition (no `remember`) and
  derives the month to show from it with `IfcYearMonth.from(today.ifcDate)` — an intercalary today
  (Leap Day or Year Day) resolves to the month it follows, so the widget always shows a real month, never
  a page for a single floating day. `buildMonthWidgetState` (`widget/month/MonthWidgetState.kt`) is the
  pure function that shapes this into what the content renders: the month title, both weekday header rows
  (nominal and actual, from `IfcYearMonth.actualDayOfWeek`, never derived from each other per calendar-spec
  §4.1), the 28 day cells with a Gregorian-date-matched `isToday` flag, the trailing Leap Day / Year Day
  band from `IfcYearMonth.trailingIntercalary`, and the Gregorian span.
- Two `SizeMode.Responsive` breakpoints (the task's minimum): `COMPACT` (250x180dp, about 4x3 home-screen
  cells — `docs/ARCHITECTURE.md` §5's "Month grid: 4x3 and larger") shows the grid with one actual-weekday
  header row, one number per cell and no Gregorian span line; `FULL` (320x320dp) adds the nominal weekday
  header row too — the app's `BOTH` default (Reconciled decisions #7) — the Gregorian span line, and a
  Gregorian day number under the IFC one in every cell. That last pairing is the same "IFC day large,
  Gregorian day small" the app's own `MonthGrid` cell uses (FEATURES C1), and it exists because the two
  header rows name an IFC weekday *and* a real one: cells carrying a single number promise a second date
  the grid never delivers (owner device feedback, 2026-09-19).
  `COMPACT`/`FULL` are `SizeMode.Responsive` content breakpoints, not touch-target sizes. ROADMAP M8 T1
  (accessibility audit finding #14) raised `month_widget_info.xml`'s `minWidth`/`minResizeWidth`
  separately — but not all the way to the 48dp touch-target floor. The seven grid columns split what is
  left of the width after the widget's own 8dp padding on each side, so a column is
  `(minWidth - 16dp) / 7`; clearing 48dp needs `minWidth >= 352dp`. A 352dp+ minimum, though, does not
  fit a 360dp-wide phone's launcher grid (a 4-column grid there gives about `4 * 90 - 30 = 330dp`; a
  5-column grid about `5 * 72 - 30 = 330dp`; both under 352dp) — and a launcher that cannot fit a
  widget's declared minimum simply refuses to place it, which is worse than a too-small cell. `minWidth`
  is 320dp instead (five nominal 70dp home-screen cells, `70 * 5 - 30 = 320`, fitting that 330dp phone
  grid): a column there is `(320 - 16) / 7 ~= 43.4dp`, about 5dp under the 48dp floor. The floor itself
  is met once a placement reaches six or more of the launcher's nominal cells (about 390dp and up, the
  same `70dp * cells - 30dp` formula) — so the real guarantee this file gives is "48dp met at any
  six-cell-or-larger placement, within 5dp of it at the five-cell minimum," not "48dp everywhere."
  `res/xml/month_widget_info.xml` and the `res/xml-v31` split mirror the Today widget's pattern, with the
  same 4-hour `updatePeriodMillis` backstop; `targetCellWidth`/`Height` was 4x3 until this task moved
  `targetCellWidth` to 5, matching the new `minWidth`. `maxResizeWidth` moved to 460dp (seven cells,
  `70 * 7 - 30 = 460`) only to stay a valid, larger bound above the new minimum.
- **A widget larger than `FULL` is stretched, not re-laid-out.** `SizeMode.Responsive` picks the largest
  breakpoint that fits and the launcher stretches that RemoteViews, so on a tall widget every
  `wrap_content` child pins to the top and the rest is dead space. The four grid rows therefore carry a
  vertical `defaultWeight()` and each day cell a `fillMaxHeight()`: the grid grows into the available
  height at any size, and the day tap targets grow with it. Adding a third breakpoint would not fix this —
  there is always a size above the largest one.
- The Gregorian span sits directly under the month title rather than below the grid: it names the whole
  month, so it belongs with the month's heading, and at the bottom it competed with the grid's last row.
- **Rule lines without extra views.** The grid container is filled with the outline colour; each cell
  insets itself by 1dp and paints the widget background over the rest, so what shows through the inset
  is a hairline between and around the cells. Glance 1.2.0 has no border modifier (the same gap that
  makes the today mark a filled pill rather than the app's ring), and interleaving divider views is not
  an option either — Glance's generated layouts cap a `Row`'s children, and seven cells plus six
  dividers would exceed it.
- **Marks: holiday first, then event**, the same order and the same shape-not-colour distinction the
  app's `DayMarks` uses (FEATURES C4; CLAUDE.md rule 3) — a diamond for a holiday, a round dot for an
  event. Every cell renders the marks line even when empty, so a mark appearing never changes a row's
  height. The widget shows at most one event dot where the app's cell shows up to three: its snapshot is
  `ObserveAgendaUseCase.presence`, a boolean per day, because a count is closer to event content than a
  home screen should carry (CLAUDE.md rule 8). Holidays are likewise presence-only and never named.
- **Where the holiday marks come from.** `fetchMonthHolidays` takes one bounded snapshot per render,
  the same shape and the same never-hang/never-crash guarantee as `fetchMonthEventPresence` beside it:
  `HolidaySetProvider.enabledSets()` for which packs are on, then `HolidayEngine.occurrences` for the
  month's dates. Both are `:core:domain` types, so `:widget` takes **no new module dependency** and
  never touches `:core:holidays`, settings or resources. Only the Hilt bindings had to move: they were
  in `:feature:calendar`, whose own KDoc said to relocate them to `:app` the moment a second module
  needed them, so `HolidayModule`/`HolidaySetProviderModule` now live in `:app`'s `di` package — the
  same move `FormatterModule` records for `IfcDateFormatter`, and the one CLAUDE.md rule 10 requires
  since no module may depend on a feature. `PackHolidaySetProvider` stays in `:feature:calendar` and is
  injected across that boundary, exactly as `HolidayCatalog` already is.
- Today is marked by shape and weight, never colour alone (CLAUDE.md rule 3; FEATURES Q4): a rounded,
  filled pill behind a bold day number, or — when today is the intercalary day — the band itself switches
  from the tertiary container to the primary container plus bold text. Glance 1.2.0 has no border/outline
  modifier (unlike the app's own `MonthGrid`/`IntercalaryBand`, which use a border ring), so the widget
  uses a filled shape instead; both satisfy "shape, not colour alone".
- One merged content description (month, today's IFC date with both labelled weekdays, and the Gregorian
  equivalent, built from `IfcDateFormatter.dayDescription`) sits on the whole tappable widget outside the
  grid (the title, header rows and Gregorian span), the same place Today puts its own description.
- **As built (ROADMAP M8 T1, accessibility audit finding #1) — supersedes the single-description-only
  ruling this section used to record.** Each of the 28 day cells also carries its own short
  `GlanceModifier.semantics { contentDescription = … }`: the cell's day name alone (`Sol 13`,
  `IfcDateFormatter.formatDay` — no weekday, since a cell that names none cannot mislabel one under
  calendar-spec §4.1 item 7) with `today`/`holiday`/`has events` appended as short, comma-joined,
  presence-only qualifiers (never a holiday's name or an event's count or title, CLAUDE.md rule 8 —
  `docs/security-and-privacy.md` §3.2). The old reasoning was that 28 extra TalkBack nodes were the
  greater harm; the audit's finding is that a clickable cell with **no** description is announced as a
  bare, context-free digit, which is worse for a screen-reader user who can already tap that cell
  individually (ROADMAP M3 T5's per-day routing): they could act on the cell but not hear what it was.
  Glance/RemoteViews 1.2.0 still has no modifier to mark a *child* unimportant for accessibility, but
  that does not block this fix — the day number, the Gregorian day and the mark glyphs inside a cell are
  plain, non-clickable `Text`s with no semantics of their own, so this description on the cell's own
  clickable node is what TalkBack reports, exactly as the whole-widget description above already works
  for the title/header block without hiding any of its own children. The two weekday header rows still
  carry no semantics of their own: they are static labels read once for the whole grid, not per-cell
  state, and are not themselves clickable, so the "clickable cell, no description" harm this finding
  addresses does not apply to them.
- The tap action builds on the same `launchAppIntent` helper as Today (`docs/security-and-privacy.md`
  §6.4). **As built (ROADMAP M3 T5):** the whole-widget area (title, header rows, Gregorian-span line)
  uses `monthLaunchIntent` (`WidgetIntents.ACTION_OPEN_MONTH`, opens the current month); each of the 28
  day cells and the trailing intercalary band is individually clickable with `dayLaunchIntent`
  (`WidgetIntents.ACTION_OPEN_DAY` plus that cell's epoch day), overriding the whole-widget target
  inside its own bounds (see "Intent routing" above).
- `WidgetRefresher` (see above) and the renamed `WidgetRolloverListener` cover both widgets with the same
  multibinding entry: `GlanceWidgetRefresher.refreshAll` calls `updateAll` on `TodayGlanceWidget` and
  `MonthGlanceWidget`, one call each, and its target list is `internal` so a test can verify that without a
  real `AppWidgetManager`.
- Holiday markers are H-series work and are explicitly out of scope: this widget never reads the holiday
  repository. Event dots are M5 T6, below.

**As built (M5 T6, `:widget` + `:core:domain` + `:core:data`).** Event dots on the Month widget, and the
update-on-write path this section's "Data" bullet describes, per the two designs offered there.

- **Update path -- design (a) chosen.** `WidgetUpdater` (`:core:domain`, package `core.domain.widget`,
  `fun interface { fun requestUpdate() }`, deliberately not `suspend` -- it enqueues and returns) is
  called by `RoomEventRepository` (`:core:data`) after every successful write, at the same call sites as
  `ReminderScheduler.reschedule()`, including calendar writes (a visibility or colour change can change
  which days show a dot). Chosen over an app-scoped collector in `:widget` because it needed no new
  process-start wiring: `RoomEventRepository`'s constructor already takes `ReminderScheduler` the same
  way, `RandomEventUidGenerator`; and `docs/contracts/Events.md` §5 already named the shape ("The widget
  updater is declared separately under `core/domain/…/widget/`") before this task started. The
  production implementation, `DebouncedWidgetUpdater` (`:widget`), buffers signals in a
  `MutableSharedFlow` and a single collector coroutine (a Hilt-provided, qualified
  `CoroutineScope(SupervisorJob() + Dispatchers.Default)`) applies `Flow.debounce(1.second)` before
  calling `WidgetRefresher.refreshAll()` -- the same seam `WidgetRolloverListener` uses. Bound in
  `:widget`'s own `WidgetModule`, so `:app` links with no change of its own (it already depends on
  `:widget`). `FakeWidgetUpdater` (`:core:testing`) is the fake.
- **Event dots.** `MonthGlanceWidget.provideGlance` takes one `fetchMonthEventPresence` snapshot of
  `ObserveAgendaUseCase.presence(IfcYearMonth.gregorianRange)` per render -- **not** a continuous
  collection, unlike the future Agenda widget -- bounded by `withTimeoutOrNull(3_000)` and a catch for
  any other exception; either one renders the month with no dots rather than blocking or crashing the
  render. `MonthWidgetState`'s `MonthDayCellState.hasEvent` / `MonthIntercalaryState.hasEvent` mark the
  28 grid cells and the trailing Leap Day / Year Day band (an intercalary day is an ordinary date to the
  events contract, so it can carry a dot exactly like any other day, CLAUDE.md rule 6). A dot is a small
  bullet glyph on its own reserved line beneath the day number, shape/presence rather than colour (CLAUDE.md
  rule 3) and never a count or a title (rule 8); its absence, not a colour, is what "no events" looks
  like. The merged content description appends a plain, resource-backed "Has events." only when *today*
  has one, never a count.
- **Lock-screen category unchanged.** `docs/security-and-privacy.md` §3.2's ruling is "any widget capable
  of showing event titles declares `not_keyguard`"; a dot is presence only, never a title, a count, or a
  calendar name, so the Month widget is not "capable of showing event titles" in the sense that ruling
  means and keeps `widgetCategory="home_screen"` only, unchanged from M5 T3.

**As built (Wave 3 I3, `:widget`). Widgets follow the app's appearance** (design-plan §4.9 "Follow the
app's theme", §5.6 "Widget appearance"), superseding the "Configuration" plan below -- there is no
config activity and no per-instance Glance state; both widgets read the same global
`UserSettings` (`:core:domain`) the app screens do.

- **`WidgetEntryPoint` gained `settingsRepository(): SettingsRepository`.** `TodayGlanceWidget` and
  `MonthGlanceWidget`'s `provideGlance` each read `entryPoint.settingsRepository().settings.first()` --
  one snapshot per render, exactly like `holidaySetProvider()`/`observeAgendaUseCase()` above, never a
  continuous collection inside the composable.
- **Colour resolution** (`widget/theme/WidgetTheming.kt`, pure and unit-tested without Robolectric except
  where building a `ColorProviders` at all touches `android.graphics.Color`): `resolveWidgetColors`
  mirrors `IfcTheme`'s own order with one simplification -- when `colorSource` is `DYNAMIC` and the SDK
  is 31+, it returns `null` and the caller keeps using `GlanceTheme.colors` exactly as before this task,
  since Glance's own dynamic colour set already follows the system UI mode and building an equivalent
  forced `ColorProviders` would need a wallpaper-derived `ColorScheme` from a `Context` this function
  deliberately does not take -- a widget's own per-type theme override therefore has no effect while
  Material You is on. Otherwise it resolves `palette.colorSchemes()` (pureBlack applied to the dark
  member only) and picks which scheme(s) actually show via `resolveWidgetDark(themeMode, widgetTheme)`:
  `WidgetTheme.LIGHT`/`DARK` force that scheme in both the day and night slot regardless of the system;
  `FOLLOW_APP` defers to the app's `ThemeMode` the same way, or forces neither (`SYSTEM`) so Glance picks
  by the system UI mode, exactly like `IfcTheme`'s own light/dark resolution.
- **Background opacity.** `UserSettings.widgetBackgroundOpacity` (0..100) becomes the alpha of the
  resolved `widgetBackground` colour (`Color.copy(alpha = …)`, `applyWidgetBackgroundOpacity`), applied
  only to each widget's outermost background -- never to a day cell's or the intercalary band's own
  fill. Below `LOW_OPACITY_CHIP_THRESHOLD` (`shouldShowLowOpacityChip`, 50%) the Today widget wraps its
  date text in its own opaque chip (the un-adjusted `widgetBackground` colour, since
  `androidx.glance.color.ColorProviders` has no `surfaceContainer` role to draw a proper chip from); the
  Month widget's grid and intercalary band need no such chip, since every cell already carries its own
  opaque fill. **As built (ROADMAP M8 T1, accessibility audit finding #16):** the Month widget's
  title/Gregorian-span/header-row block, which sits directly on the translucent background and has no
  opaque fill of its own the way the grid does, now gets the same chip treatment as Today's text below
  the same threshold -- the one gap the grid's own opaque cells did not cover.
- **Month widget cell fills.** `DayNumberCell`'s own background changed from painting over
  `widgetBackground` (the hairline trick alone) to `surface`/`surfaceVariant` with a 4dp corner radius
  (`surfaceVariant` when the cell has a holiday or event mark) -- the nearest roles `ColorProviders`
  actually exposes standing in for the app's `gridCell`/`gridCellMarked` tonal tiers, which Glance's
  Material 3 integration does not carry. The today pill needed no change: it already used `primary`/
  `onPrimary` with bold text, which is exactly `todayRing` (design-plan's answer to owner note 6). Marks
  (the holiday diamond, the event dot) are two `Text`s, not one, so each can carry `tertiary`
  (`holidayMark`) or `primary` (`eventMark`) -- Glance's plain `Text` has no rich-span support for mixing
  colours within one string.
- **The refresh-on-settings-change hook lives in `DebouncedWidgetUpdater`** (`:widget`), not as new
  wiring: its constructor now also takes `SettingsRepository`, and a second collector in `init` requests
  a refresh (through the same debounced, off-main-thread path an event write already uses) on every
  settings change after the one already in effect when the collector attaches. That collector reads a
  definite baseline with `Flow.first()` and then `dropWhile { it == baseline }`, rather than a plain
  `drop(1)`: since the settings flow is conflated like any `StateFlow`, a `drop(1)` could drop an
  *already-changed* value if the change happened before the collector attached, silently losing it. A
  widget only reacts once this singleton exists, which in practice is well before a user can reach the
  Settings screen (the same reasoning `IfcApplication`'s eager field-injected singletons rely on), but
  nothing here forces it eagerly at process start the way `WidgetPreviewUpdater` is.
- `values-night/colors.xml`'s `widget_preview_surface` moved from the pre-rework `#0E1615` to the
  current `DarkSurface`, `#121C1B` (design-plan §3.1 "Dark scheme rework"); the light preview file
  already matched `LightSurface`.

### Configuration (superseded, kept for history)

- Use an optional config activity with `configuration_optional|reconfigurable`.
- Options are: show the Gregorian line, the weekday mode, and background opacity.
- Settings are stored per widget in Glance `PreferencesGlanceStateDefinition`.

### Picker previews

- Override `providePreview` and call `setWidgetPreview` on API 35 and later.
  - The call is rate-limited to about two per hour.
  - Call it on app start, only when the (versionCode, locale) pair changes.
- Provide an XML `previewLayout` for API 31 to 34.
- Provide a `previewImage` PNG below API 31, generated from a Roborazzi capture.

**As built (M5 T5, `:widget`).**

- **`previewLayout` (API 31-34)** is a real static mock-up, `res/layout/today_widget_preview.xml` /
  `res/layout/month_widget_preview.xml` -- plain `LinearLayout`/`TextView` (RemoteViews-safe, no
  Compose/Glance), every piece of language text a string resource (CLAUDE.md rule 9), on a fixed,
  illustrative sample date chosen to be a regular day on purpose (Today: IFC Sol 13, 2026; Month:
  September 2026, today = IFC day 8, the same date `MonthWidgetStateTest` hand-checks) so a static
  mock-up can never misrepresent Year Day or Leap Day, which have no month/day number of their own
  (CLAUDE.md rule 6). Colours are plain resources (`res/values/colors.xml` +
  `res/values-night/colors.xml`) carrying the same hex values as `:core:designsystem`'s brand light/dark
  schemes, since a static layout cannot run `GlanceTheme`.
- **`previewImage` (verified valid since API 11 against the SDK's own `api-versions.xml`, not API 30 as
  once assumed) lives in the base `res/xml/*_widget_info.xml`**, not a separate qualifier split, and is a
  simple vector drawable rather than a Roborazzi-captured PNG -- a deliberate simplification the task
  authorized over this section's original wording, since API 26-30 is an increasingly small, legacy-only
  audience for a picker thumbnail.
- **`providePreview` + `setWidgetPreview` (API 35+)** reuse `TodayWidgetContent` / `MonthWidgetContent`
  unchanged, the same composables the real widgets render, fed a `Clock.fixed` / literal `ZoneProvider`
  pair on the exact same sample dates as the static layouts above (`TodayGlanceWidget.PREVIEW_CLOCK`,
  `MonthGlanceWidget.PREVIEW_CLOCK`), so the dynamic and static previews cannot drift apart the way two
  independently hand-built mock-ups could. `WidgetPreviewUpdater` (`:widget`, package `widget.preview`)
  is the version/locale guard: SDK-gated (`Build.VERSION_CODES.VANILLA_ICE_CREAM`), persisted in a plain
  `SharedPreferences` file keyed by `"$versionCode|$localeTag"`, and calling
  `PreviewRegistrar.registerPreviews()` -- a seam over `GlanceAppWidgetManager.setWidgetPreviews`,
  exactly like `WidgetRefresher` is a seam over `updateAll`, so the guard logic is unit-tested without
  the real, API-35-only system call. The pair is persisted only when both widgets' calls report success;
  a rate-limited result leaves it unset so the next process start retries. `IfcApplication.onCreate` calls
  `updateIfNeeded()` off the main thread through `AppStartup`, isolated from the reminder re-arm so that
  neither can fail the other.

## 6. Testing strategy

| Layer | Tests | Runner |
|---|---|---|
| `:core:calendar` | Exhaustive round trip for every date in years 1 to 9999 (about 3.65 million): `from(d).toLocalDate()==d`, successor monotonicity, 13x28 plus the intercalary counts per year. | JUnit 6 plus kotest-property. Runs in seconds. |
| | An independent enumeration oracle (build each year's date list by brute force) checked against the arithmetic implementation. | |
| | Golden CSV vectors, from section 3.1 plus historical dates. | |
| | kotest-property for arithmetic laws (plus/minus inverses, clamping, ordering). | |
| | Parse/format round trip, invalid construction, and ordering. | |
| `:core:domain` | Property tests for the recurrence expander. For every IFC rule, each occurrence converts back to a matching `IfcDate`, and Leap Day rules fire only in leap years. | JUnit 6 |
| | DST edge cases with fixed zones. | |
| | Holiday rules against published tables: `HolidayOracleTest` (Easter 1900–2100 Western and Orthodox, OPM federal holidays 2020–2030) in `:core:domain`; `UsPackOracleTest` / `BundledPacksTest` (OPM 2024–2028, 2026 observances, Easter family) in `:core:holidays`. Oracle CSVs carry their source URL and fetch date. | |
| | Agenda bucketing. | |
| `:core:data` | Room 3 DAO tests on the JVM with `BundledSQLiteDriver` in memory. No emulator is needed. | JUnit4 (plus Robolectric only where a Context is needed) |
| | Range-query correctness against a naive filter (property test). | |
| | Migration tests from exported schemas (`core/data/schemas/` is committed). | |
| | DataStore serializer round trip and corruption fallback. | |
| | ICS parser fixtures. | |
| ViewModels | Fakes from `:core:testing`, a fake `Clock` and `DateTicker`, Turbine, and `runTest`. Include a test that advances the clock across midnight. | JUnit4 |
| Compose UI | Stateless `XScreen` tests under Robolectric (`@GraphicsMode(NATIVE)` for any test that depends on text metrics — legacy mode fakes every Text at one height). Cover semantics (content descriptions, selection) and the intercalary band in June 2028 and in December. `configureIfcAndroid` pins every library module's Robolectric run to `sdk=<catalog targetSdk>` (generated, ROADMAP R7 — see "build-logic" above): without a `targetSdk` in the test manifest Robolectric picks its newest SDK, where the Compose test rule's input injection breaks. | JUnit4 plus Robolectric 4.17 |
| Screenshots | Roborazzi. `generateComposePreviewRobolectricTests` (the Compose preview scanner) auto-captures every `@Preview` it finds; as of R6 / M2 T10 that is wired for `:core:designsystem` only — the features follow once `:core:designsystem`'s goldens are committed and reviewed. | `verifyRoborazziDebug`, guarded in CI (§6 "Goldens") |
| | The MonthGrid matrix {normal, June-leap, December} x {light, dark} x {font 1.0, 2.0} x {compact, expanded} x {LTR, RTL} and the IfcDatePicker matrix {regular, Leap Day, Year Day, clamped, invalid year} x {dark, font 2.0, narrow} are `@Preview` combinations in `MonthGridPreviews.kt` / `IfcDatePickerPreviews.kt`, not hand-written Roborazzi tests — the scanner captures each combination once per preview function. | |
| | Glance widgets through glance-appwidget-testing or previews. | |
| Instrumented | Minimal smoke tests. They run nightly and on manual dispatch, not per push: app launch, a widget receiver smoke test, and the alarm re-arm after `TIME_SET` (adb broadcast). | emulator-runner |

### Dispatcher seams in tests

A ViewModel (or a pure-JVM use case) whose flow genuinely leaves the caller's own dispatcher — a
`flowOn`, or a background `CoroutineScope` it owns — takes that dispatcher through an `internal`
constructor parameter, with a second, public constructor (`@Inject` where Hilt applies) that defaults
it to `Dispatchers.Default`. A test in the same module can then reach the `internal` constructor
directly and pass the same `TestDispatcher` it gave `runTest`, so the work runs under the test's own
virtual-time scheduler instead of a real thread pool: with the hard-coded dispatcher, whether a second
emission had landed before a Turbine `awaitItem()` or a plain `advanceUntilIdle()` returned was a
real-time race — passing alone, failing under a loaded full-suite run (docs/WORKFLOW.md §2).
`HolidaysViewModel` and `DefaultObserveAgendaUseCase` are the two production sites this applies to
(ROADMAP R9). Every other ViewModel in the app only reads flows a test already fully controls —
`viewModelScope.launch`/`combine`/`stateIn` over a `:core:testing` fake backed by a plain
`MutableStateFlow`, or `DateTicker` — so it needs no such seam; adding one there would be speculative.

### Goldens

Robolectric native-graphics output differs between Windows and Linux, so CI (Linux) is the only recorder.
Runbook, with the exact commands: [screenshots.md](screenshots.md).

- **Tracked output directory.** `ifc.android.compose` sets `roborazzi { outputDir }` to
  `<module>/src/test/screenshots/` (a tracked source directory, not Roborazzi's gitignored
  `build/outputs/roborazzi` default) and `roborazzi { compare { outputDir } }` to an explicit `build/`
  subdirectory, so `*_actual.png` / `*_compare.png` diff artifacts from a local `compareRoborazziDebug`
  never dirty the tracked directory or need a `.gitignore` rule of their own.
- **Recording.** A `workflow_dispatch` "record-screenshots" job (`.github/workflows/record-screenshots.yml`)
  runs `recordRoborazziDebug` on `ubuntu-latest`, prints a per-module image count to the run summary, and
  uploads the recorded `src/test/screenshots/` trees as a build artifact with their relative paths
  preserved. It does not commit them: every commit in this repo is GPG-signed (WORKFLOW.md §1), and a bot
  cannot hold that key, so the owner downloads the artifact, reviews it, and commits the goldens locally
  with a signed commit.
- **Local and agent runs** use `compareRoborazziDebug`, which never blocks — Windows rendering differs
  from the Linux-recorded goldens by construction, so a local pixel mismatch is not a signal.
  `./gradlew check` does not run any Roborazzi lifecycle task (`record`/`compare`/`verify`) at all, so
  with no goldens tracked every `captureRoboImage` call in `testDebugUnitTest` is a no-op: `check` is
  green whether or not goldens exist, and never fails from a pixel difference.
- **First screenshot tests.** As of R6 / M2 T10, `:core:designsystem` is wired to Roborazzi's Compose
  preview scanner (`roborazzi { generateComposePreviewRobolectricTests { ... } }` in
  `core/designsystem/build.gradle.kts`, using the `sergio-sastre/ComposablePreviewScanner` +
  `roborazzi-compose-preview-scanner-support` libraries, MIT and Apache-2.0 respectively,
  `testImplementation` only). It generates one Robolectric test per `@Preview` under
  `io.github.chrisjmendoza.yearal.core.designsystem`, fixed at `sdk=36`/`qualifiers=w360dp-h640dp-xhdpi`
  and `@GraphicsMode(NATIVE)` for determinism, and currently produces 56 images (the MonthGrid and
  IfcDatePicker preview matrices) with no golden committed yet. No other module has screenshot tests yet;
  they are added module by module once this one's goldens exist and are reviewed.
- **`verifyRoborazziDebug` in CI** is guarded, not unconditional: a step in `ci.yml` checks
  `git ls-files '**/src/test/screenshots/*.png'` and only runs `verifyRoborazziDebug` when that is
  non-empty, so CI stays green with zero goldens and starts enforcing them automatically the moment the
  owner commits the first ones — no manual flag flip needed.

### CI gate per push

`check` (per module: `spotlessCheck`, `lint` on Android modules, `test` — JVM and Robolectric —, the Dokka KDoc
gate on JVM modules) and `:app:assembleDebug`; `verifyRoborazziDebug` runs whenever at least one golden PNG
is tracked in git (guarded, not unconditional — see "Goldens" above).

## 7. CI/CD (GitHub Actions)

- **`ci.yml`**
  - Triggers: pushes to `main`, and pull requests (cloud agents and Dependabot; see WORKFLOW.md §1).
  - Setup: ubuntu-latest, `actions/setup-java` (temurin 21), and `gradle/actions/setup-gradle` with caching.
  - Steps: `./gradlew check :app:assembleDebug`, then `verifyRoborazziDebug` guarded behind a
    `git ls-files` check for tracked goldens (§6 "Goldens").
  - Artifacts: the debug APK on every run; test, lint and Roborazzi diff reports (`build/reports/roborazzi/`,
    `build/outputs/roborazzi/`) on failure.
  - Add concurrency cancellation.
  - Optional: a separate fast job that runs `:core:calendar:test :core:domain:test :core:holidays:test`. It finishes in under a minute and gives agents early feedback.
- **`record-screenshots.yml`:** manual dispatch. See section 6.
- **`nightly.yml`:** instrumented smoke tests on `reactivecircus/android-emulator-runner` (API 26, API 36, and API 37 when images exist), plus a dependency-updates report.
- **`release.yml`:**
  - Trigger: a `v*` tag.
  - Steps: run the full CI gate, build the release bundle to prove the tag builds, and create a GitHub
    Release with the changelog and the R8 mapping file. **Not yet implemented** — this workflow file
    doesn't exist yet (M2 T11's remaining piece: the offline upload key and the Play Console app). Note
    for whoever writes it: since M2 T11's signing half (below), a CI run with no keystore secrets no
    longer produces an *unsigned* bundle — the `release` build type always has a signing config, so a
    keyless CI run is **debug-signed** instead, same as any other machine without the upload key
    (docs/release-builds.md).
  - For 1.0 the signed AAB is built and uploaded to Play from the owner's machine, because the upload key stays offline (security-and-privacy.md). Signing in CI from GitHub secrets, and automated upload to the internal track, are optional later steps; if adopted, use an upload action rather than Gradle Play Publisher, which has had open AGP 9 compatibility issues.
  - No APKs are attached to GitHub Releases. They would be signed with a different key than the Play build, so users could not cross-update. F-Droid, if pursued, builds from source with its own key.
  - Promotion between tracks happens in the Play Console.
- **Signing:**
  - Use Play App Signing. Google holds the app key and you hold an upload key, which can be reset through Play Console if lost.
  - Keep the keystore out of the repo; `.gitignore` covers keystores, signing properties, and key material.
  - Use the debug keystore for local development.
  - **As built (M2 T11, `:app`'s `release` build type, wired in `build-logic/convention/.../ifc.android.application.gradle.kts`):**
    the `release` signing config is read from `keystore.properties` at the repo root (gitignored; keys
    `storeFile`/`storePassword`/`keyAlias`/`keyPassword`, see [release-builds.md](release-builds.md)) or,
    if that file is absent, from `YEARAL_RELEASE_STORE_FILE`/`_STORE_PASSWORD`/`_KEY_ALIAS`/`_KEY_PASSWORD`
    environment variables — the file wins when both exist. When neither source is complete,
    `:app:assembleRelease` falls back to the **debug** signing config and logs a one-line
    configuration-time warning; no password is ever logged either way. This applies uniformly (a local
    machine, a fork, CI) — see security-and-privacy.md §8.2 for why an unsigned release APK was rejected
    in favour of this fallback. The `release` build type also sets `isProfileable = true` (not
    `isDebuggable`) so the owner can attach Android Studio's profiler to the exact build they judge for
    performance; `isMinifyEnabled` is untouched (R8 is ROADMAP M8 T2, once Hilt/Room3/kotlinx-serialization
    keep rules exist).
- **Dependencies and repo hygiene** (details in security-and-privacy.md):
  - Dependabot for the `gradle` and `github-actions` ecosystems with grouped PRs and a cooldown. Review AGP, Kotlin and KSP bumps manually.
  - GitHub Actions pinned by full commit SHA, read-only `GITHUB_TOKEN` by default, secret scanning with push protection, branch protection on `main`, private vulnerability reporting, and a `SECURITY.md`.
  - Repositories restricted with content filtering (`google()`, `mavenCentral()`, Gradle Plugin Portal only); no JitPack, no dynamic versions; Gradle wrapper checksum validated.
  - A CI check compares the merged manifest's permissions against an allow-list, so a dependency cannot silently add `INTERNET` or anything else.
- **Versioning:**
  - Use SemVer in `gradle.properties` (`VERSION_NAME=0.1.0`).
  - `versionCode = major*1_000_000 + minor*10_000 + patch*100 + build`.
  - Tags are `vX.Y.Z`, with a `CHANGELOG.md` in Keep a Changelog style.
- **Play path:** internal track from M2, then closed testing, then production.
  - **Schedule risk:** personal Play developer accounts created after November 2023 must run a closed test with at least 12 testers for 14 continuous days before production access. Start recruiting testers at M5.
- **F-Droid:** there are no proprietary dependencies, so F-Droid is a cheap second channel after 1.0.
- **Privacy policy:** host it on GitHub Pages from this repo and link it in the app. Play requires it, together with the Data safety form, before *any* track including closed testing.

## 8. Security and privacy architecture

[security-and-privacy.md](security-and-privacy.md) holds the threat model and the full decision table. The points that shape the code:

- **Data at rest:** app-private storage only (settings are plain JSON at `filesDir/datastore/user_settings.json`, inside the backup include set), relying on the app sandbox and file-based encryption. **No SQLCipher** — it only helps against root/forensic attackers (out of scope), and an auth-gated key would break widgets, reminders and workers. Caches go in `cacheDir` / `noBackupFilesDir`. Device-calendar data is read live and never copied into Room.
- **No network:** no `INTERNET` permission until URL subscriptions (1.3). This is a verifiable privacy claim and means a compromised dependency cannot exfiltrate anything. No analytics, ads, crash SDK, Firebase or Play Services; Play Console vitals only.
- **Logging:** no event content in release logs, ever. A "Delete all data" action ships in 1.0.
- **Notifications:** reminders use `VISIBILITY_PRIVATE` with a redacted public version. No full-screen intents.
- **Widgets:** any widget that can show event titles ships with a privacy mode (titles / counts only / date only) and `widgetCategory="not_keyguard"` (in an `xml-36` resource folder), because since Android 16 QPR2 widgets are lock-screen eligible by default.
- **App lock (1.x, optional):** `BiometricPrompt` with `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`, no custom PIN. It is a UI gate, not encryption: one gate in `:app` covers launcher entry, widget taps, notification taps, import intents and widget configuration. Enabling it also turns on `FLAG_SECURE`.
- **Backups:** Auto Backup stays on but encrypted-only — `dataExtractionRules` with `disableIfNoEncryptionCapabilities="true"`, plus the legacy `fullBackupContent` for API 30 and below. Verified by a backup → reinstall → restore round trip with `bmgr` before release.
- **Export / import (1.2):** Storage Access Framework only, no storage permissions. Plaintext export warns; optional passphrase encryption uses platform PBKDF2-SHA256 + AES-256-GCM. Restored JSON and imported `.ics` are untrusted input: size/count/length caps, bounded lazy RRULE expansion, imported reminders off by default, plain-text rendering only, transactional import with preview and undo, and a hostile-file test corpus.
- **Components and intents:** minimal exported components; typed, validated intent extras carrying IDs only; immutable explicit `PendingIntent`s; `RECEIVER_NOT_EXPORTED` for context-registered receivers; no deep links or App Links. Android Lint security checks are CI errors.
- **Permissions by release:** 1.0 — `POST_NOTIFICATIONS` (asked when the first reminder is added), `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` (max SDK 32), `RECEIVE_BOOT_COMPLETED`. 1.1 — `READ_CALENDAR`, just-in-time with a rationale. 1.3 — `INTERNET`. Never: `WRITE_CALENDAR`, storage, contacts, location, `AD_ID`.

## 9. Risks

1. **Toolchain freshness.**
   - AGP 9 new DSL and built-in Kotlin with the Hilt, Room 3 and Roborazzi plugins.
   - Robolectric 4.17 is one week old.
   - KSP lags Kotlin 2.4.
   - Mitigation: the M0-T3 spike plus an ADR, and conservative pins (Kotlin 2.3.21, AGP 9.3.3). The fallbacks are Room 2.8.5, or a temporary `android.newDsl=false`.
2. **Play closed-testing gate.** A new personal account needs 12 testers for 14 days, which is the largest calendar-time risk. Confirm your account type now.
3. **`USE_EXACT_ALARM` policy review.** The app should qualify as a calendar app with reminders. If the request is rejected, fall back to the user-granted `SCHEDULE_EXACT_ALARM` flow, with `setWindow` for rollover.
4. **OEM background killers** (Xiaomi, Samsung, Huawei) can defeat alarms. The `updatePeriodMillis` backstop and update-on-app-open limit the damage. Document this in the app's help.
5. **Weekday confusion is the core UX risk.** The IFC "Sunday" is not the real Sunday. The dual headers and the Learn screen mitigate it. Decide the default with real users during internal testing.
6. **Agents hallucinating older APIs** (Room 2 packages, Navigation Compose, `kotlin-android` plugin usage). The CLAUDE.md rules and the compile-time gates cover this.
7. **Screenshot goldens across operating systems.** Windows development against Linux CI is solved by CI-only recording, but that adds a step to UI changes.
8. **Scope creep in events.** Per-occurrence edits, attendees and sync are explicitly out of version 1.

## Sources

Version and platform facts were checked against these pages on 2026-09-17.

- [AGP 9.4.0 release notes](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [AGP 9.3 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes)
- [AGP 9.0 release notes (built-in Kotlin, new DSL)](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [About AGP: Studio and API-level compatibility](https://developer.android.com/build/releases/about-agp)
- [Android Studio Quail 4 stable](https://androidstudio.googleblog.com/2026/09/android-studio-quail-4-now-available.html)
- [Gradle 9.7.1 release notes](https://docs.gradle.org/current/release-notes.html)
- [What's new in Kotlin 2.4.20](https://kotlinlang.org/docs/whatsnew2420.html)
- [KSP releases](https://github.com/google/ksp/releases)
- [KSP issue 2965: Upgrade to Kotlin 2.4.0](https://github.com/google/ksp/issues/2965)
- [Third-party report of KSP and Kotlin 2.4 lock-out](https://github.com/uny/a2ui-compose/issues/64)
- [Compose August '26 release](https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html)
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping)
- [AndroidX all-channel versions](https://developer.android.com/jetpack/androidx/versions/all-channel)
- [AndroidX stable channel](https://developer.android.com/jetpack/androidx/versions/stable-channel)
- [Room 3 releases](https://developer.android.com/jetpack/androidx/releases/room3)
- [Glance releases](https://developer.android.com/jetpack/androidx/releases/glance)
- [Glance widget updates guide](https://developer.android.com/develop/ui/compose/glance/glance-app-widget)
- [Glance generated previews](https://developer.android.com/develop/ui/compose/glance/generated-previews)
- [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3)
- [Navigation 3 get started](https://developer.android.com/guide/navigation/navigation-3/get-started)
- [Dagger and Hilt releases](https://github.com/google/dagger/releases)
- [kotlinx.coroutines 1.11.0](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.11.0)
- [kotlinx.serialization changelog](https://github.com/Kotlin/kotlinx.serialization/blob/master/CHANGELOG.md)
- [JUnit releases](https://github.com/junit-team/junit-framework/releases)
- [Kotest releases](https://github.com/kotest/kotest/releases)
- [Turbine releases](https://github.com/cashapp/turbine/releases)
- [Robolectric releases](https://github.com/robolectric/robolectric/releases)
- [Roborazzi releases](https://github.com/takahirom/roborazzi/releases)
- [Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing)
- [androidx.test releases](https://developer.android.com/jetpack/androidx/releases/test)
- [detekt releases](https://github.com/detekt/detekt/releases)
- [ktlint releases](https://github.com/pinterest/ktlint/releases)
- [Spotless Gradle plugin](https://plugins.gradle.org/plugin/com.diffplug.spotless)
- [Gradle Play Publisher AGP 9 issue](https://github.com/Triple-T/gradle-play-publisher/issues/1182)
- [Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
- [Implicit broadcast exceptions](https://developer.android.com/develop/background-work/background-tasks/broadcasts/broadcast-exceptions)
- [Schedule alarms and exact alarm permissions](https://developer.android.com/develop/background-work/services/alarms/schedule)
