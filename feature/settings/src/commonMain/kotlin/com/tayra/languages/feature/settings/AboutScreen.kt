package com.tayra.languages.feature.settings

import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.components.ScreenTitle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.navigation.Route

@Composable
fun AboutScreen(onNavigate: (Route) -> Unit) {
    Scaffold(topBar = { AppTopBar(title = tr("About"), onNavigate = onNavigate, section = NavSection.SETTINGS) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenTitle(tr("About"))
            Text("Tayra Languages", style = MaterialTheme.typography.headlineSmall)
            Text(tr("Learn languages by reading. Import texts, click words to look them up and track what you know."), style = MaterialTheme.typography.bodyLarge)
            Text(tr("A reader for learning languages through texts, running on Android, iOS, desktop and the web with a shared Compose UI."), style = MaterialTheme.typography.bodyMedium)
            Text(tr("Credits"), style = MaterialTheme.typography.titleMedium)
            CREDITS.forEach { Text(tr(it), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/** Where the app's dictionaries, sentences, lists and recordings come from, and under which licences. */
private val CREDITS = listOf(
    "Offline dictionaries are built from Wiktionary, through kaikki.org and wiktionary-to-yomitan, licensed CC BY-SA 4.0. Japanese ones add JMdict, the names pack is JMnedict, and kanji come from KANJIDIC2, all by the Electronic Dictionary Research and Development Group (EDRDG), licensed CC BY-SA 4.0. Chinese-English adds CC-CEDICT by MDBG, licensed CC BY-SA 4.0. The Toki Pona dictionaries come from lipu Linku, licensed CC BY-SA 4.0.",
    "The pitch accent notation was provided by Uros O. through his free database Kanjium, licensed CC BY-SA 4.0.",
    "Example sentences come from Tatoeba, licensed CC BY 2.0 FR. Recorded pronunciations come from Lingua Libre and Wiktionary on Wikimedia Commons; each names its speaker and links to its licence.",
    "Word frequency lists come from wordfreq by Robyn Speer and from FrequencyWords by Hermit Dave (OpenSubtitles counts), both licensed CC BY-SA 4.0, and from the Leipzig Corpora Collection (Universität Leipzig).",
    "The inflection rules, furigana and reading code port Yomitan (GPL-3.0); the Japanese inflections and number, era and area readers follow 10ten Japanese Reader (GPL-3.0); the tone colours, zhuyin and grammar links come from the Zhongwen Chinese reader (GPL-2.0 or later). Japanese text on desktop is cut with Kuromoji (Apache 2.0) and MeCab's IPADIC dictionary. Chinese is converted between simplified and traditional characters with the tables of Open Chinese Convert (OpenCC, Apache 2.0), as GoldenDict converts it.",
    "PDF files are read with Apache PDFBox, and on Android with PDFBox-Android by Tom Roush, both licensed Apache 2.0.",
    "Text in photos is read on the device: with Tesseract (Apache 2.0) and Leptonica (BSD) through bytedeco's JavaCPP builds (Apache 2.0), using the tessdata_fast models (Apache 2.0); on Android also with Google's ML Kit, and on iOS with Apple's Vision.",
)
