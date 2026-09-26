# Privacy Policy — Yearal

**Version:** 1.0
**Effective date:** YYYY-MM-DD

<!-- source: docs/security-and-privacy.md §5.2 "Privacy policy" row (must be public, non-PDF, versioned; must name the app/developer and give a contact) -->

## Who we are

Yearal is developed by **Chris Mendoza**, an individual developer. If you have a question about this
policy or the app, contact **chrisjmendoza@gmail.com**.

<!-- source: task brief (owner's public GitHub identity); docs/security-and-privacy.md §5.2 "Privacy policy" row -->

## The short version

Your calendar stays on your phone. Yearal has no accounts, no ads, no analytics, and no crash-reporting
service. It has no permission to use the internet at all, so the app itself cannot send anything
anywhere. The only copy of your data that can ever leave your phone is Android's own encrypted device
backup, which is covered below and which you control.

<!-- source: feature/settings/src/main/res/values/strings.xml (privacy_summary_body); docs/security-and-privacy.md §7 "Posture in one paragraph" -->

## What the app stores, and where

Yearal stores your **events, reminders, settings, and the holiday packs you've turned on** — nothing
else. All of it lives only in the app's own private storage on your device. There is no server, so
there is nowhere else for it to live.

<!-- source: feature/settings/src/main/res/values/strings.xml (privacy_data_body); docs/security-and-privacy.md §2.1, §2.4 -->

## No internet access, no tracking

Yearal does not request permission to use the internet, so it cannot connect to any server, ours or
anyone else's. It has no advertising, no analytics or usage tracking, no crash-reporting software, and
no advertising identifier, and it does not use Google Play Services or Firebase. There is nothing for a
third party to collect, because nothing is transmitted.

<!-- source: docs/security-and-privacy.md §7 "Privacy posture" table; CLAUDE.md rule 7; app/src/main/AndroidManifest.xml (no INTERNET permission) -->

## Permissions we ask for, and why

Yearal asks Android for a small, fixed set of permissions, all used only to make your own reminders and
widgets work correctly on your own device:

- **Post notifications** — so a reminder you set on your own event can actually appear. Asked the first
  time you add a reminder, not at launch. If you say no, the rest of the app keeps working; reminders
  are just not shown.
- **Exact alarms** — so a reminder (and the app's midnight date change) arrives at the minute you chose,
  even while the phone is idle, rather than up to ten minutes late.
- **Receive boot completed** — so reminders and the date rollover are re-armed after your phone
  restarts, since a restart otherwise cancels them.
- **Wake lock** — a brief, standard mechanism that lets a home-screen widget finish redrawing itself
  reliably.

Yearal does not ask for your contacts, your location, your photos or files, your microphone or camera,
your device calendars, or the internet.

<!-- source: feature/settings/src/main/res/values/strings.xml (privacy_permission_boot/wake/notifications/exact_alarm/none); docs/security-and-privacy.md §5 allow-list table and §5.1 -->

## Device backup and moving to a new phone

If you have Android's device backup turned on with a screen lock set, your events and settings may be
included in an encrypted backup to your Google account. Google's documentation states this backup is
end-to-end encrypted with your own screen-lock credential on Android 9 and later, so Google itself
cannot read it. Yearal only allows this backup when the device can encrypt it this way; with no screen
lock, no cloud backup of Yearal's data occurs. The same data can also be copied directly between two
devices during a device-to-device transfer, which you initiate and control.

You can turn cloud backup off any time in your device's system settings — Yearal has no separate switch
for it.

<!-- source: docs/security-and-privacy.md §4.1 "Android Auto Backup & device-to-device transfer"; app/src/main/res/xml/data_extraction_rules.xml; feature/settings/src/main/res/values/strings.xml (privacy_backup_body) -->

## Notifications and your lock screen

A reminder notification shows only the event's time on your lock screen by default — never the title,
notes, or location — unless your phone is unlocked or you've chosen to show sensitive notification
content in your device's own settings.

<!-- source: docs/security-and-privacy.md §3.3 "Notifications"; feature/settings/src/main/res/values/strings.xml (privacy_permission_notifications) -->

## Home-screen widgets

Yearal's home-screen widgets show only calendar dates — never an event's title, notes, or location.

<!-- source: docs/security-and-privacy.md §3.2 "Widget privacy mode" ruling on the Month widget's dots; widget/src/main/AndroidManifest.xml (TodayWidgetReceiver, MonthWidgetReceiver) -->

## Sending feedback

"Send feedback" in the More screen opens your own email app with a message addressed to the developer.
The message is prefilled with the app version, your Android version and device model, the app's language,
and your Yearal display settings — never your events, reminders or holiday choices. You see the whole
message before you send it, and nothing is sent unless you do.

<!-- source: docs/security-and-privacy.md §6.3 "Send feedback" email; feature/settings FeedbackBody.kt -->

## Deleting your data

Settings has a **"Delete all data"** action that erases your events, reminders, and settings from the
app. Uninstalling the app removes everything else it stored. Because nothing is ever sent off your
device, there is no separate copy for us to delete on request — deleting it on your device is deleting
it everywhere it exists.

<!-- source: feature/settings source (delete-all-data action in Settings); docs/security-and-privacy.md §2.4 "Delete all data" action -->

## Children's privacy

Yearal is not directed at children and does not knowingly collect information from anyone, regardless
of age, because it does not collect information from anyone at all.

<!-- source: docs/security-and-privacy.md §7 outline item 12 -->

## Changes to this policy

If this policy changes, we'll update the effective date above and the version number. The history of
every change is public in this project's GitHub repository.

<!-- source: docs/security-and-privacy.md §7 outline item 14 -->

## Contact

Questions about this policy: **chrisjmendoza@gmail.com**.

<!-- source: task brief; docs/security-and-privacy.md §5.2 "Privacy policy" row -->
