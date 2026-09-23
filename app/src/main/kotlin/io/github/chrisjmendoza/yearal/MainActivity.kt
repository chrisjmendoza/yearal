package io.github.chrisjmendoza.yearal

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.chrisjmendoza.yearal.core.designsystem.theme.IfcTheme
import io.github.chrisjmendoza.yearal.core.domain.settings.ColorSource
import io.github.chrisjmendoza.yearal.core.domain.settings.ThemeMode
import io.github.chrisjmendoza.yearal.ui.IfcApp

/**
 * The single activity. Edge-to-edge (enforced at target 36), no orientation lock
 * (docs/ARCHITECTURE.md §4 "Adaptive layouts"). The theme follows the user's settings (FEATURES W2).
 *
 * **System bars follow the app's [ThemeMode], not the system** (`docs/design-plan.md` §3.2, finding
 * D6). The initial [enableEdgeToEdge] call in [onCreate] is the one Android needs before the first
 * frame; once the resolved [resolveDarkTheme] flag is known, a `DisposableEffect` inside [setContent]
 * calls [enableEdgeToEdge] again with an explicit light/dark [SystemBarStyle] — never
 * [SystemBarStyle.auto], which would silently re-derive the icon colour from the *system* setting and
 * reintroduce the bug (a forced-light app on a dark-mode phone would get light status icons on a cream
 * background, and vice versa).
 *
 * **Intent routing (ROADMAP M3 T5, M4 T10).** `android:launchMode="singleTask"` in the manifest is
 * deliberate and unchanged by this task: it is what makes a widget or notification tap that finds this
 * activity already running deliver through [onNewIntent] instead of creating a second instance, which
 * is the only way [MainViewModel.routeFromNewIntent] can see it at all. [onCreate] and [onNewIntent]
 * each hand their intent to [MainViewModel] (see its KDoc for exactly-once semantics across a
 * configuration change and a process restart); [io.github.chrisjmendoza.yearal.ui.IfcApp] is the one
 * that actually applies the resulting route, once the tab back stacks it needs exist.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Internal, not private: MainActivityTest verifies the routing hand-off directly against this
    // instance, without needing a full Compose composition to observe it indirectly.
    internal val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        viewModel.routeFromCreate(intent)
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val darkTheme = resolveDarkTheme(settings.themeMode, isSystemInDarkTheme())
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = statusBarStyleFor(darkTheme),
                    navigationBarStyle = navigationBarStyleFor(darkTheme),
                )
                onDispose {}
            }
            IfcTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.colorSource == ColorSource.DYNAMIC,
                palette = settings.palette,
                pureBlack = settings.pureBlack,
            ) {
                IfcApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.routeFromNewIntent(intent)
    }
}

/**
 * Resolves [themeMode] against whether the system is currently in dark mode — the same three-way
 * mapping [IfcTheme]'s `darkTheme` parameter documents. Pulled out of [MainActivity.onCreate] as a
 * plain function so a unit test can assert the mapping without a Compose composition.
 */
internal fun resolveDarkTheme(
    themeMode: ThemeMode,
    systemInDarkTheme: Boolean,
): Boolean =
    when (themeMode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

/** The status bar's icon style for [darkTheme]: dark icons on a light app, light icons on a dark one. */
private fun statusBarStyleFor(darkTheme: Boolean): SystemBarStyle =
    if (darkTheme) {
        SystemBarStyle.dark(Color.TRANSPARENT)
    } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    }

/**
 * The navigation bar's icon style for [darkTheme]. Mirrors [androidx.activity.enableEdgeToEdge]'s own
 * default scrims (a translucent white behind light icons, translucent near-black behind dark icons) so
 * three-button navigation stays legible pre-gesture-navigation devices; only which one is chosen stops
 * following the system and starts following the app.
 */
private fun navigationBarStyleFor(darkTheme: Boolean): SystemBarStyle =
    if (darkTheme) {
        SystemBarStyle.dark(NavigationBarDarkScrim)
    } else {
        SystemBarStyle.light(NavigationBarLightScrim, NavigationBarDarkScrim)
    }

// Written as the equivalent of Color.argb(0xe6, 0xff, 0xff, 0xff) / (0x80, 0x1b, 0x1b, 0x1b) rather than
// calling it: android.graphics.Color's real methods are unavailable outside Robolectric, and a call in
// a top-level property initializer would fail every plain JUnit test that merely loads this file's
// class, resolveDarkTheme (ThemeModeResolutionTest) included.
private val NavigationBarLightScrim = 0xe6ffffffu.toInt()
private val NavigationBarDarkScrim = 0x801b1b1bu.toInt()
