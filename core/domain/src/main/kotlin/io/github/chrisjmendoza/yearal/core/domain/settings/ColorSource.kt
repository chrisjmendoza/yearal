package io.github.chrisjmendoza.yearal.core.domain.settings

/**
 * Where the app's colour scheme comes from (`docs/design-plan.md` §5.1; FEATURES W2, W6). Replaces the
 * earlier `dynamicColor` boolean: a stored file that still carries that key is read as [BRAND], the
 * one-time pre-1.0 migration the design plan calls for.
 */
public enum class ColorSource {
    /** One of the curated Yearal palettes ([ColorPalette]). The default on every API level. */
    BRAND,

    /**
     * Material You wallpaper colours. Honoured on API 31 and later only; below that the app behaves
     * as [BRAND] and Settings shows the option disabled.
     */
    DYNAMIC,
}
