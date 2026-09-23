package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// The Ink palette (`docs/design-plan.md` §5.2): near-monochrome charcoal and paper with a single amber tertiary accent, the deliberate black and white. Every role pairing below is
// self-checked (a throwaway script, not shipped) against WCAG 2 relative luminance and meets the
// floors `docs/design-plan.md` §3.1 sets: >=4.5:1 for every on-colour on its own colour or
// container and for onSurface against every surfaceContainer tier; >=4.5:1 for primary,
// secondary, tertiary, error and onSurfaceVariant on surface; >=3:1 for outline on surface.
// `ColorSchemeContrastTest` (a separate task, from this same doc) turns this into a gate
// assertion instead of a comment.

/**
 * The Ink palette's light scheme: near-monochrome charcoal and paper with a single amber tertiary accent, the deliberate black and white.
 */
public val InkLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF5C5E63),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE2E2E3),
        onPrimaryContainer = Color(0xFF1B1B1C),
        inversePrimary = Color(0xFFC5C6C9),
        secondary = Color(0xFF615E5A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE3E2E1),
        onSecondaryContainer = Color(0xFF1C1B1B),
        tertiary = Color(0xFF7B581A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE9E2D4),
        onTertiaryContainer = Color(0xFF1F1B15),
        background = Color(0xFFF9F9F9),
        onBackground = Color(0xFF1C1B1B),
        surface = Color(0xFFF9F9F9),
        onSurface = Color(0xFF1C1B1B),
        surfaceVariant = Color(0xFFE4E2DF),
        onSurfaceVariant = Color(0xFF494642),
        surfaceTint = Color(0xFF5C5E63),
        inverseSurface = Color(0xFF31302E),
        inverseOnSurface = Color(0xFFF1F1F0),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF7A7771),
        outlineVariant = Color(0xFFC8C6C2),
        scrim = Color.Black,
        surfaceBright = Color(0xFFF9F9F9),
        surfaceDim = Color(0xFFDBDAD7),
        surfaceContainer = Color(0xFFEEEEEC),
        surfaceContainerHigh = Color(0xFFE9E8E6),
        surfaceContainerHighest = Color(0xFFE3E2E0),
        surfaceContainerLow = Color(0xFFF4F3F2),
        surfaceContainerLowest = Color(0xFFFFFFFF),
    )

/**
 * The Ink palette's dark scheme: near-monochrome charcoal and paper with a single amber tertiary accent, the deliberate black and white.
 */
public val InkDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFC5C6C9),
        onPrimary = Color(0xFF303031),
        primaryContainer = Color(0xFF464749),
        onPrimaryContainer = Color(0xFFE2E2E3),
        inversePrimary = Color(0xFF5C5E63),
        secondary = Color(0xFFC8C6C4),
        onSecondary = Color(0xFF313030),
        secondaryContainer = Color(0xFF484644),
        onSecondaryContainer = Color(0xFFE3E2E2),
        tertiary = Color(0xFFE5C184),
        onTertiary = Color(0xFF363024),
        tertiaryContainer = Color(0xFF54452B),
        onTertiaryContainer = Color(0xFFE6E2DC),
        background = Color(0xFF121315),
        onBackground = Color(0xFFE2E2E3),
        surface = Color(0xFF121315),
        onSurface = Color(0xFFE2E2E3),
        surfaceVariant = Color(0xFF44474C),
        onSurfaceVariant = Color(0xFFC5C6CA),
        surfaceTint = Color(0xFFC5C6C9),
        inverseSurface = Color(0xFFE2E2E4),
        inverseOnSurface = Color(0xFF303031),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF8E9196),
        outlineVariant = Color(0xFF44474C),
        scrim = Color.Black,
        surfaceBright = Color(0xFF393B40),
        surfaceDim = Color(0xFF121315),
        surfaceContainer = Color(0xFF1E2022),
        surfaceContainerHigh = Color(0xFF282A2D),
        surfaceContainerHighest = Color(0xFF333539),
        surfaceContainerLow = Color(0xFF1A1C1E),
        surfaceContainerLowest = Color(0xFF0D0E10),
    )
