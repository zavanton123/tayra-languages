# Tayra Languages

Learn languages by reading. Tayra Languages is a Kotlin Multiplatform port of
[Lute](https://github.com/LuteOrg/lute-v3) (Learning Using Texts) for Android, iOS,
desktop (JVM) and the web (Wasm), with a shared Compose Multiplatform UI.

Import a text, read it page by page, click words to define them and track what you know.
Terms are highlighted by learning status, multi-word expressions are recognised, parents
and tags group related terms, and reading statistics track your progress.

## Stack

| Concern | Library |
| --- | --- |
| UI | Compose Multiplatform, Material 3 |
| Presentation | JetBrains `lifecycle-viewmodel`, `StateFlow` |
| Navigation | JetBrains `navigation-compose` (type-safe routes) |
| Dependency injection | Koin |
| Database | SQLDelight (Android/JVM/native drivers, sql.js web worker on Wasm) |
| Preferences | multiplatform-settings |
| Networking | Ktor client with HTTP cache (web page import) |
| Serialization | kotlinx.serialization |
| Images | Coil 3 |
| Files | FileKit (import txt/epub/srt/vtt, CSV export) |
| Logging | Kermit |
| Date/time | kotlinx-datetime |
| Testing | kotlin-test, kotlinx-coroutines-test, Turbine |

## Modules

```
core/domain      pure Kotlin: models, text parsers, page rendering, services, predefined languages
core/data        SQLDelight database, repositories, settings, HTTP and file import
core/ui          theme, navigation routes, shared composables
feature/books    book listing, create/edit, bookmarks, page editing
feature/reading  reading screen, term popups, keyboard shortcuts
feature/terms    term form, term listing, bulk edit, tags, CSV import/export
feature/languages, feature/settings, feature/stats
shared           app composition: DI, bootstrap, navigation graph, iOS framework
androidApp, desktopApp, webApp, iosApp   thin platform launchers
build-logic      Gradle convention plugins
```

Domain services depend on repository interfaces declared in `core/domain`; `core/data`
provides the SQLDelight implementations. Each feature exposes a Koin module and a
navigation graph that `shared` composes into the app.

## Building and running

```
./gradlew :desktopApp:run                      # desktop
./gradlew :androidApp:installDebug             # android
./gradlew :webApp:wasmJsBrowserDevelopmentRun  # web
open iosApp/iosApp.xcodeproj                   # ios (builds the Shared framework via Gradle)
```

Tests:

```
./gradlew :core:domain:jvmTest :core:data:jvmTest
./gradlew :core:domain:wasmJsBrowserTest       # parser tests in a headless browser
```

## Notes

- On first start the database is seeded with a tutorial and sample languages from the
  [Lute language definitions](https://github.com/LuteOrg/lute-language-defs); regenerate
  `PredefinedLanguages.kt` with `tools/generate_language_defs.py`.
- Languages that need external tokenisers (Japanese via MeCab, Thai, Khmer, Mandarin) are
  listed but not supported yet.
- Dictionaries open in the platform browser; embedded web views are not used.
- The web build keeps its database in memory for the session; audio playback, backups
  and Anki export from Lute are not ported yet.
