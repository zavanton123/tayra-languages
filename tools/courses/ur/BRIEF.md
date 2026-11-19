# Writing an Urdu frequency course

Each course teaches one band of 100 words from the Urdu word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its forms). Course `ur-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 7,189 words: `ur-mini-0100` to `ur-mini-7200`
(the last band has 89 words).

## The register

**Standard written Urdu of Pakistan (معیاری اردو)**, plain and correct, as graded readers, children's
books and newspapers write it. Dialogue may be everyday spoken Urdu as it is written in stories
(*کیا حال ہے؟*, *جی ہاں*, *اچھا*, *ٹھیک ہے*, *شکریہ*), but no Hindi spellings, no Roman Urdu and no
English mixed into a sentence beyond the loanwords Urdu writes in its own letters (*اسکول*, *فون*,
*ڈاکٹر*, *بس*).

| Point | Write | Not |
|---|---|---|
| "you" | *آپ* to an adult, a stranger, a parent or anyone you address politely (verb in the plural: *آپ جاتے ہیں*); *تم* to a friend, a child or a younger sibling; *تو* only to a small child or in a prayer; one form per pair of speakers | mixing *آپ* and *تم* to the same person |
| Respect | a parent, a teacher or an older person takes the plural verb in narration too: *امی آتی ہیں*, *فیصل کام کرتے ہیں* | *امی آتی ہے* |
| Postpositions | written apart: *گھر میں*, *اس کو*, *ان کے لیے*, *لاہور سے* | joined *اسکو*, *انکے*, *انکا*, *کیلئے* (the list leaves these out) |
| Future | the verb and *گا / گی / گے* apart, as the list counts them: *جائیں گے*, *کروں گی* | *جائینگے*, *کرونگی* |
| Numbers | Urdu digits: *۱۰ سال*, *۷ بجے*, *۲۰۲۴* | *10*, Arabic *١٠* |

**Spelling as Urdu writes it (and as the reader needs it):**

- **Urdu letters**: *ی* (not Arabic *ي* or *ى*), *ک* (not *ك*), *ہ* (gol he) inside and at the end of
  words (*کہنا*, *یہ*, *بچہ*), *ھ* (do-chashmi he) only for aspiration (*بھی*, *تھا*, *گھر*, *کھانا*,
  *ٹھیک*), *ے* (bari ye) at the end where Urdu has it (*کے*, *ہے*, *بچے*), *ں* (noon ghunna) for the nasal
  vowel (*میں*, *ہیں*, *ماں*). The reader keeps *ھ* and *ہ*, *ے* and *ی* apart: *بہی* is not *بھی*.
  The Arabic *ك ي ه* are read as *ک ی ہ*.
- **Hamza**: *آئے*, *جائیں*, *گئی*, *لیے* (or *لئے*), *مسئلہ*; *ۓ* and *ئے* are one letter to the reader.
- **No vowel marks** (zer, zabar, pesh, shadda); *فوراً* keeps its tanvin as everyone writes it, and
  the izafat after *ہ* is *ۂ* (*ذخیرۂ الفاظ*). The checker ignores all marks.
- **Compounds**: write as standard Urdu does; a compound written with a space (*دہشت گرد*, *بات چیت*)
  is two words to the reader and the checker; one written joined (*خوبصورت*, *پاکستانی*) is one word.
- **Punctuation**: the Urdu full stop *۔*, the comma *،*, the question mark *؟*, the exclamation mark.
  Speech after a colon in straight double quotes: *زینب کہتی ہے: "میں ٹھیک ہوں۔"* Each speaker's line is a
  paragraph of its own (or *Name: "..."* lines in a dialogue).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. Everyday life in Pakistan: Lahore, Karachi, Multan, Peshawar, Quetta,
  Hyderabad, Sialkot, Sukkur, a village in Punjab or Sindh, the northern valleys; and now and then
  Urdu-speaking families in Delhi, Lucknow or Hyderabad Deccan, Manchester, Dubai or Toronto.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony or open endings. Events in
  the order they happen. Few characters (one to three), named clearly; *وہ* is "he", "she", "it" and
  "they", so repeat the name wherever *وہ* could be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, Urdu order: subject,
  object, verb at the end (*زینب کتاب پڑھتی ہے۔*). Present tense up to rank 300; from rank 400 the
  story may be told in the past. Dialogue short and simple.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300 (A1): *ہے / ہیں / ہوں / ہو*, *تھا / تھی / تھے*, the habitual present (*وہ کام کرتا ہے*,
    *وہ جاتی ہیں*), the continuous (*کر رہی ہے*), the possessive *کا / کی / کے* agreeing with what is owned,
    *میرا / ہمارا / آپ کا*, postpositions *میں, سے, کو, پر, تک, کے لیے, کے ساتھ, کے بعد, سے پہلے*,
    the simple future (*جاؤں گا*, *آئیں گے*), the polite imperative (*دیکھیں*, *آئیے*) and the familiar
    one (*دیکھو*, *آؤ*), *سکنا* and *چاہنا* (*جا سکتا ہے*, *جانا چاہتی ہے*), questions with *کیا*, *کون*,
    *کہاں*, *کیوں*, *کب*, *کیسے*, *کتنا*; the adjective in *-ا* agreeing (*اچھا لڑکا, اچھی لڑکی, اچھے لوگ*);
  - ranks 301–600 (A2): the perfective past with *نے* (*زینب نے کتاب پڑھی*, *احمد نے کہا*) and the
    intransitive past (*وہ آئی*, *ہم گئے*), the negative *نہیں / نہ / مت*, conjunct verbs with *کرنا* and
    *ہونا* (*کام کرنا*, *شروع ہونا*, *ختم کرنا*), comparison with *سے* (*احمد سے بڑا*, *سب سے اچھا*), *جب ... تو*,
    *اگر ... تو* with the present or future, *کیونکہ*, *اس لیے*; days, months and the time;
  - ranks 601–1000 (A2): the present perfect (*آیا ہے*, *دیکھا ہے*) and the past continuous (*جا رہا تھا*),
    compound verbs with *لینا, دینا, جانا* (*کھا لیا*, *بتا دیا*, *ہو گیا*), the conjunctive participle (*گھر
    جا کر*, *کھانا کھا کے*), the subjunctive after *چاہیے*, *تاکہ*, *شاید* (*آپ کو جانا چاہیے*, *شاید وہ آئے*);
  - ranks 1001–1300 (B1): the past perfect (*وہ جا چکا تھا*, *آیا تھا*), *جو ... وہ* relative clauses, the
    passive with *جانا* (*کہا جاتا ہے*, *بنایا گیا*), *لگنا* with an infinitive (*وہ رونے لگی*);
  - ranks 1301–2000 (B1): reported speech (*اس نے کہا کہ وہ تھکا ہوا ہے*), the conditional past
    (*اگر میں جانتا تو آتا*), longer sentences joined with *لیکن*, *بلکہ*, *حالانکہ*, *جبکہ*; a story may be
    told by a first-person narrator or as a letter, a message or a diary entry;
  - ranks 2001–3000 (B2): the Persian and Arabic plurals of everyday words (*اخبارات*, *معلومات*,
    *ممالک*), the izafat in set phrases (*وزیر اعظم*, *ذخیرۂ الفاظ*), participles as adjectives (*لکھا ہوا*,
    *ٹوٹی ہوئی*); a lesson may also be a short informative or opinion text, as long as it is everyday,
    calm and easy to follow;
  - ranks 3001–4000 (B2): connectors such as *اس کے باوجود*, *دوسری طرف*, *اسی لیے*, *نتیجتاً*; the light
    column (*کالم*) on everyday life, common proverbs and idioms explained by context;
  - ranks 4001–7200 (C1): formal written style, verbal nouns in place of clauses (*گھر پہنچنے کے بعد*), a
    short essay or reflective column that weighs two sides, an interview, a portrait, a review; the
    specialised words of professions, sciences and the arts, each made clear by its context; poetry's
    words (*دل*, *غم*, *عشق*, *جہاں*) in their prose senses or in a quoted, explained couplet.
  A learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: the new words three times or more where it reads naturally, the story's
  key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `سوالات:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words (after rank 1000 they may also ask *کیوں* and *کیسے*, with fuller answers):

  ```
  سوالات:
  کیا زینب لاہور میں ہے؟
  جی، زینب لاہور میں ہے۔
  ```

  The header `سوالات:` is layout: the checker does not count it as a word.
- **Original texts**: write your own stories; never copy or retell existing course material.
- **Topics**: any everyday, cultural or story topic is fine (WRITER-BRIEF.md step 4: no topic
  rules); avoid only explicit sexual content, slurs used as insults and gratuitous gore.

## The file

Write `tools/courses/ur/<id>.json`, the same shape as the other languages':

```json
{
  "id": "ur-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Urdu, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Urdu",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ur/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *جاتی* or *گئے* for *جانا*, *بچوں* for *بچہ*, *اچھی* for *اچھا*).
  `newWords` holds the word as the list writes it: the infinitive of a verb (*جانا*, *کرنا*), the direct
  singular of a noun, the masculine of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240 up
  to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550
  up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words; the checker's last line lists the
  band words not taught yet.

Things to know about the Urdu list and the checker:

- The courses are pinned to `tools/courses/ur/wordlist.tsv` (a copy of the app's `ur.tsv`, counted from
  wordfreq, OpenSubtitles and Leipzig's news and Wikipedia sentences); the `bands/*.txt` come from it, and
  `extra_forms.tsv` adds the pack's regular forms the corpus never showed. The checker reads words as the
  reader does: cut at spaces and punctuation, marks and the non-joiner ignored, Arabic *ك ي ه* as *ک ی ہ*.
- **The news shows in the first ranks**: *حکومت*, *وزیر*, *ملک*, *افراد*, *خلاف*, *مطابق*, *صدر*, *فوج*,
  *پارٹی*, *انتخابات* rank high and everyday nouns later than you might expect: *بچہ* 102, *گھر* 105,
  *کھانا* 167, *ماں* 226, *بھائی* 260, *کتاب* 264, *پانی* 284, *بیٹا* 309, *ابو* (*ابا*) 473, *صبح* 542, *باپ*
  631, *بیٹی* 724, *بہن* 746, *چائے* 1486, *امی* 2366. Check a word's rank (`rank.py`) rather than guess.
- **Verbs are listed by the infinitive** with their forms under it (*کرنا*: *کر, کرتا, کرتی, کیا, کی,
  کریں, کرو*), and a conjunct verb is its two words (*کام کرنا* is *کام* and *کرنا*).
- **Words that are also forms**: *کیا* is "what" and *کرنا*'s past; *کی* is *کا*'s feminine and *کرنا*'s;
  *میں* is "I" and "in"; *اس, ان, اسے* are forms of both *یہ* and *وہ*; *ہوں, ہیں* are *ہے*'s, while *ہو,
  ہوا, ہوتا* are *ہونا*'s; *تھا, تھی, تھے* are *تھا*'s. A stem the list sends to its verb (*لگ* to *لگنا*,
  *بتا* to *بتانا*, *لو* to *لینا*) counts for the verb, so never teach the homograph (*لو* "flame").
- **Not names**: the list has *مریم*, *اسد*, *کرن*, *حنا*, *نوید*, *سلمی* as words; use names that are none
  (see the writer notes) and check any other with `rank.py`. Cities: *لاہور, کراچی, ملتان, پشاور, کوئٹہ,
  سیالکوٹ, حیدرآباد, سکھر* are names; *اسلام آباد* and *فیصل آباد* are not (each has a list word).
