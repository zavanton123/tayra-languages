package com.tayra.languages.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.morphology.ChineseScriptTables
import com.tayra.languages.core.domain.morphology.ChineseScripts
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test

/** A title of the language being learned, shown in the script Chinese is read in. */
@OptIn(ExperimentalTestApi::class)
class LearnedScriptTest {

    @Test
    fun aTitleIsShownInTheScript() = runDesktopComposeUiTest {
        runBlocking { ChineseScripts.prepare(ChineseScriptTables { name -> File("src/commonMain/composeResources/files/opencc/$name").takeIf { it.isFile }?.readText() }) }
        setContent {
            CompositionLocalProvider(LocalLearnedScript provides ChineseScript.TRADITIONAL) { Text("我的头发".inLearnedScript()) }
            Text("Minha casa".inLearnedScript())
        }
        onNodeWithText("我的頭髮").assertExists()
        onNodeWithText("Minha casa").assertExists()
    }
}
