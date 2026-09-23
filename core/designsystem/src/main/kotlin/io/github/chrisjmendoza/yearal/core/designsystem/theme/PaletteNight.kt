package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// The Night palette (`docs/design-plan.md` §5.2): indigo primary, cream neutrals, a coral tertiary; tuned for people who live in dark mode. Every role pairing below is
// self-checked (a throwaway script, not shipped) against WCAG 2 relative luminance and meets the
// floors `docs/design-plan.md` §3.1 sets: >=4.5:1 for every on-colour on its own colour or
// container and for onSurface against every surfaceContainer tier; >=4.5:1 for primary,
// secondary, tertiary, error and onSurfaceVariant on surface; >=3:1 for outline on surface.
// `ColorSchemeContrastTest` (a separate task, from this same doc) turns this into a gate
// assertion instead of a comment.

/**
 * The Night palette's light scheme: indigo primary, cream neutrals, a coral tertiary; tuned for people who live in dark mode.
 */
public val NightLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF554BCB),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE2E1EF),
        onPrimaryContainer = Color(0xFF1B1A25),
        inversePrimary = Color(0xFFC6C2ED),
        secondary = Color(0xFF635884),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE3E1E9),
        onSecondaryContainer = Color(0xFF1C1B1E),
        tertiary = Color(0xFFA43C23),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEDE0DC),
        onTertiaryContainer = Color(0xFF221917),
        background = Color(0xFFFAF9F7),
        onBackground = Color(0xFF1D1B17),
        surface = Color(0xFFFAF9F7),
        onSurface = Color(0xFF1D1B17),
        surfaceVariant = Color(0xFFEAE2D0),
        onSurfaceVariant = Color(0xFF504631),
        surfaceTint = Color(0xFF554BCB),
        inverseSurface = Color(0xFF343027),
        inverseOnSurface = Color(0xFFF1F1EF),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF83765B),
        outlineVariant = Color(0xFFD1C5AF),
        scrim = Color.Black,
        surfaceBright = Color(0xFFFAF9F7),
        surfaceDim = Color(0xFFE2D9C8),
        surfaceContainer = Color(0xFFF1EDE6),
        surfaceContainerHigh = Color(0xFFEDE8DD),
        surfaceContainerHighest = Color(0xFFE8E2D5),
        surfaceContainerLow = Color(0xFFF6F3EE),
        surfaceContainerLowest = Color(0xFFFFFFFF),
    )

/**
 * The Night palette's dark scheme: indigo primary, cream neutrals, a coral tertiary; tuned for people who live in dark mode.
 */
public val NightDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFC6C2ED),
        onPrimary = Color(0xFF302E41),
        primaryContainer = Color(0xFF444077),
        onPrimaryContainer = Color(0xFFE2E2E9),
        inversePrimary = Color(0xFF554BCB),
        secondary = Color(0xFFC9C4D8),
        onSecondary = Color(0xFF313036),
        secondaryContainer = Color(0xFF49445A),
        onSecondaryContainer = Color(0xFFE3E2E6),
        tertiary = Color(0xFFEEBBAE),
        onTertiary = Color(0xFF3D2D29),
        tertiaryContainer = Color(0xFF643D33),
        onTertiaryContainer = Color(0xFFE8E1DF),
        background = Color(0xFF131123),
        onBackground = Color(0xFFE2E2E8),
        surface = Color(0xFF131123),
        onSurface = Color(0xFFE2E2E8),
        surfaceVariant = Color(0xFF45426E),
        onSurfaceVariant = Color(0xFFC6C4DA),
        surfaceTint = Color(0xFFC6C2ED),
        inverseSurface = Color(0xFFE2E2E9),
        inverseOnSurface = Color(0xFF302F38),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF908DAE),
        outlineVariant = Color(0xFF45426E),
        scrim = Color.Black,
        surfaceBright = Color(0xFF3A3661),
        surfaceDim = Color(0xFF131123),
        surfaceContainer = Color(0xFF1F1D36),
        surfaceContainerHigh = Color(0xFF292644),
        surfaceContainerHighest = Color(0xFF333056),
        surfaceContainerLow = Color(0xFF1B1931),
        surfaceContainerLowest = Color(0xFF0E0C1B),
    )
