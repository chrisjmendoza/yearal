package io.github.chrisjmendoza.yearal.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.chrisjmendoza.yearal.core.domain.DateTicker
import io.github.chrisjmendoza.yearal.core.domain.RealDateTicker
import io.github.chrisjmendoza.yearal.core.domain.SystemZoneProvider
import io.github.chrisjmendoza.yearal.core.domain.TimeChangeSignal
import io.github.chrisjmendoza.yearal.core.domain.ZoneProvider
import io.github.chrisjmendoza.yearal.time.AndroidTimeChangeSignal
import java.time.Clock
import javax.inject.Singleton

/**
 * The one place the real clock, zone and time-change signal enter the app. Everything else takes
 * [Clock], [ZoneProvider], [TimeChangeSignal] or [DateTicker] by injection and is tested with the
 * fakes in `:core:testing` (CLAUDE.md rule 2).
 *
 * [io.github.chrisjmendoza.yearal.IfcApplication] separately injects the concrete
 * [AndroidTimeChangeSignal] type (not just the [TimeChangeSignal] interface bound below) so it can
 * call `start()` once at process start; construction itself does no registration (see that class's
 * KDoc for why).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    /** The production [TimeChangeSignal]: broadcasts and activity resumes, ROADMAP R1. */
    @Binds
    @Singleton
    abstract fun bindTimeChangeSignal(impl: AndroidTimeChangeSignal): TimeChangeSignal

    /** Provider side of the module (Dagger reads a module's companion object as static providers). */
    companion object {
        /** The system clock in the current default zone. Callers must not cache the zone. */
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()

        /** Reads the device zone afresh on every call, so a zone change is seen without a restart. */
        @Provides
        @Singleton
        fun provideZoneProvider(): ZoneProvider = SystemZoneProvider

        /**
         * The app-wide "today" stream, re-armed immediately on a [TimeChangeSignal] firing (ROADMAP
         * R1) as well as at every local midnight.
         */
        @Provides
        @Singleton
        fun provideDateTicker(
            clock: Clock,
            zoneProvider: ZoneProvider,
            timeChangeSignal: TimeChangeSignal,
        ): DateTicker = RealDateTicker(clock, zoneProvider, timeChangeSignal)
    }
}
