package io.github.chrisjmendoza.yearal.core.domain.event

import io.github.chrisjmendoza.yearal.core.domain.NoTimeChangeSignal
import io.github.chrisjmendoza.yearal.core.domain.TimeChangeSignal
import io.github.chrisjmendoza.yearal.core.domain.ZoneProvider
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayEngine
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidaySetProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import java.time.LocalDate
import java.time.ZoneId

/**
 * The production [ObserveAgendaUseCase]: [EventRepository.observeAgendaCandidates] expanded by
 * [RecurrenceExpander], coloured from [EventRepository.observeCalendars], combined with
 * [HolidayEngine] evaluated over [HolidaySetProvider]'s enabled sets, and bucketed by device-zone date
 * with [Occurrence.dates] — exactly the pipeline of `docs/ARCHITECTURE.md` §3.4.
 *
 * It is a plain value with no clock of its own (CLAUDE.md rule 2): [zoneProvider] is read again on
 * every recomputation, never cached. Recomputation happens whenever the repository or holiday-set flows
 * emit, **and** whenever [timeChangeSignal] fires — [ZoneProvider] is a plain synchronous read, not a
 * flow, so a zone change on its own emits nothing on those other sources and needs this separate prompt
 * to be picked up by a screen that is already open (`docs/contracts/Events.md` §5; ROADMAP R1). It does
 * no I/O and logs no event content (rule 8).
 *
 * @param timeChangeSignal defaults to [NoTimeChangeSignal] so existing callers keep exactly their
 *   previous behaviour: a zone change is still picked up, just only on the next fresh subscription
 *   (screen re-open) rather than immediately.
 *
 * Spec: `docs/ARCHITECTURE.md` §3.4; `docs/contracts/Events.md` §5 "ObserveAgendaUseCase".
 */
public class DefaultObserveAgendaUseCase
    internal constructor(
        private val eventRepository: EventRepository,
        private val recurrenceExpander: RecurrenceExpander,
        private val holidayEngine: HolidayEngine,
        private val holidaySetProvider: HolidaySetProvider,
        private val zoneProvider: ZoneProvider,
        private val timeChangeSignal: TimeChangeSignal,
        /**
         * Where [invoke] and [presence] recompute the agenda, off the caller's thread.
         *
         * Injectable **only so a test can pass its own dispatcher**: with the hard-coded
         * [Dispatchers.Default] this ran on a real thread pool while a test drove virtual time through
         * `kotlinx-coroutines-test`, so whether a live collector's second emission had landed before the
         * test asserted on it was a real-time race (docs/WORKFLOW.md §2) rather than something
         * `advanceUntilIdle` could settle — the same trap `HolidaysViewModel`'s own `workDispatcher`
         * documents. The public constructor below always supplies [Dispatchers.Default]; ROADMAP R9.
         */
        private val workDispatcher: CoroutineDispatcher,
    ) : ObserveAgendaUseCase {
        public constructor(
            eventRepository: EventRepository,
            recurrenceExpander: RecurrenceExpander,
            holidayEngine: HolidayEngine,
            holidaySetProvider: HolidaySetProvider,
            zoneProvider: ZoneProvider,
            timeChangeSignal: TimeChangeSignal = NoTimeChangeSignal,
        ) : this(
            eventRepository,
            recurrenceExpander,
            holidayEngine,
            holidaySetProvider,
            zoneProvider,
            timeChangeSignal,
            Dispatchers.Default,
        )

        /**
         * The agenda of every date of [range] with an entry or a holiday, re-emitted whenever the
         * candidate events, the calendars (colour, visibility) or the enabled holiday sets change, or
         * [timeChangeSignal] fires. Work happens off the calling thread ([workDispatcher]).
         */
        override fun invoke(range: ClosedRange<LocalDate>): Flow<Map<LocalDate, DayAgenda>> =
            combine(
                eventRepository.observeAgendaCandidates(range),
                eventRepository.observeCalendars(),
                holidaySetProvider.enabledSets(),
                timeChangeSignal.changes.onStart { emit(Unit) },
            ) { candidates, calendars, sets, _ ->
                val zone = zoneProvider.currentZone()
                val entriesByDate = entriesByDate(candidates, calendars, range, zone)
                val holidaysByDate =
                    if (range.isEmpty()) {
                        emptyMap()
                    } else {
                        holidayEngine.occurrences(sets, range).groupBy { it.date }
                    }
                val dates = entriesByDate.keys + holidaysByDate.keys
                dates
                    .associateWith { date ->
                        DayAgenda.of(date, entriesByDate[date].orEmpty(), holidaysByDate[date].orEmpty())
                    }.toSortedMap()
            }.flowOn(workDispatcher)

        /**
         * The dates of [range] with at least one event occurrence, ignoring holidays; re-emitted on the
         * same changes as [invoke] except the enabled holiday sets, which this never reads.
         */
        override fun presence(range: ClosedRange<LocalDate>): Flow<Set<LocalDate>> =
            combine(
                eventRepository.observeAgendaCandidates(range),
                eventRepository.observeCalendars(),
                timeChangeSignal.changes.onStart { emit(Unit) },
            ) { candidates, calendars, _ ->
                entriesByDate(candidates, calendars, range, zoneProvider.currentZone()).keys.toSortedSet()
            }.flowOn(workDispatcher)

        /**
         * Expands every candidate that has a calendar, resolves its colour, and buckets its occurrences by
         * the dates of [range] they touch ([Occurrence.dates]). A candidate whose calendar has vanished
         * since the query was built (a concurrent delete) is skipped rather than crashing the flow — the
         * same fail-soft rule `docs/contracts/Events.md` asks of corrupted storage rows.
         */
        private fun entriesByDate(
            candidates: List<Event>,
            calendars: List<EventCalendar>,
            range: ClosedRange<LocalDate>,
            zone: ZoneId,
        ): Map<LocalDate, List<AgendaEntry>> {
            if (range.isEmpty()) return emptyMap()
            val calendarsById = calendars.associateBy { it.id }
            val result = LinkedHashMap<LocalDate, MutableList<AgendaEntry>>()
            for (event in candidates) {
                val calendar = calendarsById[event.calendarId] ?: continue
                val colorArgb = event.colorArgb ?: calendar.colorArgb
                for (occurrence in recurrenceExpander.expand(event, range, zone)) {
                    val entry = AgendaEntry(event, occurrence, colorArgb, zone)
                    val touched = occurrence.dates(zone)
                    var date = maxOf(touched.start, range.start)
                    val last = minOf(touched.endInclusive, range.endInclusive)
                    while (!date.isAfter(last)) {
                        result.getOrPut(date) { mutableListOf() }.add(entry)
                        date = date.plusDays(1)
                    }
                }
            }
            return result
        }
    }
