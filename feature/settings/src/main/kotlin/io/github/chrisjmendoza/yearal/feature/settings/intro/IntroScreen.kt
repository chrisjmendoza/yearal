package io.github.chrisjmendoza.yearal.feature.settings.intro

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.chrisjmendoza.yearal.core.designsystem.format.IfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.format.rememberIfcDateFormatter
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.navigation.ConverterKey
import io.github.chrisjmendoza.yearal.core.navigation.LearnKey
import io.github.chrisjmendoza.yearal.core.navigation.Navigator
import io.github.chrisjmendoza.yearal.feature.settings.R

/** How many screens the intro has (`docs/FEATURES.md` L1: "at most three screens"). */
private const val PAGE_COUNT = 3

/**
 * The first-run intro (`docs/FEATURES.md` L1; `docs/ARCHITECTURE.md` §4 "Screens and navigation"):
 * `:app` pushes [io.github.chrisjmendoza.yearal.core.navigation.IntroKey] onto the Today tab's stack the
 * first time the store confirms [io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
 * .hasSeenIntro] is `false`, and the Learn screen can push it again later for a user who skipped it the
 * first time (`learn_replay_intro_title` there). Every exit but "Learn more" marks the intro seen through
 * [IntroViewModel.markSeen] before popping back through [navigator], so it is never shown again.
 *
 * @param navigator pops back to whatever the intro was shown over; also used for the "Learn more" link
 * and the "find my IFC birthday" hook, both of which push onto the current tab exactly like
 * `io.github.chrisjmendoza.yearal.feature.calendar.day.DayScreen`'s "Open in converter" action does.
 */
@Composable
fun IntroRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: IntroViewModel = hiltViewModel(),
) {
    IntroScreen(
        onSkip = {
            viewModel.markSeen()
            navigator.goBack()
        },
        onFinish = {
            viewModel.markSeen()
            navigator.goBack()
        },
        onFindBirthday = {
            viewModel.markSeen()
            navigator.navigate(ConverterKey())
        },
        onLearnMore = { navigator.navigate(LearnKey) },
        modifier = modifier,
    )
}

/**
 * The stateless intro screen — the unit for previews and Compose tests
 * (`docs/ARCHITECTURE.md` §4 "State management"). Holds only which of the [PAGE_COUNT] screens is
 * showing; every worked example comes from [IntroFacts], computed through `:core:calendar`, and is
 * rendered through [rememberIfcDateFormatter] exactly like the Learn screen (CLAUDE.md rule 1).
 *
 * Uses plain Back/Next buttons rather than a swipeable pager: this task's brief is content, not a new
 * visual language, and the design pass (`docs/ROADMAP.md` M2 T13) is deliberately still ahead.
 *
 * @param onSkip skips the intro from any screen; marks it seen.
 * @param onFinish finishes from the last screen without the birthday hook; marks it seen.
 * @param onFindBirthday the closing hook (FEATURES L1, D3): marks the intro seen and opens the converter.
 * @param onLearnMore opens the Learn screen without marking the intro seen or finished.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroScreen(
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onFindBirthday: () -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val formatter = rememberIfcDateFormatter()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.intro_title)) },
                actions = {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.intro_skip)) }
                },
            )
        },
        bottomBar = {
            IntroNavigationBar(
                page = page,
                onBack = { page-- },
                onNext = { page++ },
                onFinish = onFinish,
                onFindBirthday = onFindBirthday,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.intro_page_indicator, page + 1, PAGE_COUNT),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            when (page) {
                0 -> WhatIsIfcPage()
                1 -> WeekdayPage(formatter)
                else -> FloatingDaysPage(formatter)
            }
            TextButton(onClick = onLearnMore, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.intro_learn_more))
            }
        }
    }
}

/** Back (from screen 2 on) / Next / the last screen's Done and "find my IFC birthday" actions. */
@Composable
private fun IntroNavigationBar(
    page: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    onFindBirthday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (page > 0) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.intro_back)) }
        } else {
            // An empty leading slot keeps "Next"/"Done" anchored to the trailing edge on screen 1.
            Row {}
        }
        if (page < PAGE_COUNT - 1) {
            Button(onClick = onNext) { Text(stringResource(R.string.intro_next)) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onFinish) { Text(stringResource(R.string.intro_done)) }
                Button(onClick = onFindBirthday) { Text(stringResource(R.string.intro_find_birthday)) }
            }
        }
    }
}

/** Screen 1: what the IFC is (calendar-spec §2.2 R4) — month count, day count, and where Sol sits. */
@Composable
private fun WhatIsIfcPage() {
    PageHeading(stringResource(R.string.intro_what_heading))
    BodyParagraph(stringResource(R.string.intro_what_months, IntroFacts.monthCount, IntroFacts.daysPerMonth))
    BodyParagraph(stringResource(R.string.intro_what_sol))
}

/** Screen 2: nominal vs. actual weekday (calendar-spec §4.1), with the spec's own worked example. */
@Composable
private fun WeekdayPage(formatter: IfcDateFormatter) {
    PageHeading(stringResource(R.string.intro_weekday_heading))
    BodyParagraph(stringResource(R.string.intro_weekday_intro))
    val nominal = requireNotNull(IntroFacts.weekdayExampleIfc.nominalDayOfWeek)
    BodyParagraph(
        stringResource(
            R.string.intro_weekday_example,
            formatter.formatGregorianLong(IntroFacts.weekdayExampleGregorian),
            formatter.formatLong(IntroFacts.weekdayExampleIfc),
            formatter.weekdayName(nominal),
            formatter.weekdayName(IntroFacts.weekdayExampleIfc.actualDayOfWeek),
        ),
    )
    BodyParagraph(stringResource(R.string.intro_weekday_warning))
}

/** Screen 3: Year Day and Leap Day (calendar-spec §2.4 R8, R9), ending with the birthday hook's prompt. */
@Composable
private fun FloatingDaysPage(formatter: IfcDateFormatter) {
    PageHeading(stringResource(R.string.intro_floating_heading))
    BodyParagraph(
        stringResource(
            R.string.intro_floating_year_day,
            formatter.formatLong(IntroFacts.yearDayExample),
            formatter.formatGregorianLong(IntroFacts.yearDayExample.toLocalDate()),
        ),
    )
    BodyParagraph(
        stringResource(
            R.string.intro_floating_leap_day,
            formatter.formatLong(IntroFacts.leapDayExample),
            formatter.formatGregorianLong(IntroFacts.leapDayExample.toLocalDate()),
        ),
    )
    BodyParagraph(stringResource(R.string.intro_find_birthday_prompt))
}

@Composable
private fun PageHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 12.dp).semantics { heading() },
    )
}

@Composable
private fun BodyParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

// Previews — Roborazzi's preview scanner captures these once docs/ROADMAP.md M2 T10 records goldens.

@Preview(name = "Screen 1", showBackground = true)
@Composable
internal fun IntroScreenPage1Preview() {
    IntroPreview()
}

@Preview(name = "Screen 3 — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun IntroScreenPage3DarkPreview() {
    IntroPreview(darkTheme = true)
}

@Preview(name = "Font 2.0", showBackground = true, fontScale = 2f, heightDp = 1000)
@Composable
internal fun IntroScreenLargeFontPreview() {
    IntroPreview()
}

@Composable
private fun IntroPreview(darkTheme: Boolean = false) {
    IfcTheme(darkTheme = darkTheme, dynamicColor = false) {
        IntroScreen(onSkip = {}, onFinish = {}, onFindBirthday = {}, onLearnMore = {})
    }
}
