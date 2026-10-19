package com.tayra.languages.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Screens are built from the design system: its controls (core/ui components/Controls.kt) rather
 * than Material's buttons, text fields, dialogs and filter chips, and its corner radii
 * (theme/Shapes.kt) rather than radii of their own. A shape with different corners
 * (`RoundedCornerShape(topStart = …)`) is fine.
 */
class DesignSystemTest {

    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }

    private val sources: List<File> = listOf("core/ui", "shared", "feature", "desktopApp", "androidApp", "webApp")
        .map { File(root, it) }
        .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" && "/src/" in it.path && "Main/" in it.path && "/build/" !in it.path }.toList() }

    private val controls = Regex("""^import androidx\.compose\.material3\.(Button|OutlinedButton|TextButton|ElevatedButton|FilledTonalButton|OutlinedTextField|TextField|AlertDialog|FilterChip)$""")

    private val radius = Regex("""RoundedCornerShape\(\s*\d+(\.\d+)?(\.dp)?\s*\)""")

    @Test
    fun screensUseTheDesignSystemsControls() {
        val found = sources.filter { !it.path.endsWith("core/ui/components/Controls.kt") }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                controls.find(line.trim())?.let { "${file.relativeTo(root)}:${i + 1}: ${it.groupValues[1]} (use PrimaryButton, SecondaryButton, QuietButton, AppTextField, AppDialog or AppFilterChip)" }
            }
        }
        if (found.isNotEmpty()) fail("Material controls outside the design system (${found.size}):\n" + found.joinToString("\n"))
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
