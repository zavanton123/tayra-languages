package com.tayra.languages.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.tayra.languages.core.domain.model.TermStatus

/** Colours used to highlight terms by status on the reading screen. */
data class StatusColors(
    val backgrounds: Map<TermStatus, Color>,
    /** Text colour on highlighted terms, or unspecified to keep the body colour. */
    val onHighlight: Color = Color.Unspecified,
    /** Whether unknown terms are shown by background (light themes) or by text colour. */
    val unknownAsText: Boolean = false,
) {
    fun background(status: TermStatus): Color = backgrounds[status] ?: Color.Transparent
}

data class AppTheme(
    val id: String,
    val label: String,
    val isDark: Boolean,
    val colorScheme: ColorScheme,
    val statusColors: StatusColors,
    val readingBackground: Color,
    val readingText: Color,
    val hoverUnderline: Color,
    val markedUnderline: Color,
    val selectionBackground: Color,
)

object AppThemes {
    private val luteStatuses = mapOf(
        TermStatus.UNKNOWN to Color(0xFFD5FFFF),
        TermStatus.NEW_1 to Color(0xFFF5B8A9),
        TermStatus.NEW_2 to Color(0xFFF5CCA9),
        TermStatus.LEARNING_3 to Color(0xFFF5E1A9),
        TermStatus.LEARNING_4 to Color(0xFFF5F3A9),
        TermStatus.LEARNED to Color(0xFFDDFFDD),
        TermStatus.IGNORED to Color(0xFFEE8577),
        TermStatus.WELL_KNOWN to Color(0xFF72DA88),
    )

    val default = AppTheme(
        id = "default",
        label = "Default",
        isDark = false,
        colorScheme = lightColorScheme(
            primary = Color(0xFF1F5F8B),
            secondary = Color(0xFF5B7C99),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFFFFFFF),
        ),
        statusColors = StatusColors(luteStatuses),
        readingBackground = Color.White,
        readingText = Color.Black,
        hoverUnderline = Color(0xFF1F5FFF),
        markedUnderline = Color(0xFFD32F2F),
        selectionBackground = Color(0xFFFFF176),
    )

    val sepia = default.copy(
        id = "sepia",
        label = "Sepia",
        colorScheme = lightColorScheme(
            primary = Color(0xFF7A4B2A),
            secondary = Color(0xFF9C7A54),
            surface = Color(0xFFF7EEDD),
            background = Color(0xFFF7EEDD),
            surfaceVariant = Color(0xFFEFE3CB),
        ),
        readingBackground = Color(0xFFF7EEDD),
        readingText = Color(0xFF3B2F2F),
    )

    val darkSlate = AppTheme(
        id = "dark_slate",
        label = "Dark slate",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = Color(0xFFACACF9),
            secondary = Color(0xFF8FA3B8),
            surface = Color(0xFF48484A),
            background = Color(0xFF48484A),
            surfaceVariant = Color(0xFF3A3A3C),
            onSurface = Color(0xFFC4C8CE),
            onBackground = Color(0xFFC4C8CE),
        ),
        statusColors = StatusColors(
            backgrounds = mapOf(
                TermStatus.UNKNOWN to Color(0xFFD5FFFF),
                TermStatus.NEW_1 to Color(0xFFB46B7A),
                TermStatus.NEW_2 to Color(0xFF988542),
                TermStatus.LEARNING_3 to Color(0xFF699859),
                TermStatus.LEARNING_4 to Color(0xFF5692AE),
                TermStatus.LEARNED to Color(0xFF877AAD),
                TermStatus.IGNORED to Color(0xFF7A4B4B),
                TermStatus.WELL_KNOWN to Color(0xFF419252),
            ),
            onHighlight = Color(0xFFEFF1F2),
            unknownAsText = true,
        ),
        readingBackground = Color(0xFF48484A),
        readingText = Color(0xFFC4C8CE),
        hoverUnderline = Color(0xFFACACF9),
        markedUnderline = Color(0xFFFF5C5C),
        selectionBackground = Color(0xFF6B6B2E),
    )

    val night = darkSlate.copy(
        id = "night",
        label = "Night",
        colorScheme = darkColorScheme(
            primary = Color(0xFF8AB4F8),
            secondary = Color(0xFF9AA0A6),
            surface = Color(0xFF121212),
            background = Color(0xFF121212),
            surfaceVariant = Color(0xFF1E1E1E),
            onSurface = Color(0xFFE0E0E0),
            onBackground = Color(0xFFE0E0E0),
        ),
        readingBackground = Color(0xFF121212),
        readingText = Color(0xFFE0E0E0),
    )

    val all: List<AppTheme> = listOf(default, sepia, darkSlate, night)

    fun byId(id: String): AppTheme = all.firstOrNull { it.id == id } ?: default

    fun next(id: String): AppTheme {
        val index = all.indexOfFirst { it.id == id }
        return all[(index + 1) % all.size]
    }
}
