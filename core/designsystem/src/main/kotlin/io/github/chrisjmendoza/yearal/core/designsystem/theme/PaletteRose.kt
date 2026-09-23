package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// The Rose palette (`docs/design-plan.md` §5.2): plum primary, blush neutrals, a gold tertiary. Every role pairing below is
// self-checked (a throwaway script, not shipped) against WCAG 2 relative luminance and meets the
// floors `docs/design-plan.md` §3.1 sets: >=4.5:1 for every on-colour on its own colour or
// container and for onSurface against every surfaceContainer tier; >=4.5:1 for primary,
// secondary, tertiary, error and onSurfaceVariant on surface; >=3:1 for outline on surface.
// `ColorSchemeContrastTest` (a separate task, from this same doc) turns this into a gate
// assertion instead of a comment.

/**
 * The Rose palette's light scheme: plum primary, blush neutrals, a gold tertiary.
 */
public val RoseLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF993A7A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFECDFE7),
        onPrimaryContainer = Color(0xFF21191E),
        inversePrimary = Color(0xFFE5BAD7),
        secondary = Color(0xFF834F60),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE9E0E3),
        onSecondaryContainer = Color(0xFF1F1A1C),
        tertiary = Color(0xFF705D10),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE8E3CD),
        onTertiaryContainer = Color(0xFF1E1C13),
        background = Color(0xFFFBF9F9),
        onBackground = Color(0xFF201A1B),
        surface = Color(0xFFFBF9F9),
        onSurface = Color(0xFF201A1B),
        surfaceVariant = Color(0xFFF0DEE1),
        onSurfaceVariant = Color(0xFF603D43),
        surfaceTint = Color(0xFF993A7A),
        inverseSurface = Color(0xFF3B2D2F),
        inverseOnSurface = Color(0xFFF2F0F0),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF966C73),
        outlineVariant = Color(0xFFDAC0C5),
        scrim = Color.Black,
        surfaceBright = Color(0xFFFBF9F9),
        surfaceDim = Color(0xFFE8D6D9),
        surfaceContainer = Color(0xFFF4ECED),
        surfaceContainerHigh = Color(0xFFF1E5E7),
        surfaceContainerHighest = Color(0xFFEDDFE1),
        surfaceContainerLow = Color(0xFFF8F2F3),
        surfaceContainerLowest = Color(0xFFFFFFFF),
    )

/**
 * The Rose palette's dark scheme: plum primary, blush neutrals, a gold tertiary.
 */
public val RoseDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFE5BAD7),
        onPrimary = Color(0xFF3A2C36),
        primaryContainer = Color(0xFF613A54),
        onPrimaryContainer = Color(0xFFE7E1E5),
        inversePrimary = Color(0xFF993A7A),
        secondary = Color(0xFFD9C0C8),
        onSecondary = Color(0xFF362E31),
        secondaryContainer = Color(0xFF5A3F48),
        onSecondaryContainer = Color(0xFFE6E1E3),
        tertiary = Color(0xFFE4C445),
        onTertiary = Color(0xFF343021),
        tertiaryContainer = Color(0xFF4F4624),
        onTertiaryContainer = Color(0xFFE5E2D9),
        background = Color(0xFF1B1017),
        onBackground = Color(0xFFE6E1E4),
        surface = Color(0xFF1B1017),
        onSurface = Color(0xFFE6E1E4),
        surfaceVariant = Color(0xFF5C3D52),
        onSurfaceVariant = Color(0xFFD4C1CE),
        surfaceTint = Color(0xFFE5BAD7),
        inverseSurface = Color(0xFFE7E1E5),
        inverseOnSurface = Color(0xFF352E33),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFFA4899B),
        outlineVariant = Color(0xFF5C3D52),
        scrim = Color.Black,
        surfaceBright = Color(0xFF4F3245),
        surfaceDim = Color(0xFF1B1017),
        surfaceContainer = Color(0xFF2B1A26),
        surfaceContainerHigh = Color(0xFF382431),
        surfaceContainerHighest = Color(0xFF462D3E),
        surfaceContainerLow = Color(0xFF271621),
        surfaceContainerLowest = Color(0xFF140B11),
    )
