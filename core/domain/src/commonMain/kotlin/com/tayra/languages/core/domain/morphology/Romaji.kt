package com.tayra.languages.core.domain.morphology

/**
 * Kana written in Hepburn romaji, as learners' subtitles show it: しんぶん "shinbun", きっと "kitto",
 * きんえん "kin'en", ラーメン "raamen" (long vowels doubled rather than marked).
 */
object Romaji {
    private val single: Map<Char, String> = (
        "あa いi うu えe おo かka きki くku けke こko さsa しshi すsu せse そso たta ちchi つtsu てte とto なna にni ぬnu ねne のno " +
            "はha ひhi ふfu へhe ほho まma みmi むmu めme もmo やya ゆyu よyo らra りri るru れre ろro わwa ゐi ゑe をo " +
            "がga ぎgi ぐgu げge ごgo ざza じji ずzu ぜze ぞzo だda ぢji づzu でde どdo ばba びbi ぶbu べbe ぼbo ぱpa ぴpi ぷpu ぺpe ぽpo " +
            "ぁa ぃi ぅu ぇe ぉo ゃya ゅyu ょyo ゎwa ゔvu ゕka ゖke"
        ).split(' ').associate { it[0] to it.substring(1) }

    private val digraphs: Map<String, String> = (
        "きゃkya きゅkyu きょkyo しゃsha しゅshu しょsho ちゃcha ちゅchu ちょcho にゃnya にゅnyu にょnyo ひゃhya ひゅhyu ひょhyo " +
            "みゃmya みゅmyu みょmyo りゃrya りゅryu りょryo ぎゃgya ぎゅgyu ぎょgyo じゃja じゅju じょjo ぢゃja ぢゅju ぢょjo " +
            "びゃbya びゅbyu びょbyo ぴゃpya ぴゅpyu ぴょpyo ふぁfa ふぃfi ふぇfe ふぉfo てぃti でぃdi とぅtu どぅdu しぇshe じぇje ちぇche " +
            "うぃwi うぇwe うぉwo ゔぁva ゔぃvi ゔぇve ゔぉvo つぁtsa つぃtsi つぇtse つぉtso"
        ).split(' ').associate { it.substring(0, 2) to it.substring(2) }

    fun of(kana: String): String {
        val text = Japanese.katakanaToHiragana(kana)
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == 'っ' -> {
                    // Doubles the next consonant: きっと "kitto", まっちゃ "matcha".
                    val next = syllableAt(text, i + 1)
                    if (next != null && next[0] !in VOWELS) out.append(if (next.startsWith("ch")) 't' else next[0])
                    i++
                }
                c == 'ん' -> {
                    out.append('n')
                    val next = syllableAt(text, i + 1)
                    if (next != null && (next[0] in VOWELS || next[0] == 'y')) out.append('\'')
                    i++
                }
                else -> {
                    val syllable = syllableAt(text, i)
                    if (syllable != null) {
                        out.append(syllable)
                        i += if (i + 1 < text.length && text.substring(i, i + 2) in digraphs) 2 else 1
                    } else {
                        out.append(c)
                        i++
                    }
                }
            }
        }
        return out.toString()
    }

    private fun syllableAt(text: String, i: Int): String? {
        if (i >= text.length) return null
        if (i + 1 < text.length) digraphs[text.substring(i, i + 2)]?.let { return it }
        return single[text[i]]
    }

    private const val VOWELS = "aeiou"
}
