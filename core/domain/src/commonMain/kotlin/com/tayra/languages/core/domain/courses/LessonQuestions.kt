package com.tayra.languages.core.domain.courses

/** A question on a lesson's story, with the answer the lesson gives. */
data class LessonQuestion(val question: String, val answer: String)

/** A lesson's text cut in two: the story to read, and the questions on it that ended the text. */
data class SplitLesson(val text: String, val questions: List<LessonQuestion>)

/**
 * The questions that end the app's lessons. Each lesson's text closes with a header line in its
 * language ("Questions:", "Вопросы:", "質問：") and then a question and its answer on a line each,
 * one after the other. The lines go by their place, not their marks: Japanese and Thai questions
 * often have no question mark, and an answer may end in one when it quotes a question.
 */
object LessonQuestions {
    /** The headers of the questions in the app's courses, one per language. */
    private val HEADERS = setOf(
        "أسئلة:", "Въпроси:", "Pitanja:", "Preguntes:", "Otázky:", "Cwestiynau:", "Spørgsmål:", "Fragen:",
        "Ερωτήσεις:", "Questions:", "Demandoj:", "Preguntas:", "Küsimused:", "پرسش‌ها:", "Kysymykset:",
        "Questions :", "Ceisteanna:", "Ceistean:", "שאלות:", "सवाल:", "Kérdések:", "Հարցեր․", "Հարցեր։",
        "Pertanyaan:", "Spurningar:", "Domande:", "質問：", "კითხვები:", "질문:", "Quaestiones:", "Klausimai:",
        "Прашања:", "Vragen:", "Spørsmål:", "Pytania:", "Perguntas:", "Întrebări:", "Вопросы:", "Vprašanja:",
        "Frågor:", "คำถาม:", "Mga tanong:", "Sorular:", "Запитання:", "問題：", "问题：",
    ).mapTo(HashSet()) { normalized(it) }

    /**
     * Cuts the questions off [text]: the story before the last header line, and the lines after it
     * paired up. Null when the text has no header, or the lines after it do not pair up, so a text
     * that only looks like a lesson keeps all its words.
     */
    fun split(text: String): SplitLesson? {
        val lines = text.lines()
        val header = lines.indexOfLast { normalized(it) in HEADERS }
        if (header < 0) return null
        val story = lines.subList(0, header).joinToString("\n").trim()
        if (story.isEmpty()) return null
        val qa = dropRepeats(lines.subList(header + 1, lines.size).map { it.trim() }.filter { it.isNotEmpty() })
        if (qa.isEmpty() || qa.size % 2 != 0) return null
        return SplitLesson(story, qa.chunked(2).map { (question, answer) -> LessonQuestion(question, answer) })
    }

    /** A line written twice in a row, which leaves an odd count, said once: a slip in a few lessons. */
    private fun dropRepeats(lines: List<String>): List<String> =
        if (lines.size % 2 == 0) lines else lines.filterIndexed { i, line -> i == 0 || line != lines[i - 1] }

    private fun normalized(line: String) = line.trim().replace(' ', ' ').replace(' ', ' ')
}
