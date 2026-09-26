# Play Console — Data safety form answers

For the owner, filling in **Play Console → App content → Data safety**. Walks the form's actual
sections in the order Play presents them. Each answer names the source it's traceable to, so it can be
re-verified against the code and the security doc at each release. Re-verify the whole page against the
current Play Console UI before submitting — Google reorders and renames sections between releases.

---

## 1. Data collection and security — overview questions

### "Does your app collect or share any of the required user data types?"

**Answer: No.**

Play's own definition is the one that matters here, and it is narrower than "the app touches data
you'd call personal":

> "Collect" means transmitting user data off the device — or, if the data is stored only on the
> device, providing a way for it to be sent off the device (e.g. through an SDK or API) — to a
> destination outside the mobile device the app is running on.

Yearal has no `INTERNET` permission (`app/src/main/AndroidManifest.xml`; CLAUDE.md rule 7), so nothing
in the app — its own code or any dependency — is capable of transmitting anything off the device. All
processing of events, reminders, settings, and holiday-pack choices happens only in the app's private
storage on the device. That is on-device processing, not "collection" under Play's definition, so it
does not populate the data-types matrix below at all.

**Re-verify against:** <https://support.google.com/googleplay/android-developer/answer/10787469?hl=en>
(the exact URL cited in `docs/security-and-privacy.md` §5.2) before every submission — Google has
tightened this definition before and may again.

<!-- source: docs/security-and-privacy.md §5.2 "Data safety form" row; §7 "Privacy posture" -->

### Android Auto Backup — does this count as "sharing"?

**No, and don't declare it.** Auto Backup is a platform mechanism Google itself operates, not something
the app's own code initiates as a transmission to a third party for the app's purposes; it is the kind
of OS-level, user-controlled backup Play's guidance treats as outside the app's own data collection and
sharing practices. `docs/security-and-privacy.md` §5.2 flags this exact point as *not verified as an
explicit carve-out* in Google's help text — re-read the current Data safety help page's wording on
backups specifically before relying on this for submission, since the carve-out is inferred, not quoted
from a policy sentence.

<!-- source: docs/security-and-privacy.md §5.2 Data safety row; §4.1 Auto Backup section -->

### Security practices section

| Question | Answer | Why |
|---|---|---|
| Is all user data encrypted in transit? | **N/A — no data is transmitted.** If the form forces a yes/no here, answer based on the fact that nothing leaves the device, so there is no "in transit" leg to encrypt or fail to encrypt. | No `INTERNET` permission; nothing is sent anywhere (`app/src/main/AndroidManifest.xml`; CLAUDE.md rule 7). |
| Do you provide a way for users to request that their data be deleted? | **Yes — an in-app "Delete all data" action, plus uninstalling the app.** There is no server copy to separately request deletion of. | `docs/security-and-privacy.md` §2.4 "Delete all data" action; §7 outline item 11. |
| Has your app had an independent security review? | **No.** | Not undertaken; do not claim one. |

<!-- source: docs/security-and-privacy.md §2.4, §7 -->

---

## 2. Data types matrix

Because the overview answer is "No data collected or shared," the per-category matrix (Location,
Personal info, Financial info, Health and fitness, Messages, Photos/videos, Audio, Files/docs,
Calendar, Contacts, App activity, Web browsing, App info and performance, Device or other IDs) should
be left with **nothing selected** — every category is "not collected" because nothing is collected.

One category worth calling out specifically: **"Calendar"** as a data type in Play's schema refers to
data that leaves the device (synced to a server, shared with another party). Yearal's events and
reminders never leave the device, so this box stays unchecked even though the app's whole purpose is a
calendar. Re-read Play's data-type definitions page if unsure, since this is the one place a reviewer
is most likely to double-take.

<!-- source: docs/security-and-privacy.md §5.2 Data safety row (calendar events "a listed data type only if it leaves the device") -->

---

## 3. Advertising ID declaration

**Answer: No, the app does not use advertising ID.**

Justification: no ad SDK, no analytics SDK, and no dependency that would pull in
`com.google.android.gms.permission.AD_ID` (`docs/security-and-privacy.md` §5.1 permission inventory,
last row; §7 "Ads / ad SDKs: None, ever"). Before submitting, confirm `AD_ID` is absent from a release
merged manifest — the same manifest `scripts/check_manifest_permissions.py` checks; if any dependency
ever adds it, strip it with `tools:node="remove"` per the doc's guidance rather than declaring "Yes."

<!-- source: docs/security-and-privacy.md §5.1 permission inventory; §5.2 "Advertising ID declaration" row -->

---

## 4. Exact alarm permission declaration

This is a separate Play Console page (**App content → "Exact alarm permission"**), not part of the Data
safety form, but it gates the same release train and is easy to forget alongside it.

**Use the text already drafted for this in `docs/security-and-privacy.md` §5.4 verbatim** — it was
written to match exactly what `:core:scheduling` does (a single `setExactAndAllowWhileIdle` alarm for
the next reminder, with a `canScheduleExactAlarms()`-gated windowed fallback) and is already reviewed
against the manifest (`android.permission.USE_EXACT_ALARM`, `android.permission.SCHEDULE_EXACT_ALARM`
with `maxSdkVersion="32"` in `core/scheduling/src/main/AndroidManifest.xml`). Complete this before the
first upload of any build that declares the permission — every build since M6 T3 does.

<!-- source: docs/security-and-privacy.md §5.4 "Play Console exact-alarm declaration — text to submit" -->

---

## 5. Content rating questionnaire

As far as the doc and code support an answer:

| Question area | Answer | Basis |
|---|---|---|
| Violence, blood/gore | None | This is a calendar app; no such content exists anywhere in it. |
| Sexual content / nudity | None | Same. |
| Profanity / crude humor | None | All user-visible strings are the app's own UI text (CLAUDE.md rule 9); no user-generated or third-party content is displayed by the app itself. |
| User-generated content shared with other users | **No** — there is no sharing of any kind. Events and notes a user types stay on their device; there is no feature that publishes, syncs, or shares user content to any other user or the public. | No `INTERNET` permission; no accounts; no backend (`docs/security-and-privacy.md` §7, §1). |
| Digital purchases / gambling | None | No in-app purchases, no monetization of any kind exist in the app as built. |
| Shares location | No | No location permission requested (`docs/security-and-privacy.md` §5.1). |
| Controlled substances | Not applicable | No such content. |

This should land the app at the lowest available rating tier in every region's rating body. Answer any
question the doc doesn't cover conservatively ("no"/"none") and re-check against the actual questionnaire
wording, since Google's phrasing varies by region and changes over time.

<!-- source: CLAUDE.md rule 9; docs/security-and-privacy.md §1, §5.1, §7 -->

---

## Before submitting — checklist

- [ ] Re-read the current Data safety help page and data-types definitions page against §1–§2 above; the
      "no collection" and "Calendar only counts if it leaves the device" reasoning is Google's
      *current* wording as of the security doc's last verification (2026-09-17) — confirm it still holds.
- [ ] Confirm the release merged manifest has no `INTERNET`, no `AD_ID`, and matches
      `docs/security-and-privacy.md`'s permission allow-list (run `scripts/check_manifest_permissions.py`
      against a release build, not just debug).
- [ ] Complete the exact-alarm declaration (§4 above) before uploading a build that declares
      `USE_EXACT_ALARM`/`SCHEDULE_EXACT_ALARM` — every build since M6 T3.
- [ ] Have the hosted privacy policy URL (`docs/privacy-policy.md` on GitHub Pages) live and entered in
      Play Console's privacy policy field, and the in-app Privacy screen's "full policy" text updated
      with the same link, before submitting this form — Play requires the policy to exist first.
- [ ] Submit the advertising-ID declaration as "No."
- [ ] Save a dated screenshot or export of the completed form for `docs/security-and-privacy.md`'s
      records — the doc's acceptance checklist (§9.2 item 11) expects this to be done, not just claimed.

## Re-audit when 1.3 (URL subscriptions) lands

The moment `INTERNET` is added to the manifest for URL subscriptions (v1.3), **every answer in this
document changes**: the overview question becomes "Yes, we collect/transmit data" in some form (at
minimum, the subscription URL is sent to a third-party server, and that server sees the device's IP
address), the Calendar data-type row may need to be checked, "encrypted in transit" needs a real answer
(HTTPS-only, per `docs/security-and-privacy.md` §6.2), and the privacy policy text describing "no
internet access" must be rewritten (see `docs/privacy-policy.md`'s "No internet access, no tracking"
section and the outline's item 6 in `docs/security-and-privacy.md` §7, which already anticipates this).
Treat this whole document as stale the day that permission is declared, not just the policy.

<!-- source: docs/security-and-privacy.md §5.1 INTERNET row; §6.2 URL subscriptions; §7 outline item 6 -->
