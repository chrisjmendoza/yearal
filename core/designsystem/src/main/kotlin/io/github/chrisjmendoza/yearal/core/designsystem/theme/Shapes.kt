package io.github.chrisjmendoza.yearal.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The app's Material 3 corner-radius scale (`docs/design-plan.md` §3.1 "Shapes"): `extraSmall` for
 * marks, `small` for cells and chips, `medium` for cards and tiles, `large` for the hero card and
 * sheets, `extraLarge` for the tallest surfaces. [IfcTheme] passes this as `MaterialTheme.shapes`, so
 * any component reading `MaterialTheme.shapes.*` already gets it.
 */
public val YearalShapes: Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/**
 * A fully rounded pill, used by the intercalary band and badges (`docs/design-plan.md` §3.1
 * "Shapes"). Not part of [YearalShapes] because Material 3's [Shapes] holds only the five named
 * corner sizes; a percent-based pill needs its own token.
 */
public val PillShape: RoundedCornerShape = RoundedCornerShape(percent = 50)
