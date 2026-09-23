# Screenshot goldens (Roborazzi)

Status: **current as of ROADMAP R6 / M2 T10** (2026-09-23). Owned by [ARCHITECTURE.md](ARCHITECTURE.md)
§6 "Goldens", which this doc expands into a runbook; see that section first for the design (why goldens
are recorded only in CI, why a bot never commits them). **The baseline is still unrecorded** — it was
deliberately deferred past the M2 T13 visual design pass ([design-plan.md](design-plan.md)) rather than
spend an owner step recording goldens that pass would immediately invalidate. Recording it now, against
the post-pass UI, is the sensible first baseline; see ROADMAP.md R6.

Every `@Preview` in `:core:designsystem` is captured as a Roborazzi screenshot by Roborazzi's Compose
preview scanner (`generateComposePreviewRobolectricTests`, configured in
[`core/designsystem/build.gradle.kts`](../core/designsystem/build.gradle.kts)) — there is no
hand-written test per preview. Goldens live under `<module>/src/test/screenshots/`, tracked in git like
any other test fixture (`ifc.android.compose` sets `roborazzi { outputDir }` there; see
[`build-logic/convention/src/main/kotlin/ifc.android.compose.gradle.kts`](../build-logic/convention/src/main/kotlin/ifc.android.compose.gradle.kts)).

## Why there is no local "just record it" command

Robolectric's native-graphics rendering differs between Windows (every local/agent dev machine here) and
Linux (`ubuntu-latest`, what Play and every other Android CI runs on): the same `captureRoboImage` call
produces different pixels. A golden recorded on Windows would never match what CI renders, so **only CI
records goldens**. Locally and in agent sessions, the closest you get is a diff report — see
"Reviewing a local diff" below.

## Recording the first baseline, or updating goldens after a UI change

1. **Run the workflow.** On GitHub, Actions → "Record screenshots" → "Run workflow", choosing the
   branch with the UI change (or `main` for a from-scratch baseline). It runs
   `./gradlew recordRoborazziDebug` on `ubuntu-latest`, which writes fresh images straight into each
   module's `src/test/screenshots/` inside the runner's checkout — it does not compare against anything
   already committed.
2. **Check the run summary** for the "Recorded screenshot goldens" table (image count per module) to
   sanity-check nothing silently produced zero images.
3. **Download the `roborazzi-goldens` artifact** from the run page. It preserves each module's relative
   path (e.g. `core/designsystem/src/test/screenshots/...png`), so unzip it *at the repository root* —
   the extracted files land exactly where they belong, ready to diff or `git add`.
4. **Review before committing.** `git status` / `git diff --stat` will show new files (first baseline)
   or changed files (an update); open a handful of the changed PNGs and confirm the change is the one
   you expected — nothing else moved.
5. **Commit locally with a signed commit** (this repo requires GPG signing on every commit —
   [WORKFLOW.md](WORKFLOW.md) §1): `git add '**/src/test/screenshots/**' && git commit -S -m "..."`.
   Nothing in CI or the record workflow ever commits on your behalf.
6. **Push.** Once goldens exist anywhere in the tree, `ci.yml`'s "Check for recorded screenshot goldens"
   step flips to true and `verifyRoborazziDebug` joins the gate on every push from then on — a real
   regression now fails CI instead of being silently ignored.

### When a UI change is intentional

Same steps as above: re-run "Record screenshots" on your branch, download, review the diff (this time
against the previously committed goldens — the images that changed are exactly the ones your change
touched), and commit the update alongside the code change so the two land in one push
([WORKFLOW.md](WORKFLOW.md) §4.2 "same-push rule").

## Reading a CI diff (when `verifyRoborazziDebug` fails)

`verifyRoborazziDebug` fails the job the moment any capture does not byte-match its committed golden.
The "Upload test, lint and Roborazzi diff reports" step (`ci.yml`, `if: failure()`) then uploads a
`reports` artifact containing:

- `**/build/reports/roborazzi/**` — Roborazzi's own HTML report, with the golden/actual/diff triptych
  for every mismatched preview.
- `**/build/outputs/roborazzi/**` — the raw `*_actual.png` (what this run rendered) and `*_compare.png`
  (side-by-side diff) files Roborazzi writes for a failing comparison.

Download the artifact, open the HTML report first — it is the fastest way to see every failure at once —
then decide: if the render is wrong, fix the code and push again; if the new render is correct (an
intentional UI change whose golden was not updated), follow "When a UI change is intentional" above.

## Reviewing a local diff

`.\gradlew.bat :core:designsystem:compareRoborazziDebug` (or `compareRoborazziDebug` at the root for every
module) renders the current previews on your machine and writes the same golden/actual/diff triptych
under `build/outputs/roborazzi/` and `build/reports/roborazzi/` — but **never fails the build**, because
Windows rendering differs from the Linux goldens by construction (every comparison would "fail" on pixel
noise alone). Use it to sanity-check that a preview renders at all and that your code change moved the
right pixels, not to approve a baseline — approval only happens against the Linux-recorded CI diff above.
`.\gradlew.bat check` never touches Roborazzi at all when no goldens are committed yet (no `record`,
`compare`, or `verify` task is in that run's task graph, so every `captureRoboImage` call is a no-op),
and never fails on pixel differences once they are, because `check` does not depend on
`verifyRoborazziDebug` — see "Recording the first baseline" step 6.
