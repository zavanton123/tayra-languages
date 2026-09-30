package com.tayra.languages.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    /**
     * New words are blue and words being learned yellow. The yellow fades towards [page] as the
     * status rises, so statuses 1 to 5 stay apart on the status buttons.
     */
    private fun statuses(unknown: Color, learning: Color, page: Color, ignored: Color, wellKnown: Color): Map<TermStatus, Color> =
        mapOf(TermStatus.UNKNOWN to unknown, TermStatus.IGNORED to ignored, TermStatus.WELL_KNOWN to wellKnown) +
            listOf(TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4, TermStatus.LEARNED)
                .mapIndexed { level, status -> status to lerp(learning, page, level * LEARNING_FADE) }

    private const val LEARNING_FADE = 0.1f

    private fun lightStatuses(page: Color) = StatusColors(
        statuses(unknown = Color(0xFFCDE0FB), learning = Color(0xFFFAE39E), page = page, ignored = Color(0xFFEE8577), wellKnown = Color(0xFF72DA88)),
    )

    private fun darkStatuses(unknown: Color, learning: Color, page: Color) = StatusColors(
        statuses(unknown = unknown, learning = learning, page = page, ignored = Color(0xFF7A4B4B), wellKnown = Color(0xFF419252)),
        onHighlight = Color(0xFFEFF1F2),
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
            onSurface = Color(0xFF1B1F24),
            onBackground = Color(0xFF1B1F24),
            surfaceVariant = Color(0xFFF1F3F5),
            onSurfaceVariant = Color(0xFF5B6470),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF8F9FB),
            surfaceContainer = Color(0xFFF3F4F6),
            surfaceContainerHigh = Color(0xFFEDEFF2),
            surfaceContainerHighest = Color(0xFFE6E8EB),
            outline = Color(0xFF8A929C),
            outlineVariant = Color(0xFFDDE1E6),
        ),
        statusColors = lightStatuses(page = Color.White),
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
            onSurfaceVariant = Color(0xFF6B5A45),
            surfaceContainerLowest = Color(0xFFFDF8EE),
            surfaceContainerLow = Color(0xFFF4EAD6),
            surfaceContainer = Color(0xFFF0E4CD),
            surfaceContainerHigh = Color(0xFFEADCC2),
            surfaceContainerHighest = Color(0xFFE3D3B6),
            outline = Color(0xFF9C8B70),
            outlineVariant = Color(0xFFDCCDB0),
        ),
        statusColors = lightStatuses(page = Color(0xFFF7EEDD)),
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
            onSurfaceVariant = Color(0xFFA6ABB3),
            surfaceContainerLowest = Color(0xFF3A3A3C),
            surfaceContainerLow = Color(0xFF424244),
            surfaceContainer = Color(0xFF474749),
            surfaceContainerHigh = Color(0xFF515153),
            surfaceContainerHighest = Color(0xFF5B5B5D),
            outline = Color(0xFF8A8A8E),
            outlineVariant = Color(0xFF5E5E61),
        ),
        statusColors = darkStatuses(unknown = Color(0xFF3E5277), learning = Color(0xFF8C7539), page = Color(0xFF48484A)),
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
            onSurfaceVariant = Color(0xFFA0A0A0),
            surfaceContainerLowest = Color(0xFF0E0E0E),
            surfaceContainerLow = Color(0xFF181818),
            surfaceContainer = Color(0xFF1E1E1E),
            surfaceContainerHigh = Color(0xFF262626),
            surfaceContainerHighest = Color(0xFF2E2E2E),
            outline = Color(0xFF7A7A7A),
            outlineVariant = Color(0xFF383838),
        ),
        statusColors = darkStatuses(unknown = Color(0xFF26345A), learning = Color(0xFF6E5A2C), page = Color(0xFF121212)),
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
