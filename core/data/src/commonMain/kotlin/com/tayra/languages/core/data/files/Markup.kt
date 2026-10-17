package com.tayra.languages.core.data.files

/** A piece of an XML or HTML document; names are lowercased, entities in text and attributes resolved. */
internal sealed interface MarkupToken {
    data class Open(val name: String, val attributes: Map<String, String>, val selfClosing: Boolean) : MarkupToken
    data class Close(val name: String) : MarkupToken
    data class Text(val text: String) : MarkupToken
}

/**
 * Reads XML and HTML as a stream of tags and text, forgiving the HTML that books and web pages are
 * written in: unquoted attributes, a "<" that starts no tag, the raw insides of script and style.
 */
internal object Markup {

    fun tokens(source: String): Sequence<MarkupToken> = sequence {
        var i = 0
        val text = StringBuilder()
        suspend fun SequenceScope<MarkupToken>.flush() {
            if (text.isNotEmpty()) {
                yield(MarkupToken.Text(decodeEntities(text.toString())))
                text.clear()
            }
        }
        while (i < source.length) {
            val c = source[i]
            if (c != '<' || i + 1 >= source.length) {
                text.append(c)
                i++
                continue
            }
            val next = source[i + 1]
            when {
                source.startsWith("<!--", i) -> {
                    flush()
                    val end = source.indexOf("-->", i + 4)
                    i = if (end < 0) source.length else end + 3
                }
                source.startsWith("<![CDATA[", i) -> {
                    flush()
                    val end = source.indexOf("]]>", i + 9).let { if (it < 0) source.length else it }
                    yield(MarkupToken.Text(source.substring(i + 9, end)))
                    i = minOf(source.length, end + 3)
                }
                next == '!' || next == '?' -> {
                    flush()
                    val close = source.indexOf('>', i)
                    // A DOCTYPE can carry declarations of its own in brackets, each ending in '>'.
                    val subset = source.indexOf('[', i)
                    val end = if (source.startsWith("<!DOCTYPE", i, ignoreCase = true) && subset in 0 until close) {
                        source.indexOf("]>", subset).let { if (it < 0) -1 else it + 1 }
                    } else close
                    i = if (end < 0) source.length else end + 1
                }
                next == '/' || next.isLetter() -> {
                    val end = tagEnd(source, i)
                    if (end < 0) {
                        text.append(c)
                        i++
                        continue
                    }
                    flush()
                    val body = source.substring(i + 1, end)
                    i = end + 1
                    if (body.startsWith("/")) {
                        yield(MarkupToken.Close(body.drop(1).trim().lowercase()))
                        continue
                    }
                    val selfClosing = body.endsWith("/")
                    val inner = if (selfClosing) body.dropLast(1) else body
                    val nameEnd = inner.indexOfFirst { it.isWhitespace() }.let { if (it < 0) inner.length else it }
                    val name = inner.substring(0, nameEnd).lowercase()
                    yield(MarkupToken.Open(name, attributes(inner.substring(nameEnd)), selfClosing))
                    // A script or style holds code, not markup: its text runs to its closing tag.
                    if (!selfClosing && name in RAW_TEXT) {
                        val close = source.indexOf("</$name", i, ignoreCase = true).let { if (it < 0) source.length else it }
                        yield(MarkupToken.Text(source.substring(i, close)))
                        i = close
                    }
                }
                else -> {
                    text.append(c)
                    i++
                }
            }
        }
        flush()
    }

    /** Where the tag starting at [start] ends, its ">" outside quotes; -1 when it never does. */
    private fun tagEnd(source: String, start: Int): Int {
        var quote: Char? = null
        var i = start + 1
        while (i < source.length) {
            val c = source[i]
            when {
                quote != null -> if (c == quote) quote = null
                c == '"' || c == '\'' -> quote = c
                c == '>' -> return i
                c == '<' -> return -1
            }
            i++
        }
        return -1
    }

    private val attribute = Regex("""([^\s=/]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+)))?""")

    private fun attributes(text: String): Map<String, String> {
        if (text.isBlank()) return emptyMap()
        return attribute.findAll(text).associate { m ->
            m.groupValues[1].lowercase() to decodeEntities(m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } })
        }
    }

    private val entity = Regex("""&(#[0-9]+|#[xX][0-9a-fA-F]+|[A-Za-z][A-Za-z0-9]*);""")

    /** [text] with its character references (&amp;, &#233;, &#x1F600;) and the HTML 4 named entities resolved. */
    fun decodeEntities(text: String): String {
        if ('&' !in text) return text
        return entity.replace(text) { m ->
            val name = m.groupValues[1]
            val code = when {
                name.startsWith("#x") || name.startsWith("#X") -> name.drop(2).toIntOrNull(16)
                name.startsWith("#") -> name.drop(1).toIntOrNull()
                else -> NAMED[name] ?: if (name == "apos") 39 else null
            }
            if (code == null || code < 0 || code > 0x10FFFF) m.value else codePointString(code)
        }
    }

    fun codePointString(code: Int): String = if (code < 0x10000) code.toChar().toString() else {
        val offset = code - 0x10000
        charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
    }

    /** Elements whose content is code. */
    private val RAW_TEXT = setOf("script", "style")

    /** The HTML 4 entities (Python's html.entities.name2codepoint), name to code point. */
    private val NAMED: Map<String, Int> by lazy {
        ENTITIES.split(',').associate { it.substringBefore('=') to it.substringAfter('=').toInt() }
    }

    private const val ENTITIES =
        "AElig=198,Aacute=193,Acirc=194,Agrave=192,Alpha=913,Aring=197,Atilde=195,Auml=196,Beta=914,Ccedil=19" +
        "9,Chi=935,Dagger=8225,Delta=916,ETH=208,Eacute=201,Ecirc=202,Egrave=200,Epsilon=917,Eta=919,Euml=203" +
        ",Gamma=915,Iacute=205,Icirc=206,Igrave=204,Iota=921,Iuml=207,Kappa=922,Lambda=923,Mu=924,Ntilde=209," +
        "Nu=925,OElig=338,Oacute=211,Ocirc=212,Ograve=210,Omega=937,Omicron=927,Oslash=216,Otilde=213,Ouml=21" +
        "4,Phi=934,Pi=928,Prime=8243,Psi=936,Rho=929,Scaron=352,Sigma=931,THORN=222,Tau=932,Theta=920,Uacute=" +
        "218,Ucirc=219,Ugrave=217,Upsilon=933,Uuml=220,Xi=926,Yacute=221,Yuml=376,Zeta=918,aacute=225,acirc=2" +
        "26,acute=180,aelig=230,agrave=224,alefsym=8501,alpha=945,amp=38,and=8743,ang=8736,aring=229,asymp=87" +
        "76,atilde=227,auml=228,bdquo=8222,beta=946,brvbar=166,bull=8226,cap=8745,ccedil=231,cedil=184,cent=1" +
        "62,chi=967,circ=710,clubs=9827,cong=8773,copy=169,crarr=8629,cup=8746,curren=164,dArr=8659,dagger=82" +
        "24,darr=8595,deg=176,delta=948,diams=9830,divide=247,eacute=233,ecirc=234,egrave=232,empty=8709,emsp" +
        "=8195,ensp=8194,epsilon=949,equiv=8801,eta=951,eth=240,euml=235,euro=8364,exist=8707,fnof=402,forall" +
        "=8704,frac12=189,frac14=188,frac34=190,frasl=8260,gamma=947,ge=8805,gt=62,hArr=8660,harr=8596,hearts" +
        "=9829,hellip=8230,iacute=237,icirc=238,iexcl=161,igrave=236,image=8465,infin=8734,int=8747,iota=953," +
        "iquest=191,isin=8712,iuml=239,kappa=954,lArr=8656,lambda=955,lang=9001,laquo=171,larr=8592,lceil=896" +
        "8,ldquo=8220,le=8804,lfloor=8970,lowast=8727,loz=9674,lrm=8206,lsaquo=8249,lsquo=8216,lt=60,macr=175" +
        ",mdash=8212,micro=181,middot=183,minus=8722,mu=956,nabla=8711,nbsp=160,ndash=8211,ne=8800,ni=8715,no" +
        "t=172,notin=8713,nsub=8836,ntilde=241,nu=957,oacute=243,ocirc=244,oelig=339,ograve=242,oline=8254,om" +
        "ega=969,omicron=959,oplus=8853,or=8744,ordf=170,ordm=186,oslash=248,otilde=245,otimes=8855,ouml=246," +
        "para=182,part=8706,permil=8240,perp=8869,phi=966,pi=960,piv=982,plusmn=177,pound=163,prime=8242,prod" +
        "=8719,prop=8733,psi=968,quot=34,rArr=8658,radic=8730,rang=9002,raquo=187,rarr=8594,rceil=8969,rdquo=" +
        "8221,real=8476,reg=174,rfloor=8971,rho=961,rlm=8207,rsaquo=8250,rsquo=8217,sbquo=8218,scaron=353,sdo" +
        "t=8901,sect=167,shy=173,sigma=963,sigmaf=962,sim=8764,spades=9824,sub=8834,sube=8838,sum=8721,sup=88" +
        "35,sup1=185,sup2=178,sup3=179,supe=8839,szlig=223,tau=964,there4=8756,theta=952,thetasym=977,thinsp=" +
        "8201,thorn=254,tilde=732,times=215,trade=8482,uArr=8657,uacute=250,uarr=8593,ucirc=251,ugrave=249,um" +
        "l=168,upsih=978,upsilon=965,uuml=252,weierp=8472,xi=958,yacute=253,yen=165,yuml=255,zeta=950,zwj=820" +
        "5,zwnj=8204"
}

/**
 * The text of a document, built a paragraph at a time: the app reads each line as a paragraph,
 * and a blank line parts sections (chapters, a book's files). Runs of white space inside a
 * paragraph become one space, as a browser shows them, unless kept as they are (preformatted text).
 */
internal class TextBuilder {
    private val lines = mutableListOf<String>()
    private val line = StringBuilder()
    /** White space read but not yet written: a space, or a line wrap, which joins Chinese or Thai without one. */
    private var pending = NONE

    fun text(text: String, preserve: Boolean = false) {
        if (preserve) {
            flushPending(null)
            val parts = text.split('\n')
            parts.forEachIndexed { index, part ->
                line.append(part)
                if (index < parts.lastIndex) breakLine()
            }
            return
        }
        for (c in text) {
            when {
                c == '\n' -> if (pending == NONE) pending = WRAP
                c.isWhitespace() -> pending = SPACE
                else -> {
                    flushPending(c)
                    line.append(c)
                }
            }
        }
    }

    /** A space between two words that markup alone separates (table cells, a tab). */
    fun space() {
        pending = SPACE
    }

    /** Where a source line wraps inside a paragraph: a space, except between letters of scripts written without spaces. */
    fun wrap() {
        if (pending == NONE) pending = WRAP
    }

    private fun flushPending(next: Char?) {
        val kind = pending
        pending = NONE
        if (kind == NONE || line.isEmpty() || line.last() == ' ') return
        if (kind == WRAP && next != null && isUnspaced(line.last()) && isUnspaced(next)) return
        line.append(' ')
    }

    /** Ends the paragraph. */
    fun breakLine() {
        pending = NONE
        val done = line.toString().trim()
        line.clear()
        if (done.isNotEmpty()) lines += done
    }

    /** Ends the paragraph and the section. */
    fun blankLine() {
        breakLine()
        if (lines.isNotEmpty() && lines.last().isNotEmpty()) lines += ""
    }

    fun build(): String {
        breakLine()
        return lines.joinToString("\n").trim()
    }

    private companion object {
        const val NONE = 0
        const val SPACE = 1
        const val WRAP = 2
    }
}

/**
 * Whether [c] belongs to a script written without spaces between words (Chinese, Japanese kana,
 * Thai, Lao, Khmer, Burmese, Tibetan) or is East Asian punctuation, so that a line wrapped between
 * two such characters is joined without a space.
 */
internal fun isUnspaced(c: Char): Boolean = when (c.code) {
    in 0x0E00..0x0EFF, in 0x0F00..0x0FFF, in 0x1000..0x109F, in 0x1780..0x17FF -> true
    in 0x3000..0x30FF, in 0x31F0..0x31FF, in 0x3400..0x4DBF, in 0x4E00..0x9FFF, in 0xF900..0xFAFF -> true
    in 0xFF00..0xFF65, in 0xFF66..0xFF9F, in 0xD840..0xD87F, in 0xDC00..0xDFFF -> true
    else -> false
}

/** The text of HTML and XHTML: what a reader sees, without code, ruby readings or note references. */
internal object HtmlText {

    fun text(html: String): String = TextBuilder().also { append(html, it) }.build()

    /** Adds [html] to [out] as paragraphs. */
    fun append(html: String, out: TextBuilder) {
        val tokens = Markup.tokens(html).iterator()
        var preformatted = 0
        while (tokens.hasNext()) {
            when (val token = tokens.next()) {
                is MarkupToken.Text -> out.text(token.text, preserve = preformatted > 0)
                is MarkupToken.Open -> {
                    val name = token.name
                    when {
                        !token.selfClosing && (name in SKIPPED || isNote(token)) -> skip(tokens, name)
                        name == "br" -> out.breakLine()
                        name == "pre" && !token.selfClosing -> { out.breakLine(); preformatted++ }
                        name == "td" || name == "th" -> out.space()
                        name in BLOCKS -> out.breakLine()
                    }
                }
                is MarkupToken.Close -> when (token.name) {
                    "pre" -> { out.breakLine(); preformatted = maxOf(0, preformatted - 1) }
                    "td", "th" -> out.space()
                    in BLOCKS -> out.breakLine()
                }
            }
        }
    }

    /** The document's <title>, if it has one. */
    fun title(html: String): String? {
        val tokens = Markup.tokens(html).iterator()
        while (tokens.hasNext()) {
            val token = tokens.next()
            if (token is MarkupToken.Open && token.name == "title" && !token.selfClosing) {
                val text = StringBuilder()
                while (tokens.hasNext()) {
                    val inner = tokens.next()
                    if (inner is MarkupToken.Close && inner.name == "title") break
                    if (inner is MarkupToken.Text) text.append(inner.text)
                }
                return text.toString().replace(Regex("\\s+"), " ").trim().takeIf { it.isNotEmpty() }
            }
            if (token is MarkupToken.Open && token.name == "body") return null
        }
        return null
    }

    /** Skips the rest of the element [name] just opened, its nested namesakes included. */
    fun skip(tokens: Iterator<MarkupToken>, name: String) {
        var depth = 1
        while (depth > 0 && tokens.hasNext()) {
            when (val token = tokens.next()) {
                is MarkupToken.Open -> if (token.name == name && !token.selfClosing) depth++
                is MarkupToken.Close -> if (token.name == name) depth--
                is MarkupToken.Text -> Unit
            }
        }
    }

    /** A note reference ("1" after a word) or the note itself, which would break into the text. */
    private fun isNote(token: MarkupToken.Open): Boolean {
        val type = (token.attributes["epub:type"].orEmpty() + " " + token.attributes["role"].orEmpty()).lowercase()
        return NOTE_TYPES.any { it in type }
    }

    private val NOTE_TYPES = listOf("noteref", "footnote", "endnote", "rearnote")

    /** Elements whose content is no text to read: code, the head, ruby readings over kanji. */
    private val SKIPPED = setOf("head", "title", "script", "style", "noscript", "template", "svg", "math", "rt", "rp", "object", "iframe", "select", "button")

    /** Elements that start and end a paragraph. */
    private val BLOCKS = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "ul", "ol", "dl", "dt", "dd", "tr", "table", "caption",
        "blockquote", "section", "article", "header", "footer", "nav", "aside", "main", "figure", "figcaption",
        "hr", "address", "center", "body", "html", "details", "summary", "mbp:pagebreak", "legend", "fieldset",
    )
}
