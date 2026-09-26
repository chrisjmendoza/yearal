# Device test matrix

Status: **started at M6 T1 (2026-09-19); nothing here has been run yet.** These are the checks the automated
gate cannot make (Robolectric does not enforce exported receivers, Doze, launchers, the lock screen or OEM
battery management). [ROADMAP.md](ROADMAP.md) M5 T8 owns this file; tick a row with the device, Android
version and date when it passes. Target coverage: API 26, 33 and 36, plus one Samsung or Xiaomi device.

## Date correctness and the widgets (M5 T1–T3, T5–T6)

| # | Check | Result |
|---|---|---|
| W1 | Place the Today widget at each of its three sizes and the Month widget at both; the lines shown match the size. | 🟡 2026-09-19, Samsung, build `6175108`: the Today widget (2×1) reads well. The Month widget resized tall keeps the grid in the top third and leaves the rest empty — correct but wasteful; tracked as a design item in ROADMAP M2 T13. |
| W2 | Dynamic colour on API 31+, the brand palette below; light and dark. | |
| W3 | Tapping either widget opens the app. | |
| W4 | With the app force-closed, wait past midnight (or change the device date): both widgets show the new date. | |
| W5 | Change the device time zone across a date line: both widgets follow. | |
| W6 | Reboot: the widgets are right after unlock. | |
| W7 | Add, edit and delete an event: the Month widget's dot appears or disappears within about a second, also after a burst of quick edits. | |
| W8 | An event on Year Day and one on Leap Day (2028) mark the band. | |
| W9 | Widget picker previews on API ≤ 30, 31–34 and 35+. | ❌ 2026-09-19, Samsung (One UI), build `6175108`: both entries showed only a loading spinner — that build's `previewLayout` was the loading layout. M5 T5 (`ed1aa22`) replaces it with static mock-ups; **re-check on the same phone with a build from `ed1aa22` or later**, and if the spinner persists, investigate the Samsung launcher's handling of `previewLayout` / `previewImage`. |
| W10 | TalkBack on the Month widget: swipe through the grid and confirm each of the 28 day cells is announced with its own short description (`Sol 13`, `Sol 13, today`, `Sol 21, holiday, has events`) rather than a bare digit, and that the title/header block above the grid is still one separate stop with the merged month/today description (ROADMAP M8 T1, accessibility audit finding #1). Note whether any cell is announced twice (once for the cell, once for a child number) — unit tests cannot see whether Glance/RemoteViews merges child `Text`s into the cell's own node on a real device. | |
| W11 | logcat shows no WorkManager complaint about the removed `ACCESS_NETWORK_STATE`. | |
| W12 | Set each widget's own appearance override (Settings → Appearance → widget theme) to Light and to Dark while the app itself is on System/the opposite mode: the widget follows its own override, not the app, on both home screen placements. Set both back to "Follow app" and confirm they track the app's `ThemeMode` (including System) again. Also try a non-`TEAL` palette and Material You: both widgets pick it up after the debounced refresh (within a couple of seconds of leaving Settings), without a manual widget re-add. | |
| W13 | Transparent-widget contrast (design-plan §5.6): set the widget background opacity to 0%, 40% and 100% on a busy home-screen wallpaper, for both widgets, light and dark. Below 50% the Today widget's date text, and the Month widget's title/Gregorian-span/header-row block, should each sit on their own solid chip (ROADMAP M8 T1, accessibility audit finding #16 added the Month widget's); the Month widget's day numbers and marks should stay readable regardless, because each cell keeps its own opaque fill even when the surrounding widget background is transparent. Confirm nothing is unreadable at 0%. Robolectric's contrast test does not cover this (`ColorSchemeContrastTest` only asserts fixed role pairs, not a wallpaper showing through). | |
| W14 | Resize the Today widget down to its new minimum (accessibility audit finding #15) and confirm with a layout inspector or a ruler overlay that the whole widget measures at least 48dp tall. For the Month widget (finding #14), the declared minimum (320dp) is a known-short 43dp per column, not 48dp — that trade-off is deliberate (see `month_widget_info.xml`'s own comment: 352dp would clear 48dp but does not fit a 360dp-wide phone's launcher grid, so 320dp was chosen to stay placeable). Confirm instead that the widget **can be placed at all** on a 360dp-wide phone at its declared minimum, and separately resize it up to a six-cell-or-larger width and confirm a column then measures at least 48dp. The gate's `MonthWidgetInfoTest`/`TodayWidgetInfoTest` only prove the declared XML attributes, not what a real launcher actually renders at either size. | |
| W15 | Widget-picker preview at 200% system font scale (Settings → Accessibility → Display size and text → Font size, or `adb shell settings put system font_scale 2.0`): open the widget picker and confirm the Month widget's static preview (`month_widget_preview.xml`) does not clip or overlap its day-cell digits. Not shown to clip from reading the XML alone (accessibility audit finding #29: no `maxLines`/`ellipsize` is set, so a `wrap_content`-height `TextView` wraps rather than truncates, but the picker renders this layout into a fixed-size thumbnail whose real dimensions are launcher-controlled and not reproducible under Robolectric) — **verify on device**; if it does clip, cap the day-cell `TextView`s with `android:maxLines="3"` plus `android:autoSizeTextType="uniform"` (or a fixed smaller `textSize`) in `res/layout/month_widget_preview.xml`. | |

## Live date and zone (R1)

| # | Check | Result |
|---|---|---|
| L1 | With Today, Month (and its day card) or Year open, change the system time zone across a date line: the date and the agenda update within a second or two, without leaving the screen. | |
| L2 | Background the app, change the time zone or the date, return: the same, through the resume path. | |
| L3 | Set the clock backwards across midnight with a screen open: the earlier date is shown. | |
| L4 | The Convert tab icon reads as "swap", not "refresh", in light and dark, on the bar and on the rail. | |

## Reminders (M6 T1, T3)

| # | Check | Result |
|---|---|---|
| R1 | A timed event a few minutes out with a 1-minute reminder fires within a minute. | |
| R2 | Doze: `adb shell dumpsys deviceidle force-idle`, then a reminder a few minutes out still fires on time; `adb shell dumpsys deviceidle unforce`. | |
| R3 | Lock screen with sensitive content hidden: "Event reminder" and the time, **no title**. | |
| R4 | Notifications denied: nothing crashes; after re-enabling, the next reminder appears. | |
| R5 | First reminder chip on Android 13+ asks for the notification permission once; denying never blocks saving. | |
| R6 | Reboot with a reminder ~5 minutes out: it arrives shortly after unlock. One 2 hours past stays silent. | |
| R7 | Change the time zone, and set the clock forward and back, with a reminder armed: the next one is still right. | |
| R8 | An all-day event tomorrow reminds at 09:00 local, not at midnight. | |
| R9 | API 31/32: revoke "Alarms & reminders"; reminders still arrive (up to ~10 minutes late); re-grant restores punctuality without reopening the app. | |
| R10 | Force-stop the app, reopen it: the pending reminder is armed again. | |
| R11 | Samsung / Xiaomi battery management: the app is not put to sleep; note any OEM limit for the in-app help text. | |

## Intent routing (M3 T5)

| # | Check | Result |
|---|---|---|
| N1 | Tap a reminder notification: the app opens that event's editor, not the last-used tab. | |
| N2 | Tap a day cell on the Month widget: the Month opens on that day, selected, with its details in the card below the grid. Tap the widget's heading or a gap: the current month opens. **Unit-tested only — the per-cell click regions were never rendered on a device.** | |
| N3 | Tap the Today widget: the Today tab opens. | |
| N4 | Leave the app in the background until Android kills it, then reopen it from a widget or a notification: it routes correctly, and rotating afterwards does not route a second time. | |
| N5 | At expanded width (tablet or unfolded foldable), a widget or notification tap opens the Month on that day, with the day selected in the right-hand pane — no sheet. | |

## TalkBack (M8 T1)

The accessibility audit of 2026-09-26 (ROADMAP M8 T1) was done by code review; these are the checks
that need a screen reader on a device. TalkBack on, English, default font scale unless a row says
otherwise. Expected text comes from each module's own `strings.xml`; substitute today's real dates.

| # | Check | Result |
|---|---|---|
| T1 | Today: swipe to the hero. The bare weekday above the date (e.g. "Thursday") is spoken as "IFC weekday: Thursday", never as the bare word; on Year Day / Leap Day there is no such line at all. | |
| T2 | Today, further down the hero: a line spoken as "Actual weekday: <today's real weekday>", distinct from T1. | |
| T3 | Today: the year-progress bar and its "N% of the year" caption are one spoken node — you hear the percentage once, not twice in a row. | |
| T4 | Today: the Holidays and Today's events cards read either their quiet empty line or one merged sentence per row. | |
| T5 | Month: the header rows announce "IFC weekdays" then the abbreviated names, then "Actual weekdays" and its row. | |
| T6 | Month: focus a day cell. One sentence ("Sol 13, IFC Friday. Gregorian Tuesday, June 30, 2026. 2 events. Holiday: …", with "Today." on the current date) and **no stray bare number** spoken after it. Repeat on the Year Day / Leap Day band and on a Year-overview mini-month tile. | |
| T7 | Month: the Year Day / Leap Day band reads "no IFC weekday, actual <weekday>", never an unlabelled weekday. | |
| T8 | Month day card: an event row is announced as a button and is comfortably tappable (≥48dp); TalkBack's actions menu on it offers "Delete this occurrence" / "Delete event" without a real long-press. | |
| T9 | Converter: a date on Year Day or Leap Day reads "no IFC weekday, actual <weekday>"; changing the input to a new date announces the new result on its own (polite live region) without swiping back to it. | |
| T10 | Event editor: Back and Delete in the top bar, and every dialog button, are easy to hit (≥48dp). Set End before Start and swipe to the End field itself: TalkBack says the field is invalid right there, not only at the banner; a failed save announces its banner. | |
| T11 | Event editor: "Number of times" opens the numeric keyboard. | |
| T12 | Holidays: a set's switch is one merged node ("…, switch, on/off"); Back and the Previous/Next-year buttons are easy to hit. | |
| T13 | Settings and More: every group heading is announced as a heading; every More row (Holidays, Settings, Learn, Privacy, Send feedback) is a full-width button. Intro: switching pages starts at the top of the new page with its heading focused. | |
| T14 | Month widget: each day cell reads "Sol 13", "Sol 13, today", "Sol 21, holiday, has events" — never an event title; the widget root still reads both labelled weekdays. Today widget at its smallest size: the tap target is not cramped. | |
| T15 | 200% font (Settings → Accessibility → Display size and text): Today, Month with its day card, Year, Events, Settings and More show nothing clipped or overlapping; grid cells and buttons stay tappable. | |
| T16 | Reduced motion (Settings → Accessibility → Remove animations): note that the Month/Year pager still animates page changes — recorded as the one unimplemented bullet of ARCHITECTURE §4 "Accessibility" (audit finding #22, deferred). | |

## App

| # | Check | Result |
|---|---|---|
| A1 | "Every Sol 13" and "every Year Day" events appear on the right days, on the grid, in the Month day card and on Today. | |
| A1b | Tap Save several times quickly on a new event: exactly one event is created. | |
| A2 | "Delete this occurrence" removes only that day; undo restores it. | |
| A3 | "Delete all data" empties events, resets settings, clears widget dots and any pending reminder. | |
| A4 | `bmgr` backup → reinstall → restore round trip (security-and-privacy.md acceptance checklist). | |
