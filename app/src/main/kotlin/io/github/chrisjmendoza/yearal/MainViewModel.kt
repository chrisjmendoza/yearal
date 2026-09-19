package io.github.chrisjmendoza.yearal

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chrisjmendoza.yearal.core.domain.DateTicker
import io.github.chrisjmendoza.yearal.core.domain.settings.SettingsRepository
import io.github.chrisjmendoza.yearal.core.domain.settings.UserSettings
import io.github.chrisjmendoza.yearal.intent.AppRoute
import io.github.chrisjmendoza.yearal.intent.IntentRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * App-level state: the current local date, so the shell can open the Calendar tab on the current
 * month, and the user settings that shape the whole window (theme, dynamic colour). "Today" comes
 * only from [DateTicker] (CLAUDE.md rule 2), so it rolls over at midnight.
 *
 * Also owns the intent-routing hand-off (ROADMAP M3 T5, M4 T10; `docs/ARCHITECTURE.md` §4 "Intent
 * routing"): [MainActivity] calls [routeFromCreate] once from `onCreate` and [routeFromNewIntent] every
 * time from `onNewIntent`; [io.github.chrisjmendoza.yearal.ui.IfcApp] applies [pendingRoute] to the tab
 * back stacks and then calls [consumeRoute]. See [routeFromCreate]'s KDoc for why a configuration
 * change or a process restart never re-routes the same launch intent.
 */
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        dateTicker: DateTicker,
        settingsRepository: SettingsRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        /** Today's Gregorian date, or `null` until the first tick arrives (which is immediate). */
        val today: StateFlow<LocalDate?> =
            dateTicker.today.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        /** The current settings; starts at the defaults until the store has been read. */
        val settings: StateFlow<UserSettings> =
            settingsRepository.settings.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                UserSettings.DEFAULT,
            )

        private val pendingRouteState = MutableStateFlow<AppRoute?>(null)

        /** The route [io.github.chrisjmendoza.yearal.ui.IfcApp] should apply, or `null` when there is none. */
        val pendingRoute: StateFlow<AppRoute?> = pendingRouteState.asStateFlow()

        /**
         * `MainActivity.onCreate`'s hook. Routes [intent] through [IntentRouter] **only the first time
         * this is called for this activity instance's lifetime** — tracked by [KEY_ROUTED] in
         * [savedStateHandle], which is what makes this correct across both cases `onCreate` can mean:
         *
         * - A configuration change (rotation): the same [MainViewModel] instance survives it (an
         *   activity-scoped `@HiltViewModel`'s `ViewModelStore` outlives a config change), so
         *   [KEY_ROUTED] is already `true` in memory and this is a no-op.
         * - A process restart after the system killed the app in the background: a **new**
         *   [MainViewModel], but Android re-delivers the *original* launch `Intent` (extras intact) to
         *   `getIntent()`, and [savedStateHandle] restores [KEY_ROUTED] from the `Bundle` the dying
         *   process saved — so this is still a no-op, even though nothing here is in memory anymore.
         *
         * A genuinely new tap while the activity is already running arrives through `onNewIntent`
         * instead ([routeFromNewIntent]), which always routes — that is a distinct user action, never a
         * repeat of the intent this activity was created with.
         */
        fun routeFromCreate(intent: Intent) {
            if (savedStateHandle.get<Boolean>(KEY_ROUTED) == true) return
            savedStateHandle[KEY_ROUTED] = true
            pendingRouteState.value = IntentRouter.resolve(intent)
        }

        /** `MainActivity.onNewIntent`'s hook: always routes — see [routeFromCreate]'s KDoc. */
        fun routeFromNewIntent(intent: Intent) {
            savedStateHandle[KEY_ROUTED] = true
            pendingRouteState.value = IntentRouter.resolve(intent)
        }

        /** Marks [pendingRoute] applied, so it is not re-applied on the next recomposition. */
        fun consumeRoute() {
            pendingRouteState.value = null
        }

        private companion object {
            const val KEY_ROUTED = "io.github.chrisjmendoza.yearal.routed_intent"
        }
    }
