package com.tayra.languages.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.ui.i18n.tr

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
    /** The name in English; [label] is the one to show. */
    val englishLabel: String,
    val isDark: Boolean,
    val colorScheme: ColorScheme,
    val statusColors: StatusColors,
    val readingBackground: Color,
    val readingText: Color,
    /** The letters of the selected word or phrase; its status background stays as it is. */
    val selectedText: Color,
) {
    /** The name in the interface language. */
    val label: String get() = tr(englishLabel)
}

object AppThemes {
    /**
     * New words are blue and words being learned yellow. The yellow fades towards [page] as the
     * status rises, so statuses 1 to 5 stay apart on the status buttons.
     */
    private fun statuses(unknown: Color, learning: Color, page: Color, ignored: Color, wellKnown: Color): Map<TermStatus, Color> =
        mapOf(TermStatus.UNKNOWN to unknown, TermStatus.IGNORED to ignored, TermStatus.WELL_KNOWN to wellKnown) +
            listOf(TermStatus.NEW_1, TermStatus.NEW_2, TermStatus.LEARNING_3, TermStatus.LEARNING_4)
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
        englishLabel = "Default",
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
        selectedText = Color(0xFFC2410C),
    )

    val sepia = default.copy(
        id = "sepia",
        englishLabel = "Sepia",
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

    val night = AppTheme(
        id = "night",
        englishLabel = "Night",
        isDark = true,
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
        selectedText = Color(0xFFFFB37A),
    )

    /**
     * A theme made from an editor colour scheme: its page and text colours, the accent for buttons
     * and links, and the hues that colour new (blue), learning (yellow), ignored (red) and
     * well-known (green) words. Panels, borders and highlights are mixed from these.
     */
    private fun palette(
        id: String,
        name: String,
        page: Long,
        text: Long,
        muted: Long,
        accent: Long,
        secondary: Long,
        blue: Long,
        yellow: Long,
        red: Long,
        green: Long,
        selected: Long,
    ): AppTheme {
        val bg = Color(page)
        val fg = Color(text)
        val dark = bg.luminance() < 0.5f
        val primary = Color(accent)
        val second = Color(secondary)
        val error = Color(red)
        fun mix(other: Color, amount: Float) = lerp(bg, other, amount)
        // Text on the accent: whichever of the page and text colours stands out more.
        val onPrimary = listOf(bg, fg, Color.White, Color.Black).maxBy { contrast(it, primary) }
        val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = mix(primary, 0.25f),
            onPrimaryContainer = fg,
            inversePrimary = lerp(primary, bg, 0.3f),
            secondary = second,
            onSecondary = onPrimary,
            secondaryContainer = mix(second, 0.25f),
            onSecondaryContainer = fg,
            tertiary = second,
            onTertiary = onPrimary,
            tertiaryContainer = mix(second, 0.25f),
            onTertiaryContainer = fg,
            background = bg,
            onBackground = fg,
            surface = bg,
            onSurface = fg,
            surfaceVariant = mix(fg, if (dark) 0.06f else 0.05f),
            onSurfaceVariant = Color(muted),
            surfaceTint = primary,
            inverseSurface = fg,
            inverseOnSurface = bg,
            error = error,
            onError = listOf(bg, fg, Color.White, Color.Black).maxBy { contrast(it, error) },
            errorContainer = mix(error, 0.25f),
            onErrorContainer = fg,
            outline = mix(fg, 0.45f),
            outlineVariant = mix(fg, if (dark) 0.16f else 0.12f),
            scrim = Color.Black,
            surfaceBright = mix(fg, 0.12f),
            surfaceDim = lerp(bg, Color.Black, 0.1f),
            surfaceContainerLowest = if (dark) lerp(bg, Color.Black, 0.15f) else lerp(bg, Color.White, 0.6f),
            surfaceContainerLow = mix(fg, 0.025f),
            surfaceContainer = mix(fg, 0.045f),
            surfaceContainerHigh = mix(fg, 0.07f),
            surfaceContainerHighest = mix(fg, 0.095f),
        )
        val statuses = if (dark) {
            StatusColors(
                statuses(unknown = mix(Color(blue), 0.38f), learning = mix(Color(yellow), 0.38f), page = bg, ignored = mix(error, 0.35f), wellKnown = mix(Color(green), 0.42f)),
                onHighlight = fg,
            )
        } else {
            StatusColors(statuses(unknown = mix(Color(blue), 0.22f), learning = mix(Color(yellow), 0.42f), page = bg, ignored = mix(error, 0.45f), wellKnown = mix(Color(green), 0.45f)))
        }
        return AppTheme(id, name, dark, scheme, statuses, readingBackground = bg, readingText = fg, selectedText = Color(selected))
    }

    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05f) / (dark + 0.05f)
    }

    private val editorThemes = listOf(
        palette("ayu_dark", "Ayu Dark", 0xFF0D1017, 0xFFBFBDB6, 0xFF8A9199, 0xFFE6B450, 0xFFFF8F40, 0xFF59C2FF, 0xFFE6B450, 0xFFF07178, 0xFFAAD94C, 0xFFFF8F40),
        palette("ayu_mirage", "Ayu Mirage", 0xFF1F2430, 0xFFCCCAC2, 0xFF9A9FA8, 0xFFFFCC66, 0xFFFFA659, 0xFF73D0FF, 0xFFFFCC66, 0xFFF28779, 0xFFD5FF80, 0xFFFFA659),
        palette("ayu_light", "Ayu Light", 0xFFFCFCFC, 0xFF5C6166, 0xFF787B80, 0xFFC25F00, 0xFF399EE6, 0xFF399EE6, 0xFFF2AE49, 0xFFE65050, 0xFF86B300, 0xFFC25F00),
        palette("base16_default_dark", "Base16 Default Dark", 0xFF181818, 0xFFD8D8D8, 0xFFA0A0A0, 0xFF7CAFC2, 0xFFBA8BAF, 0xFF7CAFC2, 0xFFF7CA88, 0xFFAB4642, 0xFFA1B56C, 0xFFDC9656),
        palette("base16_ocean", "Base16 Ocean", 0xFF2B303B, 0xFFC0C5CE, 0xFF9AA3AF, 0xFF8FA1B3, 0xFFB48EAD, 0xFF8FA1B3, 0xFFEBCB8B, 0xFFBF616A, 0xFFA3BE8C, 0xFFD08770),
        palette("catppuccin_latte", "Catppuccin Latte", 0xFFEFF1F5, 0xFF4C4F69, 0xFF6C6F85, 0xFF8839EF, 0xFF1E66F5, 0xFF1E66F5, 0xFFDF8E1D, 0xFFD20F39, 0xFF40A02B, 0xFFFE640B),
        palette("catppuccin_frappe", "Catppuccin Frappé", 0xFF303446, 0xFFC6D0F5, 0xFFA5ADCE, 0xFFCA9EE6, 0xFF8CAAEE, 0xFF8CAAEE, 0xFFE5C890, 0xFFE78284, 0xFFA6D189, 0xFFEF9F76),
        palette("catppuccin_macchiato", "Catppuccin Macchiato", 0xFF24273A, 0xFFCAD3F5, 0xFFA5ADCB, 0xFFC6A0F6, 0xFF8AADF4, 0xFF8AADF4, 0xFFEED49F, 0xFFED8796, 0xFFA6DA95, 0xFFF5A97F),
        palette("catppuccin_mocha", "Catppuccin Mocha", 0xFF1E1E2E, 0xFFCDD6F4, 0xFFA6ADC8, 0xFFCBA6F7, 0xFF89B4FA, 0xFF89B4FA, 0xFFF9E2AF, 0xFFF38BA8, 0xFFA6E3A1, 0xFFFAB387),
        palette("cyberdream", "Cyberdream", 0xFF16181A, 0xFFFFFFFF, 0xFF7B8496, 0xFF5EA1FF, 0xFFBD5EFF, 0xFF5EA1FF, 0xFFF1FF5E, 0xFFFF6E5E, 0xFF5EFF6C, 0xFFFF5EF1),
        palette("darcula", "Darcula", 0xFF2B2B2B, 0xFFA9B7C6, 0xFF8E959C, 0xFF589DF6, 0xFFCC7832, 0xFF6897BB, 0xFFFFC66D, 0xFFBC3F3C, 0xFF6A8759, 0xFFCC7832),
        palette("dracula", "Dracula", 0xFF282A36, 0xFFF8F8F2, 0xFFA0A8CC, 0xFFBD93F9, 0xFFFF79C6, 0xFF8BE9FD, 0xFFF1FA8C, 0xFFFF5555, 0xFF50FA7B, 0xFFFF79C6),
        palette("everforest", "Everforest", 0xFF2D353B, 0xFFD3C6AA, 0xFF9DA9A0, 0xFFA7C080, 0xFF83C092, 0xFF7FBBB3, 0xFFDBBC7F, 0xFFE67E80, 0xFFA7C080, 0xFFE69875),
        palette("github_dark", "GitHub Dark", 0xFF0D1117, 0xFFE6EDF3, 0xFF8D96A0, 0xFF58A6FF, 0xFFBC8CFF, 0xFF58A6FF, 0xFFD29922, 0xFFF85149, 0xFF3FB950, 0xFFFFA657),
        palette("github_light", "GitHub Light", 0xFFFFFFFF, 0xFF1F2328, 0xFF59636E, 0xFF0969DA, 0xFF8250DF, 0xFF0969DA, 0xFFD4A72C, 0xFFCF222E, 0xFF1A7F37, 0xFFBC4C00),
        palette("gruvbox_dark", "Gruvbox Dark", 0xFF282828, 0xFFEBDBB2, 0xFFA89984, 0xFFFE8019, 0xFFB8BB26, 0xFF83A598, 0xFFFABD2F, 0xFFFB4934, 0xFFB8BB26, 0xFFD3869B),
        palette("gruvbox_light", "Gruvbox Light", 0xFFFBF1C7, 0xFF3C3836, 0xFF7C6F64, 0xFFAF3A03, 0xFF79740E, 0xFF076678, 0xFFD79921, 0xFF9D0006, 0xFF79740E, 0xFF8F3F71),
        palette("kanagawa", "Kanagawa", 0xFF1F1F28, 0xFFDCD7BA, 0xFF9C9A8E, 0xFF7E9CD8, 0xFF957FB8, 0xFF7E9CD8, 0xFFE6C384, 0xFFE46876, 0xFF98BB6C, 0xFFFFA066),
        palette("material_darker", "Material Darker", 0xFF212121, 0xFFEEFFFF, 0xFFB0BEC5, 0xFF80CBC4, 0xFFC792EA, 0xFF82AAFF, 0xFFFFCB6B, 0xFFF07178, 0xFFC3E88D, 0xFFF78C6C),
        palette("material_ocean", "Material Ocean", 0xFF0F111A, 0xFFA6ACCD, 0xFF8F93A2, 0xFF84FFFF, 0xFFC792EA, 0xFF82AAFF, 0xFFFFCB6B, 0xFFF07178, 0xFFC3E88D, 0xFFF78C6C),
        palette("material_palenight", "Material Palenight", 0xFF292D3E, 0xFFA6ACCD, 0xFF868CB0, 0xFF82AAFF, 0xFFC792EA, 0xFF82AAFF, 0xFFFFCB6B, 0xFFF07178, 0xFFC3E88D, 0xFFF78C6C),
        palette("monokai", "Monokai", 0xFF272822, 0xFFF8F8F2, 0xFFA59F85, 0xFF66D9EF, 0xFFF92672, 0xFF66D9EF, 0xFFE6DB74, 0xFFF92672, 0xFFA6E22E, 0xFFFD971F),
        palette("night_owl", "Night Owl", 0xFF011627, 0xFFD6DEEB, 0xFF8FA2B5, 0xFF82AAFF, 0xFFC792EA, 0xFF82AAFF, 0xFFECC48D, 0xFFEF5350, 0xFFADDB67, 0xFFF78C6C),
        palette("nord", "Nord", 0xFF2E3440, 0xFFD8DEE9, 0xFF9AA3B5, 0xFF88C0D0, 0xFF81A1C1, 0xFF5E81AC, 0xFFEBCB8B, 0xFFBF616A, 0xFFA3BE8C, 0xFFD08770),
        palette("one_dark", "One Dark", 0xFF282C34, 0xFFABB2BF, 0xFF7F848E, 0xFF61AFEF, 0xFFC678DD, 0xFF61AFEF, 0xFFE5C07B, 0xFFE06C75, 0xFF98C379, 0xFFD19A66),
        palette("oxocarbon", "Oxocarbon", 0xFF161616, 0xFFF2F4F8, 0xFFA2A9B0, 0xFF78A9FF, 0xFFBE95FF, 0xFF33B1FF, 0xFFF1C21B, 0xFFEE5396, 0xFF42BE65, 0xFFFF7EB6),
        palette("rose_pine", "Rosé Pine", 0xFF191724, 0xFFE0DEF4, 0xFF908CAA, 0xFFC4A7E7, 0xFFEBBCBA, 0xFF31748F, 0xFFF6C177, 0xFFEB6F92, 0xFF9CCFD8, 0xFFEBBCBA),
        palette("rose_pine_dawn", "Rosé Pine Dawn", 0xFFFAF4ED, 0xFF575279, 0xFF797593, 0xFF7A639A, 0xFFD7827E, 0xFF286983, 0xFFEA9D34, 0xFFB4637A, 0xFF56949F, 0xFFD7827E),
        palette("solarized_dark", "Solarized Dark", 0xFF002B36, 0xFF93A1A1, 0xFF839496, 0xFF268BD2, 0xFF2AA198, 0xFF268BD2, 0xFFB58900, 0xFFDC322F, 0xFF859900, 0xFFCB4B16),
        palette("solarized_light", "Solarized Light", 0xFFFDF6E3, 0xFF586E75, 0xFF657B83, 0xFF268BD2, 0xFF2AA198, 0xFF268BD2, 0xFFB58900, 0xFFDC322F, 0xFF859900, 0xFFCB4B16),
        palette("tokyo_night", "Tokyo Night", 0xFF1A1B26, 0xFFC0CAF5, 0xFF9AA5CE, 0xFF7AA2F7, 0xFFBB9AF7, 0xFF7AA2F7, 0xFFE0AF68, 0xFFF7768E, 0xFF9ECE6A, 0xFFFF9E64),
        palette("tomorrow", "Tomorrow", 0xFFFFFFFF, 0xFF4D4D4C, 0xFF737573, 0xFF4271AE, 0xFF8959A8, 0xFF4271AE, 0xFFEAB700, 0xFFC82829, 0xFF718C00, 0xFFC8600F),
        palette("tomorrow_night", "Tomorrow Night", 0xFF1D1F21, 0xFFC5C8C6, 0xFF969896, 0xFF81A2BE, 0xFFB294BB, 0xFF81A2BE, 0xFFF0C674, 0xFFCC6666, 0xFFB5BD68, 0xFFDE935F),
    )

    val all: List<AppTheme> = listOf(default, sepia, night) + editorThemes

    /** Themes that were removed, and the one that now stands in for each. */
    private val retired = mapOf("dark_slate" to "darcula")

    fun byId(id: String): AppTheme = (retired[id] ?: id).let { wanted -> all.firstOrNull { it.id == wanted } } ?: default

    fun next(id: String): AppTheme {
        val index = all.indexOf(byId(id))
        return all[(index + 1) % all.size]
    }
}
