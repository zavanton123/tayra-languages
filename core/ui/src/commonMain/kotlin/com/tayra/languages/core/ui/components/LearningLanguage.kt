package com.tayra.languages.core.ui.components

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import com.tayra.languages.core.domain.morphology.ChineseScript
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.ui.components.itemMotion
import com.tayra.languages.core.ui.i18n.formatCount
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.i18n.trPlural
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppAlpha
import com.tayra.languages.core.ui.theme.AppBorders
import com.tayra.languages.core.ui.theme.AppShapes
import com.tayra.languages.core.ui.theme.AppSizes
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText
import kotlinx.coroutines.launch

/**
 * The language being learned and the ones to choose from; [languages] are ids with their English names, shown through `tr`.
 * [nativeIds] are listed but cannot be chosen, being the native language. [knownWords] is how many
 * words the reader knows in the current language, shown after its name; null leaves it out.
 * [scripts] are the Chinese languages with the script each is read in: such a language is offered
 * once in each script, as two languages that share its words, and choosing one calls [onSelectScript].
 * [mine] are the languages being learned, which the language menu switches between, with
 * [wordCounts], the words known in each.
 */
class LearningLanguageState(
    val languages: List<Pair<Long, String>>,
    val currentId: Long,
    val nativeIds: Set<Long> = emptySet(),
    val knownWords: Int? = null,
    val scripts: Map<Long, ChineseScript?> = emptyMap(),
    val onSelectScript: ((Long, ChineseScript) -> Unit)? = null,
    val mine: List<Long> = emptyList(),
    val wordCounts: Map<Long, Int> = emptyMap(),
    val onSelect: (Long) -> Unit,
) {
    val currentName: String? get() = languages.firstOrNull { it.first == currentId }?.second

    /** Every language as it is offered, by its English label. */
    val choices: List<LanguageChoice> = languages.flatMap { (id, name) ->
        if (id in scripts) ChineseScript.entries.map { LanguageChoice(id, name, it) } else listOf(LanguageChoice(id, name))
    }.sortedBy { it.label }

    /** The language being learned as it is offered: in its script, or as written when it has none. */
    val current: LanguageChoice? get() = currentName?.let { LanguageChoice(currentId, it, scripts[currentId]) }

    /** The languages being learned as the menu lists them, a Chinese one in the script it is read in; just the current one before they are known. */
    val myChoices: List<LanguageChoice> get() = mine.ifEmpty { listOf(currentId) }.mapNotNull { id ->
        languages.firstOrNull { it.first == id }?.let { LanguageChoice(id, it.second, scripts[id]) }
    }.sortedBy { it.label }

    /** The words known in the language with [id], when counted. */
    fun wordsKnown(id: Long): Int? = if (id == currentId) knownWords ?: wordCounts[id] else wordCounts[id]

    /** Learns [choice] from now on, in its script. */
    fun select(choice: LanguageChoice) {
        val script = choice.script
        if (script != null && onSelectScript != null) onSelectScript.invoke(choice.id, script) else onSelect(choice.id)
    }
}

/** A language to learn as the language choice offers it: Chinese in [script], when it has one, or the language. */
data class LanguageChoice(val id: Long, val name: String, val script: ChineseScript? = null) {
    /** The English label, shown through `tr`. */
    val label: String get() = when (script) {
        ChineseScript.SIMPLIFIED -> "Chinese (Simplified)"
        ChineseScript.TRADITIONAL -> "Chinese (Traditional)"
        null -> name
    }

    /** The tag a test finds it by: the language's code, and the script's. */
    val tag: String get() = "language-" + (LanguageCodes.codeFor(name) ?: name) + when (script) {
        ChineseScript.SIMPLIFIED -> "-hans"
        ChineseScript.TRADITIONAL -> "-hant"
        null -> ""
    }

    /** Whether [needle], lowercase, is in its label or its name, in English or as shown. */
    @Composable
    fun matches(needle: String): Boolean = needle.isEmpty() || listOf(tr(label), label, tr(name), name).any { it.lowercase().contains(needle) }
}

/** Provided at the root of the app; without it the top bar shows no language selector. */
val LocalLearningLanguage = staticCompositionLocalOf<LearningLanguageState?> { null }

private val badgeTints = AppAccents.sequence.take(7)

/** A round badge with the language's two-letter code, tinted per language. */
@Composable
fun LanguageCodeBadge(languageName: String, size: Dp = 28.dp) {
    val code = LanguageCodes.codeFor(languageName)?.uppercase() ?: languageName.take(2).uppercase()
    val tint = badgeTints[(code.hashCode() and Int.MAX_VALUE) % badgeTints.size]
    Box(Modifier.size(size).clip(AppShapes.circle).background(tint.copy(alpha = AppAlpha.soft)), contentAlignment = Alignment.Center) {
        Text(code, color = AppAccents.onWash(tint), style = AppText.badge)
    }
}

/**
 * The global choice of the language being learned, for the top bar: the name with its flag on
 * wide screens, just the flag on [compact] ones. It opens a menu of the languages being learned
 * and a way to add one, which opens the language dialog, or calls [onOpen] where the choice is a
 * screen of its own (phones). Shows nothing until languages exist.
 */
@Composable
fun LearningLanguageSelector(compact: Boolean, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) {
    val state = LocalLearningLanguage.current ?: return
    val current = state.current ?: return
    val colors = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    // "Portuguese (432)": the words known in the language, as the Word frequency page counts them.
    val label = state.knownWords?.let { "${tr(current.label)} (${formatCount(it)})" } ?: tr(current.label)
    Box(modifier) {
        Row(
            Modifier.clip(AppShapes.panel)
                .background(colors.primary.copy(alpha = AppAlpha.wash))
                .border(AppBorders.hairline(colors.primary.copy(alpha = AppAlpha.soft)), AppShapes.panel)
                .clickable { menu = true }
                .semantics { contentDescription = tr("Learning language: {0}", label) }
                .padding(horizontal = if (compact) AppSpacing.s else AppSpacing.m, vertical = AppSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) AppSpacing.xs else AppSpacing.s),
        ) {
            LanguageFlag(current.label, if (compact) 18.dp else 22.dp)
            if (!compact) {
                Text(label, style = AppText.smallTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(AppSizes.icon))
        }
        AppMenu(menu, onDismissRequest = { menu = false }, Modifier.testTag("language-menu")) {
            state.myChoices.forEach { choice ->
                AppMenuItem(
                    text = { MyLanguageRow(choice, state.wordsKnown(choice.id), chosen = choice == current) },
                    onClick = {
                        menu = false
                        if (choice != current) state.select(choice)
                    },
                    modifier = Modifier.testTag("my-" + choice.tag),
                )
            }
            AppDivider(Modifier.padding(vertical = AppSpacing.xs))
            AppMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(AppSizes.icon))
                        Text(tr("Add a language…"), color = colors.primary)
                    }
                },
                onClick = {
                    menu = false
                    if (onOpen != null) onOpen() else adding = true
                },
                modifier = Modifier.testTag("add-language"),
            )
        }
        if (adding) LanguageDialog(state, onDismiss = { adding = false })
    }
}

/** A language being learned in the language menu: its flag, name and the words known in it, with a check on the current one. */
@Composable
private fun MyLanguageRow(choice: LanguageChoice, words: Int?, chosen: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.padding(vertical = AppSpacing.xxs), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        LanguageFlag(choice.label, 22.dp)
        Column(Modifier.weight(1f, fill = false)) {
            Text(tr(choice.label), fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (words != null) Text(trPlural(words, "{0} word", "{0} words", formatCount(words)), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        if (chosen) Icon(Icons.Default.Check, contentDescription = tr("Selected"), tint = colors.primary, modifier = Modifier.size(AppSizes.iconCompact))
    }
}

/**
 * The phone's screen for adding a language: every language not being learned yet as a row with its
 * flag, with a letter index down the right edge that jumps the list. Choosing one goes back, and
 * the app moves on to that language's courses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseLanguageScreen(onBack: () -> Unit) {
    val state = LocalLearningLanguage.current
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val needle = query.trim().lowercase()
    val mine = state?.myChoices.orEmpty()
    val others = state?.choices.orEmpty().filter { it !in mine && it.matches(needle) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // The rows before the languages: search and the heading.
    val rowsBefore = 2
    val letters = others.map { tr(it.label).first().uppercaseChar() }.distinct()
    var indexWidth by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tr("Add a language"), style = AppText.sectionTitle) },
                navigationIcon = { AppIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back"), onClick = onBack, tint = colors.primary) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colors.surface),
            )
        },
        containerColor = colors.surface,
    ) { padding ->
        if (state == null) return@Scaffold
        Box(Modifier.padding(padding).fillMaxSize().testTag("choose-language")) {
            LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(bottom = AppSpacing.xxl)) {
                item {
                    SearchField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = tr("Search languages"),
                        modifier = Modifier.padding(horizontal = AppSpacing.l).fillMaxWidth(),
                        fieldModifier = Modifier.testTag("language-search"),
                    )
                }
                item { PhoneSectionTitle(tr("All languages"), trPlural(others.size, "{0} language", "{0} languages")) }
                if (others.isEmpty()) {
                    item { Text(tr("No languages match"), Modifier.padding(horizontal = AppSpacing.l, vertical = AppSpacing.l), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant) }
                }
                itemsIndexed(others, key = { _, choice -> choice.tag }) { index, choice -> Column(itemMotion()) {
                    val native = choice.id in state.nativeIds
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = !native) { onBack(); state.select(choice) }.alpha(if (native) AppAlpha.disabled else 1f)
                            .padding(start = AppSpacing.xxl, end = AppSpacing.huge, top = AppSpacing.m, bottom = AppSpacing.m).testTag(choice.tag),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xl),
                    ) {
                        LanguageFlag(choice.label, 30.dp, Modifier.clip(AppShapes.tiny))
                        Text(tr(choice.label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (native) Text(tr("Your native language"), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
                    }
                    if (index < others.lastIndex) AppDivider(Modifier.padding(start = AppSpacing.l, end = AppSpacing.xxxl), color = colors.outlineVariant.copy(alpha = AppAlpha.muted))
                }}
            }
            if (letters.size > 1) {
                Column(Modifier.align(Alignment.CenterEnd).onSizeChanged { indexWidth = it.width }.padding(end = AppSpacing.xs), horizontalAlignment = Alignment.CenterHorizontally) {
                    letters.forEach { letter ->
                        Text(
                            letter.toString(),
                            Modifier.clip(AppShapes.circle)
                                .clickable(onClickLabel = tr("Jump to {0}", letter.toString())) {
                                    val target = others.indexOfFirst { tr(it.label).first().uppercaseChar() == letter }
                                    if (target >= 0) scope.launch { list.animateScrollToItem(rowsBefore + target) }
                                }
                                .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs)
                                .testTag("index-$letter"),
                            style = AppText.badge,
                            color = colors.primary,
                        )
                    }
                }
            }
            // On the desktop, the bar runs beside the letters rather than under them.
            EdgeScrollbar(list, Modifier.padding(end = with(LocalDensity.current) { indexWidth.toDp() }))
        }
    }
}

@Composable
private fun PhoneSectionTitle(title: String, note: String? = null) {
    Row(Modifier.padding(start = AppSpacing.l, end = AppSpacing.l, top = AppSpacing.xxl, bottom = AppSpacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Text(title, style = AppText.sectionTitle)
        if (note != null) Text(note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Asks for the language to learn while none is chosen, as on first start; it stays until one is. */
@Composable
fun TargetLanguageDialog(state: LearningLanguageState) {
    LanguageDialog(state, onDismiss = {}, required = true)
}

/**
 * The languages to add, as a dialog: those not being learned yet, each as a tile in a grid,
 * narrowed by a search box that has the focus from the start on wide screens. The native language
 * is listed but cannot be chosen. A [required] choice, the first language, offers every language
 * and has no way to close the dialog but choosing.
 */
@Composable
private fun LanguageDialog(state: LearningLanguageState, onDismiss: () -> Unit, required: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val width = LocalWindowWidth.current
    val columns = when (width) {
        WindowWidth.COMPACT -> 1
        WindowWidth.MEDIUM -> 2
        WindowWidth.EXPANDED -> 3
    }
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    // A phone's keyboard would cover the languages.
    LaunchedEffect(Unit) { if (!width.isCompact) runCatching { focus.requestFocus() } }
    val needle = query.trim().lowercase()
    val mine = if (required) emptyList() else state.myChoices
    val others = state.choices.filter { it !in mine && it.matches(needle) }
    AppDialogSurface(onDismissRequest = onDismiss, modifier = Modifier.fillMaxHeight(0.92f).testTag(if (required) "target-language-dialog" else "language-dialog"), maxWidth = 1160.dp, dismissible = !required) {
        Column(Modifier.padding(horizontal = if (width.isCompact) AppSpacing.xl else AppSpacing.xxxl, vertical = AppSpacing.xxl)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(if (required) tr("Select target language") else tr("Add a language"), style = AppText.screenTitle)
                    Text(
                        if (required) tr("Choose the language you want to study") else tr("Start learning another language"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                if (!required) AppIconButton(Icons.Default.Close, contentDescription = tr("Close"), onClick = onDismiss, style = IconButtonStyle.Tonal, tint = colors.onSurface)
            }
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = tr("Search languages"),
                modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.xxl),
                fieldModifier = Modifier.focusRequester(focus).testTag("language-search"),
            )
            ScrollColumn(Modifier.fillMaxWidth().weight(1f), contentModifier = Modifier.padding(top = AppSpacing.s, end = AppSpacing.l)) {
                SectionTitle(tr("All languages"), trPlural(others.size, "{0} language", "{0} languages"))
                if (others.isEmpty()) {
                    Text(tr("No languages match"), Modifier.padding(vertical = AppSpacing.l), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                }
                others.chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = AppSpacing.m), horizontalArrangement = Arrangement.spacedBy(AppSpacing.l)) {
                        row.forEach { choice ->
                            LanguageTile(choice, native = choice.id in state.nativeIds, Modifier.weight(1f)) { onDismiss(); state.select(choice) }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, note: String? = null) {
    Row(Modifier.padding(top = AppSpacing.xl, bottom = AppSpacing.m), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        Text(title, style = AppText.sectionTitle)
        if (note != null) Text(note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A language to choose: its flag and name. The native one is dimmed and says why it cannot be chosen. */
@Composable
private fun LanguageTile(choice: LanguageChoice, native: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val press = remember { MutableInteractionSource() }
    Row(
        modifier.height(LANGUAGE_TILE_HEIGHT).pressScale(press).cardSurface(AppShapes.panel)
            .clickable(press, LocalIndication.current, enabled = !native, onClick = onClick).alpha(if (native) AppAlpha.disabled else 1f).padding(horizontal = AppSpacing.l).testTag(choice.tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        LanguageFlag(choice.label, 22.dp)
        Text(tr(choice.label), Modifier.weight(1f), style = AppText.emphasis, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (native) Text(tr("Your native language"), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
    }
}

/**
 * One language in a language menu: its flag, [label] and a check when [chosen]. One that cannot be
 * chosen is dimmed, with [unavailableNote] saying why.
 */
@Composable
fun LanguageMenuRow(flagName: String, label: String, chosen: Boolean, unavailableNote: String? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.alpha(if (unavailableNote != null) AppAlpha.disabled else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        LanguageFlag(flagName, 18.dp)
        Text(label, Modifier.weight(1f, fill = false), fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.weight(1f))
        if (unavailableNote != null) Text(unavailableNote, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
        if (chosen) Icon(Icons.Default.Check, contentDescription = tr("Selected"), tint = colors.primary, modifier = Modifier.size(AppSizes.iconCompact))
    }
}

private val LANGUAGE_TILE_HEIGHT = 50.dp
