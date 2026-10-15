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
}
