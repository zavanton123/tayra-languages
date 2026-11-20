# Writing a Sinhala frequency course

Each course teaches one band of 100 words from the Sinhala word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `si-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 10,000 words: the last course is `si-mini-10000`.

## The list

The list is counted by `tools/frequency_sinhala.py` from a large count of written Sinhala (news, books,
the web: the STM and SinMin corpora), subtitles (a fifth of the blend) and Wikipedia. A word is listed
when the si-en pack has it (the English Wiktionary's Sinhala entries, a parallel Sinhala-English word
dictionary, and everyday words added by hand), and a corpus form is counted for the word it is a form of.

- **The ranks lean to the news.** Words of reports rank high (මහතා 30, රජය 79, පොලිසිය 151,
  ජනාධිපති 261), everyday words lower than in speech (පොත 338, ගෙදර 359, අම්මා 371, වතුර 907).
  Write everyday stories anyway: a news word goes into an ordinary scene (a police officer who helps,
  a village festival, a school match). Subtitles bring in the spoken words (මම, ඒක, ඕනෑ, එයා, ඔයා).
- **Verbs are listed by the -නවා form** (කරනවා, යනවා, එනවා, බලනවා), with all their forms: the present
  and its relative (කරනවා, කරන), the past (කළා, කළේ, ගියා, ආවා, බැලුවා), the participles (කරලා,
  ගිහින්, ඇවිත්, බලා), the infinitive (කරන්න), the verbal noun (කිරීම, යාම, බැලීම) and the literary
  forms (කළේය, කරයි, කරති, වූ, විය). `newWords` holds the headword as the band has it. A compound verb is
  its own word when the list has it (සිදුවෙනවා, ලබාදෙනවා, ලබාගන්නවා, කරගන්නවා, පත්වෙනවා), else it is
  the noun and the verb (උදව් කරනවා). තියෙනවා is listed as තිබෙනවා, the written form, with all of its
  forms (තියෙනවා, තිබුණා, තිබේ, තිබූ); teach තිබෙනවා and write whichever the story's register wants.
- **Nouns**: a noun's cases, its indefinite and plural, and the clitics on them are its forms (පොත:
  පොතක්, පොතට, පොතේ, පොතෙන්, පොත්, පොත්වලට; ළමයා: ළමයාට, ළමයාගේ, ළමයෙක්, ළමයි, ළමයින්ට). A learned
  noun in -ය or -ව is listed by that form, its stem being its plural (ප්‍රශ්නය: ප්‍රශ්න, ප්‍රශ්නවලට;
  කතාව: කතා). A few stems are words of their own, used before a noun or in a compound verb (රාජ්‍ය
  "state", ප්‍රකාශ කරනවා "to declare", සාකච්ඡා කරනවා "to discuss").
- **Spelling variants are folded**: misspellings with ල for ළ, න for ණ, ද for ඳ (ලඟ, පමන, සදහා) and
  the older ක්‍ෂ and කථා are forms of the standard spelling (ළඟ, පමණ, සඳහා, පක්ෂය, කතාව). Write the
  standard spelling: ළඟ, පහළ, ඉහළ, කළා, පමණ, සඳහා, ඊළඟ, තත්ත්වය.
- **List noise**: the rude pronouns උඹ (28) and ඌ (82) rank high from the subtitles: skip them, or use
  them only where a story needs them. A few names and English letters spelled out (එස්, ඩී) are left
  out; skip any fragment you meet.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *නිමල්ට පොතක් ඕනෑ. ඔහු කඩයට යනවා. ඔහු පොතක් ගන්නවා.* Everyday life where
  Sinhala is spoken: Sri Lanka (Colombo, Kandy, Galle, Matara, Kurunegala, Anuradhapura, Polonnaruwa,
  Nuwara Eliya, Badulla, Ella, Negombo, Ratnapura, Jaffna, Trincomalee; Sigiriya, Adam's Peak, the
  Mahaweli, tea estates, paddy fields, the coast) and now and then the cities where Sri Lankans live
  abroad (London, Melbourne, Dubai, Toronto): home, family, food (rice and curry, hoppers, kiribath,
  tea), shopping, work, school, the town, the bus and the train, health, free time, cricket, travel,
  festivals (the Sinhala and Tamil New Year, Vesak, Poson, the Kandy Perahera, Christmas) and the
  stories of the themes.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, jumps in time, irony or open endings.
  Events in the order they happen. Few characters (one to three), named clearly; say who ඔහු / ඇය /
  ඔවුන් is whenever it could be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order
  subject, object, verb, the verb last (*නිමල් බත් කනවා.*). No copula in the present for "X is Y"
  (*මම ගුරුවරයෙක්. ගෙදර ලොකුයි.*); තිබෙනවා / තියෙනවා for things and ඉන්නවා for people and animals
  ("there is, has": *මට පොතක් තියෙනවා, ගෙදර බල්ලෙක් ඉන්නවා*); නැහැ for their negative, and after
  the focused form of a verb (*ඔහු යන්නේ නැහැ*). The dative subject for wanting, being able and
  liking (*මට ඕනෑ, මට පුළුවන්, මට කැමතියි* or *මම කැමතියි*).
- **The register**: narration in the plain written Sinhala of today's stories and feature pages, with
  the -නවා present and the -ා past (*යනවා, ගියා, කළා, ආවා*) and the written pronouns ඔහු, ඇය,
  ඔවුන්; dialogue in polite everyday speech. The formal literary verb endings (*ගියේය, කරයි, කරති,
  පැවසීය*) and the written passive come in from rank 1001, for letters, news, columns and essays.
  Never mix the two in one sentence.
- **Politeness and address**: Sinhala speakers call each other by name or by a kin word (අම්මා,
  තාත්තා, අක්කා, අයියා, නංගී, මල්ලී, සීයා, ආච්චි) more than by "you". ඔයා is the everyday "you"
  between family and friends; ඔබ is the polite and written one (to strangers, in shops, offices and
  letters). Never use උඹ, තෝ or තමුසේ, which are rude. Adults are named with මහතා, මහත්මිය, මිස්, සර්
  where respect is due (*පෙරේරා මහතා*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the -නවා present and the -ා past (*යනවා, ගියා*); තිබෙනවා / ඉන්නවා and නැහැ; the
    definite and indefinite (පොත / පොතක්, ළමයා / ළමයෙක්) and the plural (පොත්, ළමයි); the cases
    -ට "to", -ගේ (people) and -ේ (things, places) "of, in", -ගෙන් / -ෙන් "from, by"; postpositions
    after a noun (*ගෙදර ළඟ, මේසය උඩ, නිමල් එක්ක, ඒ ගැන*); yes/no questions with -ද (*ඔයා යනවද?*) and
    කවුද, මොකක්ද, කොහෙද, කවදද, ඇයි, කොහොමද where they are in your band (before that, yes/no
    questions); ඕනෑ, පුළුවන්, කැමති;
  - ranks 301–600: the infinitive -න්න (*යන්න ඕනෑ*); polite imperatives (*එන්න, බලන්න, කන්න*); the
    participles that join actions (*කාලා නිදා ගන්නවා, ගෙදර ගිහින් බලනවා*); -න්නම් "I will"; the
    clitics -ත් "also" and -ම "just, very" (*මමත්, අදම*); a verb before a noun (*මම කියවන පොත, මම
    කියවපු පොත*); the focused form in questions and negatives (*කොහෙද යන්නේ? මම යන්නේ නැහැ*);
  - ranks 601–1000: the conditional -ොත් (*වැස්සොත්*), "even if" -ත් (*ගියත්*), "when" -නකොට /
    -ද්දී, නිසා "because", වඩා in comparisons (*නිමල් කමල්ට වඩා උසයි*), reported speech with කියලා
    (*ඔහු එනවා කියලා කිව්වා*), යුතුයි and අවශ්‍යයි for "must, needs";
  - ranks 1001–2000: the involitive verbs in -ෙනවා (*මට ඇහුණා, පෙනෙනවා, වැටුණා*); causatives
    (*කරවනවා, ඉගැන්නුවා*); compound verbs with කරනවා and වෙනවා (*උදව් කරනවා, සතුටු වෙනවා*); the
    written register for letters and news (*ඔහු පැවසීය, කළේය, කරයි*);
  - ranks 2001–3000: longer joined sentences (*නමුත්, එබැවින්, එසේම, එහෙත්*), relative clauses with
    the participle, the verbal noun with endings (*යාමට, කිරීමෙන්, ලැබීම නිසා*), the written passive
    (*කරනු ලැබේ*) where Sinhala prose uses it;
  - ranks 3001–6000: a richer written register: the learned (Sanskrit and Pali) words of newspapers
    and books beside their everyday ones, made plain by context; a column, a letter, an interview, a
    short essay that weighs two sides; long sentences that stay easy to parse;
  - ranks 6001–10000 (C1–C2): the same, with the specialised words of the sciences, the arts and
    public life, each made clear by its context.
  After rank 1000 sentences get longer and dialogue can carry more of the story; a story may be told
  by a first-person narrator or as a letter, a message or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text, as long as it is calm and easy to follow.
- **Repetition on purpose**: the new words three times or more where it reads naturally (the
  checker needs two), the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `ප්‍රශ්න:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  ප්‍රශ්න:
  නිමල් කඩයට යනවද?
  ඔව්, නිමල් කඩයට යනවා.
  ```

  The header `ප්‍රශ්න:` is layout: the app and the checker do not count it as a word.
- **Spelling and typing**: write the text in NFC (ො ෝ ෞ ේ as one sign each; the checker flags
  them typed as two). A conjunct is a letter, al-lakuna, the zero-width joiner and ර or ය
  (ප්‍ර, ශ්‍රී, ක්‍රම, අවශ්‍ය, ව්‍යාපාරය) or the touching ක්‍ෂ: type the joiner (U+200D) there, as the
  list and the reader have it; without it the word is another word to them, and the checker flags it.
  Never type a joiner anywhere else, nor a non-joiner (U+200C). Write the standard spelling (see "The
  list").
- **Sinhala typography**: a sentence ends with `.`, `?` or `!` (not the kunddaliya ෴); direct speech
  in double quotation marks, each speaker's line a paragraph of its own. Write numbers as digits or as
  words: the checker counts a number with an ending written on it (*10ට, 2024දී*) as a number, but the
  reader shows that ending as a word of its own, so write *අවුරුදු 10ක්* rarely and prefer the number
  word where it is in your band (*පොත් දෙකක්*). A hyphen splits a word in two for the reader and the
  checker; write compounds as Sinhala writes them.
- **Names are names**: Sinhala has no capital letters, so a name that is also a word reads as the
  word. Never put a list word in `names`, and never use one as a character's name (no රුවන් "gem",
  සඳ "moon", මල "flower", ජය "victory", සමන් as names if your band has the word). Common names are fine
  (නිමල්, කමල්, සුනිල්, අමාලි, නදීෂා, කසුන්, ඉෂාරා, දිල්ෂාන්, චමිලා, තරිඳු, හසිනි, පියුමි, සහන්,
  ලහිරු, මධුෂා, ගයාන්, ෆාතිමා, රාජන්); a country or city name may be taught as what it is; language
  names are everyday words: teach them (සිංහල, දෙමළ, ඉංග්‍රීසි).
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any everyday, cultural or story topic is fine
(festivals, history, myths and folk tales, politics, cricket). Avoid only explicit sexual content,
slurs used as insults and gratuitous gore. Teach ordinary words in their main sense. Skip list
noise: fragments, abbreviations and words of other languages.

## The file

Write `tools/courses/si/<id>.json`, the shape of `si-mini-0100.json`:

```json
{
  "id": "si-mini-0200",
  "rankUpTo": 200,
  "level": "A1",
  "title": "The title from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["නිමල්", "ගාල්ල"],
  "lessons": [
    {
      "title": "story title in Sinhala",
      "summary": "One line in English on what happens.",
      "newWords": ["the band headwords this lesson introduces"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

`names` holds every person and place name the lessons use, in the plain form (ගාල්ල, not ගාල්ලේ):
the checker reads the endings written on a name (නිමල්ට, නිමල්ගේ, ගාල්ලේ, කොළඹදී, නිමල්ලා) and the
clitics on them. A name of several words (ශ්‍රී ලංකාව, පෙරේරා මහතා) counts as one name where it is
written whole.

## The rules, checked by `python3 tools/check_course.py tools/courses/si/<id>.json`

- **Vocabulary**: 90% (ranks up to 100), 93% (to 300) or 95% (above) of a lesson's words are ranked
  within the course's `rankUpTo`; names count as known. A form the list lacks counts when
  `extra_forms.tsv` has it (the builder's rule forms of the first 5,000 words), or when it is a listed
  form with a clitic on it (-ත්, -ද, -ම, -යි: ගෙදරත්, පොතකි).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson. Reuse earlier courses' words freely.
- **Length** (story and questions together, in words as the reader cuts them): 80–180 to rank 100,
  120–240 to 300, 180–320 to 600, 220–380 to 1000, 260–450 to 2000, 300–520 to 3000, 320–550 to
  4000, 350–600 after.
- **Coverage of the band**: teach 80–100 of the band's words; the checker's last line lists the band
  words not taught yet.
