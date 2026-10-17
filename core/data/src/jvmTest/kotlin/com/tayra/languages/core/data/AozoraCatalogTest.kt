package com.tayra.languages.core.data

import com.tayra.languages.core.data.discover.AozoraCatalog
import com.tayra.languages.core.data.discover.AozoraText
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Aozora Bunko's works: the most read for children, a search by title or by author, and texts without Aozora's markup. */
class AozoraCatalogTest {
    private val requests = mutableListOf<HttpRequestData>()

    private val catalog = AozoraCatalog(
        HttpClient(MockEngine { request ->
            requests += request
            val q = request.url.parameters
            when (request.url.encodedPath) {
                "/v1/books" -> respond(
                    when {
                        q["juvenile"] == "true" -> BOOKS
                        q["q"] == "銀河" -> """{"books": [{"id": 456, "title": "銀河鉄道の夜", "subTitle": "", "characterType": "新字新仮名", "author": {"id": 81, "fullName": "宮沢賢治"}}], "hasNext": false, "totalCount": 1}"""
                        q["author_id"] == "81" -> """{"books": [{"id": 43754, "title": "注文の多い料理店", "subTitle": "", "characterType": "新字新仮名", "beginning": "二人の若い紳士が", "author": {"id": 81, "fullName": "宮沢賢治"}}], "hasNext": false, "totalCount": 1}"""
                        else -> """{"books": [], "hasNext": false, "totalCount": 0}"""
                    },
                    HttpStatusCode.OK, JSON,
                )
                "/v1/persons" -> respond(
                    if (q["q"]!!.startsWith("宮沢")) """{"persons": [{"id": 81, "fullName": "宮沢賢治"}], "hasNext": false, "totalCount": 1}"""
                    else """{"persons": [], "hasNext": false, "totalCount": 0}""",
                    HttpStatusCode.OK, JSON,
                )
                "/v1/books/92/text" -> respond(KUMO, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8"))
                else -> error("Unexpected request ${request.url}")
            }
        }) { install(HttpTimeout) },
    )

    @Test
    fun withoutASearchItListsTheMostReadWorksForChildren() = runTest {
        val page = catalog.search("ja", "", 2)
        val request = requests.single()
        assertEquals("true", request.url.parameters["juvenile"])
        assertEquals("false", request.url.parameters["rights_reserved"], "works still under copyright are left out")
        assertEquals("access_count", request.url.parameters["sort"])
        assertEquals("20", request.url.parameters["offset"])
        assertEquals(1590, page.total)
        assertTrue(page.hasMore)

        val kumo = page.books.first()
        assertEquals("92", kumo.id)
        assertEquals("蜘蛛の糸", kumo.title)
        assertEquals(listOf("芥川竜之介"), kumo.authors)
        assertEquals("ある日の事でございます。", kumo.description)
        assertNull(kumo.level, "modern writing is the usual")
        assertEquals("https://www.aozora.gr.jp/cards/000879/card92.html", kumo.pageUrl)
        assertEquals("https://api.bungo.app/v1/books/92/text", kumo.downloadUrl)

        val old = page.books[1]
        assertEquals("風の又三郎 異稿", old.title, "a subtitle follows the title")
        assertEquals("新字旧仮名", old.level, "old kana spellings are pointed out")
    }

    @Test
    fun aSearchNamingAnAuthorListsTheAuthorsWorksAndAnotherMatchesTitles() = runTest {
        val page = catalog.search("ja", "宮沢賢治の作品をすべて読みたいと思います", 1)
        assertEquals("宮沢賢治の作品をすべて読", requests.first().url.parameters["q"], "the API takes 12 characters")
        assertEquals(listOf("43754"), page.books.map { it.id })
        assertFalse(page.hasMore)
        assertEquals("81", requests.last().url.parameters["author_id"])

        assertEquals(listOf("銀河鉄道の夜"), catalog.search("ja", "銀河", 1).books.map { it.title })
    }

    @Test
    fun aTextLosesItsTitleNotesReadingsAndColophon() = runTest {
        val kumo = catalog.search("ja", "", 1).books.first()
        val expected = "一\nある日の事でございます。御釈迦様は極楽の蓮池のふちを、独りでぶらぶら御歩きになっていらっしゃいました。\nこの極楽の蓮池の下は、丁度地獄の底に当って居ります。\n陀多と云う男が一人。\n（大正七年四月十六日）"
        assertEquals(expected, catalog.text(kumo))
        assertEquals(expected, catalog.sample(kumo))
        assertEquals("bytes=0-65535", requests.last().headers[HttpHeaders.Range])
    }

    @Test
    fun aTextWithoutNotesOnItsMarkupLosesItsTitleLines() {
        assertEquals("むかしむかし。\nおしまい。", AozoraText.clean("桃太郎\n楠山正雄\n\n　むかしむかし。\n　おしまい。\n\n\n底本：「日本の神話と十大昔話」講談社学術文庫\n"))
    }

    private companion object {
        val JSON = headersOf(HttpHeaders.ContentType, "application/json")

        val BOOKS = """{"books": [
            {"id": 92, "title": "蜘蛛の糸", "subTitle": "", "characterType": "新字新仮名", "rightsReserved": false, "accessCount": 1467475, "wordsCount": 2904,
             "beginning": "ある日の事でございます。", "juvenile": true, "author": {"id": 879, "fullName": "芥川竜之介"}},
            {"id": 1943, "title": "風の又三郎", "subTitle": "異稿", "characterType": "新字旧仮名", "beginning": "", "author": {"id": 81, "fullName": "宮沢賢治"}}
            ], "hasNext": true, "totalCount": 1590}"""

        val KUMO = "蜘蛛の糸\r\n芥川龍之介\r\n\r\n-------------------------------------------------------\r\n【テキスト中に現れる記号について】\r\n\r\n《》：ルビ\r\n（例）蓮池《はすいけ》のふち\r\n" +
            "-------------------------------------------------------\r\n\r\n［＃８字下げ］一［＃「一」は中見出し］\r\n\r\n" +
            "　ある日の事でございます。御釈迦様《おしゃかさま》は極楽の蓮池《はすいけ》のふちを、独りでぶらぶら御歩きになっていらっしゃいました。\r\n" +
            "　この極楽の蓮池の下は、丁度｜地獄《じごく》の底に当って居ります。\r\n" +
            "　※［＃「特のへん＋廴＋聿」、第3水準1-87-71］陀多《かんだた》と云う男が一人。\r\n" +
            "［＃地から１字上げ］（大正七年四月十六日）\r\n\r\n\r\n\r\n" +
            "底本：「芥川龍之介全集2」ちくま文庫、筑摩書房\r\n　　　1986（昭和61）年10月28日第1刷発行\r\n入力：平山誠、野口英司\r\n"
    }
}
