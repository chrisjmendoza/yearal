# Play Store listing — text and asset plan

ROADMAP M8 T4. Written for two audiences: **the owner**, who pastes this straight into Play Console
fields, and **a prospective user**, reading the listing before installing. Every factual claim here is
checkable against `README.md`, `docs/FEATURES.md`, `docs/competitive-analysis.md`,
`docs/play-data-safety.md`, `docs/security-and-privacy.md` and `docs/ROADMAP.md` — nothing here invents a
feature, an award, or a user count.

Status: **not submitted.** This is copy and a plan, not a filled-in Console form. See the "Before
submitting" checklist (§8) for what the owner still has to do.

---

## 1. App name (30-character limit)

**"Yearal: 13-Month Calendar"** — the title decided in ROADMAP.md open decision #1 (2026-09-18) and
already checked for Play/iOS/domain/trademark collisions in `docs/competitive-analysis.md` §8.

| Title | Length | Limit | Fits? |
|---|---|---|---|
| `Yearal: 13-Month Calendar` | **25 characters** | 30 | Yes, 5 to spare |

No alternative is proposed — the name is decided and the availability research (§8 of the competitive
analysis) is already done. The one thing still **owed by the owner**, per that decision: register
`yearal.com` / `yearal.app` and run a manual USPTO/EUIPO search before the first upload.

---

## 2. Short description (80-character limit)

Three alternatives, each counted. All three lead with a documented review complaint from
`docs/competitive-analysis.md` §3.1/§6 rather than an adjective.

| # | Text | Length |
|---|---|---|
| A | `IFC calendar: today's date, a widget you can trust, and events on Sol 13.` | **73** |
| B | `The 13-month International Fixed Calendar, done properly. No ads, ever.` | 71 |
| C | `A 13-month calendar with a real widget, converter and events. No ads.` | 69 |

**Recommended: A.** It names "IFC" for search (the app title deliberately doesn't, per the competitive
analysis's naming notes — "leading with IFC is bad ASO... keep it in the short description instead"), and
it packs in the top three documented asks against the incumbent in one line: a trustworthy date/widget, a
converter, and recurring events on an IFC date ("Sol 13") — a capability no Gregorian calendar app has.

---

## 3. Full description (4000-character limit)

Two versions of the same copy: **Markdown** (for reading in this repo) and **plain text** (what actually
goes in the Play Console field — Play strips Markdown formatting, so the plain-text version has no `**`
or `-` bullet syntax beyond a hyphen that reads fine unformatted).

Coverage check against the brief: leads with what the calendar is and who it's for; one paragraph on the
IFC-weekday-vs-actual-weekday confusion (the thing every first-time user gets wrong, per
`docs/competitive-analysis.md` §3.1 "Align weekdays with the real-world week" and the owner's own ruling
in `docs/design-plan.md` §4.1); every bullet is a feature from README's "What works today"; a privacy
paragraph; and a "what it's not" paragraph that only uses "yet" for the two items the release map in
`docs/ROADMAP.md` actually schedules (device-calendar overlay → 1.1, `.ics` → 1.2) and says plainly,
without a release commitment, that multiple reminders per event and per-occurrence edits (FEATURES E11,
unscheduled 🔵) aren't there. Sol and the two floating days are named plainly in the second paragraph, not buried.

### Markdown version

```markdown
Yearal brings the International Fixed Calendar (IFC) to Android.

**Who it's for:** not just calendar-reform enthusiasts who already know the words "Cotsworth" and
"Eastman", but anyone who wants a genuinely 28-day month, a converter that answers "what would today be
under a different calendar?", and a widget that actually works.

**What the IFC is:** 13 months of exactly 28 days each, plus a 13th month called **Sol** between June and
July, and two extra days that belong to no week at all: **Year Day** (after December 28) and, in leap
years, **Leap Day** (after June 28). The year number and January 1 line up with the ordinary Gregorian
calendar; only the months in between are rearranged.

**The one thing everyone gets wrong at first:** because every IFC month starts on a Sunday, the IFC's own
weekday for a date and the real-world weekday you'd tell a friend are usually different days. Yearal
never shows one without the other — a labelled "IFC weekday" and a labelled "actual weekday" appear
together on Today, on the month grid, and on every day's detail, so it's never possible to mistake one
for the other.

**What's in Yearal:**

- Today, Month and Year views. Today shows both weekdays, day of year, week number and a countdown to
  the next Year Day or Leap Day. The month grid swipes through all 13 months with the Leap Day / Year Day
  band shown in place. The year view shows all 13 months at once.
- Tap any day for its Gregorian equivalent, both weekdays, and its holidays and events, right there under
  the grid.
- A two-way Gregorian to IFC date converter for any year from 1583 to 9999, with copy and share.
- Events with reminders, including recurrence no Gregorian-only calendar app can offer — "every Sol 13",
  "every Year Day", "every Leap Day" — alongside ordinary weekly and Gregorian recurrence. Reminders
  still arrive in Doze, hide the event's title on the lock screen, and can be snoozed for ten minutes or dismissed
from the notification.
- Built-in holidays: the IFC's own observances (Year Day, Leap Day, Sol 1) and a US federal and
  observances pack, shown on the grid, in the day detail, and in a dedicated Holidays screen.
- Today and Month home-screen widgets that follow your chosen colour palette, refresh at midnight on
  their own, and keep working after a reboot or an app update.
- A skippable first-run introduction and a Learn section covering the calendar's rules, its history, and
  the most common points of confusion.
- Six colour palettes, a pure-black dark mode, and Material You support on Android 12 and up.

**Privacy:** Yearal has no internet permission at all, anywhere in the app, so it cannot send anything to
anyone. There are no ads, no analytics, no crash reporting, and no account — nothing to sign into and
nothing to opt out of. Everything typed into the app stays in its own storage on the device; the one copy
that can ever leave the phone is Android's own encrypted device backup, which the user controls. "Delete
all data" is one tap away in Settings.

**What Yearal doesn't do yet:** it doesn't show or sync with the phone's regular calendar app, and it
doesn't import or export `.ics` files — both are on the roadmap for upcoming releases. An event has one
reminder, not several. There's also no agenda widget yet; that one is planned too, alongside the widget
privacy mode it needs first.

Yearal follows the International Fixed Calendar as devised by Moses Cotsworth and championed by George
Eastman of Kodak — one specific 13-month proposal, not the only one, and not a lunar calendar.
```

### Plain-text version (paste this into Play Console)

```
Yearal brings the International Fixed Calendar (IFC) to Android.

Who it's for: not just calendar-reform enthusiasts who already know the words "Cotsworth" and "Eastman",
but anyone who wants a genuinely 28-day month, a converter that answers "what would today be under a
different calendar?", and a widget that actually works.

What the IFC is: 13 months of exactly 28 days each, plus a 13th month called Sol between June and July,
and two extra days that belong to no week at all: Year Day (after December 28) and, in leap years, Leap
Day (after June 28). The year number and January 1 line up with the ordinary Gregorian calendar; only the
months in between are rearranged.

The one thing everyone gets wrong at first: because every IFC month starts on a Sunday, the IFC's own
weekday for a date and the real-world weekday you'd tell a friend are usually different days. Yearal
never shows one without the other - a labelled "IFC weekday" and a labelled "actual weekday" appear
together on Today, on the month grid, and on every day's detail, so it's never possible to mistake one
for the other.

What's in Yearal:

- Today, Month and Year views. Today shows both weekdays, day of year, week number and a countdown to
the next Year Day or Leap Day. The month grid swipes through all 13 months with the Leap Day / Year Day
band shown in place. The year view shows all 13 months at once.
- Tap any day for its Gregorian equivalent, both weekdays, and its holidays and events, right there under
the grid.
- A two-way Gregorian to IFC date converter for any year from 1583 to 9999, with copy and share.
- Events with reminders, including recurrence no Gregorian-only calendar app can offer - "every Sol 13",
"every Year Day", "every Leap Day" - alongside ordinary weekly and Gregorian recurrence. Reminders still
arrive in Doze, hide the event's title on the lock screen, and can be snoozed for ten minutes or dismissed
from the notification.
- Built-in holidays: the IFC's own observances (Year Day, Leap Day, Sol 1) and a US federal and
observances pack, shown on the grid, in the day detail, and in a dedicated Holidays screen.
- Today and Month home-screen widgets that follow your chosen colour palette, refresh at midnight on
their own, and keep working after a reboot or an app update.
- A skippable first-run introduction and a Learn section covering the calendar's rules, its history, and
the most common points of confusion.
- Six colour palettes, a pure-black dark mode, and Material You support on Android 12 and up.

Privacy: Yearal has no internet permission at all, anywhere in the app, so it cannot send anything to
anyone. There are no ads, no analytics, no crash reporting, and no account - nothing to sign into and
nothing to opt out of. Everything typed into the app stays in its own storage on the device; the one copy
that can ever leave the phone is Android's own encrypted device backup, which the user controls. "Delete
all data" is one tap away in Settings.

What Yearal doesn't do yet: it doesn't show or sync with the phone's regular calendar app, and it doesn't
import or export .ics files - both are on the roadmap for upcoming releases. An event has one reminder,
not several. There's also no agenda widget yet; that one is planned too, alongside the widget privacy
mode it needs first.

Yearal follows the International Fixed Calendar as devised by Moses Cotsworth and championed by George
Eastman of Kodak - one specific 13-month proposal, not the only one, and not a lunar calendar.
```

| Version | Character count | Limit |
|---|---|---|
| Plain text (the one that counts for the Console field) | **3,515** | 4,000 |
| Markdown (this doc only; not pasted anywhere) | 3,585 | — |

---

## 4. Screenshot plan

### Play's current rules (from memory — **verify against the current Play Console help page before
uploading**; Google has changed these numbers before)

- **Phone screenshots:** minimum 2, maximum 8. Each side between 320 px and 3840 px. Historically Play
  asked for a 16:9 or 9:16 aspect ratio; recent Console versions accept a wider range and auto-crop for
  the store listing preview, so treat "16:9/9:16" as a safe target rather than a hard rule until checked.
- **Tablet screenshots (7-inch and 10-inch):** required if the listing opts into tablet distribution
  (which it should — the app has real dual-pane tablet/foldable layouts, ROADMAP M3 T4). Play has, at
  different times, required at least one screenshot per tablet size class and, separately, offered to
  generate tablet previews automatically from phone screenshots if none are supplied. **Do not rely on
  memory for this one** — confirm the current minimum count and whether auto-generation is still offered
  before deciding whether dedicated 7"/10" captures are mandatory or merely recommended.
- No screenshot may contain device frames, badges ("Editor's Choice", star ratings) or promotional text
  overlays per Play's metadata policy.

### What can be produced from Roborazzi vs what needs a device

`docs/screenshots.md` describes exactly one pipeline: every `@Preview` in **`:core:designsystem`** is
captured by Roborazzi's Compose preview scanner. That covers individual components (`MonthGrid`,
`DayCell`, the hero card style, date pickers) rendered in isolation at test resolution — it does **not**
cover a full app screen with its app bar, bottom navigation, status bar, or a widget sitting on a home
screen. None of the seven screenshots below can be produced by `compareRoborazziDebug` or the CI
`recordRoborazziDebug` job as they stand; every one needs a real device or emulator capture through the
running app (or, for the widgets, a launcher with the widget placed). The `MonthGrid`/hero-card previews
are still useful as a fast visual sanity check before setting up the device capture, since they show the
same component the full screen will use.

### The table

All dates are set via the app's fake/injectable `Clock` in a debug build (never a real device's clock —
CLAUDE.md rule 2), so every capture is reproducible. The two headline examples reuse dates already
verified elsewhere in the docs rather than inventing new ones: Gregorian **Thursday 17 September 2026**
is the machine-checked IFC-weekday-mismatch vector in `docs/calendar-spec.md` §6.4 (IFC September 8,
2026, IFC weekday Sunday), and **Sol 12, 2026** (Gregorian Tuesday 23 June 2026, IFC weekday Thursday) is
the worked example in `docs/design-plan.md` §4.1.

| # | Screen | Date / clock to set | Events / palette to seed | Caption (≤ few words) | Source |
|---|---|---|---|---|---|
| 1 | Today | Thu 17 Sep 2026 (IFC weekday Sunday) | Teal palette (default), light mode; one holiday, one event on Today's agenda | "Today, in both calendars." | Device capture — no Roborazzi full-screen preview exists |
| 2 | Month — Sol | Selected day Sol 12, 2026 (Tue 23 Jun 2026) | Teal palette; a recurring "every Sol 13" event visible as a dot | "Every month, 28 days. Even Sol." | Device capture; `MonthGrid` preview is a useful pre-check |
| 3 | Month — December, Year Day selected | Selected day Year Day 2026 (Thu 31 Dec 2026) | Teal palette; the IFC-native "Year Day" holiday showing in the day card | "Year Day belongs to no week." | Device capture; `IntercalaryBand` preview is a useful pre-check |
| 4 | Year overview | Any date in 2026 (e.g. today's real date) | Teal palette; a couple of months with event/holiday marks visible | "All 13 months, at a glance." | Device capture |
| 5 | Converter | Gregorian 4 Jul 1976 → IFC | N/A (converter has no events) | "Find your IFC birthday." | Device capture |
| 6 | Events list / editor | Any date; one event with IFC recurrence | An event titled generically (no personal data), recurrence "every Sol 13", category chip shown | "Repeats every Sol 13." | Device capture |
| 7 | Home screen with both widgets placed | Thu 17 Sep 2026 (matches #1, so the widget and the app agree) | Teal palette on both widgets | "A widget you can trust." | Device capture; launcher, not the app |

Seven screenshots, within the 2–8 range. Recommend also capturing #2 (Month, Sol) and #6 (Events) again
in the tablet dual-pane layout for the 7"/10" tablet sets once that requirement is confirmed (§4 rules
above) — the same dates, no new state needed, since the dual-pane layout (M3 T4) reuses the same screens.

Use a distinct, non-personal placeholder for every event title and note in these captures — no real event
content should ever appear in a published screenshot (CLAUDE.md rule 8 extends naturally to marketing
assets even though it's written for logs).

---

## 5. Feature graphic (1024 × 500 PNG, required)

A brief the owner can hand to a designer, or build directly from the existing brand assets in
`docs/brand/README.md` (`yearal-icon.svg`, `yearal-icon-512.png`).

- **Colours:** teal `#123F3D` (background or dominant field), cream `#F4ECDA` (the grid/glyph), accent
  orange `#F28C28` (used sparingly — the Year Day dot, or the tagline). These are the exact three hex
  values `docs/brand/README.md` names for the launcher icon; do not introduce a fourth brand colour here.
- **Glyph:** the "perfect month" — the same 28-day, 4×7 dot grid with a small pill/dot beneath it for
  Year Day, already used as the launcher icon and, per `docs/brand/README.md`, avoid a bitmap re-render;
  either export the existing `yearal-icon.svg` at the right size/crop, or reproduce it vector-for-vector
  so the feature graphic and the launcher icon are visibly the same mark.
- **Layout:** graphic is 1024×500 (roughly 2:1) — the square icon glyph doesn't fill that shape well, so
  place the dot-grid glyph left- or center-aligned on a teal field with generous cream margin, and set the
  wordmark "Yearal" plus (optionally) one tagline to its right or beneath it. Leave the outer ~10% of the
  canvas free of text/logo — Play's device frames in some placements crop the edges.
- **Tagline options (≤ 6 words each):**
  - "13 months. 28 days. Exactly."
  - "The calendar that finally fits."
  - "No ads. No lunar confusion."
  - "13-Month Calendar, done properly."
  - "Every month, the same size."
- Do **not** put a screenshot, a rating, or "New" inside the feature graphic — Play's metadata policy
  disallows badges/ratings in graphic assets, and the app has no rating yet to show truthfully anyway.

---

## 6. Other required Console fields

| Field | Value | Basis |
|---|---|---|
| **Category** | **Productivity** (recommended over Tools) | The direct incumbent (`erkantr`) lists under Productivity; the only other direct competitor (`Eizuberg`) is under Tools and is the weaker, ad-supported listing (`docs/competitive-analysis.md` §2a). Yearal is a scheduling/events app with reminders, which is squarely Productivity, not a utility. |
| **Tags / search terms** | "international fixed calendar", "IFC calendar", "13 month calendar", "Cotsworth calendar", "Sol calendar", "perpetual calendar" | Descriptive terms already used across `docs/competitive-analysis.md` §8's naming research; no invented buzzwords. |
| **Contact email** | `chrisjmendoza@gmail.com` | Owner's email. |
| **Privacy policy URL** | **Placeholder — not live yet.** Points at `docs/privacy-policy.md` once it is published to GitHub Pages (ROADMAP M2 T12 owner step). Do not submit any Play track before this URL resolves. | `docs/security-and-privacy.md` §5.2 "Privacy policy" row; ROADMAP M2 T12. |
| **Content rating (IARC questionnaire)** | Answer per `docs/play-data-safety.md` §5 — should land at the lowest tier in every region (no violence, sexual content, profanity, UGC sharing, purchases, or location data). | `docs/play-data-safety.md` §5. |
| **Target audience and content** | Not designed for or directed at children. Select only the adult/general age ranges in the target-audience flow; do not opt into the "designed for children" declaration — nothing in the app is built or reviewed for COPPA/Ads-for-Kids compliance. | Inferred from the absence of any child-directed design or policy work in the repo; no doc claims otherwise. |
| **Ads** | **No** — the app contains no ad SDK. | CLAUDE.md rule 7; `docs/play-data-safety.md` §3. |
| **App access** | No login of any kind; every feature is reachable without special access or test credentials, so the reviewer needs nothing extra to test the app. | README "Privacy" section; no accounts anywhere in the app. |
| **Government apps declaration** | No. | Not a government app. |
| **Financial features declaration** | No. | No financial functionality anywhere in the app. |
| **Health apps declaration** | No. | No health functionality anywhere in the app. |
| **Data safety** | See `docs/play-data-safety.md` in full — do not re-derive the answers here; that document is the source of record and is designed to be re-verified against the current Play help page at submission time. | `docs/play-data-safety.md` (whole document). |

---

## 7. Release notes for the first internal build (500-character limit)

Derived from `CHANGELOG.md`'s `[Unreleased]` section, in user-facing wording only (no module names, no
task IDs, no architecture).

```
First internal build: Today/Month/Year views with both IFC and real weekdays shown together; a
Gregorian-IFC converter (1583-9999); events and reminders, including recurrence on Sol 13, Year Day and
Leap Day; built-in IFC and US holiday packs; Today and Month home-screen widgets; six colour palettes and
pure-black dark mode; a first-run intro and Learn section. No ads, no accounts, no internet permission.
```

**408 characters** — within the 500-character limit, with room for the owner to add a build number if
Console wants one.

---

## 8. Before submitting — checklist

Cross-referenced against `docs/security-and-privacy.md` §9.1 rows 30–31 and the owner steps still open on
ROADMAP M2 T11 and T12. None of this is done by writing this listing document — it is what remains after
this doc is approved.

- [ ] **(ROADMAP M2 T11)** Generate the offline Play upload key, enroll in Play App Signing, and create
      the app entry in Play Console. `docs/release-builds.md` covers the signing half that's already
      built; the key itself and the Console app are owner actions with no code dependency.
- [ ] **(ROADMAP M2 T12 / security-and-privacy.md §9.1 row 30)** Publish `docs/privacy-policy.md` to
      GitHub Pages, then put the live URL into both the Privacy screen's `privacy_policy_body` string
      **and** the Play Console privacy-policy field. Play requires the policy to exist and resolve before
      any track — including internal and closed testing — can be submitted.
- [ ] **(ROADMAP M2 T12 / security-and-privacy.md §9.1 row 31)** Submit the Data safety form using the
      answers in `docs/play-data-safety.md`, after re-reading that document's own "Before submitting"
      checklist (its §1–2 reasoning is dated 2026-09-17 and needs re-verifying against the current Data
      safety help page). Submit the Advertising ID declaration as "No" in the same pass.
- [ ] **(security-and-privacy.md §5.4)** Submit the exact-alarm permission declaration using the text
      already drafted there, before uploading any build — every build since M6 T3 declares
      `USE_EXACT_ALARM`.
- [ ] **(ROADMAP open decision #4 / security-and-privacy.md §5.2 "Closed-testing gate")** Confirm whether
      the Play developer account is a personal account created after 2023-11-13. If it is, the mandatory
      closed test needs **12 testers opted in for 14 continuous days** before production access —
      budget roughly 3–4 weeks and start recruiting testers before relying on this listing being live
      soon.
- [ ] Run `scripts\check_manifest_permissions.py` against a signed **release** build (not debug) to
      confirm the merged manifest has no `INTERNET`, no `AD_ID`, and matches the allow-list in
      `docs/security-and-privacy.md` §5.1 before this listing is used for a real upload.
- [ ] **(ROADMAP open decision #1, still owed)** Register `yearal.com` / `yearal.app` and run a manual
      USPTO/EUIPO trademark search on "Yearal" before the first upload.
- [ ] Capture the seven screenshots in §4 on a real device or emulator (not from Roborazzi, which only
      renders isolated `:core:designsystem` components) and re-verify the phone/tablet screenshot rules in
      §4 against the current Play Console help page, since both were written from memory.
- [ ] Once every field above is filled and verified, treat this document as the draft and the Console
      form as the source of truth going forward — re-audit this file if the Console's wording or limits
      have visibly changed since 2026-09-26.
