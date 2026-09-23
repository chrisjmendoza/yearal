package io.github.chrisjmendoza.yearal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
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
            val darkTheme =
                when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            // TODO(H2): pass palette and pureBlack once IfcTheme takes them (A1, :core:designsystem).
            IfcTheme(darkTheme = darkTheme, dynamicColor = settings.colorSource == ColorSource.DYNAMIC) {
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
