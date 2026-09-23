package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * The top app bar colour scheme every feature screen should adopt (`docs/design-plan.md` §3.2 "Top
 * app bars"): a `surfaceContainerLow` container so the bar reads as a shade of the page rather than
 * floating on it, one tier up to `surfaceContainer` once the content behind it has scrolled, and
 * `onSurface` titles/icons throughout (Material's own default, left unchanged).
 *
 * A feature screen passes this to its `TopAppBar`'s `colors` parameter instead of accepting
 * Material's plain-`surface` default. Not applied automatically by [IfcTheme] because `TopAppBar`
 * lives in each feature module, not in `:core:designsystem` (CLAUDE.md rule 10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun yearalTopAppBarColors(): TopAppBarColors =
    TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
