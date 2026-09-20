package io.github.chrisjmendoza.yearal.feature.settings.intro

import io.github.chrisjmendoza.yearal.core.calendar.IfcMonth
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * [IntroFacts] against `docs/calendar-spec.md` directly (not against [io.github.chrisjmendoza.yearal
 * .feature.settings.learn.LearnFacts]'s own values, even though this object reuses them) — the test
 * author checks the numbers the spec states, per `docs/WORKFLOW.md` §6 ("the test author works from the
 * spec, not the implementation").
 */
class IntroFactsTest {
    @Test
    fun `there are 13 months of 28 days each, spec R4`() {
        IntroFacts.monthCount shouldBe 13
        IntroFacts.daysPerMonth shouldBe 28
    }

    @Test
    fun `Sol is month 7, between June and July, spec R4`() {
        IntroFacts.solNumber shouldBe 7
        IntroFacts.monthBeforeSol shouldBe IfcMonth.JUNE
        IntroFacts.monthAfterSol shouldBe IfcMonth.JULY
        IntroFacts.monthBeforeSol.number shouldBe (IntroFacts.solNumber - 1)
        IntroFacts.monthAfterSol.number shouldBe (IntroFacts.solNumber + 1)
    }

    @Test
    fun `the weekday example is the spec's own worked example, spec section 4-1`() {
        IntroFacts.weekdayExampleGregorian shouldBe LocalDate.of(2026, 9, 17)
        IntroFacts.weekdayExampleGregorian.dayOfWeek shouldBe DayOfWeek.THURSDAY
        IntroFacts.weekdayExampleIfc.nominalDayOfWeek shouldBe DayOfWeek.SUNDAY
        IntroFacts.weekdayExampleIfc.actualDayOfWeek shouldBe DayOfWeek.THURSDAY
    }

    @Test
    fun `Year Day is always Gregorian December 31, spec R8`() {
        IntroFacts.yearDayExample.toLocalDate() shouldBe LocalDate.of(IntroFacts.yearDayExample.year, 12, 31)
        IntroFacts.yearDayExample.nominalDayOfWeek shouldBe null
    }

    @Test
    fun `Leap Day only exists in leap years and is always Gregorian June 17, spec R9`() {
        IntroFacts.leapDayExample.toLocalDate() shouldBe LocalDate.of(IntroFacts.leapDayExample.year, 6, 17)
        IntroFacts.leapDayExample.nominalDayOfWeek shouldBe null
        java.time.Year.isLeap(IntroFacts.leapDayExample.year.toLong()) shouldBe true
    }
}
