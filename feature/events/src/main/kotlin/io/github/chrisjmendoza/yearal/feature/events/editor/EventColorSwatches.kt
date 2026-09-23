package io.github.chrisjmendoza.yearal.feature.events.editor

import androidx.annotation.StringRes
import io.github.chrisjmendoza.yearal.feature.events.R

/**
 * The fixed set of absolute colours the event colour picker offers (`docs/design-plan.md` §5.4): seven
 * named hues, each an absolute `0xAARRGGBB` literal rather than a value derived from the active
 * `ColorPalette` or `YearalColors` — the whole point of an event's own colour is that it stays the same
 * swatch under every palette and theme, unlike the calendar-inherited default. The eighth choice,
 * "Calendar colour" (`docs/contracts/Events.md` `Event.colorArgb == null`), is not a [Swatch] here: it
 * has no fixed colour of its own, so [EventEditorScreen][io.github.chrisjmendoza.yearal.feature.events.editor.EventEditorScreen]
 * renders it separately.
 */
internal object EventColorSwatches {
    /** One fixed colour choice: its absolute ARGB value and its localized name. */
    data class Swatch(
        val colorArgb: Int,
        @param:StringRes val nameRes: Int,
    )

    /** The seven fixed hues, in the order `docs/design-plan.md` §5.4 lists them. */
    val ALL: List<Swatch> =
        listOf(
            Swatch(TEAL, R.string.events_editor_color_teal),
            Swatch(AMBER, R.string.events_editor_color_amber),
            Swatch(INDIGO, R.string.events_editor_color_indigo),
            Swatch(CORAL, R.string.events_editor_color_coral),
            Swatch(MOSS, R.string.events_editor_color_moss),
            Swatch(PLUM, R.string.events_editor_color_plum),
            Swatch(SLATE, R.string.events_editor_color_slate),
        )

    private const val TEAL = 0xFF1E6962.toInt()
    private const val AMBER = 0xFFF28C28.toInt()
    private const val INDIGO = 0xFF554BCB.toInt()
    private const val CORAL = 0xFFA43C23.toInt()
    private const val MOSS = 0xFF286B3F.toInt()
    private const val PLUM = 0xFF993A7A.toInt()
    private const val SLATE = 0xFF5C5E63.toInt()
}
