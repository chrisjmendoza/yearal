package io.github.chrisjmendoza.yearal.core.designsystem.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.chrisjmendoza.yearal.core.designsystem.theme.Dimens

/**
 * Caps single-column content at [maxWidth] and centres it in the window (docs/ARCHITECTURE.md §4
 * "Adaptive layouts", tablet pass 2026-09-27). On a phone the cap never binds and the content fills
 * the width exactly as before; on a tablet or an unfolded foldable a list of cards or a page of
 * prose stops stretching to a 1,200dp line and sits in a readable column instead. Screens whose wide
 * layout is a real two-pane split ([TwoPaneLayout]) do not use this — it is for every screen that
 * stays one column.
 *
 * Apply it to the scrolling column's own modifier, after any `padding`/`safeDrawingPadding`, so the
 * cap measures the content and not the window insets. Prefer [Dimens.ContentMaxWidth] for card-based
 * screens and [Dimens.ReadingMaxWidth] for pages that are mostly text (Learn, Privacy, the intro).
 *
 * @param maxWidth the widest the content may be; the default suits card-based screens.
 */
fun Modifier.limitContentWidth(maxWidth: Dp = Dimens.ContentMaxWidth): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)
