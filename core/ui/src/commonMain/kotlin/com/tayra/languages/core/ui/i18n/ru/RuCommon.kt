package com.tayra.languages.core.ui.i18n.ru

/** Shared words, the language names, and the app's frame: top bar, components, themes and the platform launchers. */
internal val ruCommon: Map<String, String> = mapOf(
    // Language names, shown through tr(name).
    "Belarusian" to "Белорусский",
    "Bulgarian" to "Болгарский",
    "Catalan" to "Каталанский",
    "Croatian" to "Хорватский",
    "Czech" to "Чешский",
    "Danish" to "Датский",
    "Dutch" to "Нидерландский",
    "English" to "Английский",
    "Estonian" to "Эстонский",
    "Finnish" to "Финский",
    "French" to "Французский",
    "Galician" to "Галисийский",
    "German" to "Немецкий",
    "Greek" to "Греческий",
    "Hungarian" to "Венгерский",
    "Icelandic" to "Исландский",
    "Italian" to "Итальянский",
    "Latin" to "Латинский",
    "Latvian" to "Латышский",
    "Lithuanian" to "Литовский",
    "Macedonian" to "Македонский",
    "Norwegian" to "Норвежский",
    "Polish" to "Польский",
    "Portuguese" to "Португальский",
    "Portuguese (Brazil)" to "Португальский (Бразилия)",
    "Romanian" to "Румынский",
    "Russian" to "Русский",
    "Serbian" to "Сербский",
    "Slovak" to "Словацкий",
    "Slovene" to "Словенский",
    "Spanish" to "Испанский",
    "Swedish" to "Шведский",
    "Turkish" to "Турецкий",
    "Ukrainian" to "Украинский",

    // App.kt
    "Could not start: {0}" to "Не удалось запустить: {0}",

    // AppTopBar.kt (the tab and menu labels are shown through tr(label))
    "Home" to "Главная",
    "Books" to "Книги",
    "Courses" to "Курсы",
    "Vocabulary" to "Слова",
    "Flashcards" to "Карточки",
    "Settings" to "Настройки",
    "About" to "О программе",
    "All books" to "Все книги",
    "Create new book" to "Новая книга",
    "Book archive" to "Архив книг",
    "Word frequency" to "Частотность слов",
    "Review flashcards" to "Повторить карточки",
    "Flashcard settings" to "Настройки карточек",
    "Languages" to "Языки",
    "Translation" to "Перевод",
    "Dictionaries" to "Словари",
    "Speech" to "Озвучивание",
    "Keyboard shortcuts" to "Сочетания клавиш",
    "Backups" to "Резервные копии",
    "Statistics" to "Статистика",
    "Back" to "Назад",
    "Go to Home" to "На главную",
    "{0} flashcards due" to "{0} карточка к повторению|{0} карточки к повторению|{0} карточек к повторению",
    "Menu" to "Меню",

    // LearningLanguage.kt
    "Learning language: {0}" to "Изучаемый язык: {0}",
    "Selected" to "Выбрано",

    // TagInput.kt
    "Remove {0}" to "Убрать «{0}»",

    // ContentCards.kt (the package tabs are shown through tr(label))
    "Decrease {0}" to "Уменьшить {0}",
    "Increase {0}" to "Увеличить {0}",
    "{0} GB" to "{0} ГБ",
    "{0} MB" to "{0} МБ",
    "{0} kB" to "{0} КБ",
    "{0} B" to "{0} Б",
    "Installed" to "Установлено",
    "Available" to "Доступно",

    // Dialogs.kt
    "OK" to "ОК",
    "Cancel" to "Отмена",
    "Save" to "Сохранить",

    // StatusBar.kt (the scopes are shown through tr(scope))
    "Vocabulary {0}" to "Слова {0}",
    "in this book" to "в этой книге",
    "on this page" to "на этой странице",
    "Unknown" to "Незнакомое",
    "Learning" to "Изучаю",
    "Known" to "Знаю",
    "{0} words" to "{0} слово|{0} слова|{0} слов",
    "{0} words in total" to "Всего {0} слово|Всего {0} слова|Всего {0} слов",

    // Formatting.kt
    "just now" to "только что",
    "{0} years ago" to "{0} год назад|{0} года назад|{0} лет назад",
    "{0} months ago" to "{0} месяц назад|{0} месяца назад|{0} месяцев назад",
    "{0} weeks ago" to "{0} неделю назад|{0} недели назад|{0} недель назад",
    "{0} days ago" to "{0} день назад|{0} дня назад|{0} дней назад",
    "{0} hours ago" to "{0} час назад|{0} часа назад|{0} часов назад",
    "{0} minutes ago" to "{0} минуту назад|{0} минуты назад|{0} минут назад",

    // AudioPlayer.kt, SpeechSynthesizer.kt
    "Stop recording" to "Остановить запись",
    "Play recording" to "Воспроизвести запись",
    "Stop" to "Остановить",
    "Pronounce" to "Произнести",

    // AppTheme.kt, ReadingFonts.kt (shown through label, which is tr(englishLabel))
    "Default" to "Стандартная",
    "Sepia" to "Сепия",
    "Dark slate" to "Графитовая",
    "Night" to "Ночная",
    "System serif" to "Системный с засечками",
    "System sans-serif" to "Системный без засечек",
    "Monospace" to "Моноширинный",

    // OnDeviceTranslatorBridge.kt (iOS)
    "Google ML Kit translates on this device with no network once a language's model is downloaded. Models come from Google and stay on the device." to "Google ML Kit переводит на этом устройстве без сети, когда модель языка скачана. Модели загружаются из Google и остаются на устройстве.",

    // LanguagesScreen.kt
    "Choose what you learn, how translations appear, and the language of the app." to "Выберите, что изучать, на какой язык переводить и язык приложения.",
    "Language setup" to "Настройка языков",
    "I'm learning" to "Изучаемый язык",
    "Used for books, courses, vocabulary and flashcards." to "Для книг, курсов, слов и карточек.",
    "Also shown in the header" to "Также показан в верхней панели",
    "Studying" to "Изучение",
    "Translations" to "Переводы",
    "Show meanings in" to "Переводить на",
    "Used for translations, definitions and example sentences." to "Для переводов, определений и примеров.",
    "App" to "Приложение",
    "Interface language" to "Язык интерфейса",
    "Used for menus, buttons and messages." to "Для меню, кнопок и сообщений.",
    "More interface languages will appear as Tayra is translated." to "Другие языки интерфейса появятся по мере перевода Tayra.",
    "Changes saved" to "Изменения сохранены",
    "Learn {0} with {1} translations" to "Учите {0} с переводом на {1}",
    "Meanings in {0}" to "Значения на {0} языке",
    "with translations in" to "с переводом на",
    "Your setup" to "Ваш выбор",
    "App interface: {0}" to "Интерфейс: {0}",

    // ManageDictionariesScreen.kt, ManageDictionariesViewModel.kt
    "{0} · changes apply immediately" to "{0} · изменения применяются сразу",
    "Preferred" to "Предпочтительные",
    "No dictionaries enabled yet." to "Пока нет включённых словарей.",
    "Disable" to "Отключить",
    "All resources" to "Все ресурсы",
    "Enable" to "Включить",
    "Keep at least one dictionary enabled." to "Оставьте включённым хотя бы один словарь.",

    // StatsScreen.kt
    "No reading recorded yet. Mark pages as read to build statistics." to "Чтение пока не записано. Отмечайте страницы прочитанными, чтобы собрать статистику.",
    "Reading streak: {0} days" to "Серия чтения: {0} день|Серия чтения: {0} дня|Серия чтения: {0} дней",
    "Words read" to "Прочитано слов",
    "Language" to "Язык",
    "Today" to "Сегодня",
    "Week" to "Неделя",
    "Month" to "Месяц",
    "Year" to "Год",
    "Total" to "Всего",
    "Cumulative words read" to "Прочитано слов нарастающим итогом",
)
