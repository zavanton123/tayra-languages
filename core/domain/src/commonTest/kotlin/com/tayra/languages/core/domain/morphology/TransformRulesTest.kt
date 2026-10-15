package com.tayra.languages.core.domain.morphology

import com.tayra.languages.core.domain.morphology.rules.*
import kotlin.test.Test

/** Each ported rule set against Yomitan's own tests of it. */
class TransformRulesTest {
    @Test fun english() = checkTransformCases(enTransforms, enTransformCases)
    @Test fun german() = checkTransformCases(deTransforms, deTransformCases)
    @Test fun spanish() = checkTransformCases(esTransforms, esTransformCases)
    @Test fun greek() = checkTransformCases(elTransforms, elTransformCases)
    @Test fun latin() = checkTransformCases(laTransforms, laTransformCases)
    @Test fun ukrainian() = checkTransformCases(ukTransforms, ukTransformCases)
    @Test fun albanian() = checkTransformCases(sqTransforms, sqTransformCases)
    @Test fun basque() = checkDeinflections(euTransforms, "etxean" to "etxe", "mendira" to "mendi")
    @Test fun esperanto() = checkTransformCases(eoTransforms, eoTransformCases)
    @Test fun georgian() = checkDeinflections(kaTransforms, "სახლში" to "სახლ", "წიგნები" to "წიგნი")
    @Test fun irish() = checkTransformCases(gaTransforms, gaTransformCases)
    @Test fun tagalog() = checkTransformCases(tlTransforms, tlTransformCases)
    @Test fun oldIrish() = checkDeinflections(sgaTransforms, "mbó" to "bó", "thech" to "tech")
    @Test fun ancientGreek() = checkTransformCases(grcTransforms, grcTransformCases)
    @Test fun arabic() = checkTransformCases(arTransforms, arTransformCases)
    @Test fun yiddish() = checkTransformCases(yiTransforms, yiTransformCases)
    @Test fun japanese() = checkTransformCases(jaTransforms, jaTransformCases)
    @Test fun korean() = checkTransformCases(koTransforms, koTransformCases) { Hangul.disassemble(it) }
}
