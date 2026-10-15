package com.tayra.languages.core.data.files

/** Text laid out in lines, as PDFs and photos give it, made paragraphs again. */
internal object LineReflow {

    /**
     * Joins the lines a PDF's or a photo's text comes in back into paragraphs. A paragraph ends at
     * an empty line, or at a line that ends a sentence and stops well short of the lines around it.
     * A word broken by a hyphen at a line's end is joined again when the next line goes on in lowercase.
     * With [trustEmptyLines] false, as for recognized text, whose empty lines are an engine's guess
     * where widely spaced lines part, an empty line ends a paragraph only after a finished sentence.
     */
    fun paragraphs(text: String, trustEmptyLines: Boolean = true): String {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').replace('\u000C', '\n').split('\n').map { it.trim() }
        val lengths = lines.filter { it.isNotEmpty() }.map { it.length }.sorted()
        val typical = if (lengths.isEmpty()) 0 else lengths[(lengths.size * 0.8).toInt().coerceAtMost(lengths.lastIndex)]
        val out = StringBuilder()
        val paragraph = StringBuilder()
        fun end() {
            if (paragraph.isNotEmpty()) out.append(paragraph.toString().trim()).append('\n')
            paragraph.clear()
        }
        for (line in lines) {
            if (line.isEmpty()) {
                if (trustEmptyLines || paragraph.lastOrNull()?.let { it in SENTENCE_ENDS } != false) end()
                continue
            }
            if (paragraph.isNotEmpty()) {
                val last = paragraph.last()
                when {
                    last == '-' && paragraph.length > 1 && paragraph[paragraph.length - 2].isLetter() && line.first().isLowerCase() ->
                        paragraph.setLength(paragraph.length - 1)
                    isUnspaced(last) && isUnspaced(line.first()) -> Unit
                    else -> paragraph.append(' ')
                }
            }
            paragraph.append(line)
            if (line.last() in SENTENCE_ENDS && line.length < typical * 0.8) end()
        }
        end()
        return out.toString().trim()
    }

    private const val SENTENCE_ENDS = ".!?:…\"»”’)。！？"
}
