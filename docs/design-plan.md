# Visual design plan — "more than black and white"

Status: **adopted 2026-09-23** (the owner answered every question in §8 the same day; §10 is the
run log). Written from a read-only audit of every screen, the design system, the settings model and
both widgets. It is the design brief for ROADMAP **M2 T13** (the visual polish pass) and the catalogue
entry FEATURES **W6** ("colour through the UI, not just in the accents").

Legend, the same one FEATURES.md uses: 🔴 do first, it fixes the complaint · 🟠 the pass is not done
without it · 🟡 worth it, can trail · ⚪ nice to have, or an open question.

---

## 1. Diagnosis — why the app reads as monochrome

The palette is good and it is not the problem. Five things, in order of how much each one costs us:

| # | Finding | Evidence |
|---|---|---|
| 🔴 D1 | **On Android 12+ the brand palette is never shown.** `dynamicColor` defaults to `true` in `UserSettings` and in `IfcTheme`, so out of the box the scheme comes from the wallpaper. A neutral or dark wallpaper yields a near-grey Material You palette, and dark Material You surfaces are almost black. The teal / cream / amber work in `Color.kt` is opt-in via a switch nobody knows to flip. | [`UserSettings.kt`](../core/domain/src/main/kotlin/io/github/chrisjmendoza/yearal/core/domain/settings/UserSettings.kt), [`IfcTheme.kt`](../core/designsystem/src/main/kotlin/io/github/chrisjmendoza/yearal/core/designsystem/theme/IfcTheme.kt) |
| 🔴 D2 | **Colour is spent only on state, never on identity or hierarchy.** Selected cell, today ring, the intercalary band, the weekday block: that is the whole list. Nearly every `Text` has no colour argument, no screen uses a `Card`, list rows float on the page background, and the Today "hero" is three lines of type on a bare surface. | Today, Month, Year, Day, Events, Converter, Holidays screens |
| 🔴 D3 | **The dark scheme collapses to black and white by construction.** `DarkSurface` is `#0E1615` and the container tiers step from `#090F0E` to `#2F3B39`, which is too little tint to read as anything but black. With D2 on top (no containers used anyway), dark mode is white text on black. | [`Color.kt`](../core/designsystem/src/main/kotlin/io/github/chrisjmendoza/yearal/core/designsystem/theme/Color.kt) dark tones |
| 🟠 D4 | **Three of the four brand hues never reach the feature screens.** `tertiary` (amber) and `primaryContainer` are not referenced anywhere in `:feature:events`, `:feature:converter` or `:feature:holidays`. Amber appears only as a 6 dp holiday diamond and the intercalary band. | grep across the three modules |
| 🟠 D5 | **There is no design voice.** No `Typography`, no `Shapes`, no spacing tokens: the app runs on Material's defaults and every composable declares its own private dp constants. The one bespoke shape (the intercalary pill) is a local literal. | `theme/` has only `Color.kt` and `IfcTheme.kt` |
| 🟡 D6 | **Widgets ignore the app's theme.** Both Glance widgets pick dynamic colour on API 31+ and follow the system dark setting; they never read `themeMode` or `dynamicColor`. Status-bar icon contrast also follows the system rather than the chosen `ThemeMode`. | `TodayGlanceWidget.kt`, `MonthGlanceWidget.kt`, `MainActivity.kt` |

One prior decision compounds D2: the Year Day tile's filled pill was removed on 2026-09-19 because "it put
a shape and a colour on screen that nothing around it shared". That was the right call *then*. The fix is
not to keep removing colour but to make the intercalary accent something the whole app shares, so the fill
can come back (§4.3).

## 2. Constraints the plan must respect

These are already binding elsewhere; listed so no phase forgets one.

- **Colour is never the only signal** (FEATURES Q4). Every colour cue keeps its shape or text twin: today
  is a ring *and* bold; holiday is a diamond *and* amber; intercalary is a pill *and* an icon.
- **Both palettes must work**: the brand scheme and the wallpaper-derived one. So screens use *roles*
  (`primaryContainer`, `tertiaryContainer`), never brand hex values. New semantic tokens (§3.1) derive
  from roles for the same reason.
- **Contrast**: 4.5:1 for text, 3:1 for non-text, at every pairing the code uses. Today this is asserted
  in KDoc comments; §3.1 turns it into a test.
- **200 % font scale never clips** (Q4, an incumbent failure); **48 dp touch targets**; TalkBack reads
  both calendars.
- **Rules 5 and 6** (CLAUDE.md): IFC numeric dates keep the `IFC` prefix; Year Day and Leap Day are
  handled in every new component and preview.
- **The 4 × 7 grid, typography and spacing are the design language** (review response, agreed). No
  gradients, no illustrations that compete with the grid, no decoration that does not carry meaning.
- **No new dependency without an ARCHITECTURE note**; a bundled font or a colour-utility library is a
  documented decision, not a drive-by.

## 3. The visual language

### 3.1 Tokens — one place to tune the whole app (`:core:designsystem`)

**Semantic colours.** A `YearalColors` class exposed through a `CompositionLocal` from `IfcTheme`,
*derived* from the active `ColorScheme` so it works under dynamic colour too:

| Token | Brand scheme (light / dark) | Derived from | Used for |
|---|---|---|---|
| `todayRing`, `todayText` | teal / mint | `primary` | the today ring, today's number, "Today" badge |
| `heroContainer`, `onHero` | teal `#123F3D` on cream / mint on deep teal | `primaryContainer` | the Today hero card, the converter result card |
| `intercalary`, `intercalaryContainer` | amber / peach | `tertiary`, `tertiaryContainer` | Year Day, Leap Day, everywhere: band, Year tile, Day detail header, holiday list rows, widget band |
| `holidayMark` | amber | `tertiary` | the diamond in cells, the leading mark on holiday rows |
| `eventMark` | teal | `primary`, or the event's own colour | dots in cells, agenda row swatch |
| `weekdayNominal`, `weekdayActual` | sage container / sage text | `secondaryContainer`, `onSecondaryContainer` | the two weekday header rows and the weekday block |
| `gridCell`, `gridCellWeekend` | cream-tinted low container / one step higher | `surfaceContainerLow`, `surfaceContainer` | every day cell gets a fill, so the grid stops being ink on paper |
| `pageBackground`, `cardContainer` | cream / cream-2 | `surface`, `surfaceContainerLow` | screens and their cards |

**Dark scheme rework** (D3). Lift `DarkSurface` to a visibly teal `#121C1B`, and spread the container
tiers so each step is a real tint (`#182423`, `#1E2C2A`, `#25342F`, `#2D3F3A`). `primaryContainer` in
dark stays the brand teal so the hero card is unmistakably teal at night. Every changed pair gets its
ratio re-checked by the test below. Pure black moves to an explicit user option (§5.3).

**Contrast test.** `ColorSchemeContrastTest` in `:core:designsystem` (Robolectric-free: it only needs
`Color`) iterates every scheme the app can produce (brand light, brand dark, each palette in §5.2, pure
black) and asserts 4.5:1 for every on-colour on its container and 3:1 for `outline` on `surface`. The
KDoc ratios stay as documentation, but the gate is the test.

**Shapes.** A `Shapes` object: `extraSmall` 4 dp (marks), `small` 8 dp (cells, chips), `medium` 12 dp
(cards, tiles), `large` 20 dp (hero, sheets), plus a named `PillShape` token for the intercalary band
and badges, replacing the local `RoundedCornerShape(percent = 50)`.

**Typography.** A `Typography` object with: `displayMedium` for the Today hero date, tabular figures
(`FontFeatureSettings "tnum"`) on `titleLarge` and `bodyLarge` so grid numerals align, and a
`labelSmall` eyebrow style (uppercase, letter-spaced) for the "IFC" / "Gregorian" captions that the
review asked to make visibly secondary. ⚪ **Open decision:** bundle one display face for the hero
numerals (a variable font is 100–300 KB, SIL OFL; candidates in §7) or stay on Roboto. The Typography
object lands either way; the font is a one-line swap later.

**Spacing.** `Dimens`: 4 / 8 / 12 / 16 / 24 / 32 dp, and the grid's own `CellGap`, `CellPadding`,
`TodayRingWidth` move here from the composables' private constants.

### 3.2 The shell

- **Navigation bar / rail:** indicator on `secondaryContainer` with `onSecondaryContainer` icons, which
  is Material's default and already sage in the brand scheme; nothing to override once the palette shows.
- **Top app bars:** `surfaceContainerLow` container so the bar is a shade of cream rather than the page,
  with `onSurface` titles. Scrolled state one tier higher.
- **System bars:** `enableEdgeToEdge` gets a `SystemBarStyle` recomputed from the *app's* `ThemeMode`,
  not the system's, so a forced-light app never has white status icons on cream (D6). Small, real bug.
- **Screen backgrounds:** `surface` (cream / teal-black). Cards sit on `surfaceContainerLow`; the hero
  on `heroContainer`.

## 4. Screen by screen

Each item says what changes and what it fixes; nothing here changes behaviour.

### 4.1 Today (🔴)

```
┌────────────────────────────────────────┐
│ ▓▓ Sol 12, 2026 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓  │  hero card, heroContainer
│ ▓▓ IFC 2026-07-12 · Tue 23 Jun 2026 ▓  │  eyebrow captions "IFC" and "Gregorian"
│ ▓▓ [ IFC weekday: Thursday ]  ▓▓▓▓▓▓▓  │  weekday block keeps its sage container
│ ▓▓ [ Actual weekday: Tuesday ] ▓▓▓▓▓▓  │
│ ▓▓ Day 180 · Week 26 · Q3   ▓▓▓▓▓▓▓▓▓  │
│ ▓▓ ━━━━━━━━━━━━━━━──────── 49 % ▓▓▓▓▓  │  year progress, tinted track
└────────────────────────────────────────┘
  ◆ Year Day in 185 days                     intercalary accent chip (amber)
  ◆ Holidays today          (card, cardContainer, diamond marks)
  ● Today's events          (card, event swatches, times)
```

- Hero card on `heroContainer` with the IFC date in `displayMedium`, the numeric IFC form and the
  Gregorian date as *eyebrow + value* pairs so a newcomer sees which is which (the review's acceptance
  test: "what Gregorian date is this?").
- The year-progress bar and the day/week/quarter line move into the hero; the intercalary countdown
  becomes an amber chip under it. The "next holiday" line joins the Holidays card.
- Holidays get a leading diamond mark; agenda rows sit in a card with their swatch and a hairline
  between rows. Both lists keep their headings, styled as `labelSmall` eyebrows.
- Empty Today (no holidays, no events) shows one quiet line inside a card, not nothing.

### 4.2 Month (🔴)

- **Anchor the grid** (owner note 2): the page becomes app bar → grid → *selected-day summary* filling
  the space below. The summary shows the selected date in both calendars, its holidays and events in the
  same rows as Day detail, and a "Details" action; with nothing selected it shows today. This is the
  review's "selected-date summary in the dead space" and it removes the floating-grid feel.
- **One title** (owner note 3): the grid's inner heading goes; the app bar carries the month and year.
- **A visible zoom-out affordance** (owner note 4): the title becomes a `FilterChip`-style control with
  a trailing chevron, content-described "Show year".
- **Cells get a fill**: `gridCell` for every day, `gridCellWeekend` for the two Saturday/Sunday columns
  of the IFC week. Selected keeps `primaryContainer`; today keeps the ring and bold. The Gregorian
  number in the corner drops to `labelSmall` in `onSurfaceVariant` so it reads as secondary.
- **Marks grow**: holiday diamond and event dots from 6 dp to 8 dp, and a day with any mark tints its
  cell one tier up so colour, shape and fill all say "something is here".
- **The intercalary row for the eleven ordinary months** becomes a quiet pill on `surfaceContainer`
  showing the Gregorian span, so all thirteen months share one silhouette and the amber band no longer
  looks like a mistake in the two months that have it.
- Weekday header rows: "IFC" and "Actual" eyebrows on the left, the actual row in the sage text colour
  rather than a lighter grey.

### 4.3 Year (🟠)

- Every mini-month is a **card** on `cardContainer` with a 12 dp shape; the month containing today gets
  the `todayRing` border instead of standing alone as the only bordered tile.
- Mini-grid squares use `gridCell`, marks use `eventMark`; a **holiday diamond** appears in the mini grid
  too, since the data is already there and the Year page currently hides it.
- **Year Day tile and the Leap Day marker get the intercalary fill back**, now that the same accent is
  shared by Month, Day and the widgets (§1 last paragraph).
- Owner note 5: mini-months show weekday structure by drawing the Saturday/Sunday columns one tier
  darker, which is cheap on the existing `Canvas` and answers "which day is the 13th" at a glance (it is
  always a Friday).
- Fix the clipped top row under the app bar with proper content padding.

### 4.4 Day detail (🟠)

- Year Day and Leap Day get the **intercalary header**: amber container, the intercalary icon and the
  "no IFC weekday" explanation inside it, matching what the user tapped in Month or Year.
- Holidays rows carry the diamond; events rows the swatch; "Add event" and "Open in converter" become a
  `FilledTonalButton` and an `OutlinedButton`.
- The expanded-width empty pane gets a muted calendar glyph over its text.

### 4.5 Events list and editor (🟠)

- **List**: group rows under sticky **IFC month headers** (the list is already sorted by start date, so
  the grouping is a `LazyColumn` header per month, no query change). Rows show the colour swatch as a
  4 dp leading bar, the title, then *one* date line "Sol 12 · Tue 23 Jun" and the time. Recurrence and
  category ("Birthday", "Observance") become small `AssistChip`s instead of a fifth grey line. Empty
  state gets an icon.
- **Editor**: selected recurrence and policy options fill with `secondaryContainer`; advisory notices
  move from grey `surfaceVariant` to `tertiaryContainer` (they are advice, not neutral); validation
  errors become an `errorContainer` banner and the offending field shows `isError`. Save becomes a
  filled button in the app bar, not a bare check icon. The **colour swatch row** is §5.4.

### 4.6 Converter (🟠)

- The result becomes a **card on `heroContainer`**, the same component as Today's hero, so the answer
  looks like an answer. The swap-arrows icon (`SwapHoriz`) sits between the two directions, replacing the
  glyph the review said reads as "refresh". Copy / Share / Open day get icons.

### 4.7 Holidays (🟡)

- Pack rows get a leading colour dot from a static map keyed by pack id (IFC-native packs amber,
  national packs teal, religious packs sage) until packs carry their own colour (§5.5). Enabled packs sit
  on `surfaceContainerLow`; disabled on the page surface. Holiday rows carry the diamond, and Year Day /
  Leap Day rows use the intercalary accent.

### 4.8 Settings, More, Intro, Learn (🟡)

- Settings gains an **Appearance** section (§5). Theme and colour choices show a **live preview strip**:
  a miniature 7-day row with a today ring and one holiday diamond, drawn with the design system's own
  components, so the owner and users see the palette before committing.
- More hub rows get tinted leading icons in a `secondaryContainer` circle.
- Intro pages each get one **illustration built from the grid itself** (13 × 28 dots, the Sol month
  highlighted, the Year Day pill outside the week), no bitmap assets; the third page shows the pill in
  amber. Learn reuses the same three drawings as section headers.

### 4.9 Widgets (🟠)

- **Follow the app's theme** (D6): `SettingsRepository` joins the widget Hilt entry point, and both
  widgets choose brand vs dynamic and light vs dark from `UserSettings`, with "follow system" as the
  default so nothing changes for existing placements.
- Month widget: cells get a faint fill and the band uses the intercalary token, matching the app. The
  filled today pill stays (Glance has no border modifier); it is the widget's own idiom and the owner's
  note 6 is answered by making the pill the `todayRing` colour with bold `onPrimary` text.
- Large Today widget shows the year-progress bar and the next intercalary countdown, as the review asked
  ("large widgets should show more").

## 5. Colour customisation — what users get to choose

All of these live in a new **Appearance** section of Settings, stored as fields on `UserSettings`
(typed JSON DataStore, so a new field is one line in the domain class, one in the DTO, and a default).
Ordered by value to the owner's complaint.

### 5.1 🔴 Colour source: **Yearal palette** or **Material You**

Replace the `dynamicColor` boolean with `colorSource: ColorSource = BRAND` (`BRAND`, `DYNAMIC`).
Because the DTO ignores unknown keys, existing installs drop the old `dynamicColor = true` and land on
the brand palette once; the app is pre-1.0 and this is the intended migration. Below API 31 the option
is shown disabled with its existing "not available" subtitle. This alone changes what the owner sees on
their device.

### 5.2 🟠 Palettes: six curated schemes, not a colour wheel

A `Palette` enum on `UserSettings`, honoured when the source is `BRAND`:

| Palette | Seed idea | Notes |
|---|---|---|
| **Teal** (default) | the icon's teal / cream / amber | the current brand scheme |
| **Sol** | amber primary, teal accents | inverts the two brand hues; warm |
| **Night** | indigo primary, cream, coral accent | for people who live in dark mode |
| **Moss** | forest green, parchment, ochre | |
| **Rose** | plum primary, blush, gold accent | |
| **Ink** | near-monochrome: charcoal, paper, one amber accent | *the deliberate* black-and-white, for users who want it |

Each palette is a full light/dark `ColorScheme` pair declared like the current one in `Color.kt` and
covered by the contrast test, so every pairing is guaranteed rather than generated. Six hand-tuned
schemes are roughly 250 lines of colour declarations; a generator library (`material-kolor` or Google's
`material-color-utilities`) would cut that to a seed per palette but adds a dependency and makes contrast
a runtime property. **Recommendation: curated.** It fits "no dependency without a decision", matches the
launcher icon's craft, and keeps `Ink` honest.

### 5.3 🟠 Dark options: System / Light / Dark, plus **Pure black**

A `pureBlack: Boolean = false` toggle, enabled only when a dark scheme is active, that overrides
`surface`, `background` and the container tiers of *whichever* dark scheme is in use (brand palette or
Material You) with `#000000` and tight greys. This is the AMOLED request the incumbent's reviews mention
and it makes "black and white" an opt-in again instead of the accidental default.

### 5.4 🟠 Per-event colour

Three of four layers exist: `Event.colorArgb` in the domain, the `color_argb` column in Room (no
migration), and the list swatch already resolves `event.colorArgb ?: calendar.colorArgb`. Missing is the
editor control: a row of eight swatches (the palette's primary, tertiary and secondary plus five fixed
hues) with "Calendar colour" as the reset. The colour then shows in the list bar, the Today and Day agenda
swatches, the Month cell dot and the Month widget dot, all of which already read the resolved colour.
`EventCategory` (`EVENT`, `OBSERVANCE`, `BIRTHDAY`) also exists with no UI; the editor gets a three-way
segmented control and the list shows a chip. Both are stored today, so `docs/contracts/Events.md` stays
frozen.

### 5.5 🟡 Holiday pack colour

Add an optional `color` field to the pack JSON (ADR 0004 amendment) so each pack carries its own hue;
the static map in §4.7 is the fallback for packs without one. Holiday marks then take the pack colour
where the diamond shape still says "holiday".

### 5.6 🟡 Widget appearance

Per widget *type*, on `UserSettings`: `widgetTheme: FOLLOW_APP | LIGHT | DARK` and
`widgetBackgroundOpacity: 0–100 %`. Global rather than per placed instance, which avoids a configure
activity and a per-`GlanceId` state store; per-instance can come later if anyone asks. A transparent
widget needs a text shadow or a scrim under the numerals, which the contrast test cannot check, so it
goes on the device matrix.

### 5.7 ⚪ Not proposed

Custom accent from a colour wheel (unbounded contrast risk), per-month colours (thirteen choices nobody
will make), themed launcher icons beyond the monochrome layer (Android already handles it), animated or
gradient backgrounds (competes with the grid).

## 6. Making the features solid — the hardening that rides along

The owner's second question. These are not new features; they are what stops the polish from breaking
what works, plus the UX gaps the audit turned up.

| | Item | Why |
|---|---|---|
| 🔴 | **Record the screenshot baseline first** (ROADMAP R6, owner step) on `main` before phase 1 lands, then re-record per phase. Add preview captures for each feature screen and both widgets while at it. | Otherwise the pass has no regression cover and every diff is a judgement call. |
| 🔴 | **Contrast test** (§3.1) in the gate. | Six palettes × two modes × pure black is too many pairs to eyeball. |
| 🔴 | **Font-scale previews**: every new component gets a `@Preview(fontScale = 2f)` and a dark preview, so 200 % and dark are captured as goldens. | Q4; the incumbent's documented failure. |
| 🟠 | **System bar icons follow `ThemeMode`** (§3.2). | Real bug today in forced light/dark. |
| 🟠 | **Widgets follow the app theme** (§4.9). | Real gap today; a user who picks Dark gets a light widget on a light wallpaper. |
| 🟠 | **Editor error states**: field-level `isError`, an `errorContainer` banner, and a disabled Save while invalid. | Today errors are bare red text under the form. |
| 🟠 | **Event list grouping and one date line per row.** | Five stacked text lines per row is the least scannable list in the app. |
| 🟠 | **Device pass after each phase** on the matrix in `device-test-matrix.md`: dark + light, dynamic + brand, 200 % font, TalkBack through Today and Month, both widgets at every size. | Robolectric cannot judge colour or launcher rendering. |
| 🟡 | **The unfamiliar-user test** (review, accepted): two people who have never seen the IFC answer "what Gregorian date is this?" and "when will this event happen?" from Today, Month and an event row, before and after the pass. | The acceptance criterion ROADMAP already names for T13. |
| 🟡 | **Decide the "actual" weekday label** with those testers. Recommendation: keep *actual* in the spec and the code, and in the UI show the two header rows with eyebrows "IFC" and "Actual", each with the explainer that already exists. If testers stumble, change the glossary first. | Open question in the review response. |
| ⚪ | A `Dimens`-driven layout audit for the editor's app-bar icon buttons that could not be measured at 48 dp under Robolectric (ROADMAP M4 T4 note). | Cheap once the tokens exist. |

## 7. Phases and sequencing

One task = one module owner; parallel agents never share a module (CLAUDE.md). Tests, KDoc and docs
land with each phase; `README.md` and `CHANGELOG.md` update in the phase that first makes a
user-visible change (phase 1).

| Phase | Scope | Modules | Depends on | Docs to update |
|---|---|---|---|---|
| **0 Baseline** (owner, half a day) | Record goldens on `main`; write the contrast test against the *current* schemes so it is green before anything moves. | `:core:designsystem` (test only) | — | `screenshots.md` status line |
| **1 Foundation** (🔴, 1–2 days) | Tokens (`YearalColors`, `Shapes`, `Typography`, `Dimens`); dark scheme rework; `colorSource` replaces `dynamicColor` with `BRAND` default; system-bar style from `ThemeMode`; app-bar and nav styling; Settings shows the new Appearance section with just the source switch. | `:core:designsystem`, `:core:domain` + `:core:data` (one field), `:app`, `:feature:settings` | 0 | ARCHITECTURE §4 (theme), FEATURES W2/W6 status, README, CHANGELOG |
| **2 Calendar screens** (🔴, 2 days) | Today hero; Month anchoring, single title, chevron, cell fills, marks, placeholder pill, selected-day summary; Year cards and intercalary fill; Day detail intercalary header. Two agents: `:core:designsystem` grid components first, then `:feature:calendar` screens. | `:core:designsystem`, `:feature:calendar` | 1 | ROADMAP M2 T13 notes 2–5 closed; ARCHITECTURE §4 screen behaviours |
| **3 Lists and forms** (🟠, 1–2 days, parallel with 2) | Events list grouping and rows, editor states, converter result card and swap icon, holidays rows. Three agents, one per feature module. | `:feature:events`, `:feature:converter`, `:feature:holidays` | 1 | FEATURES rows touched; CHANGELOG |
| **4 Customisation** (🟠, 2 days) | Palettes (§5.2), pure black (§5.3), per-event colour and category in the editor (§5.4), preview strip in Settings. | `:core:designsystem`, `:core:domain`/`:core:data`, `:feature:settings`, `:feature:events` | 1, 3 | ARCHITECTURE settings model; FEATURES new rows; README; CHANGELOG |
| **5 Widgets and onboarding** (🟠/🟡, 1–2 days) | Widgets follow the app theme, widget appearance settings (§5.6), Month widget fills, large Today content; Intro and Learn grid illustrations; pack colours (§5.5). | `:widget`, `:feature:settings`, `:core:holidays` | 1, 4 | ARCHITECTURE widgets; ADR 0004 amendment; security-and-privacy unchanged (no permission) |
| **6 Verify** (owner, one evening) | Re-record goldens; device pass; unfamiliar-user test; close M2 T13. | — | all | ROADMAP, device-test-matrix |

Correctness-critical code and its tests by different agents: the contrast test and the `ColorSource`
migration test (an old JSON with `dynamicColor` decodes to `BRAND`) are written from this doc and
`Color.kt`'s KDoc, not from the implementation.

## 8. Decisions for the owner

1. **Flip the default to the brand palette** (§5.1). Recommended yes; it is the single biggest lever and
   the migration is one-time and pre-1.0. -> Yes, implement
2. **Curated palettes vs a generator** (§5.2). Recommended curated, six palettes including `Ink`. -> Yes, follow recommendation
3. **A display font for the hero numerals** (§3.1). Candidates, all SIL OFL and tabular-figure capable:
   *Manrope*, *Instrument Sans*, *Fraunces* (serif, closest to an almanac). Recommended: land the
   Typography object on Roboto in phase 1 and decide the font on device with a phase 2 build, since the
   choice is aesthetic and reversible in one line. -> Roboto sounds good, I've used it before.
4. **Year Day tile fill returns** (§4.3), reversing the 2026-09-19 removal now that the accent is shared. -> I never wanted it gone, just styling to be more in line
5. **Grouping the Events list by IFC month** (§4.5) rather than Gregorian month, which is the app's
   stance but worth saying out loud. -> Yes, this focus is on being an IFC app first but give allowance to track against gregorian calendar.

## 9. Acceptance

The pass is done when, on a device with a neutral wallpaper and a fresh install:

- Today, Month, Year and the Month widget each show at least three distinct brand tints in both light
  and dark mode without the user touching Settings.
- Every colour cue has a shape or text twin (a screenshot with saturation removed is still readable).
- The contrast test and `verifyRoborazziDebug` are green in the gate; goldens are re-recorded and
  committed with the phase that changed them.
- An unfamiliar user answers the two review questions from Today and from an event row.
- Nothing in `docs/contracts/` changed.

## 10. Day run — 2026-09-23

Branch `local/design-pass`. Agents work in worktrees and hand back patches; the coordinator applies,
runs the gate and updates this list. ⬜ not started · 🔄 running · ✅ integrated and green · ⏸ deferred.

**Shared contract, landed first by the coordinator:** `ColorSource`, `ColorPalette`, `WidgetTheme`
enums in `:core:domain` (settings package), so parallel agents compile against one definition.

| | Task | Owner modules | Status |
|---|---|---|---|
| Wave 1 | A1 design-system foundation: `YearalColors` tokens, `Shapes`, `Typography`, `Dimens`, dark scheme rework, six palettes, pure black, `IfcTheme` API, `MonthGrid` title optional | `:core:designsystem` | ⬜ |
| Wave 1 | B1 settings model: `colorSource` replaces `dynamicColor`, `palette`, `pureBlack`, `widgetTheme`, `widgetBackgroundOpacity`; mechanical caller updates | `:core:domain`, `:core:data`, callers | ⬜ |
| Wave 1 | T1 oracle tests, written from this doc: `ColorSchemeContrastTest`, the `dynamicColor` → `BRAND` migration test | test files only | ⬜ |
| Integrate 1 | apply, `spotlessApply`, gate | | ⬜ |
| Wave 2 | C2 calendar components: cell fills, weekend tint, 8 dp marks, placeholder pill, Year tile cards, intercalary fill restored, holiday diamonds in Year | `:core:designsystem` | ⬜ |
| Wave 2 | D2 calendar screens: Today hero, Month anchoring and selected-day summary, single title with chevron, Year padding, Day intercalary header | `:feature:calendar` | ⬜ |
| Wave 2 | E2 events: grouped list, one date line, chips, editor states, colour swatch row, category control | `:feature:events` | ⬜ |
| Wave 2 | F2 converter result card and swap icon; holidays rows and pack dots | `:feature:converter`, `:feature:holidays` | ⬜ |
| Wave 2 | H2 Settings Appearance section, palette and theme wiring, system bars follow `ThemeMode` | `:feature:settings`, `:app` | ⬜ |
| Integrate 2 | apply, gate, `assembleDebug` | | ⬜ |
| Wave 3 | I3 widgets follow the app theme, widget appearance settings, Month widget fills, large Today content | `:widget` | ⬜ |
| Wave 3 | J3 intro and Learn grid illustrations, Settings preview strip | `:feature:settings` | ⬜ |
| Wave 3 | K3 documentation sweep: README, CHANGELOG, ARCHITECTURE, FEATURES, ROADMAP, KDoc audit | docs | ⬜ |
| Integrate 3 | gate, `assembleDebug`, install and check on a device or emulator | | ⬜ |
| Deferred | screenshot goldens (owner, CI only); holiday pack colours §5.5; the unfamiliar-user test | | ⏸ |
