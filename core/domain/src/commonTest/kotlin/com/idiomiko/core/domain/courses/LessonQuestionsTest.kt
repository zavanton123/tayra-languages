package com.idiomiko.core.domain.courses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The questions that end a lesson's text come off it, paired by their place. */
class LessonQuestionsTest {
    @Test
    fun theQuestionsComeOffTheEndOfTheText() {
        val split = LessonQuestions.split("Ana mora em Lisboa.\n\nEla gosta do mar.\n\nPerguntas:\nOnde mora a Ana?\nEla mora em Lisboa.\nDo que ela gosta?\nEla gosta do mar.")
        assertEquals("Ana mora em Lisboa.\n\nEla gosta do mar.", split?.text)
        assertEquals(listOf(LessonQuestion("Onde mora a Ana?", "Ela mora em Lisboa."), LessonQuestion("Do que ela gosta?", "Ela gosta do mar.")), split?.questions)
    }

    @Test
    fun linesArePairedByTheirPlaceNotTheirMarks() {
        // A Japanese question ends in 。, and an answer may quote a question.
        val japanese = LessonQuestions.split("ケンさんは十歳です。\n\n質問：\nケンさんは何歳ですか。\n十歳です。")
        assertEquals(listOf(LessonQuestion("ケンさんは何歳ですか。", "十歳です。")), japanese?.questions)
        val quoting = LessonQuestions.split("Sie fragt viel.\n\nFragen:\nWas fragt sie?\nSie fragt: Warum ist der Himmel blau?")
        assertEquals("Sie fragt: Warum ist der Himmel blau?", quoting?.questions?.single()?.answer)
    }

    @Test
    fun theManxHeaderIsFound() {
        assertEquals(1, LessonQuestions.split("Ta Juan ayns y thie.\n\nFeyshtyn:\nCre vel Juan?\nT'eh ayns y thie.")?.questions?.size)
    }

    @Test
    fun eachLanguagesHeaderIsFound() {
        val headers = listOf(
            "Questions :", "Հարցեր․", "پرسش‌ها:", "คำถาม:", "Mga tanong:", "问题：", "Ἐρωτήσεις:", "Pyetje:", "פֿראַגעס:",
            "Асуултууд:", "ຄຳຖາມ:", "Jautājumi:", "Goulennoù:", "ಪ್ರಶ್ನೆಗಳು:", "Nā nīnau:",
        )
        for (header in headers) {
            assertEquals(1, LessonQuestions.split("Story.\n\n$header\nQ?\nA.")?.questions?.size, header)
        }
    }

    @Test
    fun theVietnameseHeaderIsFound() {
        val split = LessonQuestions.split("Lan ở Hà Nội.\n\nCâu hỏi:\nLan ở đâu?\nLan ở Hà Nội.")
        assertEquals(listOf(LessonQuestion("Lan ở đâu?", "Lan ở Hà Nội.")), split?.questions)
    }

    @Test
    fun cornishQuestionsComeOff() {
        val split = LessonQuestions.split("Tamsin a woer redya.\n\nGovynnow:\nPyth a woer Tamsin?\nHy a woer redya.")
        assertEquals("Tamsin a woer redya.", split?.text)
        assertEquals(listOf(LessonQuestion("Pyth a woer Tamsin?", "Hy a woer redya.")), split?.questions)
    }

    @Test
    fun blankLinesAmongTheQuestionsAndALineWrittenTwiceAreLetGo() {
        val split = LessonQuestions.split("Hasan çalışıyor.\n\nSorular:\nHasan ne yapıyor?\n\nÇalışıyor.\nNeden?\nNeden?\nÇünkü para lazım.")
        assertEquals(listOf("Hasan ne yapıyor?", "Neden?"), split?.questions?.map { it.question })
    }

    @Test
    fun aTextWithoutQuestionsOrWithUnpairedLinesKeepsThem() {
        assertNull(LessonQuestions.split("Just a story.\nWith two lines."))
        assertNull(LessonQuestions.split("Story.\n\nQuestions:\nOnly a question?"))
        assertNull(LessonQuestions.split("Questions:\nQ?\nA."), "no story before the questions")
    }
}
