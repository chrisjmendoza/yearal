package io.github.chrisjmendoza.yearal.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidayEngine
import io.github.chrisjmendoza.yearal.core.domain.holiday.HolidaySetProvider
import io.github.chrisjmendoza.yearal.feature.calendar.holiday.PackHolidaySetProvider
import javax.inject.Singleton

/**
 * Hilt wiring for holiday evaluation: the one [HolidayEngine] of the process, and the
 * [HolidaySetProvider] binding for [PackHolidaySetProvider].
 *
 * The engine memoises per (set, year), so a single instance is what makes repeated month renders a map
 * lookup (docs/ARCHITECTURE.md §3.3). `:core:domain` is a pure-JVM module with no Hilt, so both
 * bindings have to live in an Android module.
 *
 * **These moved here from `:feature:calendar` when `:widget` became a second consumer** — the Month
 * widget's holiday marks resolve [HolidaySetProvider] and [HolidayEngine] through
 * `io.github.chrisjmendoza.yearal.widget.di.WidgetEntryPoint`. Both bindings' own KDoc said to move
 * them to `:app` rather than duplicate them the moment a further module needed them, since Hilt
 * rejects two bindings of the same type, and features never depend on each other (CLAUDE.md rule 10).
 * It is the same move [FormatterModule] records for `IfcDateFormatter`. [PackHolidaySetProvider] itself
 * stays in `:feature:calendar`: it is an implementation, injected across the module boundary from here,
 * exactly as `HolidayCatalog` already is.
 */
@Module
@InstallIn(SingletonComponent::class)
object HolidayModule {
    /** One thread-safe, memoising engine for the process. */
    @Provides
    @Singleton
    fun provideHolidayEngine(): HolidayEngine = HolidayEngine()
}

/** Separate module ([Binds] requires an abstract class or interface) for the [HolidaySetProvider] binding. */
@Module
@InstallIn(SingletonComponent::class)
abstract class HolidaySetProviderModule {
    /** The enabled holiday sets, resolved from the bundled packs and the user's settings. */
    @Binds
    @Singleton
    abstract fun bindHolidaySetProvider(impl: PackHolidaySetProvider): HolidaySetProvider
}
