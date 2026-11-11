# Store listing texts

Both stores: **Tayra for YouTube**, category Education, language English.
Icon: `public/icon/128.png`. Builds: `npm run zip:store` → `.output/tayra-browser-extension-<version>-chrome.zip`
and `-firefox.zip` (+ `-sources.zip`, which addons.mozilla.org asks for because the code is bundled).

## Short description (Chrome ≤ 132 characters; AMO summary ≤ 250)

Learn languages from YouTube with Tayra: subtitles coloured by the words you know, instant lookups,
and saving words to the Tayra app.

## Description

Watch YouTube in the language you are learning, with the Tayra Languages app on your computer.

**Subtitles you can learn from**
- The video's subtitles over the player, every word coloured by how well you know it in Tayra:
  new, learning or known.
- A translation under each line — YouTube's, or one made by a person when the video has one.
- Previous / replay / next line, auto-pause at the end of each line, pause while the pointer is on
  the subtitles, keys for all of it (A, S, D, W, Q, E, R).
- Hide the line or its translation until you reveal it, to train your ear.
- Furigana, pinyin or romaji above Japanese and Chinese words.

**Look words up without leaving the video**
- Rest the pointer on a word for its meaning; click it for its card with the dictionary entry,
  how common it is, and the other lines of the video that say it.
- Set a word's status in one click, or right-click to step it up. Words you save go into Tayra
  with the line as an example sentence, for your flashcards.
- Hear words and lines in Tayra's voices. Open a word in Wiktionary, Google Translate, Glosbe,
  Forvo or a dictionary of your own.

**A panel beside the video**
- Every line of the video to read along and jump to.
- The video's words: how much of the video you already know, the new words worth learning first
  (most common in the language), and mark all as known.
- The words you saved from this video.
- Print the transcript or save it as PDF, with your words marked.

**Your videos in Tayra**
- Videos you watch go into Tayra's Videos library; bookmark a line with R, and open the video in
  the app where you are.

Requires the free Tayra Languages desktop app (macOS, Windows, Linux), open while you watch. The
extension talks only to the app on your computer; nothing is sent anywhere else.

## Chrome Web Store: single purpose

Helps people learning a language watch YouTube videos with interactive subtitles, word lookups and
word saving, backed by the Tayra Languages app on their computer.

## Chrome Web Store: permission justifications

- **storage** — keeps the extension's settings, the words saved from each video, and the token for
  the Tayra app.
- **nativeMessaging** — asks the Tayra app's helper (`app.tayra.bridge`), which the app registers,
  for the token, so the extension connects to the app with no pairing code.
- **Host https://www.youtube.com/\*** — shows the subtitles, panel and word cards on YouTube's
  watch pages and reads the video's subtitle tracks the player already has.
- **Host http://127.0.0.1/\*** — talks to the Tayra app on the same computer (ports 47831–47835).
- **Remote code** — none; all code is in the package.

## Chrome Web Store: data usage

Handles "Website content" (YouTube subtitles and titles) only to show them and send them to the
Tayra app on the same computer. Not sold, not used for anything unrelated, not used for credit.
No other data types.

## Screenshots to take (1280×800)

1. A Portuguese video with coloured subtitles and the side panel's Subtitles tab.
2. A word's card open over the video.
3. The hover popup on a word.
4. The panel's Words tab with "Worth learning first".
5. The printed transcript.

## Before the first upload

- Host `PRIVACY.md` on the Tayra website and put its address in both stores.
- After the Chrome Web Store assigns the extension's ID, add it to
  `NativeHostInstaller.CHROME_EXTENSION_IDS` and ship that app version **before** the extension is
  public, or the store build cannot get its token from the app (pairing with a code still works).
- On addons.mozilla.org, the ID is fixed by the manifest: `youtube@tayra.app`, already in the app.
