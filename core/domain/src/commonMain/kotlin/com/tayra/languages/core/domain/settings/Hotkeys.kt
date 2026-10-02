package com.tayra.languages.core.domain.settings

/**
 * A keyboard shortcut. [key] is a platform-neutral key name: a single character
 * for letters and digits (`W`, `1`), or names like `Up`, `Down`, `Left`, `Right`,
 * `Enter`, `Escape`, `Space`, `Tab`, `F1`..`F12`.
 */
data class Hotkey(
    val key: String,
    val shift: Boolean = false,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
) {
    /** Serialised form, e.g. `Ctrl+Shift+T`. */
    val serialized: String
        get() = buildString {
            if (ctrl) append("Ctrl+")
            if (alt) append("Alt+")
            if (shift) append("Shift+")
            append(key)
        }

    override fun toString(): String = serialized

    companion object {
        fun parse(text: String): Hotkey? {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return null
            val parts = trimmed.split("+").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) return null
            val key = parts.last()
            val modifiers = parts.dropLast(1).map { it.lowercase() }
            return Hotkey(
                key = if (key.length == 1) key.uppercase() else key,
                shift = "shift" in modifiers,
                ctrl = "ctrl" in modifiers || "cmd" in modifiers || "meta" in modifiers,
                alt = "alt" in modifiers,
            )
        }
    }
}

enum class HotkeyCategory(val label: String) {
    NAVIGATION("Navigation"),
    LISTENING("Listening"),
    STATUS("Update status"),
    PAGING("Paging"),
    TRANSLATE("Translate"),
    COPY("Copy"),
    MISC("Misc"),
}

/**
 * All customisable hotkeys with their defaults. The plain arrows read aloud; moving between words
 * and changing a word's status take Ctrl (⌘ on a Mac) with the arrows. A key shared by a word
 * shortcut and a listening one (W) acts on the word while one is selected, otherwise on reading
 * aloud; see [HotkeyAction.resolve].
 */
enum class HotkeyAction(val category: HotkeyCategory, val description: String, val default: Hotkey?) {
    START_HOVER(HotkeyCategory.NAVIGATION, "Deselect all words", Hotkey("Escape")),
    PREV_WORD(HotkeyCategory.NAVIGATION, "Move to previous word", Hotkey("Left", ctrl = true)),
    NEXT_WORD(HotkeyCategory.NAVIGATION, "Move to next word", Hotkey("Right", ctrl = true)),
    PREV_UNKNOWN_WORD(HotkeyCategory.NAVIGATION, "Move to previous unknown word", null),
    NEXT_UNKNOWN_WORD(HotkeyCategory.NAVIGATION, "Move to next unknown word", null),
    PREV_SENTENCE(HotkeyCategory.NAVIGATION, "Move to previous sentence", null),
    NEXT_SENTENCE(HotkeyCategory.NAVIGATION, "Move to next sentence", null),

    LISTEN_PLAY_PAUSE(HotkeyCategory.LISTENING, "Play or pause reading aloud", Hotkey("Space")),
    LISTEN_PREVIOUS(HotkeyCategory.LISTENING, "Read the previous sentence", Hotkey("A")),
    LISTEN_PREVIOUS_ARROW(HotkeyCategory.LISTENING, "Read the previous sentence (second key)", Hotkey("Left")),
    LISTEN_NEXT(HotkeyCategory.LISTENING, "Read the next sentence", Hotkey("D")),
    LISTEN_NEXT_ARROW(HotkeyCategory.LISTENING, "Read the next sentence (second key)", Hotkey("Right")),
    LISTEN_REPEAT(HotkeyCategory.LISTENING, "Read the current sentence again", Hotkey("S")),
    LISTEN_REPEAT_ARROW(HotkeyCategory.LISTENING, "Read the current sentence again (second key)", Hotkey("Down")),
    LISTEN_PAUSE(HotkeyCategory.LISTENING, "Pause reading aloud", Hotkey("W")),
    LISTEN_PAUSE_ARROW(HotkeyCategory.LISTENING, "Pause reading aloud (second key)", Hotkey("Up")),
    LISTEN_AUTO_PAUSE(HotkeyCategory.LISTENING, "Turn auto-pause on or off", Hotkey("Q")),

    STATUS_1(HotkeyCategory.STATUS, "Set status to 1", Hotkey("1")),
    STATUS_2(HotkeyCategory.STATUS, "Set status to 2", Hotkey("2")),
    STATUS_3(HotkeyCategory.STATUS, "Set status to 3", Hotkey("3")),
    STATUS_4(HotkeyCategory.STATUS, "Set status to 4", Hotkey("4")),
    STATUS_IGNORE(HotkeyCategory.STATUS, "Set status to Ignore", Hotkey("I")),
    STATUS_WELL_KNOWN(HotkeyCategory.STATUS, "Set status to Well Known", Hotkey("W")),
    STATUS_UP(HotkeyCategory.STATUS, "Bump the status up by 1", Hotkey("Up", ctrl = true)),
    STATUS_DOWN(HotkeyCategory.STATUS, "Bump the status down by 1", Hotkey("Down", ctrl = true)),
    DELETE_TERM(HotkeyCategory.STATUS, "Delete term (set status to Unknown)", null),

    PREVIOUS_PAGE(HotkeyCategory.PAGING, "Go to previous page, do not mark current page read", Hotkey("Left", shift = true)),
    NEXT_PAGE(HotkeyCategory.PAGING, "Go to next page, do not mark current page read", Hotkey("Right", shift = true)),
    MARK_READ(HotkeyCategory.PAGING, "Mark page as read, go to next page", null),
    MARK_READ_WELL_KNOWN(HotkeyCategory.PAGING, "Set remaining unknown words to Well Known, mark page as read, go to next page", null),

    TRANSLATE_SENTENCE(HotkeyCategory.TRANSLATE, "Translate the sentence of the current word", Hotkey("T")),
    TRANSLATE_PARAGRAPH(HotkeyCategory.TRANSLATE, "Translate the paragraph of the current word", Hotkey("T", shift = true)),
    TRANSLATE_PAGE(HotkeyCategory.TRANSLATE, "Translate the full page", null),

    COPY_SENTENCE(HotkeyCategory.COPY, "Copy the sentence of the current word", Hotkey("C")),
    COPY_PARAGRAPH(HotkeyCategory.COPY, "Copy the paragraph of the current word", Hotkey("C", shift = true)),
    COPY_PAGE(HotkeyCategory.COPY, "Copy the full page", null),

    PAGE_TERM_LIST(HotkeyCategory.MISC, "Show the term listing for the current page", null),
    BOOKMARK(HotkeyCategory.MISC, "Bookmark the current page", Hotkey("B")),
    EDIT_PAGE(HotkeyCategory.MISC, "Edit the current page", null),
    NEXT_THEME(HotkeyCategory.MISC, "Change to the next theme", Hotkey("M")),
    TOGGLE_HIGHLIGHT(HotkeyCategory.MISC, "Toggle highlights", Hotkey("H")),
    TOGGLE_FOCUS(HotkeyCategory.MISC, "Toggle focus mode", Hotkey("F")),
    SAVE_TERM(HotkeyCategory.MISC, "Save term in term form", Hotkey("Enter", ctrl = true));

    /** Stable key used for persistence. */
    val settingKey: String get() = "hotkey_$name"

    companion object {
        val defaults: Map<HotkeyAction, Hotkey?> = entries.associateWith { it.default }

        val byCategory: Map<HotkeyCategory, List<HotkeyAction>> =
            HotkeyCategory.entries.associateWith { category -> entries.filter { it.category == category } }

        /** Actions given defaults after people already had saved shortcuts; applied once where still unassigned. */
        val laterDefaults: List<HotkeyAction> = listOf(PREVIOUS_PAGE, NEXT_PAGE)

        /**
         * Defaults that changed after people already had saved shortcuts, with the old key: a shortcut
         * still on its old default moves to the new one, once; one changed by hand stays as it is.
         */
        val changedDefaults: Map<HotkeyAction, Hotkey> = mapOf(
            PREV_WORD to Hotkey("Left"),
            NEXT_WORD to Hotkey("Right"),
            STATUS_UP to Hotkey("Up"),
            STATUS_DOWN to Hotkey("Down"),
        )

        /**
         * The action for [pressed] among [hotkeys]. Keys shared by a word shortcut and a listening one
         * act on the word while one is [wordSelected], and on reading aloud otherwise (when [listening]
         * is available at all).
         */
        fun resolve(hotkeys: Map<HotkeyAction, Hotkey?>, pressed: Hotkey, wordSelected: Boolean, listening: Boolean): HotkeyAction? {
            val matches = hotkeys.filterValues { it == pressed }.keys
            val usable = if (listening) matches else matches.filter { it.category != HotkeyCategory.LISTENING }
            return usable.firstOrNull { (it.category == HotkeyCategory.LISTENING) != wordSelected } ?: usable.firstOrNull()
        }
    }
}
