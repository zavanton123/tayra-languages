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
| Files | FileKit (picking files, CSV export); `FileTextExtractor` turns epub, fb2, mobi/azw3, docx, odt, rtf, html, mhtml, md, txt, srt and vtt into text in common code, pdf through PDFBox (desktop), PDFBox-Android and PDFKit (iOS) |
| Text recognition | `TextRecognizer` (domain): `VisionTextRecognizer` (iOS), `AndroidTextRecognizer` (ML Kit through Google Play services, Tesseract otherwise), `TesseractTextRecognizer` (desktop; bytedeco's JavaCPP builds, natives for the OS the installer is built on, arm64 only on Android). Models from tessdata_fast, pinned in `TesseractModels` |
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
cli              the `tayra` command line, on the same services; packaged inside desktopApp
androidApp, desktopApp, webApp, iosApp   thin platform launchers
build-logic      Gradle convention plugins
```

Domain services depend on repository interfaces declared in `core/domain`; `core/data`
provides the SQLDelight implementations. Each feature exposes a Koin module and a navigation
graph that `shared` composes into the app.

The desktop installers carry a second jpackage launcher, `tayra`, that runs `:cli`'s main
class on the app's own runtime and jars (see the end of `desktopApp/build.gradle.kts`);
`DesktopCommandLineTool` puts it on the PATH from Settings. The app holds a lock on
`app.lock` in the data folder while it is open, which tells the command so. Both open the
database in WAL mode with a busy timeout, so they can use it at the same time.

## Japanese with MeCab on desktop

On desktop, Japanese text is also cut with MeCab's analysis: `KuromojiBoundaries` (core/data jvmMain)
runs Kuromoji, a Java port of MeCab with its IPADIC dictionary, and is registered as the Japanese
`WordBoundaries` in `Segmenters`. It joins MeCab's pieces that cannot stand alone (auxiliaries,
suffixes, helper verbs, the て and たり joining them) to the word before; `SegmentingParser` then
never lets a word run past one of those words, and within one takes the longest run of pieces the
lexicon knows (選んでください is 選んで and ください). Its dictionary loads on a thread of its own
at start; until then, and on the other platforms, the lexicon alone cuts the text.

## Anki

`AnkiExportService` builds cloze notes from terms and either packs them into an .apkg file
(`AnkiPackagerImpl`, every platform) or, on desktop, adds them to the running Anki through the
AnkiConnect add-on (`AnkiConnectClient`, bound as `AnkiConnection` in the JVM data module, at
http://127.0.0.1:8765): it creates the "Tayra Languages" note type and the decks when missing, asks
`canAddNotesWithErrorDetail` which notes Anki already has (the same cloze in the deck), stores the
added notes' audio with `storeMediaFile` and adds them with `addNotes`. Terms whose notes are in
Anki afterwards count as exported either way.

## Building and testing

```
./gradlew :desktopApp:run
./gradlew :androidApp:installDebug
./gradlew :webApp:wasmJsBrowserDevelopmentRun
./gradlew :core:domain:jvmTest :core:data:jvmTest :feature:reading:jvmTest
./gradlew :core:domain:wasmJsBrowserTest       # parser tests in a headless browser
```

`TentenDeinflectTest` (core/domain) checks the Japanese rules against the 209 word pairs of 10ten
Japanese Reader's deinflection tests (GPL-3.0). The forms they need that Yomitan's rules lack are
in `rules/JaExtraTransforms.kt`, hand-kept beside the generated `JaTransforms.kt`: -ないで,
-ていらっしゃる, -なさる, -いたす, お〜になる, -得る, -ざるを得ない, Kansai -どる and the
irregular する-verbs, the masu-stem ones derived from Yomitan's -なさい rules.

`JapaneseNotes` (core/domain morphology) ports 10ten's readers of Japanese numbers (五十六, 39,800万),
modern era years (昭和56年 is 1981; the eras before 1873 need a calendar table and are left out)
and floor areas (四畳半 is 7.29 m², with the regional mat sizes); the term pane shows them for the
text at the term in its sentence, an era name just before the word included.

From the Zhongwen Chinese reader (GPL-2.0-or-later): `Chinese.syllables` cuts pinyin, spaced or
joined, into syllables by its syllable table (`Zhuyin.kt`), the cut with the fewest syllables;
`toneColored` (core/ui) colours them by tone in Zhongwen's standard colours, in the reader and the
term pane ("Tone colours" in the reader's settings); `Chinese.zhuyin` writes them in bopomofo
("Zhuyin"); and `ChineseGrammarWiki` links the words AllSet Learning's Chinese Grammar Wiki has
notes on (simplified spellings) from the term pane's dictionary links.

## Chinese scripts

A Mandarin language can be read in simplified or traditional characters (`Language.chineseScript`,
kept in the `language_scripts` table; none keeps texts as written). `ChineseConverter` converts with
OpenCC's tables (Apache-2.0, bundled in core/ui `files/opencc`, loaded by `ChineseScripts.prepare`
from `ResourceChineseScriptTables`): OpenCC's s2tw chain to traditional (Taiwan's character variants,
no regional words) and tw2s to simplified. `SegmentingParser` converts a text into the language's
script before cutting it, so its tokens, saved terms (`TermTextNormalizer`), highlighting and
lookups are all in that script; the term pane shows dictionary headwords, and the frequency list
its words, in it (`DictionaryLookup.inScript`, `FrequencyList.inScript`). The tables load at start
when a language uses a script (`ChineseScriptService.prepareIfUsed`) and before Chinese terms are
found or saved. `ChineseScriptService.setScript` changes a language's script, rewriting its saved
terms (a term saved in both becomes one, keeping the more advanced status) and remaking the
sentences of its texts. Choosing Mandarin asks which characters (`ChineseScriptPrompt`); the
reader's settings change it later.

`YomitanTranslatorTest` (core/data) runs Yomitan's own translator tests: its test dictionary and
expected results (`core/data/src/jvmTest/resources/yomitan`, copied from Yomitan's `test/data`,
GPL-3.0) are imported and looked up through the app, which must find the same words, readings and
inflections. Refresh them from a newer Yomitan by copying those files again.

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

The catalog is fixed: `LanguageCatalog` lists the 69 learnable languages and the nine native
languages. The predefined language definitions (parsers and dictionaries) are generated into
`PredefinedLanguages.kt` by `tools/generate_language_defs.py` from a language definitions checkout;
the languages added since, or whose definitions changed, are written in the same format in
`tools/languages/<Name>.yaml` and generated into `AddedLanguages.kt` by
`tools/generate_language_defs.py --added`. `LanguageDefinitions.all` is both, an added definition
replacing the generated one of the same name. Every language's one sample book
is a tutorial on learning it with the app, written in that language: `tools/tutorial/<code>.txt`
(English is the source), turned into `Tutorials.kt` by `tools/generate_tutorials.py`. Databases
from before the tutorial lose the sample books they came with once (`RetiredSamples`, matched by
title and a fingerprint of the text) and get the tutorial. Languages written
without spaces between words (Chinese, Cantonese, Japanese, Thai, Lao, Khmer) are parsed by
`SegmentingParser`, which cuts a run of the language's letters into the longest words of its
`Lexicon`, from the left, as Yomitan scans text; Japanese also accepts a stretch that its Yomitan
rules deinflect to a word ("食べました" for "食べる"). `LexiconService` gives a language its
lexicon before a page is parsed: the words and forms of its frequency list and every headword and
form of its installed dictionary packs, made again when the installed packs change. Without a
lexicon, each Chinese character is a word and a Thai, Lao or Khmer run stays whole. Any language
outside the catalog found in the database is removed on start together with its books and terms.

## Interface languages

The interface is in English and Russian (`UiLanguage.supported`), chosen on the Languages page
and stored as `UserSettings.uiLanguage`. Interface text goes through `tr("English text")` and
`trPlural(count, "{0} book", "{0} books")` in `core/ui/.../i18n`: the English text is the key,
`{0}`, `{1}`… are placeholders, and `UiLanguage.code` is snapshot state set at the app root, so
screens recompose when it changes. The Russian texts live in `i18n/ru/`, one map per area of the
app; a plural maps the English "other" form to its three Russian forms separated by `|`.
`TranslationsTest` reads the sources of every module and fails when a literal passed to `tr` or
`trPlural` has no Russian entry (it prints the missing ones ready to paste), when placeholders
differ, or when two areas translate the same text differently; so pass `tr` one plain literal,
never a string template. Text that comes from data (language names, option labels) goes through
`tr(value)` and needs its entries added by hand. To add a language, add its code to
`UiLanguage.supported` and `LanguageCatalog.interfaceLanguages`, a map like `russianStrings`, and
its plural rule in `trPlural`.

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

Recorded words come before any engine in the term pane: `WordRecordings` asks
`CommonsRecordingSource` for files on Wikimedia Commons the way Yomitan's audio sources do, Lingua
Libre's (`LL-Q<id> (<ISO 639-3>)-<speaker>-<word>.wav`, in the category of the language) and
Wiktionary's (`<code>(-<region>)-<word>.ogg`), and plays Commons' MP3 transcode of each, the one
format every platform's `AudioPlayer` decodes. The pane names each recording's speaker and links to
its file page, where its licence (mostly CC BY-SA) is. An inflected form with no recording falls
back to its dictionary form's.

## Offline dictionary packs

Packs are gzip-compressed SQLite files (tables `meta`, `entries`, `forms`), one per source
language and gloss language, published as GitHub release assets and listed with their release and
download size in `DictionaryPackSizes.kt`; `DictionaryPacks` offers exactly those. The `v0.1.0`
release holds the packs built from kaikki.org dumps, `dictionaries-v2` the ones converted from
Yomitan's Wiktionary dictionaries (below). Pairs the Wiktionaries cover too thinly have no pack.

`tools/build_dictionary.py --source <code> [--target ru|en|de|fr|es|pt]` builds a pack from
kaikki.org dumps kept in the ignored `dictionaries/` folder: the gloss language's Wiktionary
section for the source language (`<language>-to-russian.jsonl` and so on) and the English
Wiktionary dump for the inflection tables (`kaikki.org-dictionary-<Language>.jsonl.gz`). With
`--target en` both come from the English dump. Compound forms are dropped because the app looks
up single tokens; Finnish and Hungarian possessive variants are left out for size; English
forms missing from both dumps are generated by rule and flagged `generated`. If the layout
changes, bump `FORMAT` in the script and in `DictionaryId` so older downloads count as not
installed.

`tools/build_yomitan_dictionary.py --source <code> --target <code>` (or `--pairs ka-en,ka-ru`)
converts a dictionary of wiktionary-to-yomitan (wty), the Wiktionary dictionaries Yomitan uses,
into the same layout: glosses and their tags from the structured content, the part of speech from
wty's tags, a transcription from wty's IPA dictionaries (the gloss language's edition, else the English one), and the forms table from the dictionary's
"form of" entries, to which the forms of the language's English-gloss dictionary are added. Entries
that only say a capitalised word is a name are dropped where an edition is full of them (the Greek
Wiktionary has a bot-made entry for every surname), and a pack with fewer than 1,000 entries besides
names is not written. A sense keeps up to three of its example sentences with their
translations (`examples` in the senses JSON). Japanese and Chinese packs also have a `readings`
table (each headword's kana or pinyin, the pinyin without the other romanisations wty adds), and
Japanese packs a `kanji` table from KANJIDIC2 (EDRDG, CC BY-SA 4.0: meanings in the gloss language
where KANJIDIC2 has it, else English; on and kun readings, strokes, grade, JLPT level, frequency)
and a `pitch` table from Kanjium (CC BY-SA 4.0; "The pitch accent notation was provided by Uros O.
through his free database"), both read from `dictionaries/japanese/`. The Japanese packs of the languages JMdict has glosses in (en, de, ru, es, fr) add JMdict's entries
(EDRDG, CC BY-SA 4.0, read by `tools/jmdict.py` from `dictionaries/japanese/JMdict.gz`) for the
words and readings wty lacks, an entry per headword and reading, leaving out the spellings JMdict
keeps for search only or marks irregular (they become forms); the entries JMdict marks as common
(news1, ichi1, spec1, gai1) are listed in a `common_entries` table, badged in the term pane and
ranked first. The Chinese-English pack likewise adds CC-CEDICT's entries (MDBG, CC BY-SA 4.0, read by
`tools/cedict.py` from `dictionaries/chinese/`) for the words and readings wty lacks, each simplified
spelling a form of its traditional headword and its measure words a tag. `tools/build_jmnedict.py`
builds ja-names, Japanese names from JMnedict (EDRDG, CC BY-SA 4.0), an entry per spelling with a
sense per reading; installed, it adds its names after the entries of every Japanese lookup,
labelled JMnedict, but stays out of the lexicon that cuts text and of the readings above it.
These tables are optional:
the app reads them where a pack has them, and the format stays 1. Downloads are cached in `dictionaries/wty/`; built packs are
recorded in `dictionaries/wty/built.json`, and `--write-catalog` writes them into
`DictionaryPackSizes.kt` as the `dictionaries-v2` release, to which the files are uploaded. wty has
no Breton dictionary, so Breton's packs are built from kaikki.org with `tools/build_dictionary.py`
(its English, French and Russian Wiktionary sections) and recorded in the same list.

Word finders find a word of the learned language from the reader's own: `--glossaries` converts
wty's glossary dictionaries, the translation tables of the native language's Wiktionary, into
`<native>-<learning>-gloss` packs whose entries are native words and whose senses list the learned
language's words for one meaning, its label as the tag. They have the packs' layout, are
`DictionaryId(native, learning, glossary = true)` in the app (kept out of `DictionaryPacks.all`,
listed in `DictionaryPacks.glossaries`) and are published in the `glossaries-v1` release. The term
pane's "Find a … word from …" opens them; the Dictionaries screen lists only installed ones.

Any Yomitan dictionary can be imported from the Dictionaries screen. `YomitanConverter` (core/data)
reads the zip with `ZipArchive`, whose entries are inflated per platform (`Inflater`, zlib on iOS,
`DecompressionStream` on the web), and converts it in Kotlin the way the Python tool converts wty's:
term banks to entries and forms, wty's glosses lists, other dictionaries' glossary lists or list
items or text as senses, pitch accents, IPA and frequencies from the term meta banks (a
`frequencies` table, with the index's `frequencyMode` in the meta table), and kanji from the kanji
banks (the `kanji` table, its strokes, grade and JLPT level read from the banks' stats). A
dictionary of frequencies or kanji alone is a pack of those alone: the term pane labels entries
with the frequencies of the pair's imported dictionaries, and its kanji panel fills in from them
what the app's pack lacks. The pack is written
with `newSqliteFile` and stored by `DictionaryPackStore.installFile` as `imported-<key>`; the list
of imported dictionaries is kept in the settings (`SettingsImportedDictionaryStore`). Japanese and
Chinese words get an entry per reading (打つ read うつ and ぶつ), each entry's reading in the optional
`entry_readings` table; a word typed as one of its readings shows that reading's entries only. Lookups in a
pair's pack add the entries of its imported dictionaries after their own, each named by its title.

Android, iOS and desktop unpack a downloaded pack into app storage and open it read-only as a
second SQLDelight database; the web build keeps the compressed file in the browser Cache API and
inflates it in its own sql.js worker, which needs the pack host to allow cross-origin requests.
A pack file's `user_version` is its format, and the Android and iOS drivers refuse a file whose
version is above their schema's (the generated schema's is 1), so they open a pack with the
schema at the pack's format (`atFormat`), which neither creates nor migrates anything.

## Language rules

`core/domain/.../morphology` is Yomitan's language logic (GPL-3.0), ported: for each language,
the text processors that give a clicked word's other spellings (lowercase, without stress or
length marks, ё as е, ss as ß) and the inflection rules that lead from a form to its dictionary
word. `tools/export_yomitan_rules.mjs <yomitan checkout>` writes them from Yomitan's sources:
`YomitanLanguages.kt` names the processors each language runs, which are written by hand in
`TextProcessors.kt`, and `rules/<Code>Transforms.kt` holds a language's rules as data, listed in
`TransformRules.kt`. A Yomitan rule is a pattern, a function undoing the inflection and the
conditions (parts of speech, intermediate forms) it goes from and to; the exporter records each
function as data (letters cut and added at the ends, a fixed word, or a chain of replacements,
captured by running it on a probe string), and `LanguageTransformer` runs them as Yomitan's
transformer does. `Unicode.kt` normalises text (NFD, NFC), which Kotlin's common library cannot,
from the tables `tools/generate_unicode_tables.py` writes.

`DictionaryService` uses them when a pack has nothing under the clicked word, neither as a
headword nor as a form: the word's other spellings are looked up as the word itself, and the words
it may be an inflection of, of a part of speech its rules allow, as its lemmas, so the term still
gets its meaning and its parent.

## Course packs

The ready-made courses of a language are one download, offered in Settings > Courses: a
gzip-compressed SQLite file (tables `meta`, `courses`, `lessons`) listed in `CoursePacks` and
published as an asset of the `courses-v1` GitHub release; there are packs for `pt`, `en`, `de`, `fr`, `es`, `ru`, `it`, `sr`, `nl`, `da`, `no`, `sv` and `el`.
`tools/build_courses.py --language <code>` checks each course in `tools/courses/<code>/` (written as its `BRIEF.md` says) and writes
`course-packs/courses-<code>.sqlite.gzip` (ignored by git); upload that file to the release and
update `downloadSize` in `CoursePacks`. If the layout changes, bump `FORMAT` in the script and in
`CoursePack`. A language's courses can pin the word list they were written against as
`tools/courses/<code>/wordlist.tsv`; `tools/check_course.py` then reads it instead of the app's list,
so rebuilding the app's frequency list does not shift the courses' bands (German, French, Spanish, Russian, Italian, Serbian, Dutch, Danish, Norwegian, Swedish and Greek do this).
For Spanish the checker also splits the pronouns attached to a verb (*dándoselo* is *dando* + *se* +
*lo*), counts a name of several words or with a hyphen (*San José*, *al-Ándalus*) as one known word,
and reads capital Roman numerals (*siglo XVI*) as numbers.
For Russian it folds ё into е, splits words at hyphens, skips the ending of an ordinal after digits (*5-го*), reads
Roman numerals as numbers, and accepts the case forms of the names a course lists (*Анна*, *Анны*, *Анной*).
For Italian it splits elisions (*l'amico*, *c'è*), counts the pronouns attached to a verb (*dimmelo*, *mangiarlo*)
as words of their own, and credits a list word written whole (*farci*) even where it also splits that way.
For Serbian (Latin script, ekavian) it counts the case forms of the names a course lists (*Šapac*, *u Šapcu*) as known; the list
also holds Cyrillic and ijekavian forms, which the checker accepts, so the texts keep to ekavian Serbian by the brief.
For Dutch it reads the short forms with an apostrophe in front (*'s ochtends*, *'t*, *'n*, *'k*, *'m*) as the words they stand for and keeps
words with an apostrophe inside whole (*auto's*, *zo'n*); the pack shows a list headword wordfreq split with an underscore (*om_liggen*) as its form.
For Danish it counts decades written with an apostrophe (*1960'erne*) as numbers, keeps a hyphenated word whole when the list has it
whole (*e-mail*, *id-kort*), and, as for Dutch, counts the genitive of a name of several words (*Maja Holms*) as the name.
For Norwegian it does the same for names and hyphenated words, counts decades (*1960-tallet*) as numbers, and
`extra_forms.tsv` adds the noun genitives (*husets*, *bilens*) and many inflections the wordfreq list misses.
For Swedish it reads an ending joined to an abbreviation with a colon (*tv:n*) as the abbreviation and counts decades
(*1960-talet*) as numbers; `extra_forms.tsv` links the adjective forms the list files as words of their own (*fint* to *fin*,
*mjuka* to *mjuk*) and adds noun genitives and weak-verb participles.
For Greek it reads the elided words (*σ' το*, *απ' τη*, *γι' αυτό*) as the words they stand for, takes a word with a
second accent before an enclitic (*όνομά μου*) as the word with its first accent, reads *ό,τι* as one word, skips the ending of an
ordinal after digits (*20ός*, *3η*), and keeps a hyphenated pair whole when the list has it (*σιγά-σιγά*); `extra_forms.tsv` adds
the verb, participle and noun forms the wordfreq list and Wiktionary miss.

## Book packs

A language's ready-made books are one download too, offered in Settings > Books and on the empty
Books page: a gzip-compressed SQLite file (tables `meta`, `books`) listed in `BookPacks` and published as an
asset of the `books-v1` GitHub release; there is a pack for `pt`. The texts are authentic works under a
free licence (public domain, or CC BY-SA 4.0 for the Wikibooks and Wikipedia pages), listed with their
level, source and licence in `tools/books/<code>/books.json`. `tools/fetch_books.py --language <code>`
downloads the sources (the NLTK Machado de Assis corpus, pt.wikisource, pt.wikibooks, pt.wikipedia
leads) into `tools/books/.cache/` (ignored by git) and writes the cleaned texts to `tools/books/<code>/text/`
(checked in), each ending in its attribution line; `tools/build_books.py --language <code>` then writes
`book-packs/books-<code>.sqlite.gzip` (ignored by git) and prints the `downloadSize` for `BookPacks`.
Installing a pack adds its books to the library with the level and the manifest's tags, and a source URI
starting with `tayra-books://`, by which removing the pack finds them again; books of the pack that are
already in the library are left alone, so a user's edits survive a reinstall.

Installing a pack stores it like a dictionary pack and writes its courses into the database as
samples for every language it teaches, also ones added later. A pack lists a course's tags as
space-separated text; in the database each tag is a row of `course_tags`, linked to its courses
through `course_tag_map`, so the Courses screen can filter by several tags (any or all of them). Removing it deletes those courses,
with their lessons and the texts read from them, and the file. A course reaches a database once,
so a reworked course needs a new id.

## Free online libraries

Books > Find books (`Route.FindBooks`, `FindBooksScreen` in feature/books) searches three libraries
through `BookCatalog`s in core/data `discover/`: `GutenbergCatalog` reads Project Gutenberg's own OPDS
search (`/ebooks/search.opds/?query=l.<code> …`), which answers in about a second where Gutendex can
take most of a minute, and fetches texts from `/cache/epub/<n>/pg<n>.txt`; `GdlCatalog` reads the
Global Digital Library's content API, whose language slugs are regional (`pt-br`, `es-es,es-ni`), and
reads a book's ePUB whole, skipping picture books with fewer than 30 words. `WikisourceCatalog` uses
the language's wiki (`<code>.wikisource.org`): without a search, last month's most read pages from
Wikimedia's page-view API, kept to the works Wikidata types as literature (the most read pages also
hold laws, anthems and prayers), and with a search the wiki's own, a chapter (`Work/II`) standing for
its work; Wikidata names the authors, from the edition or the work it is an edition of (P50, P629).
A work is judged by its first chapter through the wiki's `action=parse`, without the `ws-noexport`
header, and added from the ePUB of Wikisource's export tool (`ws-export.wmcloud.org`), without the
title page, the credits and the work's page when that only lists the chapters (`EpubText.read`'s
`keep`). Wikimedia asks for a User-Agent with a contact address, and `origin=*` lets the web build
read the wikis. `BookDiscoveryService`
judges a book by the running words of its opening the reader knows (95% easy, 85% medium); a
Gutenberg or Wikisource book is judged as it is listed (`BookSource.judgedOnSight`), a GDL book only
when asked. The
downloads are sent with `Cache-Control: no-store`, or the client's cache would hand back the 64 KB sample
for the whole text. An added book's source URI is its page in the library, and its text ends with a
"Source:" line in the interface language. On the web only the Global Digital Library answers, as
Gutenberg sends no CORS headers. The idea and the API details come from the Learning with Texts fork
(HugoFara/learning-with-texts, public domain).

## Word frequency lists

The Word frequency screen (feature/frequency) shows the most common words of the language being
learned, bundled as `feature/frequency/src/commonMain/composeResources/files/frequency/<code>.tsv`
for every language in `LanguageCatalog.targetLanguages`: one dictionary word per line, most common
first, written as the dictionary writes it ("Haus"), with up to 40 lowercase forms counted for it
after a tab. Lists have up to 10,000 words, and fewer where the language's dictionary, lemmatizer
or corpus is small (Belarusian about 3,800, Basque about 2,800). Toki Pona's list is its whole
vocabulary, ranked by the usage survey of lipu Linku, from which `tools/build_toki_pona.py` also
builds its dictionary packs, since no Wiktionary covers the language. Old Irish has no list: no freely
licensed corpus of it is big enough to count.

`tools/build_frequency_list.py --language <code>|all` makes them; it needs `pip install wordfreq
simplemma` and the `dictionaries/<code>-en.sqlite` packs. Counts come from wordfreq; Croatian,
Serbian, Estonian and Galician from FrequencyWords (OpenSubtitles); Belarusian and Latin from the
Leipzig Corpora Collection; a language none of these has, from its Wikipedia, counted from the
articles dump with the markup taken out; Ancient Greek from the New Testament as MorphGNT parses
it. A corpus of a language written without spaces (Thai, Lao, Khmer, Cantonese) is cut into words
as the app's parser cuts it, the longest the dictionary knows first; wordfreq's lists are cut
already. Breton's words are grouped with its French pack's forms, which are far more than its
English pack's. A language for which wordfreq keeps only its short list is blended with
the other sources. Downloads are cached in `dictionaries/frequency-sources`.

Forms are grouped under their dictionary words with the pack's forms table, then simplemma for
forms the pack lacks. Both are noisy (pronoun tables list every pronoun as a form of the others,
aspect partners and look-alike nouns appear as forms), so each language's top 600 words were read
and corrected in `tools/frequency_fixes.py`. Change the general rules in the script with care:
those corrections were tested against them, so rebuild all lists and compare the top of each.

A word's status on the screen is its own term's, or else the furthest status among its forms the
reader has saved.

The vocabulary level (a band's button, which sets it to the rank just above the band as in
Language Reactor, or Settings → Vocabulary) is stored per language
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
