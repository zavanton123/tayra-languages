package com.tayra.languages.core.domain.morphology

import com.tayra.languages.core.domain.morphology.rules.*

/** The inflection rules exported from Yomitan (rules/<Code>Transforms.kt), by Yomitan's language code. */
internal val transformRules: Map<String, () -> List<String>> = mapOf(
    "de" to { deTransforms },
    "el" to { elTransforms },
    "en" to { enTransforms },
    "eo" to { eoTransforms },
    "es" to { esTransforms },
    "eu" to { euTransforms },
    "fr" to { frTransforms },
    "ga" to { gaTransforms },
    "ka" to { kaTransforms },
    "la" to { laTransforms },
    "sq" to { sqTransforms },
    "tl" to { tlTransforms },
    "uk" to { ukTransforms },
)
