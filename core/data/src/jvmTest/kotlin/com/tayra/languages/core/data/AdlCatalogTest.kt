package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.AdlCatalog
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Arkiv for Dansk Litteratur's works: listed from the portal's Atom feed, Andersen's unsearched, and read from their pages without the library's apparatus. */
class AdlCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val catalog = AdlCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            when (request.url.encodedPath) {
                "/text.atom" -> {
                    val volume = request.url.parameters["f[volume_title_ssi][]"]?.substringAfter("bd. ")?.substringBefore(' ')
                    val feed = when {
                        volume == "i" -> FEED
                        volume == "ii" -> feed(25, (1..25).joinToString("") { entry("adl-texts-hcaeventyr02val-shoot-workid$it", "Eventyr $it", "H. C. Andersen") })
                        volume != null -> feed(0, "")
                        request.url.parameters["page"] == "12" -> LAST_PAGE
                        else -> FEED
                    }
                    respond(feed, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/atom+xml"))
                }
                "/text/adl-texts-hcaeventyr01val-shoot-workid67635" -> respond(TALE, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
                "/text/adl-texts-aare01val-shoot-workid73531" -> respond(POEM, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
                else -> error("Unexpected request ${request.url}")
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun withoutASearchItListsAndersensFairyTalesVolumeByVolume() = runTest {
        val first = catalog.search("da", "", 1)
        val q = requests.first().url.parameters
        assertEquals("h.c. andersens eventyr bd. i kritisk udg. efter de originale eventyrhæfter med varianter ved erik dal", q["f[volume_title_ssi][]"])
        assertEquals("yes", q["f[has_text_ssi][]"], "page scans without a text are left out")
        assertEquals("adl", q["f[subcollection_ssi][]"])
        assertEquals("work", q["f[type_ssi][]"])
        assertEquals("100", q["per_page"], "a volume is fetched whole")
        // The first volume's three works and the second's 25: the second is needed to fill the page.
        assertEquals(20, first.books.size)
        assertEquals(null, first.total)
        assertTrue(first.hasMore)
        assertEquals(2, requests.size)

        val second = catalog.search("da", "", 2)
        assertEquals(8, second.books.size)
        assertEquals(28, second.total)
        assertFalse(second.hasMore)
        assertEquals(5, requests.size, "the last, empty volumes are fetched once the list reaches them")

        val tale = first.books.first()
        assertEquals("adl-texts-hcaeventyr01val-shoot-workid67635", tale.id)
        assertEquals("Den lille Havfrue.", tale.title)
        assertEquals(listOf("H. C. Andersen"), tale.authors)
        assertEquals("CC-BY-NC-SA", tale.license)
        assertEquals("https://tekster.kb.dk/text/adl-texts-hcaeventyr01val-shoot-workid67635", tale.pageUrl)
        assertEquals(listOf("Eventyr, fortalte for Børn. Første Samling.", "HAVFRUEN.", "Eventyr 1"), first.books.drop(1).take(3).map { it.title })
    }

    @Test
    fun aSearchLooksInEveryWork() = runTest {
        val page = catalog.search("da", "Havfrue", 1)
        val q = requests.single().url.parameters
        assertEquals("Havfrue", q["q"])
        assertEquals(null, q["f[volume_title_ssi][]"])
        assertEquals(null, q["page"])
        assertEquals("20", q["per_page"])
        assertEquals(225, page.total)
        assertTrue(page.hasMore)
        assertFalse(catalog.search("da", "Havfrue", 12).hasMore)
    }

    @Test
    fun aWorkIsReadWithoutLinksPageNumbersAndNotes() = runTest {
        val books = catalog.search("da", "", 1).books
        val tale = catalog.text(books[0])
        assertEquals("Den lille Havfrue.\nLangt ude i Havet er Vandet saa blaat, som Bladene paa den dejligste Kornblomst.\nDernede boe Havfolkene.", tale)
        assertEquals(tale, catalog.sample(books[0]))
        assertEquals(1, requests.count { it.url.encodedPath.startsWith("/text/") }, "the text judged is the text added")
        // Verses keep their lines.
        assertEquals("HAVFRUEN.\nI\nDronning Thora gik paa Øen\nVed den aabne, kolde Strand.", catalog.text(books[2]))
    }

    private companion object {
        fun entry(id: String, title: String, author: String) = """
            <entry>
              <title>
                        $title
                      </title>
              <link rel="alternate" type="text/html" href="https://tekster.kb.dk/text/$id"/>
            <link rel="alternate" title="xml" type="application/xml" href="https://tekster.kb.dk/text/$id.xml" />  <id>https://tekster.kb.dk/text/$id</id>
              <summary type="html">
            &lt;dl class="document-metadata dl-invert row"&gt;
                &lt;dt class="blacklight-author_id_ssi col-md-3"&gt;Forfatter:&lt;/dt&gt;
                &lt;dd class="blacklight-author_id_ssi col-md-9"&gt;&lt;a href="/text/adl-authors-x"&gt;$author&lt;/a&gt;&lt;/dd&gt;
                &lt;dt class="blacklight-subcollection_ssi col-md-3"&gt;Samling:&lt;/dt&gt;
            </summary>
            </entry>
        """

        val FEED = """<?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom" xmlns:opensearch="http://a9.com/-/spec/opensearch/1.1/">
              <title>Det Kgl. Biblioteks tekstportal søgeresultater</title>
              <link rel="next" href="https://tekster.kb.dk/text.atom?page=3&amp;per_page=20"/>
              <opensearch:totalResults>225</opensearch:totalResults>
              ${entry("adl-texts-hcaeventyr01val-shoot-workid67635", "Den lille Havfrue.", "H. C. Andersen")}
              ${entry("adl-texts-hcaeventyr01val-shoot-workid57967", "Eventyr, fortalte for Børn.\n   Første Samling.", "H. C. Andersen")}
              ${entry("adl-texts-aare01val-shoot-workid73531", "HAVFRUEN.", "Emil Aarestrup")}
            </feed>"""

        fun feed(total: Int, entries: String) = """<feed><opensearch:totalResults>$total</opensearch:totalResults>$entries</feed>"""

        val LAST_PAGE = """<feed><link rel="previous" href="https://tekster.kb.dk/text.atom?page=11"/><opensearch:totalResults>225</opensearch:totalResults>
            ${entry("adl-texts-x-shoot-workid1", "Sidste", "H. C. Andersen")}</feed>"""

        fun page(work: String) = """<html><head><title>Den lille Havfrue</title></head><body><nav>Søg Forside</nav>
            <div class="collection-adl" id="root" style="margin-top: 1em;">
              <div id="workid1" class="text snippetRoot"><span id="expose_link_workid1"><a class="the_whole" href="http://tekster.kb.dk/text/x"><span class="symbol the_whole" title="Vis det hele">↩ </span></a>
              </span>$work</div>
            </div>
            <span id="scrollTop" class="scrollup"><i class="icon arrow_upward"></i></span>
            <div class="modal fade" id="comment_modal"><h4 class="modal-title">Kommentar</h4></div></body></html>"""

        val TALE = page("""
            <h1 class="head-in-work" id="h1">Den lille Havfrue.</h1>
            <p class="paragraph" id="p1"><span id="expose_link_p1"><a class="quote" href="http://tekster.kb.dk/text/p1"><span class="symbol quote" title="Vis kun denne del">✂ </span></a>
              </span>Langt ude i Havet er Vandet saa blaat, som Bladene paa den dejligste
                            Kornblomst.<a class="note" title="Note">*</a><span class="note_content" title="Note" style="display:none;"><strong>m</strong><em>H. C. Andersens Hus, Odense.</em></span></p>
            <span title="Side 88" id="s88" class="pageBreak"><a href="#s88"><small>88</small></a></span>
            <p class="paragraph" style="text-indent:2em;" id="p2"><span id="expose_link_p2"><a class="quote" href="http://tekster.kb.dk/text/p2"><span class="symbol quote">✂ </span></a></span>Dernede boe Havfolkene.</p>
            <script>var dispidm1="none";</script>
        """)

        val POEM = page("""
            <h2 class="head-in-work">HAVFRUEN.</h2>
            <h2 class="head-in-work">I</h2>
            <p class="lineGroup"><span id="expose_link_lg1"><a class="quote" href="x"><span class="symbol quote">✂ </span></a></span>
              Dronning Thora gik paa Øen<br id="l1" class="line" />
              Ved den aabne, kolde Strand.<br id="l2" class="line" />
            </p>
        """)
    }
}
