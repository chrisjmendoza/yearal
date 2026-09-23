package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** The AMOLED greys [pureBlack] overrides a dark [ColorScheme] with (`docs/design-plan.md` §5.3). */
private val PureBlackSurfaceContainerLow = Color(0xFF0A0A0A)
private val PureBlackSurfaceContainer = Color(0xFF121212)
private val PureBlackSurfaceContainerHigh = Color(0xFF1C1C1C)
private val PureBlackSurfaceContainerHighest = Color(0xFF262626)
private val PureBlackSurfaceBright = Color(0xFF2E2E2E)
private val PureBlackSurfaceVariant = Color(0xFF2A2A2A)

/**
 * The AMOLED "pure black" option (`docs/design-plan.md` §5.3, the `pureBlack` user setting): a copy
 * of this dark [ColorScheme] with `background`, `surface`, `surfaceDim` and `surfaceContainerLowest`
 * at true black and the container tiers stepped up from it, keeping every `on*` colour unchanged.
 * Every changed background is darker than (or, at `surfaceBright`, close to) what it replaces, so a
 * scheme that already met WCAG AA against its own on-colours keeps doing so here — a light `on*`
 * colour's contrast against a darker background can only improve.
 *
 * Applies on top of *whichever* dark scheme is active — a curated
 * [io.github.chrisjmendoza.yearal.core.domain.settings.ColorPalette] or a dynamic (wallpaper) one —
 * which is why it is a [ColorScheme] transform rather than a seventh palette. [IfcTheme] calls it only
 * when `darkTheme && pureBlack` are both true; calling it on a light scheme would read as a bug, not a
 * feature, so callers are expected to check `darkTheme` first.
 *
 * @receiver a dark [ColorScheme]; not checked, since [IfcTheme] is the only caller and it already
 * knows which schemes are dark.
 */
public fun ColorScheme.pureBlack(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = PureBlackSurfaceContainerLow,
        surfaceContainer = PureBlackSurfaceContainer,
        surfaceContainerHigh = PureBlackSurfaceContainerHigh,
        surfaceContainerHighest = PureBlackSurfaceContainerHighest,
        surfaceBright = PureBlackSurfaceBright,
        surfaceVariant = PureBlackSurfaceVariant,
    )
