package com.idiomiko.core.domain.books.discover

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The books of every library merged, narrowed to a topic, ranked by the share known and set out on shelves. */
class DiscoverTest {
    private data class Item(override val book: FoundBook, override val coverage: TextCoverage?) : JudgedBook

    private fun book(id: String, source: BookSource, title: String = id, author: String = "Ana Lima", subjects: List<String> = emptyList()) = FoundBook(
        source = source, id = id, title = title, authors = listOf(author), description = "", subjects = subjects,
        license = "Public domain", pageUrl = "https://${source.host}/$id", downloadUrl = "https://${source.host}/$id.txt",
    )

    private fun item(id: String, source: BookSource, percent: Int?, wholeWords: Int? = null, title: String = id) =
        Item(book(id, source, title), percent?.let { TextCoverage(100, it, wholeWords) })

    @Test
    fun aSearchThatFailsTellsWhetherTheLibraryIsDownOrOutOfReach() {
        assertEquals(LibraryStatus.AVAILABLE, LibraryStatus.of(null))
        assertEquals(LibraryStatus.DOWN_FOR_MAINTENANCE, LibraryStatus.of(LibraryDownException("Project Gutenberg is down for maintenance.")))
        assertEquals(LibraryStatus.UNREACHABLE, LibraryStatus.of(IllegalStateException("Project Gutenberg answered 502")))
        assertEquals(LibraryStatus.DOWN_FOR_MAINTENANCE, LibraryResult(BookSource.GUTENBERG, null, LibraryDownException("down")).status)
        assertEquals(LibraryStatus.AVAILABLE, LibraryResult(BookSource.WIKISOURCE, CatalogPage(emptyList(), hasMore = false), null).status)
        assertTrue(LibraryStatus.UNREACHABLE.isDown)
        assertFalse(LibraryStatus.CHECKING.isDown)
    }

    @Test
    fun theLibrariesTakeTurnsAndABookInSeveralIsListedOnce() {
        val gutenberg = listOf(item("g1", BookSource.GUTENBERG, null, title = "Dom Casmurro"), item("g2", BookSource.GUTENBERG, 80), item("g3", BookSource.GUTENBERG, 70))
        val wikisource = listOf(item("w1", BookSource.WIKISOURCE, 88, title = "Dom  casmurro!"), item("w2", BookSource.WIKISOURCE, null))
        val storybooks = listOf(item("s1", BookSource.STORYBOOKS, 96))
        val merged = Discovery.merge(listOf(gutenberg, wikisource, storybooks))
        // Dom Casmurro keeps its place, in the copy already judged.
        assertEquals(listOf("w1", "s1", "g2", "w2", "g3"), merged.map { it.book.id })
    }

    @Test
    fun booksAreRankedByTheShareKnownAndTheHarderOnesLeftOut() {
        val books = listOf(item("a", BookSource.GUTENBERG, 80), item("b", BookSource.GUTENBERG, null), item("c", BookSource.WIKISOURCE, 97), item("d", BookSource.GDL, 91))
        val ranked = Discovery.rank(books, minKnown = 90)
        assertEquals(listOf("c", "d", "b"), ranked.books.map { it.book.id })
        assertEquals(1, ranked.harder)
        assertEquals(listOf("c", "d", "a", "b"), Discovery.rank(books, minKnown = 0).books.map { it.book.id })
    }

    @Test
    fun easyWinsAreShortAndWellKnownAndClassicsReachDownWithAHardTail() {
        val books = listOf(
            item("story", BookSource.STORYBOOKS, 96),
            item("weaver", BookSource.STORYWEAVER, 89),
            item("poem", BookSource.ADAMASTOR, 93, wholeWords = 400),
            item("alienista", BookSource.ADAMASTOR, 86, wholeWords = 18_000),
            item("cortico", BookSource.GUTENBERG, 77),
            item("casmurro", BookSource.GUTENBERG, 68),
            item("lusiadas", BookSource.GUTENBERG, 61),
            item("braz", BookSource.WIKISOURCE, 52),
            item("unjudged", BookSource.GUTENBERG, null),
        )
        val easy = Discovery.shelf(Shelf.EASY_WINS, books, minKnown = 90)
        assertEquals(listOf("story", "poem"), easy.books.map { it.book.id })
        assertEquals(2, easy.hardFrom)
        val classics = Discovery.shelf(Shelf.CLASSICS_WITHIN_REACH, books, minKnown = 90)
        // The short poem is an easy win, so it is not a classic twice; two harder ones end the shelf.
        assertEquals(listOf("alienista", "cortico", "casmurro", "lusiadas"), classics.books.map { it.book.id })
        assertEquals(2, classics.hardFrom)
        // An easier level asked for moves books from the tail into reach.
        assertEquals(4, Discovery.shelf(Shelf.CLASSICS_WITHIN_REACH, books, minKnown = 75).hardFrom)
    }

    @Test
    fun aTopicIsBackedByTheLibraryLengthOrGenre() {
        val story = book("s", BookSource.STORYBOOKS)
        val novel = book("n", BookSource.GUTENBERG)
        val poems = book("p", BookSource.ADAMASTOR, subjects = listOf("Poesia"))
        val tales = book("t", BookSource.ADAMASTOR, subjects = listOf("Contos"))
        val juvenile = book("j", BookSource.WOLNE_LEKTURY, subjects = listOf("Lektury dla dzieci"))
        val short = TextCoverage(100, 90, wholeWords = 900)
        assertTrue(Discovery.matches(Topic.CHILDRENS_BOOKS, story, null))
        assertTrue(Discovery.matches(Topic.CHILDRENS_BOOKS, juvenile, null))
        assertFalse(Discovery.matches(Topic.CHILDRENS_BOOKS, novel, null))
        assertTrue(Discovery.matches(Topic.SHORT_STORIES, story, null))
        assertTrue(Discovery.matches(Topic.SHORT_STORIES, tales, null))
        assertTrue(Discovery.matches(Topic.SHORT_STORIES, novel, short))
        assertFalse(Discovery.matches(Topic.SHORT_STORIES, novel, TextCoverage(100, 90, wholeWords = 40_000)))
        assertTrue(Discovery.matches(Topic.POEMS, poems, null))
        assertFalse(Discovery.matches(Topic.POEMS, novel, null))
        assertTrue(Discovery.matches(Topic.CLASSIC_NOVELS, novel, null))
        assertFalse(Discovery.matches(Topic.CLASSIC_NOVELS, novel, short))
        assertFalse(Discovery.matches(Topic.CLASSIC_NOVELS, poems, null))
        assertFalse(Discovery.matches(Topic.CLASSIC_NOVELS, story, null))

        // Poems need a library that names genres; Portuguese has one, Greek has not.
        val portuguese = listOf(BookSource.GUTENBERG, BookSource.WIKISOURCE, BookSource.GDL, BookSource.STORYBOOKS, BookSource.STORYWEAVER, BookSource.ADAMASTOR)
        assertEquals(Topic.entries.toList(), Discovery.topics(portuguese))
        assertEquals(listOf(Topic.SHORT_STORIES, Topic.CHILDRENS_BOOKS, Topic.CLASSIC_NOVELS), Discovery.topics(listOf(BookSource.GUTENBERG, BookSource.WIKISOURCE, BookSource.GDL, BookSource.STORYBOOKS)))
    }

    @Test
    fun aWholeTextJudgedTellsItsMinutesAtTheReadingPace() {
        assertEquals(7, TextCoverage(400, 380, wholeWords = 420).minutes)
        assertEquals(null, TextCoverage(400, 380).minutes)
    }
}
