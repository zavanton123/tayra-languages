# Writing a Pashto frequency course

Each course teaches one band of 100 words from the Pashto word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its forms). Course `ps-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 3,584 words: `ps-mini-0100` to `ps-mini-3600`
(the last band has 84 words).

## The register

**Standard written Pashto of Afghanistan**, plain and correct, as graded readers, children's books and
the BBC or Azadi Radio Pashto services write it. Dialogue may be everyday spoken Pashto as stories
write it (*ښه راغلاست*, *څنګه یې؟*, *مننه*, *هو*, *سمه ده*), but no Roman Pashto, no Dari or Urdu
sentences and no English mixed into a sentence beyond the loanwords Pashto writes in its own letters
(*ډاکټر*, *موټر*, *ټلیفون*). One dialect per story: the standard written forms, not Kandahari or Peshawari
spellings (*ستاسو*, not *ستاسې*; *شو*, not *سو*).

| Point | Write | Not |
|---|---|---|
| "you" | *تاسو* (verb in the plural: *تاسو ځئ*) to an adult, a stranger, a parent; *ته* (singular verb: *ته ځې*) to a friend, a child, a sibling; one form per pair of speakers | mixing *ته* and *تاسو* to the same person |
| The past of a transitive verb | the doer in the oblique and the verb agreeing with the object: *احمد ډوډۍ وخوړله*, *ما کتاب واخیست*, *ښځو کار کاوه* | *زه کتاب واخیستم*, *خلک کار کاوه* |
| Clitics | *مې, دې, یې, مو* after the first word or phrase of the clause: *کتاب مې واخیست*, *نو یې وژني* | starting a clause with *یې* (*او یې وژني*) |
| Postpositions | apart: *په کور کې*, *له احمد سره*, *د ښار لپاره*, *کابل ته* | joined *پدې*, *ددې*, *دهغه*, *لخوا* written for *له خوا* where the list has the parts |
| Numbers | Pashto digits: *۱۰ کاله*, *۷ بجې*, *۲۰۲۴* | *10* |

**Spelling as Afghan Pashto writes it (and as the reader needs it):**

- **Pashto letters**: *ټ ډ ړ ږ ښ ځ څ ګ ڼ* (not Urdu *ٹ ڈ ڑ* or Persian *گ*), *ک* (not *ك*).
- **The yes**: *ې* for the -e of *کې, چې, دې, ښځې*; *ي* for the -i of *کوي, سړي, ملي*; *ی* for the -ay of
  *سړی, دی, ښوونځی*; *ۍ* for the feminine -əy of *نجلۍ, ډوډۍ, کورنۍ*; *ئ* for the verb's -əy of *کوئ, یاست*.
  The reader and the checker read all of them as one letter (so do texts from Pakistan, which write
  *چي* or *چی* for *چې*), so they never tell two words apart: *سړی* and *سړي*, *دی*, *دې* and *دي* are one
  word to them. Write the standard letter anyway: learners see the text as written.
- **No vowel marks** (zwar, zer, pesh, shadda); the checker ignores all marks and the non-joiner.
- **Compounds**: as standard Pashto writes them; a compound verb is two words to the reader and the
  checker (*کار کول* is *کار* and *کول*, *پیل کېدل* is *پیل* and *کېدل*); one written joined
  (*ولسمشر*, *روغتون*) is one word.
- **Punctuation**: the full stop *.*, the comma *،*, the question mark *؟*, the exclamation mark. Speech in
  *«...»* after a colon: *احمد وايي: «زه ښه یم.»* Each speaker's line is a paragraph of its own (or
  *Name: «...»* lines in a dialogue).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. Everyday life where Pashto is spoken: Kabul, Kandahar, Jalalabad, Herat, Khost,
  Ghazni, Kunduz, a village in Paktia or Nangarhar; Peshawar, Mardan, Swat, Quetta, Bannu in Pakistan;
  and now and then Pashtun families in Karachi, Dubai, London or Toronto.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony or open endings. Events in
  the order they happen. Few characters (one to three), named clearly; *هغه* is "he", "she", "it" and
  "that", so repeat the name wherever it could be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, Pashto order: subject,
  object, verb at the end (*لیلا کتاب لولي.*). Present tense up to rank 300; from rank 400 the story may
  be told in the past. Dialogue short and simple.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300 (A1): the copula *یم, یې, دی, ده, یو, یاست, دي* and its past *وم, وې, و, وه, وو*; the
    present (*کار کوي*, *کور ته ځي*), *لرل* (*یو کور لري*); *د* for "of" (*د احمد کور*); the pronouns and
    the clitics *مې, دې, یې, مو*; the postpositions and circumpositions *په ... کې, له ... سره, له ... څخه,
    د ... لپاره, ته, پر*; *دا, هغه, دغه*; adjectives agreeing (*ښه سړی, ښه ښځه, لوی کور, لویه کوټه*);
    the future with *به* and the perfective (*به راشي, به وکړي*); the imperative (*راشه, وګوره*) and its
    negative with *مه*; questions with *څه, څوک, چېرې, کله, ولې, څنګه, څومره*;
  - ranks 301–600 (A2): the past: intransitive (*هغه راغی, هغه لاړه*) and transitive, with the doer in the
    oblique and the verb agreeing with the object (*احمد کتاب واخیست*, *ملالۍ ډوډۍ پخه کړه*); the
    perfective and imperfective pair; compound verbs with *کول* and *کېدل* (*کار کول, پیل کېدل*);
    comparison with *له ... نه* and *تر* (*له احمد نه لوی, تر ټولو ښه*); *کله چې, که ... نو, ځکه چې*;
    days, months and the time;
  - ranks 601–1000 (A2): the present perfect (*راغلی دی, لیدلی یې دی*) and the past continuous (*روان و,
    یې کاوه*), the subjunctive after *باید, غواړي چې, کېدای شي* (*باید لاړ شي*), *کولای شي* ("can");
  - ranks 1001–1300 (B1): the past perfect (*تللی و*), relative clauses with *چې*, the passive with *کېدل*
    (*ویل کېږي, جوړ شو*);
  - ranks 1301–2000 (B1): reported speech (*هغه وویل چې ستړی دی*), the past conditional (*که پوهېدلی وای،
    راتلی وای*), longer sentences with *خو, بلکې, که څه هم, په داسې حال کې چې*; a story may be told by a
    first-person narrator or as a letter, a message or a diary entry;
  - ranks 2001–3600 (B2): the Arabic plurals of everyday words (*معلومات, اقدامات, حالات*), participles as
    adjectives (*لیکل شوی, ماته شوې*), connectors such as *له دې سره سره, له بلې خوا, نو ځکه*; a lesson may
    also be a short informative or opinion text, or a light column on everyday life with a proverb (*متل*)
    explained by its context, as long as it is calm and easy to follow.
  A learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: the new words three times or more where it reads naturally, the story's
  key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `پوښتنې:` with 3–6 simple questions
  about the story, each followed on its own line by a full-sentence answer that reuses the story's
  words (after rank 1000 they may also ask *ولې* and *څنګه*, with fuller answers):

  ```
  پوښتنې:
  احمد په کابل کې دی؟
  احمد په کابل کې دی.
  ```

  The header `پوښتنې:` is layout: the checker does not count it as a word.
- **Original texts**: write your own stories; never copy or retell existing course material.
- **Topics**: any everyday, cultural or story topic is fine (WRITER-BRIEF.md step 4: no topic
  rules); avoid only explicit sexual content, slurs used as insults and gratuitous gore.

## The file

Write `tools/courses/ps/<id>.json`, the same shape as the other languages':

```json
{
  "id": "ps-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Pashto, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Pashto",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, after that `B2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ps/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *کوي* or *وکړ* for *کول*, *ښځې* for *ښځه*, *ښه* for *ښه*).
  `newWords` holds the word as the list writes it: the infinitive of a verb (*کول*, *راتلل*), the direct
  singular of a noun, the masculine of an adjective.
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240 up
  to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550
  after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words; the checker's last line lists the
  band words not taught yet.

Things to know about the Pashto list and the checker:

- The courses are pinned to `tools/courses/ps/wordlist.tsv` (a copy of the app's `ps.tsv`, counted from
  Leipzig's Pashto news of 2011 and 2020 and its Wikipedia sentences, the Dari ones left out); the
  `bands/*.txt` come from it. Its words are those of the ps-en pack: the English Wiktionary's Pashto
  entries and translation tables, and some 400 common words glossed by hand that the Wiktionary lacks.
- **The news shows in the first ranks**: *ولایت, حکومت, وزارت, والي, ولسوالي, طالب, ويروس, وژل* rank in
  the first hundred, everyday words later: *زه* 107, *کورنۍ* 111, *لور* 116, *ښځه* 154, *کور* 158, *ملګری*
  229, *ماشوم* 245, *سړی* 272, *پلار* 350, *زوی* 364, *مور* 380, *ښوونځی* 387, *ورور* 471, *ډوډۍ* 660,
  *نجلۍ* 886, *چای* 1201. Teach the news words in their plain senses in everyday stories (a governor
  visits an office, people put out a fire: *اور وژل*); check a word's rank (`rank.py`) rather than guess.
- **Verbs are listed by the infinitive** with their forms under it (*کول*: *کوي, کړي, وکړ, کړه*); the
  copula is three words of the list: *دی* (with *دې, دي*), *ده* and *یم* (with *وي, وو, وه, وم*; and *و*,
  *یاست* from `extra_forms.tsv`).
- **One letter for the yes makes homographs**: *لرې* "far" reads as *لري* "has" (*لرل*), *کړۍ* "ring" as
  *کړي* (*کول*), *شی* "thing" as *شي* (*کېدل*), *ورځي* "goes there" as *ورځ*'s form. The list files such a
  form under the common word; never teach the rare one, and don't count on the other sense.
- **Glosses**: the pack's entries from translation tables sometimes give a rare sense first (*سر*
  "secret" beside "head", *کس* and *تنه* "person", *پاره* in *له ... پاره* "for"); teach the common sense,
  which the hand-glossed entries add. Read the band against the writer notes' list of such words.
- **Not names**: the list has *شبنم*, *پلوشه*, *ګل*, *نازو* (*نازي*) as words; use names that are none
  (see the writer notes) and check any other with `rank.py`. Cities: *کابل, کندهار, هرات, خوست, غزني,
  پېښور* are names; *جلال آباد* and *مزار شریف* are not (each has a list word).
