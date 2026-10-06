# Development notes

## Stack

| Concern | Library |
| --- | --- |
| UI | Compose Multiplatform, Material 3 |
| Presentation | JetBrains `lifecycle-viewmodel`, `StateFlow` |
| Navigation | JetBrains `navigation-compose` (type-safe routes) |
| Dependency injection | Koin |
| Database | SQLDelight (Android/JVM/native drivers, sql.js web worker on Wasm) |
| Preferences | multiplatform-settings; secrets through `SecureStore` |
| Networking | Ktor client with HTTP cache |
| Serialization | kotlinx.serialization |
| Files | FileKit (import txt/epub/srt/vtt, CSV export) |
| Logging | Kermit |
| Date/time | kotlinx-datetime |
| Testing | kotlin-test, kotlinx-coroutines-test, Turbine |

## Modules

```
core/domain      pure Kotlin: models, text parsers, page rendering, services, predefined languages
core/data        SQLDelight database, repositories, settings, secure storage, HTTP, translation providers
core/ui          theme, navigation routes, shared composables
feature/books    book listing, create/edit, bookmarks, page editing
feature/reading  reading screen, term popups, keyboard shortcuts
feature/terms    term form, term listing, bulk edit, CSV and Anki export
feature/languages, feature/settings, feature/stats
shared           app composition: DI, bootstrap, navigation graph, iOS framework
androidApp, desktopApp, webApp, iosApp   thin platform launchers
build-logic      Gradle convention plugins
```

Domain services depend on repository interfaces declared in `core/domain`; `core/data`
provides the SQLDelight implementations. Each feature exposes a Koin module and a navigation
graph that `shared` composes into the app.

## Building and testing

```
./gradlew :desktopApp:run
./gradlew :androidApp:installDebug
./gradlew :webApp:wasmJsBrowserDevelopmentRun
./gradlew :core:domain:jvmTest :core:data:jvmTest :feature:reading:jvmTest
./gradlew :core:domain:wasmJsBrowserTest       # parser tests in a headless browser
```

iOS: ML Kit ships only as CocoaPods, so run `pod install` in `iosApp` and build from
`iosApp/iosApp.xcworkspace`. New Swift files must be added to `project.pbxproj`. ML Kit's
binaries have no arm64 simulator slice; on Apple Silicon, `xcodebuild` builds for devices
with `-destination 'generic/platform=iOS'`, while simulator runs would need an x86_64 Kotlin
target under Rosetta.

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
- The DMG is neither signed nor notarized, so macOS asks for confirmation on first launch.

Locally:

```bash
./gradlew :androidApp:assembleRelease -PreleaseVersion=0.1.0 -PversionCode=1
./gradlew :desktopApp:packageDmg -PreleaseVersion=0.1.0
```

## Languages

The catalog is fixed: `LanguageCatalog` lists the 33 learnable languages and the six native
languages. The predefined language definitions (parsers, dictionaries, sample texts) are
generated into `PredefinedLanguages.kt` by `tools/generate_language_defs.py`. Languages that
need external tokenisers (Japanese, Thai, Khmer, Mandarin) are not supported. Any language
outside the catalog found in the database is removed on start together with its books and terms.

## Translation engines

`TranslationEngine` names the engines; `UserSettings.effectiveEngine()` falls back to MyMemory
when the chosen engine lacks credentials or a local runtime. `RoutingSentenceTranslator`
dispatches sentence translations, `TranslationSuggestionProvider` term suggestions, and
`CachedSentenceTranslator` caches results for a day per sentence and engine.

- Google: Cloud Translation v2 with an API key.
- On device: `LocalSentenceTranslator`. Desktop uses Argos Translate through a Python worker
  (`core/data/src/jvmMain/resources/argos_worker.py`); the app downloads a standalone CPython
  build into its data folder and pip-installs `argostranslate` on first use. Android uses ML Kit
  directly; iOS uses ML Kit through the Swift `MlKitTranslatorBridge`, which implements the
  Kotlin `OnDeviceTranslatorBridge` and is handed to `MainViewController`. Both phone
  implementations share `LanguageModelTranslator`.

Secrets go through `SecureStore`: macOS Keychain (Security framework via JNA), iOS Keychain,
Android Keystore (AES-GCM), Windows DPAPI, the Linux keyring via `secret-tool` (or an owner-only
file), and sessionStorage in the browser.

## Speech engines

`SpeechEngine` names the engines and `LocalSpeechEngine` is the contract for the ones that
synthesize audio themselves: packages to download, voices per language, and
`synthesize()` returning WAV bytes. `Speaker` in core/ui routes a request to the chosen engine
and plays the result with `WavPlayer`, or hands it to the system `SpeechSynthesizer` when the
engine has no voice for the language or fails.

- Desktop: `PiperSpeechEngine` and `KokoroSpeechEngine` run in the managed Python
  (`ManagedPython`, shared with Argos) through `tts_worker.py`, with the pip packages
  `piper-tts`, `kokoro-onnx` and `soundfile`. Piper voices come from the rhasspy/piper-voices
  catalog (`PiperCatalog`); Kokoro's model and voices from the kokoro-onnx release files.
- Android and iOS: `SherpaPiperEngine` and `SherpaKokoroEngine` (core/data commonMain) run in
  sherpa-onnx through a `SherpaRuntime`. Models are the `.tar.bz2` archives of the sherpa-onnx
  `tts-models` release; the list of packaged Piper voices is generated into
  `SherpaPiperVoices.kt` by `tools/generate_sherpa_voices.py`.
  - Android: `AndroidSherpaRuntime` uses sherpa-onnx's Java API, whose library is resolved from
    its GitHub release through an Ivy repository in `settings.gradle.kts`.
  - iOS: `IosSherpaRuntime` (shared iosMain) calls the Kotlin interface `OnDeviceSpeechBridge`,
    implemented in Swift by `SherpaSpeechBridge` over `iosApp/iosApp/SherpaSpeech.swift`, which
    uses the C API of the prebuilt `SherpaOnnxC.framework` (fetched by `iosApp/SherpaOnnxC.podspec`)
    and the system bzip2 through the bridging header. `SherpaSpeech.swift` has no dependency on
    the app, so it can be compiled against sherpa-onnx's macOS library and run on a Mac.
- Web binds no local engines; kokoro-js exists for the browser, should one be added.

## Offline dictionary packs

Packs are gzip-compressed SQLite files (tables `meta`, `entries`, `forms`), one per source
language and gloss language, listed in `DictionaryPacks` and published as GitHub release
assets. Pairs the source Wiktionaries lack are excluded there.

`tools/build_dictionary.py --source <code> [--target ru|en|de|fr|es|pt]` builds a pack from
kaikki.org dumps kept in the ignored `dictionaries/` folder: the gloss language's Wiktionary
section for the source language (`<language>-to-russian.jsonl` and so on) and the English
Wiktionary dump for the inflection tables (`kaikki.org-dictionary-<Language>.jsonl.gz`). With
`--target en` both come from the English dump. Compound forms are dropped because the app looks
up single tokens; Finnish and Hungarian possessive variants are left out for size; English
forms missing from both dumps are generated by rule and flagged `generated`. If the layout
changes, bump `FORMAT` in the script and in `DictionaryId` so older downloads count as not
installed.

Android, iOS and desktop unpack a downloaded pack into app storage and open it read-only as a
second SQLDelight database; the web build keeps the compressed file in the browser Cache API and
inflates it in its own sql.js worker, which needs the pack host to allow cross-origin requests.

## Word frequency lists

The Word frequency screen (feature/frequency) shows the most common words of the language being
learned, bundled as `feature/frequency/src/commonMain/composeResources/files/frequency/<code>.tsv`
for every language in `LanguageCatalog.targetLanguages`: one dictionary word per line, most common
first, written as the dictionary writes it ("Haus"), with up to 40 lowercase forms counted for it
after a tab. Lists have 10,000 words, except Latvian (about 6,600) and Belarusian (about 3,800),
whose dictionaries and lemmatizer coverage are small.

`tools/build_frequency_list.py --language <code>|all` makes them; it needs `pip install wordfreq
simplemma` and the `dictionaries/<code>-en.sqlite` packs. Counts come from wordfreq; Croatian,
Serbian, Estonian and Galician from FrequencyWords (OpenSubtitles); Belarusian and Latin from the
Leipzig Corpora Collection. A language for which wordfreq keeps only its short list is blended with
the other sources. Downloads are cached in `dictionaries/frequency-sources`.

Forms are grouped under their dictionary words with the pack's forms table, then simplemma for
forms the pack lacks. Both are noisy (pronoun tables list every pronoun as a form of the others,
aspect partners and look-alike nouns appear as forms), so each language's top 600 words were read
and corrected in `tools/frequency_fixes.py`. Change the general rules in the script with care:
those corrections were tested against them, so rebuild all lists and compare the top of each.

A word's status on the screen is its own term's, or else the furthest status among its forms the
reader has saved.

The vocabulary level (a band's "I know up to here", or Vocabulary settings) is stored per language
in `vocabulary_levels`. Setting it saves every word ranked up to it, and its forms linked to it as
parent, as known terms, except text the language already has a term for; the terms it saved are
listed in `level_terms`, so a lower level deletes those still known and leaves any the reader
changed since.

## Platform notes

- Term pronunciation uses the platform speech engine: Android `TextToSpeech`,
  `AVSpeechSynthesizer` on iOS, the Web Speech API, and the OS speech command on desktop
  (`say`, PowerShell System.Speech, `spd-say`).
- Dictionaries open in the platform browser; embedded web views are not used.
- The web build keeps its database in memory for the session.
