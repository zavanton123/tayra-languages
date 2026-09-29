# Tayra Languages

**Learn a language by reading what you love.**

Tayra Languages turns any text into a reading lesson. Import a story, an article or a book,
read it page by page, tap the words you do not know, and watch the highlights fade as the
words become yours. It runs on Android, iOS, macOS, Windows, Linux and in the browser, and
everything you learn stays on your own device.

## Reading

- **Every word is a button.** Unknown words are highlighted. Tap one to see its meaning, set
  how well you know it, add a translation or a note, and move on. Select several words to
  save an expression such as *à beira* as one term.
- **Five learning levels plus "well known" and "ignored".** Highlights follow the level, so a
  page shows at a glance what is new, what is being learned and what is already yours.
  Right-click (or long-press) a word to mark it as new, or to mark a learned word as well known.
- **Sentence translations under the text.** Turn on *Show translations* and each sentence
  gets its translation in a lighter line beneath it, or side by side in two columns.
  *One sentence per line* lays the text out for easy comparison.
- **Listen to any sentence.** A play button before each sentence reads it aloud, with the
  voice you choose on the Speech screen.
- **A reader that adapts to you.** Focus mode, adjustable font size, line height and text
  width, several colour themes, keyboard shortcuts on desktop, and bookmarks to pick up
  where you left off.
- **Term details without leaving the page.** The side pane shows the word's dictionary
  entry, its base form, example sentences, and links to online dictionaries you choose per
  language.

## Vocabulary

- **A terms list** with search, status filters and sorting, bulk status changes, and CSV
  import and export.
- **Parents and components.** Link an inflected form to its base word; the popup shows the
  base word's meaning and, for expressions, the words inside them.
- **Pronunciation** with the device's speech voices, and recorded example sentences.
- **Statistics** of words read and learned over time, per language and per book.

## Translation, online or offline

Sentence translations and suggested meanings come from the engine you pick on the
Translation screen:

| Engine | Needs |
| --- | --- |
| MyMemory | nothing; free, with a daily quota |
| Google Translate | a Google Cloud API key |
| Microsoft Translator | an Azure AI Translator key and region |
| Alibaba Cloud Translation | an Alibaba Cloud AccessKey |
| Baidu Translate | a Baidu Translate App ID and key |
| DeepL | a DeepL API key |
| Qwen-MT | an Alibaba Model Studio API key |
| On this device | nothing online after the first download |

Keys are kept in the platform's secure storage: the Keychain on macOS and iOS, the Keystore
on Android, the Data Protection API on Windows. Each engine has a *Check key* button.

**On this device** works with no network at all. On Android and iOS it uses Google ML Kit,
with one small model per language that you download once. On desktop it uses Argos
Translate, which the app installs into its own folder on first use, models included. When a
model is missing, the reader says which one and offers to install it right there.

## Speech

The play buttons and the speaker icons on terms use the engine picked on the Speech screen:

| Engine | What it is |
| --- | --- |
| System voices | the voices of the operating system; nothing to download here |
| Piper | neural voices that run offline, with downloads for most of the languages the app teaches |
| Kokoro | a high-quality offline voice for English, Spanish, French, Italian and Portuguese |

Piper and Kokoro run on desktop, Android and iOS. Voices are downloaded from the Speech screen,
one per language for Piper and a single model for Kokoro, and the screen lets you pick a voice
per language, set the speed and try a sentence. A language without a downloaded voice falls
back to the system voice. The web version uses the system voices.

## Offline dictionaries

Download a dictionary pack for a language and its meanings appear in the term pane with no
network: definitions, every sense with a one-tap add, and the link from an inflected form to
its base word. Packs exist for 33 European languages with meanings in English, French,
German, Portuguese, Russian or Spanish, and are managed on the Dictionaries screen.

## Languages

Read in Belarusian, Bosnian, Bulgarian, Catalan, Croatian, Czech, Danish, Dutch, English,
Estonian, Finnish, French, Galician, German, Greek, Hungarian, Icelandic, Irish, Italian,
Latin, Latvian, Lithuanian, Macedonian, Norwegian, Polish, Portuguese, Romanian, Russian,
Serbian, Slovak, Slovene, Spanish, Swedish, Turkish, Ukrainian or Welsh, with meanings shown
in English, French, German, Portuguese, Russian or Spanish. Each language comes with sample
texts and a short tutorial, and its dictionaries and text settings can be adjusted.

## Books

- Import plain text, EPUB and subtitle files (SRT, VTT), or paste a text; on desktop and
  Android, a web page can be imported by its address.
- Long texts are split into pages by paragraph or by word count; pages can be edited, added
  or removed later.
- The library shows every book with its language, progress and how much of its vocabulary
  is already known, with search, sorting, filters and an archive for finished books.

## Get it

Releases on GitHub include an Android APK and a macOS disk image. The web version runs in a
browser without installing anything, though its library lasts only for the session.

Build it yourself with a recent JDK:

```
./gradlew :desktopApp:run                      # desktop
./gradlew :androidApp:installDebug             # android
./gradlew :webApp:wasmJsBrowserDevelopmentRun  # web
```

For iOS, run `pod install` in `iosApp` once, then open `iosApp/iosApp.xcworkspace` in Xcode.

Details for contributors, from the module layout to how dictionary packs are built, are in
[docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).
