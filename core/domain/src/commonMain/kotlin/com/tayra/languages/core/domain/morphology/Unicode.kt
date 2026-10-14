package com.tayra.languages.core.domain.morphology

/** Canonical Unicode normalisation (NFD and NFC) for the Basic Multilingual Plane, as String.normalize does in JavaScript. */
object Unicode {
    private val decompositions: Map<Char, String> by lazy {
        val map = HashMap<Char, String>()
        var i = 0
        val data = unicodeDecompositions
        while (i < data.length) {
            val length = data[i + 1].code
            map[data[i]] = data.substring(i + 2, i + 2 + length)
            i += 2 + length
        }
        map
    }

    private val compositions: Map<String, Char> by lazy {
        unicodeComposable.associateBy { decompositions.getValue(it) }
    }

    private val classes: Map<Char, Int> by lazy {
        val map = HashMap<Char, Int>()
        val data = unicodeCombiningClasses
        for (i in data.indices step 2) map[data[i]] = data[i + 1].code
        map
    }

    fun combiningClass(c: Char): Int = classes[c] ?: 0

    /** The canonical decomposition: precomposed letters split into their base and combining marks. */
    fun nfd(text: String): String {
        if (text.all { it.code < 0xC0 }) return text
        val out = StringBuilder(text.length + 8)
        for (c in text) decompose(c, out)
        reorder(out)
        return out.toString()
    }

    /** The canonical composition: decomposed and then composed again, as NFC. */
    fun nfc(text: String): String {
        if (text.all { it.code < 0x300 }) return text
        val chars = StringBuilder(nfd(text))
        if (chars.isEmpty()) return ""
        var starterPos = 0
        var starter = chars[0]
        var lastClass = if (combiningClass(starter) == 0) 0 else 256
        var write = 1
        for (read in 1 until chars.length) {
            val c = chars[read]
            val cc = combiningClass(c)
            val composite = compose(starter, c)
            if (composite != null && (lastClass < cc || lastClass == 0)) {
                chars[starterPos] = composite
                starter = composite
            } else {
                if (cc == 0) {
                    starterPos = write
                    starter = c
                }
                lastClass = cc
                chars[write++] = c
            }
        }
        chars.setLength(write)
        return chars.toString()
    }

    private fun compose(first: Char, second: Char): Char? {
        if (first.code in HANGUL_L_BASE until HANGUL_L_BASE + HANGUL_L_COUNT && second.code in HANGUL_V_BASE until HANGUL_V_BASE + HANGUL_V_COUNT) {
            return (HANGUL_S_BASE + ((first.code - HANGUL_L_BASE) * HANGUL_V_COUNT + second.code - HANGUL_V_BASE) * HANGUL_T_COUNT).toChar()
        }
        val s = first.code - HANGUL_S_BASE
        if (s in 0 until HANGUL_S_COUNT && s % HANGUL_T_COUNT == 0 && second.code in HANGUL_T_BASE + 1 until HANGUL_T_BASE + HANGUL_T_COUNT) {
            return (first.code + second.code - HANGUL_T_BASE).toChar()
        }
        return compositions["$first$second"]
    }

    private fun decompose(c: Char, out: StringBuilder) {
        val s = c.code - HANGUL_S_BASE
        if (s in 0 until HANGUL_S_COUNT) {
            out.append((HANGUL_L_BASE + s / (HANGUL_V_COUNT * HANGUL_T_COUNT)).toChar())
            out.append((HANGUL_V_BASE + s % (HANGUL_V_COUNT * HANGUL_T_COUNT) / HANGUL_T_COUNT).toChar())
            if (s % HANGUL_T_COUNT != 0) out.append((HANGUL_T_BASE + s % HANGUL_T_COUNT).toChar())
            return
        }
        val parts = decompositions[c]
        if (parts == null) out.append(c) else for (p in parts) decompose(p, out)
    }

    /** Puts runs of combining marks in canonical order, by combining class. */
    private fun reorder(chars: StringBuilder) {
        var i = 1
        while (i < chars.length) {
            val cc = combiningClass(chars[i])
            if (cc != 0) {
                var j = i
                while (j > 0) {
                    val before = combiningClass(chars[j - 1])
                    if (before <= cc) break
                    val t = chars[j - 1]
                    chars[j - 1] = chars[j]
                    chars[j] = t
                    j--
                }
            }
            i++
        }
    }

    private const val HANGUL_S_BASE = 0xAC00
    private const val HANGUL_L_BASE = 0x1100
    private const val HANGUL_V_BASE = 0x1161
    private const val HANGUL_T_BASE = 0x11A7
    private const val HANGUL_L_COUNT = 19
    private const val HANGUL_V_COUNT = 21
    private const val HANGUL_T_COUNT = 28
    private const val HANGUL_S_COUNT = 11172
}
