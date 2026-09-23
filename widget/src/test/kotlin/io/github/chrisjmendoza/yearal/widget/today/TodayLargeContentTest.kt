package io.github.chrisjmendoza.yearal.widget.today

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chrisjmendoza.yearal.core.calendar.IfcDate
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.Locale

/**
 * [nextIntercalaryDay], [yearProgressLabel] and [countdownLabel] are the LARGE Today widget's extra
 * content (`docs/design-plan.md` §4.9). Oracle values are hand-checked against `docs/calendar-spec.md`
 * §2.4 and against [io.github.chrisjmendoza.yearal.core.calendar.IfcDate]'s own KDoc, not against this
 * module's implementation. Robolectric only for [android.content.res.Resources].
 */
@RunWith(AndroidJUnit4::class)
class TodayLargeContentTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val locale = Locale.US

    // region nextIntercalaryDay

    @Test
    fun `a regular day before June counts down to this year's Leap Day, in a leap year`() {
        val date = IfcDate.from(LocalDate.of(2028, 3, 1))

        nextIntercalaryDay(date) shouldBe IfcDate.LeapDay(2028)
    }

    @Test
    fun `a regular day before June counts down to this year's Year Day, in a common year`() {
        val date = IfcDate.from(LocalDate.of(2026, 3, 1))

        nextIntercalaryDay(date) shouldBe IfcDate.YearDay(2026)
    }

    @Test
    fun `a regular day after Leap Day counts down to this year's Year Day`() {
        val date = IfcDate.from(LocalDate.of(2028, 9, 1))

        nextIntercalaryDay(date) shouldBe IfcDate.YearDay(2028)
    }

    @Test
    fun `Leap Day itself counts down to the same year's Year Day`() {
        nextIntercalaryDay(IfcDate.LeapDay(2028)) shouldBe IfcDate.YearDay(2028)
    }

    @Test
    fun `Year Day counts down to next year's Leap Day, when next year is a leap year`() {
        nextIntercalaryDay(IfcDate.YearDay(2027)) shouldBe IfcDate.LeapDay(2028)
    }

    @Test
    fun `Year Day counts down to next year's Year Day, when next year is a common year`() {
        nextIntercalaryDay(IfcDate.YearDay(2026)) shouldBe IfcDate.YearDay(2027)
    }

    @Test
    fun `past the maximum supported year there is no next intercalary day`() {
        nextIntercalaryDay(IfcDate.YearDay(IfcDate.MAX_YEAR)).shouldBeNull()
    }

    // endregion

    // region yearProgressLabel

    @Test
    fun `an ordinary day shows its day of year, the year length, and the rounded percent`() {
        // Gregorian September 17, 2026 is day 260 of a 365-day common year: 71.2% rounds to 71.
        val date = IfcDate.from(LocalDate.of(2026, 9, 17))

        yearProgressLabel(date, resources, locale) shouldBe "Day 260 of 365 · 71%"
    }

    @Test
    fun `Year Day is always day-length-of-length, 100 percent`() {
        val date = IfcDate.YearDay(2026)

        yearProgressLabel(date, resources, locale) shouldBe "Day 365 of 365 · 100%"
    }

    @Test
    fun `Leap Day, in a leap year, shows its own day of year out of 366`() {
        val date = IfcDate.LeapDay(2028)

        yearProgressLabel(date, resources, locale) shouldBe "Day 169 of 366 · 46%"
    }

    // endregion

    // region countdownLabel

    @Test
    fun `the countdown names Year Day and the day count`() {
        val date = IfcDate.from(LocalDate.of(2026, 9, 17))

        countdownLabel(date, resources, locale) shouldBe "Year Day in 105 days"
    }

    @Test
    fun `the countdown names Leap Day when it comes first, in a leap year`() {
        val date = IfcDate.from(LocalDate.of(2028, 3, 1))

        countdownLabel(date, resources, locale) shouldBe "Leap Day in 108 days"
    }

    @Test
    fun `one day away is singular`() {
        // December 30 of a common year is one day before Year Day (December 31).
        val date = IfcDate.from(LocalDate.of(2026, 12, 30))

        countdownLabel(date, resources, locale) shouldBe "Year Day in 1 day"
    }

    @Test
    fun `past the maximum supported year the countdown is null`() {
        countdownLabel(IfcDate.YearDay(IfcDate.MAX_YEAR), resources, locale).shouldBeNull()
    }

    @Test
    fun `midnight crossing -- the same target's countdown decreases by exactly one day`() {
        // "Today" is Year Day 2026 right up to midnight, then a regular day in 2027; the target is
        // 2027's own Year Day either way (2027 is not a leap year), so only the count should move.
        val beforeMidnight = IfcDate.from(LocalDate.of(2026, 12, 31)) // Year Day, 2026
        val afterMidnight = IfcDate.from(LocalDate.of(2027, 1, 1)) // a regular day, 2027

        nextIntercalaryDay(beforeMidnight) shouldBe IfcDate.YearDay(2027)
        nextIntercalaryDay(afterMidnight) shouldBe IfcDate.YearDay(2027)
        countdownLabel(beforeMidnight, resources, locale) shouldBe "Year Day in 365 days"
        countdownLabel(afterMidnight, resources, locale) shouldBe "Year Day in 364 days"
    }

    // endregion
}
