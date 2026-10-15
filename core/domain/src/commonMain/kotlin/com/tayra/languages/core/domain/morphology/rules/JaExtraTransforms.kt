package com.tayra.languages.core.domain.morphology.rules

/**
 * Tayra's additions to Yomitan's Japanese rules ([jaTransforms]): forms 10ten Japanese Reader's
 * deinflector (GPL-3.0, Copyright Brian Birtles and contributors) handles and Yomitan's rules lack,
 * checked by its own test cases. In [jaTransforms]' format and with its conditions; the forms built
 * on a verb's masu stem are derived from Yomitan's -なさい rules, and those of -ざるを得ない from its
 * -ざる rules, so every verb class those cover is covered here too.
 */
internal fun jaExtraTransforms(base: List<String>): List<String> {
    val lines = base.asSequence().flatMap { it.split('\n') }.toList()
    fun rulesOf(transform: String): List<List<String>> {
        val start = lines.indexOfFirst { it.startsWith("T\t$transform\t") }
        if (start < 0) return emptyList()
        return lines.drop(start + 1).takeWhile { !it.startsWith("T\t") }.filter { it.startsWith("R\t") }.map { it.split('\t') }
    }

    /** A transform of [suffixes] put where [from]'s rules have [ending], the inflected form having [conditionsIn]. */
    fun derived(id: String, name: String, from: String, ending: String, suffixes: List<String>, conditionsIn: String): String = buildString {
        append("T\t$id\t$name\t\n")
        for (rule in rulesOf(from)) {
            // R  pattern  flags  guard  in  out  s  prefix-cut  suffix-cut  prefix  suffix
            val pattern = rule[1]
            if (rule.getOrNull(6) != "s" || !pattern.endsWith("$ending$")) continue
            for (suffix in suffixes) {
                val replaced = pattern.removeSuffix("$ending$") + "$suffix$"
                val cut = rule[8].toInt() - ending.length + suffix.length
                append("R\t$replaced\t${rule[2]}\t${rule[3]}\t$conditionsIn\t${rule[5]}\ts\t${rule[7]}\t$cut\t${rule[9]}\t${rule.getOrElse(10) { "" }}\n")
            }
        }
    }

    return listOf(
        // Honorific and humble forms on the masu stem: 帰りなさる, 待ちいたします, お読みになる, 考え得る.
        derived("-なさる", "-なさる", "-なさい", "なさい", listOf("なさる"), "v5"),
        derived("-いたす", "-いたす", "-なさい", "なさい", listOf("いたす", "致す"), "v5"),
        derived("-になる", "-になる", "-なさい", "なさい", listOf("になる"), "v5"),
        derived("-得る", "-得る", "-なさい", "なさい", listOf("得る", "える"), "v1"),
        derived("-うる", "-得る", "-なさい", "なさい", listOf("うる"), ""),
        // 闘わざるを得なかった, and as it is often written: ざるをえない, ざる得ない.
        derived("-ざるを得ない", "-ざるを得ない", "-ざる", "ざる", listOf("ざるを得ない", "ざるをえない", "ざる得ない", "ざるえない"), "adj-i"),
        derived("-ざるを得ぬ", "-ざるを得ない", "-ざる", "ざる", listOf("ざるを得ぬ", "ざるをえぬ", "ざる得ぬ", "ざるえぬ"), ""),
        """
        T	-ないで	-ないで	Negative -て: without doing.
        R	ないで$			-て	adj-i	s	0	1
        T	kansai-ben -いる	kansai-ben	The continuous -ている as Kansai says it: 読んどる.
        R	どる$			v5	-て	s	0	2		で
        T	-いらっしゃる	-いらっしゃる	Honorific -ている: 読んでいらっしゃる, 読んでらっしゃる.
        R	ていらっしゃる$			v5	-て	s	0	6
        R	でいらっしゃる$			v5	-て	s	0	6
        R	てらっしゃる$			v5	-て	s	0	5
        R	でらっしゃる$			v5	-て	s	0	5
        T	-ます (honorific verbs)	-ます	The polite forms of the honorific verbs whose masu stem ends in い, after another word too: 御座います, 分かっていらっしゃいます.
        R	ございます$			-ます	v5d	s	0	3		る
        R	座います$			-ます	v5d	s	0	3		る
        R	らっしゃいます$			-ます	v5d	s	0	3		る
        R	なさいます$			-ます	v5d	s	0	3		る
        R	くださいます$			-ます	v5d	s	0	3		る
        R	下さいます$			-ます	v5d	s	0	3		る
        T	-ませ	imperative	The polite imperative: いらっしゃいませ, 帰りなさいませ.
        R	ませ$				-ます	s	0	1		す
        T	imperative (honorific verbs)	imperative	The imperative of the honorific verbs: なさい, いらっしゃい, ください.
        R	^なさい$				v5d	s	0	1		る
        R	らっしゃい$				v5d	s	0	1		る
        R	^ください$				v5d	s	0	1		る
        R	^下さい$				v5d	s	0	1		る
        T	-する (irregular)	-する	The forms of する-verbs that conjugate like godan す-verbs or keep し: 発さない, 発そう, 愛せば, 発しさせる.
        R	さない$			adj-i	vs	s	0	3		する
        R	さず$				vs	s	0	2		する
        R	そう$				vs	s	0	2		する
        R	せば$			-ば	vs	s	0	2		する
        R	せ$				vs	s	0	1		する
        R	しさせる$			v1	vs	s	0	4		する
        R	しられる$			v1	vs	s	0	4		する
        T	-なさる (nouns)	-なさる	A する-noun with the honorific なさる or the humble いたす: 仕事なさる, お願いいたします.
        R	なさる$			v5		s	0	3
        R	いたす$			v5		s	0	3
        R	致す$			v5		s	0	2
        """.trimIndent() + "\n",
    )
}
