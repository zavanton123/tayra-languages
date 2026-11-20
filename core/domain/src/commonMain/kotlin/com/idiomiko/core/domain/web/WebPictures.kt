package com.idiomiko.core.domain.web

/** Pictures on the web, such as a news article's, fetched by their address as the bytes of an image file. */
fun interface WebPictures {
    /** The picture at [url]; null when it cannot be fetched, or is too big to show as a thumbnail. */
    suspend fun bytes(url: String): ByteArray?
}
