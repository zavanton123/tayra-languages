# Tayra Languages Vocabulary Practice Plan

Status: phases 1–5 implemented on `main` — see section 13
Date: 24 October 2026
Scope: the practice of a page, of a whole text and of words chosen in the vocabulary, on Android, iOS, desktop and web

## 1. Goal and constraints

Practice should take a word from recognising it to recalling it to saying it, in the sentence it was met in and in new ones.

Constraints:

1. No LLM and no API key. Every question is built by code from data the app already has or can download.
2. No manual work per word, course or language. A word saved while reading is practisable at once.
3. Practice works offline once a word's material is stored; only fetching new example sentences needs the network.
4. One practice screen and one builder serve every source (page, whole text, chosen words).

## 2. Where practice stands today

| Piece | Where | What it does |
|---|---|---|
| `ExerciseKind` | `core/domain/.../practice/Practice.kt` | `GAP_CHOICE`, `GAP_TYPED`, `NEW_CONTEXT`, `HEAR_CHOOSE`, `DICTATION` |
| `PracticeBuilder.build` | same file | About two questions a word, ten at most; kinds taken in rotation; wrong choices are the other learning words, then other words of the page |
| `PracticeChecker` | same file | Case and punctuation ignored; an answer wrong only in accents is `ALMOST` |
| `PracticeViewModel` | `feature/reading/.../practice/` | Gathers words from a page, a whole text or chosen terms; fetches up to six examples a word through `ExampleSentencesProvider` (3–14 words, random); translates the page sentence when shown |
| `PracticeScreen` | same folder | One question card: choices or a text field, listening button, result line |
| `PracticeKindsDialog` | `feature/terms/.../list/` | Four ticks: choose, listen and choose, type, listen and type |
| `Route.Practice` / `Route.PracticeWords` | `core/ui/.../navigation/Routes.kt` | Carry the ticked kinds by name |

Material already available:

- The sentence a word was met in, its translation (`SentenceTranslator`, cached in `SentenceTranslations.sq`) and the word's saved translation.
- Example sentences from Tatoeba and from the installed courses (`CourseExamplesProvider`), with translations.
- Word forms: the offline dictionary pack's `forms(form_lc, lemma)` table, the forms linked to a term as its parents and children, and Wiktionary's tagged forms online (`OnlineWiktionary.forms`, used by the conjugation dialog).
- Word frequency ranks (`FrequencyList`, `Lexicon.rank`) for 33 languages.
- Speech for every sentence (`SentenceAudio`, system voices, Piper, Kokoro).

Missing: per-word practice progress, stored examples, a reverse forms lookup (lemma to forms), speech recognition.

## 3. The exercise model

A question is one choice on each of three axes instead of one of many fixed kinds.

### 3.1 Axes

| Axis | Values |
|---|---|
| Context (`PracticeContext`) | `ORIGINAL` — the sentence the word was met in; `EXAMPLE` — a stored example sentence |
| Prompt (`PracticePrompt`) | `GAP` — the sentence with the word blanked, translation hidden; `AUDIO` — the sentence read aloud, text blurred, with the word's meaning as the question; `TRANSLATION` — the sentence's translation with the original's gap beneath; `MEANING` — the sentence shown whole, the word marked, its meaning asked |
| Response (`PracticeResponse`) | `CHOOSE` — four options; `TILES` — the word's letters shuffled; `TYPE` — free text; `SPEAK` — speech recognition (phase 5) |

`MEANING` is the only prompt whose answer is the translation; every other prompt's answer is the word as written in the sentence.

Today's kinds map onto it: `GAP_CHOICE` = ORIGINAL·GAP·CHOOSE, `GAP_TYPED` = ORIGINAL·GAP·TYPE, `NEW_CONTEXT` = EXAMPLE·GAP·CHOOSE, `HEAR_CHOOSE` = ORIGINAL·AUDIO·CHOOSE (word only), `DICTATION` = ORIGINAL·AUDIO·TYPE.

Separate exercise kinds that do not fit the three axes (section 6) keep their own `ExerciseKind` values.

### 3.2 Difficulty ladder

Each word has a practice level, stored per term:

| Level | Responses asked | Moves up after | Moves down after |
|---|---|---|---|
| 0 Recognise | CHOOSE (GAP, MEANING, AUDIO) | 2 right in a row | — |
| 1 Recall with help | TILES, CHOOSE on EXAMPLE context | 2 right in a row | 2 wrong in a row |
| 2 Recall | TYPE on GAP, TRANSLATION, AUDIO | 3 right in a row | 1 wrong |
| 3 Produce | TYPE on EXAMPLE context, SPEAK where available | — | 1 wrong |

`ALMOST` counts as right for the level. A practice of a page still asks every word at least once; the level only picks how.

### 3.3 Practice setup

Before a practice starts, a setup dialog says what will be asked. It replaces today's four ticks with filters on each axis, shows what each choice leaves, and remembers the last setup.

#### What can be filtered

| Filter | Options | Notes |
|---|---|---|
| Preset | Quick review, Balanced (default), Listening, Writing, Speaking, Custom, and the learner's own saved presets | A preset fills every filter below; changing any filter turns it into Custom |
| Skills | Reading, Listening, Translation, Word forms, Speaking | A group with a tri-state tick; expanded, it lists its exercise types, each with its own tick |
| Answer by | Choose, Letter tiles, Type, Speak | Applies across every skill; a type whose only answer mode is unticked is greyed out with the reason |
| Sentences | From the text, Other examples | "Other examples" shows how many are stored and whether more need the internet |
| Words | All being learned, New to practice, Weakest (lowest level), Not practised for a week, Only these (when started from a selection) | Counts beside each, from `practice_progress` |
| Length | 5, 10, 20 questions, or every type for every word | With the estimated time (about 20 seconds a question) |
| Adapt to my level | On / off | On, the answer mode of each question follows the word's level (3.2) within the ticked modes; off, the ticked modes are mixed evenly |

Every exercise type row shows:

- its name and one-line description, with a small example ("Eu ___ minha carteira." → *esqueci*);
- how many of the chosen words it can be asked for ("9 of 12 words"), or why none ("needs a translation", "needs other forms of the word", "needs a speech recogniser", "needs stored examples — connect to the internet");
- its icon in the group colour.

The footer sums up and starts:

- "18 questions · 6 kinds · about 6 minutes", or what blocks the start ("Tick at least one kind that fits these words");
- Start, and "Save as preset…" (a name, then it appears among the presets).

#### Defaults and memory

- The first time: Balanced — Reading, Listening and Translation; Choose, Letter tiles and Type; both sentence sources; all words; 10 questions; adapt on.
- After that, the last setup used, kept separately for a page or text practice and for a vocabulary selection (`practiceSetupReading`, `practiceSetupVocabulary` in the settings).
- Types that cannot be asked for these words are shown unticked and greyed, but stay ticked in the saved setup, so they come back when they apply.
- "Reset to Balanced" link in the dialog.

#### Layout

- Phone: a full-height sheet. The preset chips scroll sideways at the top. Skills, Answer by, Sentences, Words and Length are collapsible sections, Skills open and the rest collapsed with their current value on the section line ("Answer by — Choose, Tiles, Type"). The summary and Start stay pinned at the bottom.
- Desktop and tablet: a large dialog (like the practice and examples dialogs) with two columns: presets and the filter sections on the left, the selected exercise types with their examples and counts on the right; summary and Start in the footer.
- Keyboard: arrow keys move through rows, Space ticks, Enter starts.

#### How it is computed

A pure function in `core/domain/practice`, `PracticePlanner.preview(words, material, setup): PracticePreview`, gives for each exercise type the words it can be asked for and why the others cannot, and the questions the builder would make. The dialog shows it; `PracticeBuilder.build` takes the same `PracticeSetup`, so what the dialog promises is what is asked.

```kotlin
data class PracticeSetup(
    val types: Set<ExerciseType>,          // ticked exercise types (each belongs to a skill)
    val responses: Set<PracticeResponse>,
    val contexts: Set<PracticeContext>,
    val words: WordFilter,
    val length: Int?,                      // null: every type for every word
    val adaptive: Boolean,
)
```

`material` is what is known about each word before practice: its translation, its sentence's translation, how many stored examples and forms it has. It is loaded once when the dialog opens (`PracticeMaterialLoader`), so the counts are right and opening the practice afterwards is quick.

The route carries the setup as JSON (`Route.Practice(setup = …)`); the old `kinds` list is still read and mapped to a setup.

## 4. Data

### 4.1 Database (migration `26.sqm`)

```sql
CREATE TABLE practice_progress (
    term_id INTEGER NOT NULL PRIMARY KEY REFERENCES terms(id) ON DELETE CASCADE,
    level INTEGER NOT NULL DEFAULT 0,
    streak INTEGER NOT NULL DEFAULT 0,   -- positive: right in a row; negative: wrong in a row
    asked INTEGER NOT NULL DEFAULT 0,
    right INTEGER NOT NULL DEFAULT 0,
    last_practised INTEGER                -- epoch millis
);

CREATE TABLE practice_examples (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    term_id INTEGER NOT NULL REFERENCES terms(id) ON DELETE CASCADE,
    sentence TEXT NOT NULL,
    translation TEXT,
    form TEXT NOT NULL,                    -- the form of the word as written in the sentence
    form_start INTEGER NOT NULL,
    source TEXT NOT NULL,                  -- 'tatoeba' | 'course'
    source_id TEXT,                        -- for the CC-BY credit
    fetched INTEGER NOT NULL
);
CREATE INDEX practice_examples_term ON practice_examples(term_id);
```

Backups include both tables. Clearing local data clears both.

### 4.2 Stored examples

`PracticeExampleStore` (core/domain interface, core/data implementation):

- When a term is saved with a learning status, and again when a practice starts with fewer than three stored examples for a word, it searches `ExampleSentencesProvider` (courses first, then Tatoeba): 4–10 words, up to eight results.
- Each result is kept only when one of its words is the term or one of its forms (section 4.3); `form` and `form_start` record which.
- Results with the exact saved form come first; at most five are kept per term.
- Sentences without a translation are translated with `SentenceTranslator` and stored.
- Offline, the store returns what it has; practice falls back to the ORIGINAL context.

### 4.3 Forms of a word

`WordForms.of(term, language): Set<String>`, combining:

1. The term's parents and children in `term_parents`.
2. The dictionary pack: lemmas of the term (`selectLemmas`), then every form of those lemmas through a new `selectFormsOf: SELECT form_lc FROM forms WHERE lemma = :lemma`. The packs have no index on `lemma`; `tools/build_dictionary.py` adds `CREATE INDEX forms_lemma ON forms(lemma)` and the pack version goes up. On an older pack the query runs without the index once per word and its result is cached in memory for the session.
3. When online and the language has Wiktionary forms: `OnlineWiktionary.forms`, which also gives tags (tense, person, number). Tagged forms are cached on disk beside the conjugation dialog's cache.

## 5. Generating each prompt

### 5.1 GAP

Unchanged from today: the sentence with `range` blanked. Choices come from section 5.5.

### 5.2 MEANING

The sentence whole with the word marked; options are four translations: the word's own and three wrong ones.

- Wrong translations come from other learning words of the practice, then other saved terms of the language with the same part of speech when known, then dictionary glosses of the sentence's other words.
- A wrong translation that shares a word with the right one ("to forget" / "forgot") is dropped.
- Without a saved translation for the word, MEANING is not asked.

### 5.3 AUDIO

The sentence is read aloud and its text blurred; the question shows the word's meaning: "Which word means *I forgot*?".

- ORIGINAL context: the meaning is the term's translation.
- EXAMPLE context where `form` differs from the saved form: the question reads "Which word is a form of *esqueci* (*I forgot*)?" and the answer is `form`. No translation of the other form is made up.
- Replay button; the blur lifts once answered.

### 5.4 TRANSLATION

The sentence's translation on top; beneath it the original with the gap, so the expected word is unambiguous.

- TYPE shows the first letter as a hint after a wrong first attempt.
- Not asked when the sentence has no translation.

### 5.5 Choices for the word

Four options: the answer and three others, taken in this order until three are found:

1. Other forms of the same word that do not fit (section 6.1 rules) — only at level ≥ 1, at most one.
2. Other learning words of the practice.
3. Words of similar frequency rank (±30 %) with the same first letter or the same ending, from the frequency list.
4. Other words of the sentence and page (today's fallback).

Never offered: a second option equal to the answer apart from case or accents, or a form that would also be right (section 6.1).

### 5.6 Letter tiles

The answer's letters shuffled, plus two distractor letters from the language's alphabet; at most 12 tiles. Words longer than 12 letters fall back to TYPE with a first-letter hint.

## 6. New kinds of exercise

Each is a separate exercise type, ticked under its skill in the setup dialog (3.3) and generated by the same builder.

### 6.1 Choose the form (group: Word forms)

The sentence with the gap, the sentence translation shown as the cue, and four forms of the word.

- Forms from section 4.3. When the forms carry tags (Wiktionary), the wrong options differ from the answer in one tag (person or tense) where possible.
- A form is dropped when it is spelled like the answer, or when it is the answer of another stored sentence identical apart from the gap.
- Asked only when at least three other forms exist; otherwise skipped for that word.
- Responses: CHOOSE, TYPE (level ≥ 2), SPEAK (phase 5).

### 6.2 Matching pairs (group: Reading)

Five words and their five translations in two columns; tap a word, then its translation. Wrong pairs shake and stay.

- Needs at least four words with translations in the practice; counts as one question per word for progress.
- One matching round opens a practice of five or more words.

### 6.3 Build the sentence (group: Translation)

The sentence translation as the cue; the original's words as shuffled tiles to tap into order.

- Only sentences of 3–8 words. Punctuation is attached to its word.
- Right when the order matches; a second accepted order is any stored sentence with the same words.

### 6.4 Full dictation (group: Listening)

Already exists for sentences of six words or fewer; extend to eight, and show a word-by-word diff (right, accent-only, wrong, missing) instead of right or wrong.

### 6.5 Sound-alike (group: Listening)

The word is read aloud alone; four written options that look alike: other forms of the word, or words within edit distance 2 from the frequency list.

### 6.6 True or false (group: Reading)

"*esqueci* — *I forgot*?" with a right or a borrowed wrong translation, half each. Fast; asked at level 0 only, at most two per practice.

### 6.7 Word partners (group: Word forms, later)

From stored examples and course sentences, the word that most often stands next to the learning word ("esquecer **de**"). The sentence with the partner blanked; options from 5.5. Asked when a partner appears in at least three stored sentences.

### 6.8 Gender and plural (group: Word forms, later)

For nouns whose dictionary entry has a gender or a plural (as the flashcard hints show): choose the article, or type the plural.

## 7. Checking answers

`PracticeChecker` gains:

- `TYPO`: one letter wrong, missing, extra or swapped, in a word of five letters or more. Shown like `ALMOST` (right, with the spelling) but does not move the level up.
- Several right answers: any form recorded for the gap in another stored sentence of the same text is not accepted; only the sentence's own form is.
- Whole-sentence answers (dictation, build the sentence) compared word by word for the diff.

## 8. Screen changes

- `Question` becomes a dispatcher over prompt and response: `PromptView` (gap sentence, blurred audio, translation over gap, marked sentence) and `ResponseView` (choices, tiles, text field, microphone).
- Blurred text reuses the reader's practice blur (`ReadingText` practise mode).
- Matching pairs and build the sentence get their own composables.
- The result line names what was asked when it was a form or a meaning ("*esqueci* — past, I").
- `PracticeKindsDialog` becomes `PracticeSetupDialog` (3.3), opened from every place that starts a practice: the reader footer (page and whole text), the vocabulary selection, and the end of a lesson.
- The vocabulary list shows each word's practice level as a small four-step bar in the term row's tooltip and on the term page.
- Desktop keys: 1–4 for choices, letters for tiles, Enter to submit, Space to replay audio (already partly there).

## 9. Speech recognition (phase 5)

Speaking is an answer type, not a group of questions: any GAP, AUDIO or TRANSLATION question at level 3 may be answered by voice where recognition is available.

| Platform | Engine | Download |
|---|---|---|
| Android | `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE`; sherpa-onnx streaming recogniser where the system has no offline pack | System pack, or sherpa model ~50–150 MB per language |
| iOS | `SFSpeechRecognizer` with `requiresOnDeviceRecognition` | System |
| Desktop | `ManagedPython` with faster-whisper (tiny or base, multilingual) | ~75–150 MB, set up from the Speech settings like Piper |
| Web | Not offered | — |

Checking: the learner says the whole sentence; it is right when the recognised words contain the answer (via `PracticeChecker`, accents ignored). Single words alone are not asked, because recognition of a lone word is unreliable.

Shadowing (hear a sentence, repeat it, see which words were recognised) comes with it as its own kind in the Listening group.

Permissions: microphone on Android and iOS, asked the first time a speaking question comes up; declining switches the question to TYPE.

## 10. Phases

Each phase ends with the full build passing and is committed in small commits.

### Phase 1 — model, translation prompts and the setup dialog

- `PracticeContext`, `PracticePrompt`, `PracticeResponse`, `ExerciseType`, `PracticeSetup` in `core/domain/practice`; `Exercise` carries them; old kinds map onto them.
- MEANING and TRANSLATION prompts (5.2, 5.4).
- `PracticePlanner.preview` and `PracticeMaterialLoader`; `PracticeBuilder.build` takes a `PracticeSetup`.
- `PracticeSetupDialog` (3.3) with skills, answer modes, sentence sources, length, presets and the remembered setup; the Words filter offers "All being learned" and "Only these" until phase 4 adds levels; route carries the setup.
- Tests: planner counts and reasons for each type, builder (each prompt's answer, options and fallbacks), checker, setup serialisation and the old-kinds mapping, dialog on phone and desktop (ticks, greyed types, summary, presets, remembered setup), a practice screen test per prompt.

### Phase 2 — stored examples and forms

- Migration 26 with both tables of 4.1; `PracticeExampleStore`, fetching when a word is saved and before practice.
- `WordForms` (4.3), `selectFormsOf`, the pack index in `tools/build_dictionary.py`.
- EXAMPLE context with form-aware AUDIO questions (5.3).
- Better choices (5.5) with frequency ranks.
- Tests: store (filters, form detection, offline), forms from each source, AUDIO wording for another form.

### Phase 3 — new kinds

- Choose the form (6.1), matching pairs (6.2), build the sentence (6.3), letter tiles (5.6), longer dictation with a diff (6.4), true or false (6.6), sound-alike (6.5).
- `TYPO` in the checker.
- Tests: generation rules of each kind, screen tests, screenshots on phone and desktop in light and dark.

### Phase 4 — the ladder

- Level updates after each answer; the builder picks responses by level; the "Adapt to my level" switch and the level-based Words filters (New to practice, Weakest, Not practised for a week) in the setup dialog.
- The level shown in the vocabulary.
- Tests: level moves for every row of 3.2, builder output at each level.

### Phase 5 — speaking

- `SpeechRecognition` expect/actual with the engines of section 9; setup in Speech settings; microphone permission.
- SPEAK response; shadowing kind.
- Tests: checker on recognised text; screen tests with a fake recogniser; manual checks on an Android emulator, the iOS simulator and the desktop app.

### Later

Word partners (6.7), gender and plural (6.8).

## 11. Risks and open questions

- **Dictionary forms without tags.** The offline packs know which forms belong together but not which tense or person each is. Without Wiktionary online, choose-the-form cannot avoid a wrong option that also fits; the sentence translation is the cue, and the form must not be accepted elsewhere (7). Decide whether to bundle tagged forms in the packs instead.
- **Example translations.** Tatoeba gives human translations only for some language pairs; the rest are machine translated offline and may be rough. Mark machine translations with the engine's name, as the reader does.
- **Tatoeba licence.** CC-BY 2.0 FR: keep the sentence id and show the credit on example-based questions.
- **Recognition models.** Sizes and quality differ per language; some languages will have no offline recogniser.
- **Too many choices for the learner.** Presets and collapsed sections keep the first view short, and the dialog never shows a type it cannot ask without saying why; check with screenshots of the phone sheet and the desktop dialog at the end of phase 1 and again in phase 3, when the new types arrive.

## 12. Out of scope

- LLM-written questions, explanations or hints.
- Grammar lessons; practice stays about the words saved.
- Scoring pronunciation beyond whether the word was recognised.

## 13. Implementation status

Done, each with tests and the full build passing:

- **Phase 1:** exercise types, answer modes, sentence sources and a `PracticeSetup` in place of the five fixed kinds (old route kinds still read); the meaning, translation and heard-sentence questions; `PracticePlanner`; the setup dialog with presets, skills, answer modes, sentences, length, own presets and the setup remembered per reader and vocabulary.
- **Phase 2:** `practice_examples` and `practice_progress` (migration 26); `WordForms` from the frequency lists' word groups and the learner's linked forms, rather than the dictionary packs' untagged forms table; `PracticeExampleStore` looking sentences up when a word starts being learned and before practice; questions in kept sentences in any form of the word; wrong choices of similar frequency and spelling; Tatoeba credited under its sentences.
- **Phase 3:** typo tolerance (not within a word's last two letters, where inflections change it); letter tiles; the right form (Word forms skill); matching pairs; building the sentence; dictation of up to eight words with a word-by-word check; true or false; sound-alike.
- **Phase 5:** `SpeechRecognition` with a `SPEAK` answer for the missing word, the translation and the heard sentence (right when the word is heard in what is said, accents aside) and a Shadowing type (say the sentence back; all its words heard is right, 80 % almost); a Speaking preset; speaking offered only where the language can be heard, and typed when the microphone is refused or by choice. Android uses the system recogniser (offline first) and asks for the microphone on the first Speak; iOS uses `SFSpeechRecognizer` on the device; the desktop records until a pause and transcribes with faster-whisper's small model in the speech worker, installed from the Speech settings.
- **Phase 4:** `WordProgress` levels with the rules of 3.2, recorded one answer at a time; "Adapt to my level"; the Words filter (all, new, weakest, not practised for a week); the level on the word's page (not in the list's row tooltip).

Changed from the plan:

- Forms come from the frequency lists (offline, 33 languages) and linked terms; Wiktionary's tagged forms are not used, so choose-the-form relies on the sentence's translation as its clue.
- Matching pairs and true-or-false moves no level; matching counts as one question.
- Build the sentence is answered with tiles only.
- Speaking sits at the top practice level beside typing, and is left out of the Balanced and Listening presets: it needs a quiet room and a recogniser, so it is asked for by name.
- The desktop worker decodes the recording itself: faster-whisper's own decoder fails with current PyAV.
- Not tried on hardware yet: real speech on Android (the emulator dies opening the Mac's microphone) and iOS (compiled only).

Not done yet: word partners (6.7), gender and plural (6.8).

