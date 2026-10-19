package com.tayra.languages.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The corner radii of the design system, smallest first. Screens take their shapes from here, not
 * from radii of their own; `DesignSystemTest` holds them to it.
 */
object AppShapes {
    /** 4dp: marks and bars, such as progress tracks and colour swatches. */
    val tiny = RoundedCornerShape(4.dp)

    /** 6dp: tags, badges and small boxes inside rows. */
    val small = RoundedCornerShape(6.dp)

    /** 8dp: compact tiles, code boxes and menu items. */
    val compact = RoundedCornerShape(8.dp)

    /** 10dp: controls: buttons, text fields, chips, dropdowns and list rows. */
    val control = RoundedCornerShape(10.dp)

    /** 12dp: panels inside cards, icon tiles and banners. */
    val panel = RoundedCornerShape(12.dp)

    /** 14dp: cards. */
    val card = RoundedCornerShape(14.dp)

    /** 18dp: dialogs, sheets and large containers. */
    val large = RoundedCornerShape(18.dp)

    /** Fully rounded ends: status pills, toggles and avatars. */
    val pill = RoundedCornerShape(50)

    /** Material's own components, for those the app does not wrap, on the same scale. */
    val material = Shapes(extraSmall = tiny, small = compact, medium = control, large = card, extraLarge = large)
}
