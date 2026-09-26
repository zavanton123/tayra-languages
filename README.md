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
| Networking | Ktor client with HTTP cache (web page import, Wiktionary/MyMemory translations, Tatoeba examples) |
| Serialization | kotlinx.serialization |
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
feature/terms    term form, term listing, bulk edit, CSV import/export
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

## Releases

Publishing a GitHub release runs `.github/workflows/release.yml`, which builds a release APK
on Linux and a macOS DMG on an Apple Silicon runner and attaches both to the release. The
workflow can also be started by hand from the Actions tab for an existing tag.

- The tag gives the version: `v0.1.0` produces `TayraLanguages-0.1.0.apk` with the workflow
  run number as the Android version code. macOS installers need a major version of at least
  1, so tags below `1.0.0` are packaged as `1.0.0` inside the DMG; the file name keeps the
  tag version.
- To sign the APK with a release key, add the repository secrets `ANDROID_KEYSTORE_BASE64`
  (the keystore file encoded with `base64`), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`
  and `ANDROID_KEY_PASSWORD`. Without them the APK is signed with the debug key, which
  installs fine but cannot update an app signed with another key.
- The DMG is neither signed nor notarized, so macOS asks for confirmation on first launch
  (right-click the app and choose Open).

The same artifacts can be built locally:

```bash
./gradlew :androidApp:assembleRelease -PreleaseVersion=0.1.0 -PversionCode=1
./gradlew :desktopApp:packageDmg -PreleaseVersion=0.1.0
```

## Notes

- The languages on offer are fixed: English, German and Portuguese can be learned, and
  translations are shown in English or Russian (Settings, Native language). The three
  target languages are created on first start with sample texts and the tutorial, taken
  from the [Lute language definitions](https://github.com/LuteOrg/lute-language-defs);
  regenerate `PredefinedLanguages.kt` with `tools/generate_language_defs.py`. Their
  dictionaries and text settings can be edited, but languages cannot be created, renamed or
  deleted; any other language found in the database is removed on start together with its
  books and terms.
- The bundled language definitions also cover many other languages; those that need external
  tokenisers (Japanese via MeCab, Thai, Khmer, Mandarin) are not supported.
- Dictionaries open in the platform browser; embedded web views are not used.
- Offline dictionaries are downloadable packs: gzip-compressed prebuilt SQLite files (tables
  `meta`, `entries` and `forms`) listed with their download URLs in `DictionaryPacks`. Settings
  shows every pack with Download and Remove. Android, iOS and desktop unpack a downloaded pack
  into app storage next to a stamp with its format and open it read-only as a second SQLDelight
  database; the web build keeps the compressed file in the browser Cache API and inflates it in
  its own sql.js worker (`webApp/src/wasmJsMain/resources/dictionary.worker.js`), which needs
  the pack host to allow cross-origin requests. When a text's language and the native language
  match an installed pack, the term pane fills the translation from it, lists every meaning
  with a plus button, and links an inflected form to its lemma as the term's parent; without a
  pack the network suggestions are used. `tools/build_dictionary.py --source en` (or
  `--source de`) builds `dictionaries/<source>-ru.sqlite.gzip` from two kaikki.org dumps kept in
  the ignored `dictionaries/` folder: that language's section of the Russian Wiktionary
  (`english-to-russian.jsonl`, `german-to-russian.jsonl`; the glosses) and the English
  Wiktionary's dump for the language (`kaikki.org-dictionary-English.jsonl.gz`,
  `kaikki.org-dictionary-German.jsonl.gz`; the inflection tables). Compound forms such as
  "würde gehen" are dropped because the app looks up single tokens; for English, forms neither
  dump lists are generated by rule and flagged `generated`. Publish the file as a release asset
  and point the pack's URL at it; if the layout changes, bump `FORMAT` in the script and in
  `DictionaryId` so older downloads count as not installed.
- Term pronunciation uses the platform text-to-speech engine: Android `TextToSpeech`,
  `AVSpeechSynthesizer` on iOS, the Web Speech API in the browser, and the operating
  system's speech command on desktop (`say` on macOS, System.Speech via PowerShell on
  Windows, `spd-say` on Linux). Voices for a language must be installed on the device.
- Example sentence recordings from Tatoeba play on every platform.
- The web build keeps its database in memory for the session; book audio, backups and
  Anki export from Lute are not ported yet.
