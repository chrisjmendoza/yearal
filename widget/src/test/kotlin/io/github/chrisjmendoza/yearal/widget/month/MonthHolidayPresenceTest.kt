package io.github.chrisjmendoza.yearal.widget.month

import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayCategory
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayDefinition
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayEngine
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayRule
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidaySet
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidaySetProvider
import io.github.chrisjmendoza.yearal.core.testing.FakeHolidaySetProvider
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.longs.beGreaterThanOrEqualTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * [fetchMonthHolidays] is what keeps an unreadable settings store or a malformed pack from ever hanging
 * or crashing the Month widget's render: a timeout and a catch, both proven here with virtual time so
 * the test itself does not wait three real seconds. The same guarantees [MonthEventPresenceTest] proves
 * for the event snapshot beside it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonthHolidayPresenceTest {
    // IFC September 2026, the month the widget previews: Gregorian Sep 10 - Oct 7 (spec §2.2).
    private val range = LocalDate.of(2026, 9, 10)..LocalDate.of(2026, 10, 7)
    private val engine = HolidayEngine()

    private fun pack(
        id: String,
        month: Int,
        day: Int,
    ) = HolidaySet(
        id = id,
        region = null,
        name = mapOf("en" to id),
        sources = emptyList(),
        holidays =
            listOf(
                HolidayDefinition(
                    id = id,
                    name = mapOf("en" to id),
                    rule = HolidayRule.Fixed(month = month, day = day),
                    category = HolidayCategory.OBSERVANCE,
                ),
            ),
    )

    /** Never emits and never completes -- a settings read that hangs forever. */
    private class NeverEmittingProvider : HolidaySetProvider {
        override fun enabledSets(): Flow<List<HolidaySet>> = MutableSharedFlow()
    }

    /** Throws while the flow is collected -- a pack that fails to parse. */
    private class ThrowingProvider : HolidaySetProvider {
        override fun enabledSets(): Flow<List<HolidaySet>> =
            flow { throw IllegalStateException("simulated pack failure") }
    }

    @Test
    fun `holidays inside the month are returned as Gregorian dates`() =
        runTest {
            val provider = FakeHolidaySetProvider(listOf(pack("harvest", month = 9, day = 24)))

            val result = fetchMonthHolidays(provider, engine, range)

            result shouldContainExactlyInAnyOrder setOf(LocalDate.of(2026, 9, 24))
        }

    @Test
    fun `a holiday outside the shown month is not returned`() =
        runTest {
            // IFC September 2026 ends on Gregorian October 7, so an October 20 holiday is out of range.
            val provider = FakeHolidaySetProvider(listOf(pack("later", month = 10, day = 20)))

            val result = fetchMonthHolidays(provider, engine, range)

            result shouldBe emptySet()
        }

    @Test
    fun `no enabled sets means no marks, without consulting the engine`() =
        runTest {
            val result = fetchMonthHolidays(FakeHolidaySetProvider(emptyList()), engine, range)

            result shouldBe emptySet()
        }

    @Test
    fun `a provider that never emits renders without marks, within the timeout`() =
        runTest {
            val result = fetchMonthHolidays(NeverEmittingProvider(), engine, range)

            result shouldBe emptySet()
            // Proves the timeout -- not some other early exit -- is what ended the wait.
            currentTime should beGreaterThanOrEqualTo(PRESENCE_TIMEOUT_MILLIS)
        }

    @Test
    fun `a throwing provider renders without marks instead of crashing`() =
        runTest {
            val result = fetchMonthHolidays(ThrowingProvider(), engine, range)

            result shouldBe emptySet()
        }
}
