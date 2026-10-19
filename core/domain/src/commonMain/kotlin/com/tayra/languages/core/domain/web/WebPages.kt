package com.tayra.languages.core.domain.web

/**
 * The article of a web page, read to become a book: its text one paragraph per line, without the
 * menus, ads, comments and links around it, and what the page says about itself.
 */
data class WebArticle(
    val url: String,
    val text: String,
    val title: String? = null,
    /** The page's language as it declares it (`pt-BR`), when it does. */
    val languageCode: String? = null,
    val author: String? = null,
    val siteName: String? = null,
)

/** Why a web page gave no article. */
sealed class WebPageException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** What was given is not a web address. */
    class InvalidAddress(address: String) : WebPageException("Not a web address: $address")

    /** The page could not be fetched: no connection, or the site answered [status]. */
    class Unreachable(val status: Int?, cause: Throwable? = null) :
        WebPageException(if (status != null) "The site answered $status" else "The page could not be reached", cause)

    /** The page was fetched, but held no article's text. */
    class NoArticle : WebPageException("No article text on the page")
}

/** Reads the article of a web page, or a document (an e-book, a PDF, a text file) a link points to. */
interface WebPageReader {
    suspend fun read(address: String): WebArticle
}
