package com.tayra.languages.core.data.files

/**
 * Turns the bytes of a text file into text by the encoding it declares or starts with. Books in
 * Russian still come in windows-1251 or KOI8-R (FB2 files, RTF documents), Western ones in
 * windows-1252 (Mobipocket books, RTF); other platforms' charsets are not reachable from common code,
 * so those three are tables here.
 */
internal object TextDecoding {

    /** [bytes] as text in [encoding] (an XML, MIME or Mobipocket name), or as UTF-8 when unnamed or unknown. */
    fun decode(bytes: ByteArray, encoding: String?): String {
        val name = encoding?.trim()?.lowercase()?.replace("_", "-").orEmpty()
        val table = when (name) {
            "windows-1251", "cp1251", "1251", "x-cp1251" -> CP1251
            "koi8-r", "koi8r", "koi8" -> KOI8_R
            "windows-1252", "cp1252", "1252", "iso-8859-1", "latin1", "latin-1", "us-ascii", "ascii" -> CP1252
            else -> null
        }
        return if (table == null) utf(bytes) else singleByte(bytes, table)
    }

    /** UTF-16 when the bytes start with its byte-order mark, UTF-8 otherwise; a UTF-8 byte-order mark is dropped. */
    fun utf(bytes: ByteArray): String {
        if (bytes.size >= 2) {
            val first = bytes[0].toInt() and 0xFF
            val second = bytes[1].toInt() and 0xFF
            if (first == 0xFF && second == 0xFE) return utf16(bytes, 2, littleEndian = true)
            if (first == 0xFE && second == 0xFF) return utf16(bytes, 2, littleEndian = false)
        }
        return bytes.decodeToString().removePrefix("\uFEFF")
    }

    /** Whether [bytes] are valid UTF-8 (or start with a UTF-16 byte-order mark). */
    fun isUtf(bytes: ByteArray): Boolean = try {
        if (bytes.size >= 2 && ((bytes[0].toInt() and 0xFF) to (bytes[1].toInt() and 0xFF)).let { it == 0xFF to 0xFE || it == 0xFE to 0xFF }) true
        else { bytes.decodeToString(throwOnInvalidSequence = true); true }
    } catch (e: CharacterCodingException) {
        false
    }

    /** The encoding an XML file declares in its first line ("windows-1251"), if any. */
    fun xmlEncoding(bytes: ByteArray): String? {
        val head = bytes.copyOfRange(0, minOf(bytes.size, 200)).map { (it.toInt() and 0xFF).toChar() }.toCharArray().concatToString()
        return Regex("""<\?xml[^>]*encoding\s*=\s*["']([^"']+)["']""").find(head)?.groupValues?.get(1)
    }

    private fun utf16(bytes: ByteArray, from: Int, littleEndian: Boolean): String {
        val chars = CharArray((bytes.size - from) / 2)
        for (i in chars.indices) {
            val a = bytes[from + 2 * i].toInt() and 0xFF
            val b = bytes[from + 2 * i + 1].toInt() and 0xFF
            chars[i] = (if (littleEndian) a or (b shl 8) else (a shl 8) or b).toChar()
        }
        return chars.concatToString()
    }

    private fun singleByte(bytes: ByteArray, high: String): String {
        val chars = CharArray(bytes.size)
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xFF
            chars[i] = if (b < 0x80) b.toChar() else high[b - 0x80]
        }
        return chars.concatToString()
    }

    /** Bytes 0x80 to 0xFF of each code page; \uFFFD where it has no character. */
    private const val CP1251 =
        "\u0402\u0403\u201A\u0453\u201E\u2026\u2020\u2021\u20AC\u2030\u0409\u2039\u040A\u040C\u040B\u040F" +
        "\u0452\u2018\u2019\u201C\u201D\u2022\u2013\u2014\uFFFD\u2122\u0459\u203A\u045A\u045C\u045B\u045F" +
        "\u00A0\u040E\u045E\u0408\u00A4\u0490\u00A6\u00A7\u0401\u00A9\u0404\u00AB\u00AC\u00AD\u00AE\u0407" +
        "\u00B0\u00B1\u0406\u0456\u0491\u00B5\u00B6\u00B7\u0451\u2116\u0454\u00BB\u0458\u0405\u0455\u0457" +
        "\u0410\u0411\u0412\u0413\u0414\u0415\u0416\u0417\u0418\u0419\u041A\u041B\u041C\u041D\u041E\u041F" +
        "\u0420\u0421\u0422\u0423\u0424\u0425\u0426\u0427\u0428\u0429\u042A\u042B\u042C\u042D\u042E\u042F" +
        "\u0430\u0431\u0432\u0433\u0434\u0435\u0436\u0437\u0438\u0439\u043A\u043B\u043C\u043D\u043E\u043F" +
        "\u0440\u0441\u0442\u0443\u0444\u0445\u0446\u0447\u0448\u0449\u044A\u044B\u044C\u044D\u044E\u044F"
    private const val KOI8_R =
        "\u2500\u2502\u250C\u2510\u2514\u2518\u251C\u2524\u252C\u2534\u253C\u2580\u2584\u2588\u258C\u2590" +
        "\u2591\u2592\u2593\u2320\u25A0\u2219\u221A\u2248\u2264\u2265\u00A0\u2321\u00B0\u00B2\u00B7\u00F7" +
        "\u2550\u2551\u2552\u0451\u2553\u2554\u2555\u2556\u2557\u2558\u2559\u255A\u255B\u255C\u255D\u255E" +
        "\u255F\u2560\u2561\u0401\u2562\u2563\u2564\u2565\u2566\u2567\u2568\u2569\u256A\u256B\u256C\u00A9" +
        "\u044E\u0430\u0431\u0446\u0434\u0435\u0444\u0433\u0445\u0438\u0439\u043A\u043B\u043C\u043D\u043E" +
        "\u043F\u044F\u0440\u0441\u0442\u0443\u0436\u0432\u044C\u044B\u0437\u0448\u044D\u0449\u0447\u044A" +
        "\u042E\u0410\u0411\u0426\u0414\u0415\u0424\u0413\u0425\u0418\u0419\u041A\u041B\u041C\u041D\u041E" +
        "\u041F\u042F\u0420\u0421\u0422\u0423\u0416\u0412\u042C\u042B\u0417\u0428\u042D\u0429\u0427\u042A"
    private const val CP1252 =
        "\u20AC\uFFFD\u201A\u0192\u201E\u2026\u2020\u2021\u02C6\u2030\u0160\u2039\u0152\uFFFD\u017D\uFFFD" +
        "\uFFFD\u2018\u2019\u201C\u201D\u2022\u2013\u2014\u02DC\u2122\u0161\u203A\u0153\uFFFD\u017E\u0178" +
        "\u00A0\u00A1\u00A2\u00A3\u00A4\u00A5\u00A6\u00A7\u00A8\u00A9\u00AA\u00AB\u00AC\u00AD\u00AE\u00AF" +
        "\u00B0\u00B1\u00B2\u00B3\u00B4\u00B5\u00B6\u00B7\u00B8\u00B9\u00BA\u00BB\u00BC\u00BD\u00BE\u00BF" +
        "\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5\u00C6\u00C7\u00C8\u00C9\u00CA\u00CB\u00CC\u00CD\u00CE\u00CF" +
        "\u00D0\u00D1\u00D2\u00D3\u00D4\u00D5\u00D6\u00D7\u00D8\u00D9\u00DA\u00DB\u00DC\u00DD\u00DE\u00DF" +
        "\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5\u00E6\u00E7\u00E8\u00E9\u00EA\u00EB\u00EC\u00ED\u00EE\u00EF" +
        "\u00F0\u00F1\u00F2\u00F3\u00F4\u00F5\u00F6\u00F7\u00F8\u00F9\u00FA\u00FB\u00FC\u00FD\u00FE\u00FF"
}
