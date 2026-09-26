package com.tayra.languages.core.ui.components

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** Icons that the core material icon set does not include. */
object AppIcons {
    /** Material "volume up" glyph. */
    val VolumeUp: ImageVector by lazy {
        materialIcon(name = "VolumeUp") {
            materialPath {
                moveTo(3f, 9f)
                verticalLineTo(15f)
                horizontalLineTo(7f)
                lineTo(12f, 20f)
                verticalLineTo(4f)
                lineTo(7f, 9f)
                horizontalLineTo(3f)
                close()
                moveTo(16.5f, 12f)
                curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 7.97f)
                verticalLineTo(16.02f)
                curveTo(15.48f, 15.29f, 16.5f, 13.77f, 16.5f, 12f)
                close()
                moveTo(14f, 3.23f)
                verticalLineTo(5.29f)
                curveTo(16.89f, 6.15f, 19f, 8.83f, 19f, 12f)
                reflectiveCurveTo(16.89f, 17.85f, 14f, 18.71f)
                verticalLineTo(20.77f)
                curveTo(18.01f, 19.86f, 21f, 16.28f, 21f, 12f)
                reflectiveCurveTo(18.01f, 4.14f, 14f, 3.23f)
                close()
            }
        }
    }
}
