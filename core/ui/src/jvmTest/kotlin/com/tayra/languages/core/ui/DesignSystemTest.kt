package com.tayra.languages.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Screens are built from the design system: its controls and indicators (core/ui components'
 * Controls.kt, Indicators.kt, Overlays.kt and Patterns.kt) rather than Material's, its corner radii
 * (theme/Shapes.kt) rather than radii of their own, its colours (theme/Tokens.kt and the theme)
 * rather than colour literals, and its hairline rather than borders of their own. A shape with
 * different corners (`RoundedCornerShape(topStart = …)`) is fine; the drawn flags keep their colours.
 */
class DesignSystemTest {

    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }

    private val sources: List<File> = listOf("core/ui", "shared", "feature", "desktopApp", "androidApp", "webApp")
        .map { File(root, it) }
        .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" && "/src/" in it.path && "Main/" in it.path && "/build/" !in it.path }.toList() }

    /** The design system's own files, which wrap Material's components. */
    private val designSystem = listOf("components/Controls.kt", "components/Indicators.kt", "components/Overlays.kt", "components/Patterns.kt")

    private val controls = Regex("""^import androidx\.compose\.(?:material3\.(Button|OutlinedButton|TextButton|ElevatedButton|FilledTonalButton|OutlinedTextField|TextField|AlertDialog|FilterChip|IconButton|FilledIconButton|OutlinedIconButton|LinearProgressIndicator|CircularProgressIndicator|HorizontalDivider|VerticalDivider|Checkbox|ModalBottomSheet|Snackbar)|ui\.window\.(Dialog))$""")

    private val colour = Regex("""Color\(0x[0-9A-Fa-f]{6,8}\)""")

    private val border = Regex("""\.border\(1\.dp""")

    private val radius = Regex("""RoundedCornerShape\(\s*\d+(\.\d+)?(\.dp)?\s*\)""")

    @Test
    fun screensUseTheDesignSystemsControls() {
        val found = sources.filter { file -> designSystem.none { file.path.endsWith("core/ui/$it") } }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                controls.find(line.trim())?.let { "${file.relativeTo(root)}:${i + 1}: ${it.groupValues[1].ifEmpty { it.groupValues[2] }} (use the design system's component)" }
            }
        }
        if (found.isNotEmpty()) fail("Material controls outside the design system (${found.size}):\n" + found.joinToString("\n"))
    }

    @Test
    fun screensUseTheDesignSystemsColoursAndBorders() {
        val exempt = listOf("core/ui/theme/", "core/ui/components/LanguageFlags.kt")
        val found = sources.filter { file -> exempt.none { it in file.path } }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                when {
                    colour.containsMatchIn(line) -> "${file.relativeTo(root)}:${i + 1}: ${colour.find(line)!!.value} (use AppAccents, RatingColors or a theme colour)"
                    border.containsMatchIn(line) -> "${file.relativeTo(root)}:${i + 1}: .border(1.dp (use AppBorders.hairline or cardSurface)"
                    else -> null
                }
            }
        }
        if (found.isNotEmpty()) fail("Colours and borders outside the design system (${found.size}):\n" + found.joinToString("\n"))
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
