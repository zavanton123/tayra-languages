package com.tayra.languages.core.ui.chinese

import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.morphology.ChineseScripts
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenccTablesTest {

    @Test
    fun theBundledTablesConvertBothWays() = runBlocking {
        val started = System.currentTimeMillis()
        ChineseScripts.prepare(ResourceChineseScriptTables())
        println("OPENCC loaded in ${System.currentTimeMillis() - started} ms")
        assertTrue(ChineseScripts.isReady)
        val cases = mapOf(
            "我们" to "我們", "头发" to "頭髮", "发现" to "發現", "面条" to "麵條", "面子" to "面子", "银行" to "銀行",
            "电脑" to "電腦", "为了" to "為了", "后来" to "後來", "皇后" to "皇后", "出租车" to "出租車",
        )
        for ((simplified, traditional) in cases) {
            assertEquals(traditional, ChineseScripts.convert(simplified, ChineseScript.TRADITIONAL), simplified)
            assertEquals(simplified, ChineseScripts.convert(traditional, ChineseScript.SIMPLIFIED), traditional)
        }
        assertEquals("我們發現他的頭髮很長。", ChineseScripts.convert("我们发现他的头发很长。", ChineseScript.TRADITIONAL))
    }
}
