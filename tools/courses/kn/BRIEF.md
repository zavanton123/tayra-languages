# Writing a Kannada frequency course

Each course teaches one band of 100 words from the Kannada word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `kn-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 7,036 words: the last course is `kn-mini-7100`
(36 words).

## The list

The list is counted from Kannada Wikipedia (Leipzig Corpora Collection, 300,000 sentences), the
only Kannada text on disk, by the analyser in `tools/frequency_kannada.py`: every corpus word is
read back to its dictionary form (a noun's nominative, a verb's stem), and the forms are listed
after it. So:

- **The ranks lean to the encyclopaedia.** Words of reports and articles rank high (ಶತಮಾನ 105,
  ಪ್ರಶಸ್ತಿ 110, ಚಲನಚಿತ್ರ 159, ಜಿಲ್ಲೆ 134), everyday words lower than in speech (ಮನೆ 145, ಏನು 626,
  ನೀನು 953). Write everyday stories anyway: a high-ranked formal word goes into an ordinary scene
  (a prize at school, a film on Saturday, a district bus).
- **Verbs are listed by their stem** (ಮಾಡು, ಬರೆ, ಹೋಗು, ಇರು), nouns by the nominative (ಮನೆ, ಮರ,
  ನೀರು); `newWords` holds the headword as the band has it. The checker reads every regular form back
  to its word by the same rules (ಮನೆಯಲ್ಲಿ, ಮರದ, ನೀರಿನ, ಮಾಡುತ್ತಾಳೆ, ಬರೆದನು, ಹೋಗಿದ್ದಾರೆ, ಮನೆಗೂ,
  ಅವನೇ), so any correct form of a known word counts.
- **Many words have no entry in `dictionaries/kn-en.sqlite`** (the pack is small). Check the ones
  that have one (WRITER-BRIEF step 6); teach the others in their plain, common sense.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *ರಾಹುಲ್‌ನ ಫೋನ್ ಎಲ್ಲಿದೆ? ಅವನು ಮನೆಯಲ್ಲಿ ನೋಡುತ್ತಾನೆ. ಫೋನ್ ಅಲ್ಲಿ ಇಲ್ಲ.*
  Everyday life where Kannada is spoken: Bengaluru, Mysuru, Mangaluru, Hubballi, Dharwad,
  Belagavi, Udupi, Shivamogga, Davanagere, Ballari, Kalaburagi, Hassan, Chikkamagaluru, Madikeri,
  Hampi, Karwar; the Kaveri, Tungabhadra and Krishna rivers, the Western Ghats, the coast, coffee
  estates and village fields; and now and then Mumbai, Chennai, Hyderabad, Dubai, London or the
  United States, where Kannada speakers live: home, family, food, shopping, work, school, the town,
  health, free time, travel, festivals and the stories of the themes.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, jumps in time, irony or open
  endings. Events in the order they happen. Few characters (one to three), named clearly; say who
  ಅವನು / ಅವಳು / ಅವರು is whenever it could be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order
  subject, object, verb (*ಸುಮಾ ಕಾಫಿ ಕುಡಿಯುತ್ತಾಳೆ.*), the verb last. A copula-less sentence for
  "X is Y" (*ಇದು ಒಂದು ನಗರ. ಮನೆ ದೊಡ್ಡದು.*); ಇದೆ / ಇದ್ದಾನೆ for "is there", ಅಲ್ಲ for "is not
  (a) Y", ಇಲ್ಲ for "is not there".
- **Politeness**: ನೀನು (with the singular verb) among friends and family of the same age or
  younger; ನೀವು (with the plural verb) to elders, strangers, in shops and offices. A respected adult
  is ಅವರು / ಇವರು with the plural verb all through the story (*ಪ್ರಕಾಶ್ ಕೆಲಸ ಮಾಡುತ್ತಾರೆ.*); a
  child or a friend is ಅವನು / ಅವಳು. Keep a character's form the same in narration and questions.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present-future (*ಮಾಡುತ್ತಾನೆ, ಮಾಡುತ್ತಾಳೆ, ಮಾಡುತ್ತಾರೆ, ಮಾಡುತ್ತದೆ, ಮಾಡುತ್ತೇನೆ*);
    ಇರು (*ಇದೆ, ಇವೆ, ಇದ್ದಾನೆ, ಇದ್ದಾರೆ*) and its past (*ಇದ್ದನು / ಇದ್ದ, ಇದ್ದಳು, ಇದ್ದರು, ಇತ್ತು*);
    the cases: accusative *-ಅನ್ನು*, dative *-ಗೆ / -ಇಗೆ / -ಕ್ಕೆ*, locative *-ಅಲ್ಲಿ*, genitive
    (*ಮನೆಯ, ಮರದ, ನೀರಿನ, ಅವನ*), ablative *-ಇಂದ*; *ಜೊತೆ, ಬಗ್ಗೆ, ಮೇಲೆ*; ಅಲ್ಲ and ಇಲ್ಲ; *ಬೇಕು*
    and *ಇಷ್ಟ* with the dative (*ಸುಮಾಗೆ ನೀರು ಬೇಕು*); questions with ಏನು, ಯಾರು, ಯಾವ, ಎಲ್ಲಿ, ಏಕೆ,
    ಎಷ್ಟು and the clitic *-ಏ / -ಆ* (*ಇದೆಯೇ?*); *-ಊ* "also" and *-ಏ* "just, the very" (*ಮನೆಗೂ,
    ಅವನೇ*);
  - ranks 301–600: the simple past (*ಮಾಡಿದನು, ಮಾಡಿದಳು, ಮಾಡಿದರು, ಮಾಡಿತು, ಹೋದನು, ಬಂದಳು*); the
    plural *-ಗಳು* and its cases; the past participle that joins actions (*ಊಟ ಮಾಡಿ ಮಲಗುತ್ತಾನೆ*);
    the infinitive *-ಅಲು* (*ಓದಲು ಬರುತ್ತಾಳೆ*); imperatives (*ಬಾ / ಬನ್ನಿ, ನೋಡು / ನೋಡಿ*);
    *ಹತ್ತಿರ, ಮುಂದೆ, ಹಿಂದೆ, ಒಳಗೆ, ಹೊರಗೆ*;
  - ranks 601–1000: the verb negatives (*ಮಾಡುವುದಿಲ್ಲ, ಮಾಡಲಿಲ್ಲ, ಮಾಡಬೇಡ*); the relative participles
    (*ಓದುವ ಹುಡುಗ, ಬಂದ ಜನ*); *ಎಂದು* after a quotation or a thought (*"ಬರುತ್ತೇನೆ" ಎಂದು ಹೇಳಿದಳು*);
    *-ಬಹುದು, -ಬೇಕು* with the infinitive stem (*ಹೋಗಬಹುದು, ಮಾಡಬೇಕು*); comparison with *-ಇಂತ*
    (*ಮೈಸೂರಿಗಿಂತ ದೊಡ್ಡದು*);
  - ranks 1001–2000: the present and past progressive (*ಮಾಡುತ್ತಿದ್ದಾನೆ, ಮಾಡುತ್ತಿದ್ದಳು*); the perfect
    (*ಬಂದಿದ್ದಾಳೆ, ನೋಡಿದ್ದರು*); *-ಆಗ* "when" (*ಮನೆಗೆ ಬಂದಾಗ*); the conditional *-ಅರೆ* and the
    concessive *-ಅರೂ* (*ಮಳೆ ಬಂದರೆ, ಕೇಳಿದರೂ*); the verbal noun *-ವುದು* (*ಓದುವುದು ಇಷ್ಟ*); *ಕೊಳ್ಳು*
    and *ಬಿಡು* after the participle (*ಕಲಿತುಕೊಂಡಳು, ತಿಂದುಬಿಟ್ಟನು*);
  - ranks 2001–3000: longer joined sentences (*ಆದರೆ, ಆದ್ದರಿಂದ, ಏಕೆಂದರೆ, ಆದರೂ*); causatives in
    *-ಇಸು* (*ಕಲಿಸು, ತೋರಿಸು*); the written passive *-ಲಾಗುತ್ತದೆ / -ಲಾಯಿತು* where Kannada prose uses
    it (*ಇಲ್ಲಿ ಕಾಫಿ ಬೆಳೆಯಲಾಗುತ್ತದೆ*); *-ಅಂತೆ, ಹಾಗೆ, ರೀತಿಯಲ್ಲಿ*; reported speech with *ಎಂದು*;
  - ranks 3001–5000: a richer written register: the Sanskrit-derived words of newspapers and books
    beside their everyday ones, made plain by context; a column, a letter, an interview, a short
    essay that weighs two sides; long sentences that stay easy to parse;
  - ranks 5001–7100 (C1): the same, with the specialised words of the sciences, the arts and
    public life, each made clear by its context.
  After rank 1000 sentences get longer and dialogue can carry more of the story; a story may be told
  by a first-person narrator or as a letter, a message or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text, as long as it is calm and easy to follow.
- **Repetition on purpose**: the new words three times or more where it reads naturally (the
  checker needs two), the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `ಪ್ರಶ್ನೆಗಳು:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  ಪ್ರಶ್ನೆಗಳು:
  ಸುಮಾ ಈಗ ಯಾವ ನಗರದಲ್ಲಿ ಇದ್ದಾಳೆ?
  ಸುಮಾ ಈಗ ಮೈಸೂರಿನಲ್ಲಿ ಇದ್ದಾಳೆ.
  ```

  The header `ಪ್ರಶ್ನೆಗಳು:` is layout: the app and the checker do not count it as a word.
- **Standard written Kannada**: the narration, the dialogue and the questions use the written
  standard of books and newspapers, never the spoken contractions (*ಮಾಡುತ್ತಾನೆ*, not *ಮಾಡ್ತಾನೆ*;
  *ಹೋಗುತ್ತೇನೆ*, not *ಹೋಗ್ತೀನಿ*; *ಏನು*, not *ಏನ್*), and no regional dialect. English loans Kannada
  has made its own (*ಬಸ್, ಫೋನ್, ಶಾಲೆ, ಕಾಲೇಜು, ಡಾಕ್ಟರ್*) are fine in Kannada script; never write
  English in Latin letters.
- **Spelling**: write the list's spelling (*ಸರ್ಕಾರ*, *ಜೊತೆ*, *ಕಂಪನಿ*, *ಮುಂದುವರಿ*), not the
  variants it files under them (*ಸರಕಾರ, ಜತೆ, ಕಂಪೆನಿ, ಮುಂದುವರೆ*). An ending on a word that ends in a
  consonant with virama is written after a zero-width non-joiner, as Kannada typing does
  (*ಜೂನ್‌ನಲ್ಲಿ, ರಾಹುಲ್‌ನ, ಬಸ್‌ಗೆ*): the reader keeps the joiner inside the word.
- **Kannada typography**: a sentence ends with a full stop, `?` or `!`; direct speech in double
  quotation marks, each speaker's line a paragraph of its own. Write numbers as Western digits
  (*3 ಮಕ್ಕಳು, 5 ಗಂಟೆಗೆ, 2019ರಲ್ಲಿ*), never as Kannada digits (*೩*, which the reader reads as a word),
  except a number word that is in your band: spell that one out to teach it (*ಎರಡು ಮಕ್ಕಳು*). A
  hyphen splits a word in two for the reader and the checker; write compounds as Kannada writes them.
- **Names are names**: Kannada has no capital letters, so place names land in the list (ಭಾರತ 35,
  ಕರ್ನಾಟಕ 177, ಬೆಂಗಳೂರು 366, ಜರ್ಮನಿ 592). A country, state or city name may be taught as what it
  is (ಭಾರತ "India"); language names are everyday words: teach them (ಕನ್ನಡ, ಇಂಗ್ಲಿಷ್, ಹಿಂದಿ). A
  list word that is also a person's name (ಕೃಷ್ಣ, ಸೂರ್ಯ "sun", ಹರಿ "flow") is taught only in its
  common sense or skipped, never through the name. Never put a list word in `names`, and never use
  one as a character's name. Common Kannada first names are fine (ಸುಮಾ, ರಮೇಶ್, ಲತಾ, ಕಿರಣ್).
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any everyday, cultural or story topic is fine
(festivals and temples, history, kings and queens, myths and folk tales, politics, cricket). Avoid
only explicit sexual content, slurs used as insults and gratuitous gore. Teach ordinary words in
their main sense. Skip list noise: fragments, abbreviations and words of other languages.

## The file

Write `tools/courses/kn/<id>.json`, the shape of `kn-mini-0100.json`:

```json
{
  "id": "kn-mini-0200",
  "rankUpTo": 200,
  "level": "A1",
  "title": "The title from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["ಸುಮಾ", "ಮೈಸೂರು"],
  "lessons": [
    {
      "title": "story title in Kannada",
      "summary": "One line in English on what happens.",
      "newWords": ["the band headwords this lesson introduces"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

`names` holds every person and place name the lessons use, in the nominative (ಮೈಸೂರು, not
ಮೈಸೂರಿನಲ್ಲಿ): the checker reads its case forms.

## The rules, checked by `python3 tools/check_course.py tools/courses/kn/<id>.json`

- **Vocabulary**: 90% (ranks up to 100), 93% (to 300) or 95% (above) of a lesson's words are ranked
  within the course's `rankUpTo`; names count as known.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson. Reuse earlier courses' words freely.
- **Length** (story and questions together, in words as the reader cuts them): 80–180 to rank 100,
  120–240 to 300, 180–320 to 600, 220–380 to 1000, 260–450 to 2000, 300–520 to 3000, 320–550 to
  4000, 350–600 after. Kannada words carry their endings, so a Kannada lesson has fewer words than
  its English translation: the length is in Kannada words.
- **Coverage of the band**: teach 80–100 of the band's words; the checker's last line lists the band
  words not taught yet.
