package com.tayra.languages.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.language.LanguageCodes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import tayra_languages.core.ui.generated.resources.Res
import tayra_languages.core.ui.generated.resources.flag_ke
import tayra_languages.core.ui.generated.resources.flag_eg
import tayra_languages.core.ui.generated.resources.flag_ca
import tayra_languages.core.ui.generated.resources.flag_be
import tayra_languages.core.ui.generated.resources.flag_br
import tayra_languages.core.ui.generated.resources.flag_al
import tayra_languages.core.ui.generated.resources.flag_am
import tayra_languages.core.ui.generated.resources.flag_az
import tayra_languages.core.ui.generated.resources.flag_ba
import tayra_languages.core.ui.generated.resources.flag_bd
import tayra_languages.core.ui.generated.resources.flag_bg
import tayra_languages.core.ui.generated.resources.flag_by
import tayra_languages.core.ui.generated.resources.flag_cn
import tayra_languages.core.ui.generated.resources.flag_cz
import tayra_languages.core.ui.generated.resources.flag_de
import tayra_languages.core.ui.generated.resources.flag_dk
import tayra_languages.core.ui.generated.resources.flag_ee
import tayra_languages.core.ui.generated.resources.flag_es
import tayra_languages.core.ui.generated.resources.flag_es_ct
import tayra_languages.core.ui.generated.resources.flag_es_ga
import tayra_languages.core.ui.generated.resources.flag_es_pv
import tayra_languages.core.ui.generated.resources.flag_et
import tayra_languages.core.ui.generated.resources.flag_fi
import tayra_languages.core.ui.generated.resources.flag_fo
import tayra_languages.core.ui.generated.resources.flag_fr
import tayra_languages.core.ui.generated.resources.flag_gb
import tayra_languages.core.ui.generated.resources.flag_gb_wls
import tayra_languages.core.ui.generated.resources.flag_ge
import tayra_languages.core.ui.generated.resources.flag_gr
import tayra_languages.core.ui.generated.resources.flag_hr
import tayra_languages.core.ui.generated.resources.flag_hu
import tayra_languages.core.ui.generated.resources.flag_id
import tayra_languages.core.ui.generated.resources.flag_il
import tayra_languages.core.ui.generated.resources.flag_in
import tayra_languages.core.ui.generated.resources.flag_ir
import tayra_languages.core.ui.generated.resources.flag_is
import tayra_languages.core.ui.generated.resources.flag_it
import tayra_languages.core.ui.generated.resources.flag_jp
import tayra_languages.core.ui.generated.resources.flag_kh
import tayra_languages.core.ui.generated.resources.flag_kr
import tayra_languages.core.ui.generated.resources.flag_kz
import tayra_languages.core.ui.generated.resources.flag_lt
import tayra_languages.core.ui.generated.resources.flag_lv
import tayra_languages.core.ui.generated.resources.flag_mk
import tayra_languages.core.ui.generated.resources.flag_mx
import tayra_languages.core.ui.generated.resources.flag_nl
import tayra_languages.core.ui.generated.resources.flag_no
import tayra_languages.core.ui.generated.resources.flag_ph
import tayra_languages.core.ui.generated.resources.flag_pl
import tayra_languages.core.ui.generated.resources.flag_pt
import tayra_languages.core.ui.generated.resources.flag_ro
import tayra_languages.core.ui.generated.resources.flag_rs
import tayra_languages.core.ui.generated.resources.flag_ru
import tayra_languages.core.ui.generated.resources.flag_sa
import tayra_languages.core.ui.generated.resources.flag_se
import tayra_languages.core.ui.generated.resources.flag_si
import tayra_languages.core.ui.generated.resources.flag_sk
import tayra_languages.core.ui.generated.resources.flag_th
import tayra_languages.core.ui.generated.resources.flag_tr
import tayra_languages.core.ui.generated.resources.flag_tz
import tayra_languages.core.ui.generated.resources.flag_ua
import tayra_languages.core.ui.generated.resources.flag_us
import tayra_languages.core.ui.generated.resources.flag_va
import tayra_languages.core.ui.generated.resources.flag_vn
import tayra_languages.core.ui.generated.resources.flag_za

/**
 * The flag shown for a language: the country most associated with it, a region where the
 * language is that region's own (Catalonia, Galicia, the Basque Country, Wales), or the Vatican
 * for Latin. A language with two major countries (English, Portuguese, Spanish, French, Dutch,
 * Arabic, Swahili) shows both, split along the diagonal. Languages of no country (Esperanto, Toki
 * Pona, Gothic, Tibetan) have none. The flags
 * are PNGs rendered from flag-icons (MIT, licence in composeResources/files/licenses).
 */
private val flags: Map<String, List<DrawableResource>> by lazy {
    mapOf(
        "af" to listOf(Res.drawable.flag_za),
        "ain" to listOf(Res.drawable.flag_jp),
        "sq" to listOf(Res.drawable.flag_al),
        "am" to listOf(Res.drawable.flag_et),
        "ar" to listOf(Res.drawable.flag_sa, Res.drawable.flag_eg),
        "hy" to listOf(Res.drawable.flag_am),
        "az" to listOf(Res.drawable.flag_az),
        "eu" to listOf(Res.drawable.flag_es_pv),
        "be" to listOf(Res.drawable.flag_by),
        "bn" to listOf(Res.drawable.flag_bd),
        "bs" to listOf(Res.drawable.flag_ba),
        "br" to listOf(Res.drawable.flag_fr),
        "bg" to listOf(Res.drawable.flag_bg),
        "ca" to listOf(Res.drawable.flag_es_ct),
        "ceb" to listOf(Res.drawable.flag_ph),
        "zh" to listOf(Res.drawable.flag_cn),
        "hr" to listOf(Res.drawable.flag_hr),
        "cs" to listOf(Res.drawable.flag_cz),
        "da" to listOf(Res.drawable.flag_dk),
        "nl" to listOf(Res.drawable.flag_nl, Res.drawable.flag_be),
        "en" to listOf(Res.drawable.flag_gb, Res.drawable.flag_us),
        "et" to listOf(Res.drawable.flag_ee),
        "fo" to listOf(Res.drawable.flag_fo),
        "fa" to listOf(Res.drawable.flag_ir),
        "fi" to listOf(Res.drawable.flag_fi),
        "fr" to listOf(Res.drawable.flag_fr, Res.drawable.flag_ca),
        "gl" to listOf(Res.drawable.flag_es_ga),
        "ka" to listOf(Res.drawable.flag_ge),
        "de" to listOf(Res.drawable.flag_de),
        "el" to listOf(Res.drawable.flag_gr),
        "he" to listOf(Res.drawable.flag_il),
        "hi" to listOf(Res.drawable.flag_in),
        "hu" to listOf(Res.drawable.flag_hu),
        "is" to listOf(Res.drawable.flag_is),
        "id" to listOf(Res.drawable.flag_id),
        "it" to listOf(Res.drawable.flag_it),
        "ja" to listOf(Res.drawable.flag_jp),
        "kk" to listOf(Res.drawable.flag_kz),
        "km" to listOf(Res.drawable.flag_kh),
        "ko" to listOf(Res.drawable.flag_kr),
        "la" to listOf(Res.drawable.flag_va),
        "lv" to listOf(Res.drawable.flag_lv),
        "lt" to listOf(Res.drawable.flag_lt),
        "mk" to listOf(Res.drawable.flag_mk),
        "nah" to listOf(Res.drawable.flag_mx),
        "nv" to listOf(Res.drawable.flag_us),
        "no" to listOf(Res.drawable.flag_no),
        "ryu" to listOf(Res.drawable.flag_jp),
        "pl" to listOf(Res.drawable.flag_pl),
        "pt" to listOf(Res.drawable.flag_pt, Res.drawable.flag_br),
        "pa" to listOf(Res.drawable.flag_in),
        "ro" to listOf(Res.drawable.flag_ro),
        "ru" to listOf(Res.drawable.flag_ru),
        "sa" to listOf(Res.drawable.flag_in),
        "sr" to listOf(Res.drawable.flag_rs),
        "sk" to listOf(Res.drawable.flag_sk),
        "sl" to listOf(Res.drawable.flag_si),
        "es" to listOf(Res.drawable.flag_es, Res.drawable.flag_mx),
        "sw" to listOf(Res.drawable.flag_tz, Res.drawable.flag_ke),
        "sv" to listOf(Res.drawable.flag_se),
        "th" to listOf(Res.drawable.flag_th),
        "tr" to listOf(Res.drawable.flag_tr),
        "uk" to listOf(Res.drawable.flag_ua),
        "vi" to listOf(Res.drawable.flag_vn),
        "cy" to listOf(Res.drawable.flag_gb_wls),
    )
}

/** The language's flag at [height] (4:3), two flags split along the diagonal, or its letter badge when it has no flag. */
@Composable
fun LanguageFlag(languageName: String, height: Dp, modifier: Modifier = Modifier) {
    val found = LanguageCodes.codeFor(languageName)?.let { flags[it] }
    val width = height * 4 / 3
    if (found == null) {
        // As wide as a flag, so names beside it line up with the others.
        Box(modifier.size(width = width, height = height), contentAlignment = Alignment.Center) { LanguageCodeBadge(languageName, height) }
        return
    }
    val shape = RoundedCornerShape(3.dp)
    // A hairline keeps white edges (Japan, Poland) visible against light backgrounds.
    Box(modifier.size(width = width, height = height).clip(shape).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)) {
        Image(painterResource(found[0]), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        found.getOrNull(1)?.let { second ->
            Image(painterResource(second), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().clip(LowerRightTriangle))
            // A thin light line marks where one flag ends and the other begins.
            Canvas(Modifier.matchParentSize()) {
                drawLine(Color.White.copy(alpha = 0.9f), Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = 1.2.dp.toPx())
            }
        }
    }
}

/** The half below the diagonal from the top-right corner to the bottom-left one. */
private val LowerRightTriangle = GenericShape { size, _ ->
    moveTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}
