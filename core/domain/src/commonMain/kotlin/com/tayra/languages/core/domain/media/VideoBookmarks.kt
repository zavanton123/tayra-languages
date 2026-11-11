package com.tayra.languages.core.domain.media

import com.tayra.languages.core.domain.model.PageBookmark
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A video's bookmarks are its transcript's page bookmarks, so they sync as a book's do: each is
 * titled with the time and text of its line, which finds the line again on its page.
 */
object VideoBookmarks {
    fun title(cue: Cue): String = "${clock(cue.beginMs)} ${cue.text.replace('\n', ' ').trim()}"

    /**
     * The index of the line [bookmark] is at, of [lines] given as (index, page, cue): the line on its
     * page with its title, else one whose text it has, else the page's first; null when the page has none.
     */
    fun lineOf(bookmark: PageBookmark, lines: List<Triple<Int, Int, Cue>>): Int? {
        val onPage = lines.filter { it.second == bookmark.pageNumber }
        return (onPage.firstOrNull { title(it.third) == bookmark.title }
            ?: onPage.firstOrNull { it.third.text.isNotBlank() && it.third.text.replace('\n', ' ').trim() in bookmark.title }
            ?: onPage.firstOrNull())?.first
    }

    /** [ms] as YouTube writes times: 1:05, or 1:02:05 past an hour. */
    fun clock(ms: Long): String {
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = (total % 60).toString().padStart(2, '0')
        return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$s" else "$m:$s"
    }
}

/** Videos something outside the app's screens asked to open, such as the browser extension's "Open in Tayra". */
class VideoOpenRequests {
    private val requests = MutableSharedFlow<Long>(extraBufferCapacity = 8)

    /** The book ids of the videos to open, as they are asked for. */
    val videos: SharedFlow<Long> = requests.asSharedFlow()

    fun open(bookId: Long) {
        requests.tryEmit(bookId)
    }
}
