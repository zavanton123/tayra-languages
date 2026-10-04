package com.tayra.languages.feature.settings

import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.language.LanguageOption
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The language being learned, chosen in the top bar, as the settings screens name languages;
 * their lists of voices, models and dictionaries lead with it. Null until one is chosen.
 */
internal fun learningLanguage(languages: LanguageRepository, settings: SettingsRepository): Flow<LanguageOption?> =
    combine(languages.observeAll(), settings.settings.map { it.currentLanguageId }.distinctUntilChanged()) { all, id ->
        all.firstOrNull { it.id == id }?.let { language -> LanguageCodes.codeFor(language.name)?.let { LanguageOption(it, language.name) } }
    }
