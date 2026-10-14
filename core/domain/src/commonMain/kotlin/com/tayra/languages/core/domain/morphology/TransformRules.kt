package com.tayra.languages.core.domain.morphology

import com.tayra.languages.core.domain.morphology.rules.*

/** The inflection rules exported from Yomitan (rules/<Code>Transforms.kt), by Yomitan's language code. */
internal val transformRules: Map<String, () -> List<String>> = mapOf(
    "de" to { deTransforms },
    "en" to { enTransforms },
    "es" to { esTransforms },
    "fr" to { frTransforms },
)
