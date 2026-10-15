package com.tayra.languages.core.ui.chinese

import com.tayra.languages.core.domain.morphology.ChineseScriptTables
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.MissingResourceException
import tayra_languages.core.ui.generated.resources.Res

/** OpenCC's dictionaries, bundled in files/opencc with their licence. */
class ResourceChineseScriptTables : ChineseScriptTables {
    override suspend fun read(name: String): String? = withContext(Dispatchers.Default) {
        try {
            Res.readBytes("files/opencc/$name").decodeToString()
        } catch (e: MissingResourceException) {
            null
        }
    }
}
