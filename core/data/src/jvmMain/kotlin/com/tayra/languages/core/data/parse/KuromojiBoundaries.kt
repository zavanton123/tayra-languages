package com.tayra.languages.core.data.parse

import co.touchlab.kermit.Logger
import com.atilika.kuromoji.ipadic.Tokenizer
import com.tayra.languages.core.domain.parse.Analysis
import com.tayra.languages.core.domain.parse.WordBoundaries
import kotlin.concurrent.thread

/**
 * Japanese words from Kuromoji, a Java port of MeCab with MeCab's IPADIC dictionary, as Yomitan's
 * optional MeCab parser reads text. MeCab cuts an inflected word into pieces (食べ・まし・た); the
 * pieces that cannot stand alone (auxiliaries, suffixes, the verbs that only follow another, the
 * て joining them) are joined to the word before, so a word is what the reader looks up. Its
 * dictionary takes a moment to load, so it loads on a thread of its own; until it has, there are no
 * boundaries and the lexicon alone cuts the text.
 */
class KuromojiBoundaries : WordBoundaries {
    @Volatile
    private var tokenizer: Tokenizer? = null

    init {
        thread(name = "kuromoji", isDaemon = true) {
            tokenizer = try {
                Tokenizer()
            } catch (e: Throwable) {
                Logger.w(e) { "Kuromoji could not load its dictionary" }
                null
            }
        }
    }

    override fun of(run: String): Analysis? {
        val loaded = tokenizer ?: return null
        val pieces = HashSet<Int>()
        val words = HashSet<Int>()
        var previous: String? = null
        for (token in loaded.tokenize(run)) {
            pieces += token.position
            val joined = continues(token.partOfSpeechLevel1, token.partOfSpeechLevel2, token.surface, previous)
            if (!joined) words += token.position
            // A joining て still belongs to the verb, so the verb helping it joins too ("住んで" + "いる").
            previous = if (joined && token.partOfSpeechLevel1 == "助詞") "動詞" else token.partOfSpeechLevel1
        }
        return Analysis(pieces, words)
    }

    internal companion object {
        /** Whether a piece belongs to the word before it, by its IPADIC parts of speech and the word before's. */
        fun continues(pos: String, detail: String, surface: String, previous: String?): Boolean {
            if (previous == null || previous == "記号") return false
            val afterInflecting = previous == "動詞" || previous == "形容詞" || previous == "助動詞"
            return when {
                pos == "助動詞" -> afterInflecting
                pos == "動詞" && (detail == "非自立" || detail == "接尾") -> afterInflecting || (detail == "接尾" && previous == "名詞")
                pos == "形容詞" && (detail == "非自立" || detail == "接尾") -> afterInflecting
                pos == "名詞" && detail == "接尾" -> previous == "名詞"
                pos == "助詞" && (detail == "接続助詞" || detail == "並立助詞") && surface in JOINING -> afterInflecting
                else -> false
            }
        }

        /** The particles that join a verb to the one helping it ("住んでいる", "食べてしまう"), and -たり. */
        private val JOINING = setOf("て", "で", "ちゃ", "じゃ", "たり", "だり")
    }
}
