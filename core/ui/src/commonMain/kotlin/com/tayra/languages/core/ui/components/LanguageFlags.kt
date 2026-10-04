package com.tayra.languages.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tayra.languages.core.domain.language.LanguageCodes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import tayra_languages.core.ui.generated.resources.Res
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
 * for Latin. Languages of no country (Esperanto, Toki Pona, Gothic, Tibetan) have none. The flags
 * are PNGs rendered from flag-icons (MIT, licence in composeResources/files/licenses).
 */
private val flags: Map<String, DrawableResource> by lazy {
    mapOf(
        "af" to Res.drawable.flag_za,
        "ain" to Res.drawable.flag_jp,
        "sq" to Res.drawable.flag_al,
        "am" to Res.drawable.flag_et,
        "ar" to Res.drawable.flag_sa,
        "hy" to Res.drawable.flag_am,
        "az" to Res.drawable.flag_az,
        "eu" to Res.drawable.flag_es_pv,
        "be" to Res.drawable.flag_by,
        "bn" to Res.drawable.flag_bd,
        "bs" to Res.drawable.flag_ba,
        "br" to Res.drawable.flag_fr,
        "bg" to Res.drawable.flag_bg,
        "ca" to Res.drawable.flag_es_ct,
        "ceb" to Res.drawable.flag_ph,
        "zh" to Res.drawable.flag_cn,
        "hr" to Res.drawable.flag_hr,
        "cs" to Res.drawable.flag_cz,
        "da" to Res.drawable.flag_dk,
        "nl" to Res.drawable.flag_nl,
        "en" to Res.drawable.flag_gb,
        "et" to Res.drawable.flag_ee,
        "fo" to Res.drawable.flag_fo,
        "fa" to Res.drawable.flag_ir,
        "fi" to Res.drawable.flag_fi,
        "fr" to Res.drawable.flag_fr,
        "gl" to Res.drawable.flag_es_ga,
        "ka" to Res.drawable.flag_ge,
        "de" to Res.drawable.flag_de,
        "el" to Res.drawable.flag_gr,
        "he" to Res.drawable.flag_il,
        "hi" to Res.drawable.flag_in,
        "hu" to Res.drawable.flag_hu,
        "is" to Res.drawable.flag_is,
        "id" to Res.drawable.flag_id,
        "it" to Res.drawable.flag_it,
        "ja" to Res.drawable.flag_jp,
        "kk" to Res.drawable.flag_kz,
        "km" to Res.drawable.flag_kh,
        "ko" to Res.drawable.flag_kr,
        "la" to Res.drawable.flag_va,
        "lv" to Res.drawable.flag_lv,
        "lt" to Res.drawable.flag_lt,
        "mk" to Res.drawable.flag_mk,
        "nah" to Res.drawable.flag_mx,
        "nv" to Res.drawable.flag_us,
        "no" to Res.drawable.flag_no,
        "ryu" to Res.drawable.flag_jp,
        "pl" to Res.drawable.flag_pl,
        "pt" to Res.drawable.flag_pt,
        "pa" to Res.drawable.flag_in,
        "ro" to Res.drawable.flag_ro,
        "ru" to Res.drawable.flag_ru,
        "sa" to Res.drawable.flag_in,
        "sr" to Res.drawable.flag_rs,
        "sk" to Res.drawable.flag_sk,
        "sl" to Res.drawable.flag_si,
        "es" to Res.drawable.flag_es,
        "sw" to Res.drawable.flag_tz,
        "sv" to Res.drawable.flag_se,
        "th" to Res.drawable.flag_th,
        "tr" to Res.drawable.flag_tr,
        "uk" to Res.drawable.flag_ua,
        "vi" to Res.drawable.flag_vn,
        "cy" to Res.drawable.flag_gb_wls,
    )
}

/** The language's flag at [height] (4:3), or its letter badge when it has no flag. */
@Composable
fun LanguageFlag(languageName: String, height: Dp, modifier: Modifier = Modifier) {
    val flag = LanguageCodes.codeFor(languageName)?.let { flags[it] }
    if (flag == null) {
        // As wide as a flag, so names beside it line up with the others.
        Box(modifier.size(width = height * 4 / 3, height = height), contentAlignment = Alignment.Center) { LanguageCodeBadge(languageName, height) }
        return
    }
    val shape = RoundedCornerShape(3.dp)
    Image(
        painterResource(flag),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        // A hairline keeps white edges (Japan, Poland) visible against light backgrounds.
        modifier = modifier.size(width = height * 4 / 3, height = height).clip(shape)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    )
}
