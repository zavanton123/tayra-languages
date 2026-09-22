package com.tayra.languages.core.domain

import com.tayra.languages.core.domain.model.Language

object TestLanguages {
    val english = Language(
        id = 1,
        name = "English",
        exceptionsSplitSentences = "Mr.|Mrs.|Dr.|[A-Z].|Vd.|Vds.",
    )

    val spanish = Language(
        id = 2,
        name = "Spanish",
        regexpSplitSentences = ".!?¡",
        exceptionsSplitSentences = "Dr.|Dra.|[A-Z].|Ud.|Vd.|Vds.|Sr.|Sra.|Srta.",
    )

    val generic = Language(
        id = 3,
        name = "Generic",
        wordCharacters = "",
        regexpSplitSentences = "",
        exceptionsSplitSentences = "",
    )

    val german = Language(
        id = 4,
        name = "German",
        wordCharacters = "a-zA-ZäöüÄÖÜß\\u200C",
    )

    val turkish = Language(
        id = 5,
        name = "Turkish",
        parserType = "turkish",
        wordCharacters = "a-zA-ZÀ-ÖØ-öø-ȳáéíóúÁÉÍÓÚñÑğĞıİöÖüÜşŞçÇ",
    )

    val classicalChinese = Language(
        id = 6,
        name = "Classical Chinese",
        parserType = "classicalchinese",
        wordCharacters = "一-龥",
        regexpSplitSentences = ".!?。！？",
        characterSubstitutions = "",
        exceptionsSplitSentences = "",
    )
}
