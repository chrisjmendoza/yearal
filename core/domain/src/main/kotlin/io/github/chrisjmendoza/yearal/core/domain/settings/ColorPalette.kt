package io.github.chrisjmendoza.yearal.core.domain.settings

/**
 * The curated colour palettes a user can pick when [ColorSource.BRAND] is active
 * (`docs/design-plan.md` §5.2). Each is a hand-tuned light and dark Material 3 scheme in
 * `:core:designsystem`, covered there by a contrast test; this enum is only the stored choice.
 */
public enum class ColorPalette {
    /** The launcher icon's teal, cream and amber. The default. */
    TEAL,

    /** Amber primary with teal accents: the brand's two hues swapped. */
    SOL,

    /** Indigo primary, cream, coral accent, tuned for people who live in dark mode. */
    NIGHT,

    /** Forest green, parchment, ochre. */
    MOSS,

    /** Plum primary, blush, gold accent. */
    ROSE,

    /** Near-monochrome: charcoal, paper and a single amber accent. The deliberate black and white. */
    INK,
}
