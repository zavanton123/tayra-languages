package com.tayra.languages.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Screens are built from the design system: its components (core/ui components' Controls.kt,
 * Indicators.kt, Overlays.kt, Patterns.kt, Chips.kt, Rows.kt and Notices.kt) rather than Material's,
 * its corner radii and circle (theme/Shapes.kt) rather than shapes of their own, and its colours,
 * tint strengths, borders, spacing, sizes, text roles, elevations and motion (theme/Tokens.kt,
 * theme/Motion.kt and the theme) rather than literals; a one-off size is a named constant. A shape with different corners
 * (`RoundedCornerShape(topStart = …)`) is fine; the drawn flags and icons keep their colours.
 */
class DesignSystemTest {

    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }

    private val sources: List<File> = listOf("core/ui", "shared", "feature", "desktopApp", "androidApp", "webApp")
        .map { File(root, it) }
        .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" && "/src/" in it.path && "Main/" in it.path && "/build/" !in it.path }.toList() }

    /** The design system's own files, which wrap Material's components. */
    private val designSystem = listOf("Controls.kt", "Indicators.kt", "Overlays.kt", "Patterns.kt", "Chips.kt", "Rows.kt", "Notices.kt", "Motion.kt", "Dropdown.kt")
        .map { "core/ui/src/commonMain/kotlin/com/tayra/languages/core/ui/components/$it" }

    private fun File.isDesignSystem() = designSystem.any { path.endsWith(it) }

    private fun File.isTheme() = "core/ui/src/commonMain/kotlin/com/tayra/languages/core/ui/theme/" in path

    private val material = listOf(
        "Button", "OutlinedButton", "TextButton", "ElevatedButton", "FilledTonalButton", "OutlinedTextField", "TextField", "AlertDialog",
        "FilterChip", "ElevatedFilterChip", "InputChip", "SuggestionChip", "AssistChip", "IconButton", "FilledIconButton", "OutlinedIconButton",
        "LinearProgressIndicator", "CircularProgressIndicator", "HorizontalDivider", "VerticalDivider", "Checkbox", "TriStateCheckbox",
        "Switch", "Slider", "RangeSlider", "SegmentedButton", "SingleChoiceSegmentedButtonRow", "MultiChoiceSegmentedButtonRow",
        "ModalBottomSheet", "Snackbar", "Badge", "BadgedBox", "ListItem", "ExposedDropdownMenuBox", "Card", "ElevatedCard", "OutlinedCard",
        "Surface", "TooltipBox", "PlainTooltip", "RichTooltip",
    )

    private val controls = Regex("""^import androidx\.compose\.(?:material3\.(${material.joinToString("|")})|ui\.window\.(Dialog))$""")

    private val colour = Regex("""Color\(0x[0-9A-Fa-f]{6,8}\)""")

    private val border = Regex("""\.border\(\d|BorderStroke\(\d""")

    private val radius = Regex("""RoundedCornerShape\(\s*\d+(\.\d+)?(\.dp)?\s*\)""")

    private val circle = Regex("""\bCircleShape\b""")

    private val alpha = Regex("""copy\(alpha\s*=\s*\d""")

    private val iconSize = Regex("""\bIcon\(.*\.size\(\d+(\.\d+)?\.dp\)""")

    private val buttonHeight = Regex("""(Primary|Secondary|Quiet)Button\(.*\.height\(\d""")

    private val spacing = Regex("""(\.padding|spacedBy|PaddingValues)\([^)]*\b[1-9]\d*(\.\d+)?\.dp""")

    private val size = Regex("""\.(size|height|width|widthIn|heightIn|sizeIn|defaultMinSize|requiredSize|requiredHeight|requiredWidth|requiredSizeIn)\([^)]*\b[1-9]\d*(\.\d+)?\.dp""")

    private val weight = Regex("""fontWeight = FontWeight\.""")

    private val textSize = Regex("""\b\d+(\.\d+)?\.sp\b""")

    private val elevation = Regex("""(shadow\(|[eE]levation = )\d""")

    private val whiteOrBlack = Regex("""Color\.(White|Black)\b""")

    private val timing = Regex("""tween\(\s*\d|durationMillis\s*=\s*\d|delayMillis\s*=\s*\d|\bspring\(""")

    private fun findings(files: List<File>, rules: List<Pair<Regex, String>>): List<String> = files.flatMap { file ->
        file.readLines().mapIndexedNotNull { i, line ->
            rules.firstOrNull { (regex, _) -> regex.containsMatchIn(line) }?.let { (regex, hint) -> "${file.relativeTo(root)}:${i + 1}: ${regex.find(line)!!.value} ($hint)" }
        }
    }

    @Test
    fun screensUseTheDesignSystemsControls() {
        val found = sources.filter { !it.isDesignSystem() }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                controls.find(line.trim())?.let { "${file.relativeTo(root)}:${i + 1}: ${it.groupValues[1].ifEmpty { it.groupValues[2] }} (use the design system's component)" }
            }
        }
        if (found.isNotEmpty()) fail("Material controls outside the design system (${found.size}):\n" + found.joinToString("\n"))
    }

    @Test
    fun screensUseTheDesignSystemsColoursAndBorders() {
        val files = sources.filter { !it.isTheme() && !it.path.endsWith("components/LanguageFlags.kt") }
        val found = findings(files, listOf(colour to "use AppAccents, RatingColors or a theme colour")) +
            findings(files.filter { !it.isDesignSystem() }, listOf(border to "use AppBorders.hairline, AppBorders.selected or cardSurface"))
        if (found.isNotEmpty()) fail("Colours and borders outside the design system (${found.size}):\n" + found.joinToString("\n"))
    }

    @Test
    fun screensUseTheDesignSystemsTokens() {
        val drawn = listOf("components/LanguageFlags.kt", "components/AppIcons.kt")
        val found = findings(
            sources.filter { !it.isTheme() && drawn.none { d -> it.path.endsWith(d) } },
            listOf(
                circle to "use AppShapes.circle",
                alpha to "use an AppAlpha strength",
                iconSize to "use an AppSizes icon size",
                buttonHeight to "use ButtonSize.Large",
                spacing to "use AppSpacing",
                size to "use AppSizes, AppSpacing for a gap, or a named constant",
                weight to "use an AppText role",
                textSize to "use the typography or an AppText style",
                elevation to "use AppElevation",
                whiteOrBlack to "use AppAccents.onAccent, AppAccents.ink or a theme colour",
                timing to "use AppMotion's durations and AppMotion.spec",
            ),
        )
        if (found.isNotEmpty()) fail("Shapes, tints, spacing, sizes and text outside the design system's tokens (${found.size}):\n" + found.joinToString("\n"))
    }

    @Test
    fun screensUseTheDesignSystemsCornerRadii() {
        val found = sources.filter { !it.path.endsWith("core/ui/theme/Shapes.kt") }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                radius.find(line)?.let { "${file.relativeTo(root)}:${i + 1}: ${it.value} (use AppShapes)" }
            }
        }
        if (found.isNotEmpty()) fail("Corner radii outside AppShapes (${found.size}):\n" + found.joinToString("\n"))
    }
}
