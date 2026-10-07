package com.tayra.languages.core.domain.language

/**
 * The sample books the languages used to come with, before the tutorial replaced them: each by its
 * language, its title and a fingerprint of how its text begins, so only a book still holding that
 * sample is recognised, never one the reader made with the same title.
 */
internal object RetiredSamples {
    data class Sample(val language: String, val title: String, val opening: Long)

    val all: List<Sample> = listOf(
        Sample("Belarusian", "Усеагульная Дэкларацыя Правоў Чалавека", 3613246023178380654L),
        Sample("Bulgarian", "Вчера (1988) - Film Description", -8239577784287608807L),
        Sample("Catalan", "Merlí - Series description", 3434295743680698488L),
        Sample("Croatian", "Nebo predivan Božji dom", 8290200923704632361L),
        Sample("Czech", "Hrad Cimburk – Jak vzal vítr pasáčkovi čepici", 2447087423726204635L),
        Sample("Danish", "Prinsessen på ærten", -168116327731816553L),
        Sample("Dutch", "De doortocht", 4233769982808809355L),
        Sample("English", "Tutorial", 3127144374956303519L),
        Sample("English", "Tutorial follow-up", -8288607394895880634L),
        Sample("Estonian", "Tõde ja Õigus - Film Description", -5792170146214145548L),
        Sample("Finnish", "Sisu - Film description", 7773059616402838863L),
        Sample("French", "Boucles d’or et les trois ours", 370389177280030478L),
        Sample("Galician", "O Apóstolo - Film Description", -7509487135229240677L),
        Sample("German", "Die Bremer Stadtmusikanten", -4474778939661081686L),
        Sample("Greek", "Γεια σου, Νίκη. Ο Πέτρος είμαι.", -2716410945023280259L),
        Sample("Hungarian", "Sátántangó - Film Description", 8513455156715670137L),
        Sample("Icelandic", "Mannréttindayfirlýsing Sameinuðo Þjóðanna", -7874532329405275676L),
        Sample("Italian", "Le avventure di Pinocchio", -4112215349987435691L),
        Sample("Latin", "De re pvblica - Cicero", -7562248591544772470L),
        Sample("Latvian", "Dvēseļu putenis - Film Description", -5554043536182627866L),
        Sample("Lithuanian", "Šuolis - Film Description", -1584701619888644022L),
        Sample("Macedonian", "Оче наш", -2183944128496094456L),
        Sample("Norwegian", "VILDANDEN", -6954001250247182839L),
        Sample("Polish", "Adam i Smoczy Skarb", -3319268760976243870L),
        Sample("Portuguese", "A Maldição", 349111216254739283L),
        Sample("Romanian", "Țestoasa și iepurele", 6195182665488422148L),
        Sample("Russian", "медведь", -6463196803533432023L),
        Sample("Serbian", "Млади пас иде у лов", 5524358162754910174L),
        Sample("Slovak", "Obchod na korze - Film Description", -1092832044430096486L),
        Sample("Slovene", "Kekčeve ukane - Film Description", -1602904654028939062L),
        Sample("Spanish", "Aladino y la lámpara maravillosa", 1620354115977026714L),
        Sample("Swedish", "De tre bockarna Bruse", -5768287107619487306L),
        Sample("Turkish", "Büyük ağaç", -3343722210367337287L),
        Sample("Ukrainian", "Скринька Пандори", -4942310667660850701L),
        // Before the samples' texts were edited, books held the original wording.
        Sample("English", "Tutorial", 8364616674078216723L),
    )

    /** The fingerprint [Sample.opening] of a text: a 64-bit FNV-1a hash of its first 60 letters and digits. */
    fun opening(text: String): Long? {
        val letters = text.filter { it.isLetterOrDigit() }.take(OPENING_LENGTH)
        if (letters.length < OPENING_LENGTH) return null
        var hash = -0x340d631b7bdddcdbL
        for (c in letters) {
            hash = hash xor c.code.toLong()
            hash *= 0x100000001b3L
        }
        return hash
    }

    private const val OPENING_LENGTH = 60
}
