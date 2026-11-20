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
            "Questions :", "Հարցեր․", "پرسش‌ها:", "คำถาม:", "សំណួរ៖", "Mga tanong:", "问题：", "Ἐρωτήσεις:", "Pyetje:",
            "פֿראַגעס:", "Асуултууд:", "ຄຳຖາມ:", "Jautājumi:", "Goulennoù:", "ಪ್ರಶ್ನೆಗಳು:", "Nā nīnau:", "Galderak:",
            "Пытанні:", "Mistoqsijiet:", "Soalan:", "Maswali:", "Сұрақтар:", "Suallar:", "Vrae:", "سوالات:", "प्रश्नहरू:",
            "پوښتنې:",
        )
        for (header in headers) {
            assertEquals(1, LessonQuestions.split("Story.\n\n$header\nQ?\nA.")?.questions?.size, header)
        }
    }

    @Test
    fun theBengaliHeaderIsFound() {
        val split = LessonQuestions.split("রিনা ঢাকায় থাকে।\n\nপ্রশ্ন:\nরিনা কোথায় থাকে?\nরিনা ঢাকায় থাকে।")
        assertEquals("রিনা ঢাকায় থাকে।", split?.text)
        assertEquals(listOf(LessonQuestion("রিনা কোথায় থাকে?", "রিনা ঢাকায় থাকে।")), split?.questions)
    }

    @Test
    fun theTamilHeaderIsFound() {
        val split = LessonQuestions.split("ராணி சென்னையில் இருக்கிறாள்.\n\nகேள்விகள்:\nராணி எங்கே இருக்கிறாள்?\nராணி சென்னையில் இருக்கிறாள்.")
        assertEquals("ராணி சென்னையில் இருக்கிறாள்.", split?.text)
        assertEquals(listOf(LessonQuestion("ராணி எங்கே இருக்கிறாள்?", "ராணி சென்னையில் இருக்கிறாள்.")), split?.questions)
    }

    @Test
    fun thePunjabiHeaderIsFound() {
        val split = LessonQuestions.split("ਰੀਨਾ ਅੰਮ੍ਰਿਤਸਰ ਵਿੱਚ ਰਹਿੰਦੀ ਹੈ।\n\nਸਵਾਲ:\nਰੀਨਾ ਕਿੱਥੇ ਰਹਿੰਦੀ ਹੈ?\nਰੀਨਾ ਅੰਮ੍ਰਿਤਸਰ ਵਿੱਚ ਰਹਿੰਦੀ ਹੈ।")
        assertEquals("ਰੀਨਾ ਅੰਮ੍ਰਿਤਸਰ ਵਿੱਚ ਰਹਿੰਦੀ ਹੈ।", split?.text)
        assertEquals(listOf(LessonQuestion("ਰੀਨਾ ਕਿੱਥੇ ਰਹਿੰਦੀ ਹੈ?", "ਰੀਨਾ ਅੰਮ੍ਰਿਤਸਰ ਵਿੱਚ ਰਹਿੰਦੀ ਹੈ।")), split?.questions)
    }

    @Test
    fun theTeluguHeaderIsFound() {
        val split = LessonQuestions.split("రాము హైదరాబాద్‌లో ఉంటాడు.\n\nప్రశ్నలు:\nరాము ఎక్కడ ఉంటాడు?\nరాము హైదరాబాద్‌లో ఉంటాడు.")
        assertEquals("రాము హైదరాబాద్‌లో ఉంటాడు.", split?.text)
        assertEquals(listOf(LessonQuestion("రాము ఎక్కడ ఉంటాడు?", "రాము హైదరాబాద్‌లో ఉంటాడు.")), split?.questions)
    }

    @Test
    fun theGujaratiHeaderIsFound() {
        val split = LessonQuestions.split("મીના અમદાવાદમાં રહે છે.\n\nપ્રશ્નો:\nમીના ક્યાં રહે છે?\nમીના અમદાવાદમાં રહે છે.")
        assertEquals("મીના અમદાવાદમાં રહે છે.", split?.text)
        assertEquals(listOf(LessonQuestion("મીના ક્યાં રહે છે?", "મીના અમદાવાદમાં રહે છે.")), split?.questions)
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
