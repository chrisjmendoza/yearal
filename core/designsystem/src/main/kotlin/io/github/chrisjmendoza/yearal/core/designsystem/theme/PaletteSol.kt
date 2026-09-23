package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// The Sol palette (`docs/design-plan.md` §5.2): amber primary, teal accents: the brand's two hues swapped. Every role pairing below is
// self-checked (a throwaway script, not shipped) against WCAG 2 relative luminance and meets the
// floors `docs/design-plan.md` §3.1 sets: >=4.5:1 for every on-colour on its own colour or
// container and for onSurface against every surfaceContainer tier; >=4.5:1 for primary,
// secondary, tertiary, error and onSurfaceVariant on surface; >=3:1 for outline on surface.
// `ColorSchemeContrastTest` (a separate task, from this same doc) turns this into a gate
// assertion instead of a comment.

/**
 * The Sol palette's light scheme: amber primary, teal accents: the brand's two hues swapped.
 */
public val SolLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF865213),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFECE1D2),
        onPrimaryContainer = Color(0xFF201B14),
        inversePrimary = Color(0xFFEDBE84),
        secondary = Color(0xFF6C5C3D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE6E2DA),
        onSecondaryContainer = Color(0xFF1D1B18),
        tertiary = Color(0xFF1E6962),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD4E6E4),
        onTertiaryContainer = Color(0xFF151D1C),
        background = Color(0xFFFBF9F6),
        onBackground = Color(0xFF1E1B16),
        surface = Color(0xFFFBF9F6),
        onSurface = Color(0xFF1E1B16),
        surfaceVariant = Color(0xFFECE1CC),
        onSurfaceVariant = Color(0xFF51452E),
        surfaceTint = Color(0xFF865213),
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
 * The Sol palette's dark scheme: amber primary, teal accents: the brand's two hues swapped.
 */
public val SolDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFEDBE84),
        onPrimary = Color(0xFF382F24),
        primaryContainer = Color(0xFF5A4225),
        onPrimaryContainer = Color(0xFFE7E2DB),
        inversePrimary = Color(0xFF865213),
        secondary = Color(0xFFD1C5AE),
        onSecondary = Color(0xFF33302B),
        secondaryContainer = Color(0xFF4E4635),
        onSecondaryContainer = Color(0xFFE4E2DE),
        tertiary = Color(0xFF75D7CD),
        onTertiary = Color(0xFF253332),
        tertiaryContainer = Color(0xFF2B4C49),
        onTertiaryContainer = Color(0xFFDCE4E3),
        background = Color(0xFF17120E),
        onBackground = Color(0xFFE5E2DF),
        surface = Color(0xFF17120E),
        onSurface = Color(0xFFE5E2DF),
        surfaceVariant = Color(0xFF524437),
        onSurfaceVariant = Color(0xFFCFC5BB),
        surfaceTint = Color(0xFFEDBE84),
        inverseSurface = Color(0xFFE5E2DF),
        inverseOnSurface = Color(0xFF33302D),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF9D8E7F),
        outlineVariant = Color(0xFF524437),
        scrim = Color.Black,
        surfaceBright = Color(0xFF46392D),
        surfaceDim = Color(0xFF17120E),
        surfaceContainer = Color(0xFF261E17),
        surfaceContainerHigh = Color(0xFF31281F),
        surfaceContainerHighest = Color(0xFF3E3328),
        surfaceContainerLow = Color(0xFF211A13),
        surfaceContainerLowest = Color(0xFF110D0A),
    )
