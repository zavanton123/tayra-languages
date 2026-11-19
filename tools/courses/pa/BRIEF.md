# Writing a Punjabi frequency course

Each course teaches one band of 100 words from the Punjabi word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `pa-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 4,269 words: the last course is `pa-mini-4300`
(ranks 4201–4269).

Punjabi is written here in Gurmukhi (ਗੁਰਮੁਖੀ), the script of East Punjab, never in Shahmukhi.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *ਹਰਪ੍ਰੀਤ ਦਾ ਫ਼ੋਨ ਕਿੱਥੇ ਹੈ? ਉਹ ਘਰ ਵਿੱਚ ਵੇਖਦੀ ਹੈ। ਫ਼ੋਨ ਉੱਥੇ ਨਹੀਂ ਹੈ...*
  Everyday life where Punjabi is spoken: Amritsar, Ludhiana, Jalandhar, Patiala, Bathinda, Moga,
  Sangrur, Hoshiarpur, Kapurthala, Chandigarh; villages, fields of wheat and mustard, canals, the
  Sutlej and the Beas; and now and then Delhi, Lahore, or the diaspora in Toronto, Vancouver,
  Brampton, Birmingham, Southall or Melbourne: home, family, food, shopping, work, school, the
  town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *ਉਹ* is whenever it could be
  unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, object, verb: *ਸਿਮਰਨ ਚਾਹ ਬਣਾਉਂਦੀ ਹੈ।*), the verb last. Little dialogue; when there is
  some, short and simple, with *ਤੂੰ* among close family and friends of the same age or younger, and
  *ਤੁਸੀਂ* (with the plural verb: *ਤੁਸੀਂ ਕਿੱਥੇ ਜਾ ਰਹੇ ਹੋ?*) with parents and elders, strangers, in shops
  and offices; *ਜੀ* after a name or a kinship word for respect (*ਮਾਂ ਜੀ*, *ਗਿੱਲ ਸਾਹਿਬ*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *ਹੈ*, *ਹਨ*, *ਹਾਂ*, *ਹੋ* and *ਸੀ*, *ਸਨ*; the present habitual (*ਉਹ ਕੰਮ ਕਰਦਾ ਹੈ*,
    *ਉਹ ਪੜ੍ਹਦੀ ਹੈ*); the present progressive (*ਉਹ ਖਾ ਰਿਹਾ ਹੈ*, *ਉਹ ਜਾ ਰਹੇ ਹਨ*); the simple past of verbs
    without an object (*ਉਹ ਘਰ ਗਿਆ*, *ਸਿਮਰਨ ਆਈ*) and, with *ਨੇ*, of verbs with one (*ਹਰਪ੍ਰੀਤ ਨੇ ਚਾਹ
    ਬਣਾਈ*, *ਉਸਨੇ ਕਿਹਾ*), the verb agreeing with the object; gender and number agreement in every form
    (*ਵੱਡਾ ਘਰ*, *ਵੱਡੀ ਗੱਲ*, *ਵੱਡੇ ਲੋਕ*); the postpositions *ਦਾ/ਦੀ/ਦੇ*, *ਵਿੱਚ*, *ਨੂੰ*, *ਤੋਂ*, *ਨਾਲ*, *ਲਈ*,
    *ਤੱਕ*, *ਉੱਤੇ*, *ਬਾਰੇ*, *ਬਾਅਦ*, and the oblique forms before them (*ਘਰ ਵਿੱਚ*, *ਉਸ ਦਿਨ*, *ਮੁੰਡੇ
    ਨੂੰ*); *ਸਕਣਾ* (*ਉਹ ਆ ਸਕਦਾ ਹੈ*); the future in *-ਗਾ* (*ਉਹ ਆਵੇਗਾ*, *ਅਸੀਂ ਚੱਲਾਂਗੇ*); the imperative
    with *ਤੁਸੀਂ* (*ਆਓ*, *ਵੇਖੋ*, *ਦੱਸੋ*); questions with *ਕੀ*, *ਕੌਣ*, *ਕਿੱਥੇ*, *ਕਦੋਂ*, *ਕਿਉਂ*,
    *ਕਿਵੇਂ*, *ਕਿੰਨਾ*;
  - ranks 301–600: the dative-subject phrases (*ਮੈਨੂੰ ਪਤਾ ਹੈ*, *ਉਸਨੂੰ ਪਸੰਦ ਹੈ*, *ਮੈਨੂੰ ਲੱਗਦਾ ਹੈ
    ਕਿ*, *ਉਸਨੂੰ ਭੁੱਖ ਲੱਗੀ*); compound verbs with *ਲੈਣਾ*, *ਦੇਣਾ*, *ਜਾਣਾ* (*ਖਾ ਲਿਆ*, *ਦੱਸ ਦਿਓ*, *ਸੌਂ
    ਗਿਆ*); the conjunctive *-ਕੇ* (*ਖਾਣਾ ਖਾ ਕੇ ਉਹ ਸੌਂਦਾ ਹੈ*); comparatives with *ਤੋਂ* (*ਤੋਂ ਵੱਡਾ*, *ਸਭ
    ਤੋਂ ਚੰਗਾ*); *ਚਾਹੁਣਾ* + infinitive (*ਉਹ ਜਾਣਾ ਚਾਹੁੰਦੀ ਹੈ*); the habitual past (*ਉਹ ਪੜ੍ਹਦਾ ਸੀ*);
  - ranks 601–1000: the past progressive (*ਉਹ ਖਾ ਰਿਹਾ ਸੀ*); obligation (*ਮੈਂ ਜਾਣਾ ਹੈ*, *ਉਸਨੂੰ ਜਾਣਾ
    ਪਿਆ*, *ਤੁਹਾਨੂੰ ਆਉਣਾ ਚਾਹੀਦਾ ਹੈ*); the relative pair *ਜੋ / ਜਿਹੜਾ ... ਉਹ* (*ਜਿਹੜਾ ਮੁੰਡਾ ਆਇਆ, ਉਹ ਮੇਰਾ
    ਭਰਾ ਹੈ*); *ਜਦੋਂ ... ਤਾਂ*, *ਜੇ ... ਤਾਂ* for real plans; reported speech with *ਕਿ* (*ਉਸਨੇ ਕਿਹਾ ਕਿ ਉਹ
    ਆਵੇਗਾ*); the infinitive with *ਵਾਲਾ* (*ਉਹ ਜਾਣ ਵਾਲਾ ਹੈ*);
  - ranks 1001–1300: the present and past perfect (*ਮੈਂ ਵੇਖਿਆ ਹੈ*, *ਉਹ ਜਾ ਚੁੱਕਾ ਸੀ*); *ਲੱਗਣਾ* +
    oblique infinitive (*ਉਹ ਹੱਸਣ ਲੱਗੀ*); the passive with *ਜਾਣਾ* in plain cases (*ਇੱਥੇ ਪੰਜਾਬੀ ਬੋਲੀ
    ਜਾਂਦੀ ਹੈ*); *-ਦਿਆਂ ਹੀ* (*ਘਰ ਪਹੁੰਚਦਿਆਂ ਹੀ*);
  - ranks 1301–1600: the subjunctive (*ਸ਼ਾਇਦ ਉਹ ਆਵੇ*, *ਮੈਂ ਚਾਹੁੰਦਾ ਹਾਂ ਕਿ ਤੁਸੀਂ ਆਓ*, *ਤਾਂ ਜੋ*); the
    presumptive (*ਉਹ ਘਰ ਹੋਵੇਗਾ*); causative verbs (*ਬਣਵਾਉਣਾ*, *ਵਿਖਾਉਣਾ*, *ਪਿਆਉਣਾ*);
  - ranks 1601–2000: unreal conditions with the habitual participle (*ਜੇ ਸਮਾਂ ਹੁੰਦਾ ਤਾਂ ਮੈਂ ਆਉਂਦਾ*),
    indirect questions (*ਪਤਾ ਨਹੀਂ ਉਹ ਕਿੱਥੇ ਹੈ*), participles as adjectives (*ਬੈਠਾ ਹੋਇਆ ਆਦਮੀ*, *ਖੁੱਲ੍ਹੀ
    ਖਿੜਕੀ*), *ਜਦੋਂ ਤੱਕ ... ਨਹੀਂ*;
  - ranks 2001–3000: longer joined sentences (*ਕਿਉਂਕਿ*, *ਇਸ ਲਈ*, *ਫਿਰ ਵੀ*, *ਸਗੋਂ*, *ਜਦਕਿ*,
    *ਹਾਲਾਂਕਿ*), the more formal words of newspapers and books (*ਪ੍ਰਾਪਤ*, *ਅਨੁਸਾਰ*, *ਦੁਆਰਾ*) beside
    their everyday ones (*ਮਿਲਣਾ*, *ਮੁਤਾਬਕ*, *ਵੱਲੋਂ*); idioms (*ਮੁਹਾਵਰੇ*) explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (a light reflection on everyday life);
  - ranks 4001–4269 (C1): everything before, with more abstract and nuanced content: a short essay
    or reflective column that weighs two sides, an interview, a portrait, a review.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story
  may be told by a first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a
  lesson may also be a short informative or opinion text, as long as it is everyday, calm and easy to
  follow.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `ਸਵਾਲ:` with 4–6 simple questions
  about the story, each followed on its own line by a full-sentence answer that reuses the story's
  words:

  ```
  ਸਵਾਲ:
  ਹਰਪ੍ਰੀਤ ਕੀ ਲੱਭਦੀ ਹੈ?
  ਹਰਪ੍ਰੀਤ ਆਪਣਾ ਫ਼ੋਨ ਲੱਭਦੀ ਹੈ।
  ```

  The header `ਸਵਾਲ:` is layout: the checker does not count it as a word.
- **Standard Punjabi**: the narration is the standard written Punjabi of books and newspapers in
  East Punjab (Majhi-based), neutral, natural and everyday; not the Hindi-heavy or Sanskritised
  register, nor a rural dialect (*ਸਾਡੇ ਕੋਲ*, not *ਸਾਡੇ ਕੋਲੇ*; *ਹੈ*, never the spoken *ਆ* for it). English
  loanwords Punjabi has made its own (*ਫ਼ੋਨ*, *ਬੱਸ*, *ਸਕੂਲ*, *ਡਾਕਟਰ*, *ਟੀਵੀ*) are fine; never write
  English in Latin letters. No slang.
- **Spelling**: write the standard spelling.
  - Addak (ੱ) where the consonant is doubled (*ਵਿੱਚ*, *ਇੱਕ*, *ਗੱਲ*, *ਕੱਲ੍ਹ*, *ਉੱਤੇ*); tippi (ੰ) with
    a short vowel or none (*ਕੰਮ*, *ਪਿੰਡ*, *ਮੁੰਡਾ*, *ਨੂੰ*), bindi (ਂ) with the other vowel signs (*ਮਾਂ*,
    *ਨਹੀਂ*, *ਮੈਂ*, *ਹਾਂ*, *ਕਿਉਂ*).
  - Nukta (਼) in the Persian, Arabic and English loans that have it (*ਖ਼ੁਸ਼*, *ਜ਼ਰੂਰ*, *ਗ਼ਲਤ*, *ਫ਼ੋਨ*,
    *ਜ਼ਿਆਦਾ*), and always in *ਸ਼* (*ਸ਼ਹਿਰ*, *ਸ਼ਾਮ*). Write *ਲ* without the dot (*ਨਾਲ*, *ਕਾਲਾ*, *ਵਾਲਾ*), as
    most printed Punjabi does; the list keeps *ਲ਼* spellings as forms.
  - **Type the nukta as its own sign after the letter** (ਸ + ਼, ਜ + ਼), never the single-character
    letters ਸ਼ ਖ਼ ਗ਼ ਜ਼ ਫ਼ ਲ਼ (U+0A33, U+0A36, U+0A59–U+0A5E): the list and the dictionary have the text
    in NFC, where those are two characters, and the checker rejects the one-character ones.
- **Punjabi typography**: a sentence ends with the danda `।`, never a full stop (the app's reader
  keeps a full stop after some words, such as *ਸੀ.* and *ਈ.*, as part of the word, and the checker
  flags it); `?` and `!` as usual. Direct speech in double quotation marks “…”, each speaker's line
  a paragraph of its own. Write numbers as Western digits (*3 ਭਰਾ*, *5 ਵਜੇ*, *2019 ਵਿੱਚ*), never as
  Gurmukhi digits, except a number word that is in your band: spell that one out to teach it (*ਦੋ
  ਭਰਾ*). A hyphen splits a word in two for the reader and the checker (*ਮਾਂ-ਪਿਓ* is *ਮਾਂ* + *ਪਿਓ*).
- **Names**: people, places, shops and brands go in `names`. Never use a list word as a character's
  name: *ਸਿਮਰਨ* (1650), *ਅਮਨ* (1106, "peace"), *ਕਿਰਨ* (1130), *ਰਾਣੀ* (617), *ਅਰਸ਼* are list words. Safe:
  *ਹਰਪ੍ਰੀਤ*, *ਗੁਰਪ੍ਰੀਤ*, *ਜਸਪ੍ਰੀਤ*, *ਮਨਦੀਪ*, *ਨਵਜੋਤ*, *ਰਮਨ*, *ਬਲਜੀਤ*, *ਕੁਲਦੀਪ*, *ਹਰਜੀਤ*, *ਪਰਮਜੀਤ*,
  *ਗੁਰਮੀਤ*, *ਮਨਜੀਤ*, *ਨੀਲਮ*, *ਪ੍ਰੀਤੀ*, *ਜਸਲੀਨ*, *ਸੁਖਪ੍ਰੀਤ*, *ਰਾਜਵਿੰਦਰ*, *ਸੁਖਵਿੰਦਰ*, *ਤੇਜਿੰਦਰ*,
  *ਬਿੱਟੂ*, *ਪਿੰਕੀ*; *ਅੰਮ੍ਰਿਤਸਰ*, *ਲੁਧਿਆਣਾ*, *ਜਲੰਧਰ*, *ਪਟਿਆਲਾ*, *ਬਠਿੰਡਾ*, *ਮੋਗਾ*, *ਸੰਗਰੂਰ*,
  *ਹੁਸ਼ਿਆਰਪੁਰ*, *ਕਪੂਰਥਲਾ*, *ਚੰਡੀਗੜ੍ਹ*, *ਦਿੱਲੀ*, *ਲਾਹੌਰ*, *ਟੋਰਾਂਟੋ*, *ਲੰਡਨ*. Surnames that are list
  words (*ਸਿੰਘ* and *ਕੌਰ* are left out of the list, as names) may be used as names. Check a name's rank
  with `rank.py` before you use it.
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

No topic rules (user decision of 2026-11-13; see `tayra-writers/WRITER-BRIEF.md` step 4): any
normal everyday, cultural or story topic is fine, gurdwaras, Vaisakhi, Lohri, Diwali and Eid,
weddings, history, kings and sport included. Avoid only explicit sexual content, slurs used as
insults and gratuitous gore. Teach ordinary words in their main sense. Days of the week, months,
seasons and language names are everyday words: teach them where the list has them (*ਪੰਜਾਬੀ* 63,
*ਹਿੰਦੀ* 310, *ਮਈ* 337, *ਸੋਮਵਾਰ* 1333); the list lacks *ਜਨਵਰੀ*, *ਦਸੰਬਰ*, *ਐਤਵਾਰ*, *ਅੰਗਰੇਜ਼ੀ* and
*ਉਰਦੂ* (the dictionary files them as names), so put those in `names` when a story needs them.

## The file

Write `tools/courses/pa/<id>.json`:

```json
{
  "id": "pa-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Punjabi, e.g. ਖਾਣਾ ਅਤੇ ਖ਼ਰੀਦਦਾਰੀ",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Punjabi",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/pa/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *ਗਿਆ* or *ਜਾਂਦਾ* for *ਜਾਣਾ*, *ਵੱਡੀ* for *ਵੱਡਾ*). `newWords`
  holds the word exactly as the band writes it: the infinitive of a verb (*ਵੇਖਣਾ*), the masculine
  singular of an adjective (*ਵੱਡਾ*), the direct singular of a noun (*ਮੁੰਡਾ*), and the spelling the list
  chose (*ਫਿਲਮ*, *ਖ਼ੁਸ਼ੀ*, *ਵਾਲ਼ਾ*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after. Punjabi writes postpositions and
  auxiliaries as words of their own (*ਘਰ ਵਿੱਚ*, *ਜਾ ਰਿਹਾ ਹੈ*): keep stories short.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a list error such as a name, a form of another word, noise); the
  checker's last line lists the band words not taught yet.

## Things to know about the Punjabi list and the checker

- The courses are pinned to `tools/courses/pa/wordlist.tsv` (a copy of the app's `pa.tsv`); the
  checker reads it instead of the app's list. The list is counted from Leipzig's sentences of the
  Punjabi Wikipedia (300,000) and of Indian Punjabi web pages (100,000): written, often formal
  words rank high (*ਸਰਕਾਰ* 86, *ਖੇਤਰ* 81, *ਅਨੁਸਾਰ* 98, *ਸਾਹਿਤ*), and everyday ones low: *ਮਾਂ* 161,
  *ਭਰਾ* 223, *ਕੁੜੀ* 279, *ਭੈਣ* 343, *ਮੁੰਡਾ* 461, *ਦੋਸਤ* 464, *ਚਾਹ* 614, *ਦੁੱਧ* 623, *ਕਮਰਾ* 740,
  *ਰੋਟੀ* 819, *ਕੁੱਤਾ* 987, *ਬਿੱਲੀ* (as *ਬਿੱਲਾ*) 1142, *ਕਿੱਥੇ* 1174, *ਕਦੋਂ* 1223.
- Every list word has an entry in the pa-en dictionary pack (English Wiktionary), which is small:
  common words it lacks are not in the list at all (*ਪਰਿਵਾਰ* is, but not *ਦੌਰਾਨ*, *ਸ਼ਾਮਲ*, *ਸਿਰਫ*,
  *ਹਿੱਸਾ*, *ਲਗਭਗ*, *ਦਾਲ*, *ਪਾਪਾ*, *ਬੇਟੀ*). Such a word counts as outside the range at every level:
  write around it.
- **How the checker cuts words**, as the app's reader does: at spaces, punctuation, the danda and
  hyphens; vowel signs, tippi, bindi, addak and nukta are part of the word. A word is matched exactly;
  the list holds the usual other spellings as forms (*ਵਿਚ* of *ਵਿੱਚ*, *ਫਿਲਮ* and *ਫ਼ਿਲਮ*).
- **Postpositions are entries of their own**: *ਦਾ* 1 holds *ਦੀ*, *ਦੇ*, *ਦੀਆਂ*; *ਵਿੱਚ* 3 (and *ਚ*),
  *ਨੂੰ* 8, *ਤੋਂ* 10, *ਨੇ* 11, *ਨਾਲ* 13, *ਲਈ* 14, *ਤੱਕ* 34, *ਉੱਤੇ* 43, *ਵਿੱਚੋਂ* 49, *ਵੱਲੋਂ* 51,
  *ਬਾਰੇ* 69, *ਬਾਅਦ* 31. *ਤੇ* is filed under *ਅਤੇ* 4 ("and"); use it for "and", and *ਉੱਤੇ* for "on".
- **The copula** is *ਹੋਣਾ* 2: *ਹੈ*, *ਹਨ*, *ਹਾਂ*, *ਹੋ*, *ਸੀ*, *ਸਨ* are its forms, so teaching *ਹੋਣਾ*
  in the first lesson is natural.
- **Pronouns** hold their oblique and possessive forms: *ਮੈਂ* 64 holds *ਮੈਨੂੰ*, *ਮੇਰਾ*, *ਮੇਰੀ*, *ਮੇਰੇ*;
  *ਉਹ* 7 holds *ਉਸ*, *ਉਨ੍ਹਾਂ*, *ਉਸਨੇ*, *ਉਸਨੂੰ*, *ਉਸਦਾ*; *ਇਹ* 5 holds *ਇਸ*, *ਇਨ੍ਹਾਂ*, *ਇਸਦਾ*; *ਅਸੀਂ*
  67 holds *ਸਾਨੂੰ*, *ਸਾਡਾ*; *ਤੁਸੀਂ* 158 holds *ਤੁਹਾਨੂੰ*, *ਤੁਹਾਡਾ*; *ਜੋ* 15 holds *ਜਿਸ*, *ਜਿਨ੍ਹਾਂ*;
  *ਕੋਈ* 25 holds *ਕਿਸੇ*; *ਆਪ* 18 holds *ਆਪਣਾ*. Texts write the oblique pronoun and *ਨੇ*, *ਨੂੰ*, *ਦਾ*
  apart or joined (*ਉਸ ਨੇ*, *ਉਸਨੇ*): both count.
- **Verbs** are listed by their infinitive in *-ਣਾ* / *-ਨਾ* with their forms, the stem and the
  oblique infinitive too (*ਕਰ*, *ਕਰਨ* of *ਕਰਨਾ*; *ਜਾਣ* is shared by *ਜਾਣਾ* and *ਜਾਣਨਾ*). Some nouns
  spelled like a stem are filed under the verb (*ਖੋਜ* under *ਖੋਜਣਾ*, *ਸਮਝ* under *ਸਮਝਣਾ*, *ਪਹੁੰਚ*
  under *ਪਹੁੰਚਣਾ*), others are entries of their own (*ਖੇਡ* 259, *ਸੋਚ*, *ਮਾਰ*, *ਡਰ*, *ਜਿੱਤ*, *ਹਾਰ*):
  check with `rank.py`, and count a new word's uses in forms that are its own.
- **Homographs**: *ਖਾਣਾ* "food" and "to eat"; *ਸੋਨਾ* "gold"; *ਗੱਲ* "matter, talk"; *ਪਰ* "but";
  *ਜੀ* (respect, "yes"); *ਆਮ* "common" (and "mango"); *ਬੱਸ* "bus" and "enough"; *ਕੱਲ੍ਹ* "yesterday"
  and "tomorrow". Use a new word in its own sense.
- Forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). It is generated by
  `tayra-writers/pa-writers/pa_extra.py` from the dictionary pack's tables and the regular endings
  (nouns in -ਾ: -ੇ, -ਿਆਂ; feminine -ੀ: -ੀਆਂ; consonant stems: -ਾਂ; adjectives in -ਾ: -ੇ, -ੀ, -ੀਆਂ;
  verbs: participles, perfective, subjunctive, future, imperative). If a true form is missing,
  write around it and name the word and form in your report.
- The list's form lists are noisy (look-alikes, misspellings with a stray addak or nukta): a new word
  must appear as itself or a true inflection, never through such a form. Some entries are noise
  (*ਪੱਦ* holding *ਪਦ*, *ਕੱਪਣਾ* holding *ਕੱਪ*, *ਰਾਹਣਾ* holding *ਰਾਹੀ*): skip them.
