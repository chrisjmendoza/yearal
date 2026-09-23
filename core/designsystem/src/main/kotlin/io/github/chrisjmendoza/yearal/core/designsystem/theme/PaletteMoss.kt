package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// The Moss palette (`docs/design-plan.md` §5.2): forest green primary, parchment neutrals, an ochre tertiary. Every role pairing below is
// self-checked (a throwaway script, not shipped) against WCAG 2 relative luminance and meets the
// floors `docs/design-plan.md` §3.1 sets: >=4.5:1 for every on-colour on its own colour or
// container and for onSurface against every surfaceContainer tier; >=4.5:1 for primary,
// secondary, tertiary, error and onSurfaceVariant on surface; >=3:1 for outline on surface.
// `ColorSchemeContrastTest` (a separate task, from this same doc) turns this into a gate
// assertion instead of a comment.

/**
 * The Moss palette's light scheme: forest green primary, parchment neutrals, an ochre tertiary.
 */
public val MossLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF286B3F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E6DC),
        onPrimaryContainer = Color(0xFF161D19),
        inversePrimary = Color(0xFF91D5A8),
        secondary = Color(0xFF4B6637),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDDE5D7),
        onSecondaryContainer = Color(0xFF1A1C18),
        tertiary = Color(0xFF715C1C),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE7E2D3),
        onTertiaryContainer = Color(0xFF1E1B15),
        background = Color(0xFFFBF9F6),
        onBackground = Color(0xFF1E1B16),
        surface = Color(0xFFFBF9F6),
        onSurface = Color(0xFF1E1B16),
        surfaceVariant = Color(0xFFECE1CC),
        onSurfaceVariant = Color(0xFF51452E),
        surfaceTint = Color(0xFF286B3F),
        inverseSurface = Color(0xFF353025),
        inverseOnSurface = Color(0xFFF2F1EE),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF857557),
        outlineVariant = Color(0xFFD3C5AA),
        scrim = Color.Black,
        surfaceBright = Color(0xFFFBF9F6),
        surfaceDim = Color(0xFFE3D9C5),
        surfaceContainer = Color(0xFFF2EDE4),
        surfaceContainerHigh = Color(0xFFEEE8DB),
        surfaceContainerHighest = Color(0xFFE9E2D2),
        surfaceContainerLow = Color(0xFFF6F3ED),
        surfaceContainerLowest = Color(0xFFFFFFFF),
    )

/**
 * The Moss palette's dark scheme: forest green primary, parchment neutrals, an ochre tertiary.
 */
public val MossDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFF91D5A8),
        onPrimary = Color(0xFF27332B),
        primaryContainer = Color(0xFF2F4D39),
        onPrimaryContainer = Color(0xFFDDE4DF),
        inversePrimary = Color(0xFF286B3F),
        secondary = Color(0xFFB5CEA4),
        onSecondary = Color(0xFF2D322A),
        secondaryContainer = Color(0xFF3C4B31),
        onSecondaryContainer = Color(0xFFE0E4DD),
        tertiary = Color(0xFFDEC57A),
        onTertiary = Color(0xFF343024),
        tertiaryContainer = Color(0xFF4F462B),
        onTertiaryContainer = Color(0xFFE5E2DB),
        background = Color(0xFF0C150F),
        onBackground = Color(0xFFDEE4E0),
        surface = Color(0xFF0C150F),
        onSurface = Color(0xFFDEE4E0),
        surfaceVariant = Color(0xFF334C3B),
        onSurfaceVariant = Color(0xFFB6CCBD),
        surfaceTint = Color(0xFF91D5A8),
        inverseSurface = Color(0xFFDDE4E0),
        inverseOnSurface = Color(0xFF2C322E),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF799883),
        outlineVariant = Color(0xFF334C3B),
        scrim = Color.Black,
        surfaceBright = Color(0xFF294031),
        surfaceDim = Color(0xFF0C150F),
        surfaceContainer = Color(0xFF15231A),
        surfaceContainerHigh = Color(0xFF1D2E23),
        surfaceContainerHighest = Color(0xFF25392B),
        surfaceContainerLow = Color(0xFF121E16),
        surfaceContainerLowest = Color(0xFF09100B),
    )
