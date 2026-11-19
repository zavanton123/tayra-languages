# Writing a Telugu frequency course

Each course teaches one band of 100 words from the Telugu word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `te-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 7,307 words: the last course is `te-mini-7400`
(7 words).

## The list

The list is counted from Telugu subtitles (FrequencyWords, a quarter of the blend), news (Leipzig
news 2020 and newscrawl 2011) and Wikipedia (Leipzig, a fifth), by the analyser in
`tools/frequency_telugu.py`: every corpus word is read back to its dictionary form (a noun's
nominative, a verb's citation form in -ు), and the forms are listed after it. So:

- **The ranks lean to the news.** Words of reports rank high (ప్రభుత్వం 35, రాష్ట్రం 50, జిల్లా 84,
  పార్టీ 95), everyday words lower than in speech (ఇల్లు 108, అమ్మ 836, అన్నం 1627). Write everyday
  stories anyway: a high-ranked formal word goes into an ordinary scene (a district office, a party at
  home, a film on Sunday).
- **Verbs are listed by the citation form in -ు** (చేయు, వెళ్లు, చూడు, తీసుకొను), nouns by the
  nominative (ఇల్లు, పుస్తకం, నీరు); `newWords` holds the headword as the band has it. The checker
  reads every regular form back to its word by the same rules (ఇంటికి, పుస్తకాన్ని, చేస్తున్నారు,
  వెళ్ళాడు, తీసుకున్నారు, అదే, ఉందని), so any correct form of a known word counts.
- **Nouns are spelt as texts spell them**: the dictionary's old -ము is listed as -ం (పుస్తకం, not
  పుస్తకము); old ఱ is ర (మారు, not మాఱు). ళ్ళ and ళ్ల are the same letters to the list (వెళ్ళాడు,
  వెళ్లాడు). Write the modern spelling.
- **The dictionary pack** (`dictionaries/te-en.sqlite`) has 17,000 entries; look a word up by its
  pack spelling when the list's differs (పుస్తకం is under పుస్తకము in `forms`). A few hundred list
  words come from the analyser's own list (pronouns, postpositions, months, everyday loans) and may
  have no entry: teach those in their plain, common sense.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *సీత ఫోన్ ఎక్కడ ఉంది? ఆమె ఇంట్లో చూస్తుంది. ఫోన్ అక్కడ లేదు.*
  Everyday life where Telugu is spoken: Hyderabad, Warangal, Vijayawada, Visakhapatnam, Guntur,
  Tirupati, Nellore, Kurnool, Karimnagar, Nizamabad, Rajahmundry, Kakinada, Ongole, Khammam; the
  Godavari and Krishna rivers, the Eastern Ghats, the coast, paddy fields and villages; and now and
  then Bengaluru, Chennai, Mumbai, Dubai, London or the United States, where Telugu speakers live:
  home, family, food, shopping, work, school, the town, health, free time, travel, festivals
  (Sankranti, Ugadi, Dasara, Diwali, Bathukamma) and the stories of the themes.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, jumps in time, irony or open
  endings. Events in the order they happen. Few characters (one to three), named clearly; say who
  అతను / ఆమె / వారు is whenever it could be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order
  subject, object, verb (*సీత టీ తాగుతుంది.*), the verb last. A copula-less sentence for "X is Y"
  (*ఇది ఒక గ్రామం. ఇల్లు పెద్దది.*); ఉంది / ఉన్నాడు for "is there", కాదు for "is not (a) Y",
  లేదు for "is not there".
- **Gender and person**: Telugu verbs show it: a man ఉన్నాడు, వచ్చాడు; a woman or a thing ఉంది,
  వచ్చింది (the same ending); people or a respected person ఉన్నారు, వచ్చారు. Keep a character's
  form the same all through the story and its questions.
- **Politeness**: నువ్వు (with the singular verb) among friends and family of the same age or
  younger; మీరు (with the plural verb) to elders, strangers, in shops and offices. A respected adult
  is ఆయన / ఆవిడ (or వారు) with the plural verb (*రాము పని చేస్తారు.*); a child or a friend is
  అతను / ఆమె (or వాడు / అది in speech). మనం is "we, you included", మేము "we, not you".
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present-future (*చేస్తాడు, చేస్తుంది, చేస్తారు, చేస్తాను*) and the simple past
    (*చేశాడు, చేసింది, చేశారు, వచ్చాను*); ఉండు (*ఉంది, ఉన్నాయి, ఉన్నాడు, ఉన్నారు*), లేదు and
    కాదు; the cases: accusative *-ను / -ని*, dative *-కు / -కి*, locative *-లో*, *-తో* "with",
    *నుండి* "from", the oblique (*ఇంటి, దాని, వారి*); *కోసం, గురించి, వరకు*; *కావాలి* and *ఇష్టం* with
    the dative (*సీతకు నీరు కావాలి*); questions with ఏమి / ఏం, ఎవరు, ఏ, ఎక్కడ, ఎందుకు, ఎంత and the
    clitic *-ఆ* (*ఉందా?*); *-ఏ* "just, the very" (*ఇక్కడే, అదే*);
  - ranks 301–600: the plural *-లు* and its cases (*పుస్తకాలు, పిల్లలకు*); the past participle that
    joins actions (*అన్నం తిని పడుకుంటాడు*); the progressive (*చేస్తున్నాడు, వెళ్తున్నారు*);
    imperatives (*రా / రండి, చూడు / చూడండి*); *దగ్గర, ముందు, తరువాత, లోపల, బయట*;
  - ranks 601–1000: the verb negatives (*చేయడు, చేయలేదు, చేయకు / చేయకండి*); the relative participles
    (*చదివే అబ్బాయి, వచ్చిన ప్రజలు*); *అని* after a quotation or a thought (*"వస్తాను" అని చెప్పింది*);
    *-ాలి* "must" and *-వచ్చు* "may" (*వెళ్ళాలి, చేయవచ్చు*); *-గల* "can" and *-లేదు / -లేను*
    "cannot"; comparison with *కంటే* (*వరంగల్ కంటే హైదరాబాద్ పెద్దది*);
  - ranks 1001–2000: the verbal noun *-డం* (*చదవడం ఇష్టం, చేయడానికి*); the conditional *-తే* and
    the concessive *-ినా* (*వర్షం పడితే, అడిగినా*); *-ినప్పుడు* "when" (*ఇంటికి వచ్చినప్పుడు*);
    the reflexive with *-కొను* (*నేర్చుకున్నాడు, తీసుకుంది*) and *పోవు* after the participle
    (*వెళ్ళిపోయాడు*);
  - ranks 2001–3000: longer joined sentences (*కాబట్టి, అందుకే, ఎందుకంటే, అయినా*); causatives
    in *-ించు* (*చేయించు, నేర్పించు*); the written passive *-బడు* where Telugu prose uses it
    (*నిర్మించబడింది*); reported speech with *అని*;
  - ranks 3001–5000: a richer written register: the Sanskrit-derived words of newspapers and books
    beside their everyday ones, made plain by context; a column, a letter, an interview, a short
    essay that weighs two sides; long sentences that stay easy to parse;
  - ranks 5001–7400 (C1): the same, with the specialised words of the sciences, the arts and
    public life, each made clear by its context.
  After rank 1000 sentences get longer and dialogue can carry more of the story; a story may be told
  by a first-person narrator or as a letter, a message or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text, as long as it is calm and easy to follow.
- **Repetition on purpose**: the new words three times or more where it reads naturally (the
  checker needs two), the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `ప్రశ్నలు:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  ప్రశ్నలు:
  సీత ఎక్కడ ఉంటుంది?
  సీత పాలెం అనే గ్రామంలో ఉంటుంది.
  ```

  The header `ప్రశ్నలు:` is layout: the app and the checker do not count it as a word.
- **Standard written Telugu**: the narration, the dialogue and the questions use the standard of
  books and newspapers (*చేస్తున్నాను*, not *చేస్తున్నా*; *ఏమిటి*, not *ఏంటి*; *వెళ్తాను*, not
  *పోతా*), and no regional dialect. English loans Telugu has made its own (*బస్సు, ఫోన్, సినిమా,
  డాక్టర్, టీవీ*) are fine in Telugu script; never write English in Latin letters.
- **Spelling**: write the list's spelling (*తేదీ, పెళ్లి, గుర్తు, అర్థం*), not the variants it files
  under them (*తేది, పెండ్లి, గురుతు, అర్ధం*). An ending on a word that ends in a consonant with
  virama is written after a zero-width non-joiner, as Telugu typing does (*ఫోన్‌లో, హైదరాబాద్‌కు*):
  the reader keeps the joiner inside the word.
- **Telugu typography**: a sentence ends with a full stop, `?` or `!`; direct speech in double
  quotation marks, each speaker's line a paragraph of its own. Write numbers as Western digits
  (*3 పుస్తకాలు, 5 గంటలకు, 2019లో*), never as Telugu digits (*౩*), except a number word that is in
  your band: spell that one out to teach it (*రెండు రోజులు*). A hyphen splits a word in two for the
  reader and the checker; write compounds as Telugu writes them.
- **Names are names**: Telugu has no capital letters. State, city and person names are left out of
  the list (తెలంగాణ, హైదరాబాద్, రెడ్డి), but many first names are also words (లక్ష్మి "wealth",
  రవి "sun", గీత "song", జ్యోతి "flame", వాణి "speech"): never use a list word as a character's name,
  and never put one in `names`. Language names are everyday words: teach them (తెలుగు, హిందీ,
  ఇంగ్లీష్). Safe first names: సీత, రాము, కిరణ్, అనిల్, సరిత, శారద, వెంకట్, సురేష్, అనిత, మహేష్,
  వినోద్, ప్రసాద్, సుజాత, రమేష్, శ్రీను, సునీత, రజని, కళ్యాణి, నాగరాజు (grep the list first).
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any everyday, cultural or story topic is fine
(festivals and temples, history, kings and queens, myths and folk tales, politics, cricket, films).
Avoid only explicit sexual content, slurs used as insults and gratuitous gore. Teach ordinary words
in their main sense. Skip list noise: fragments, abbreviations and words of other languages.

## The file

Write `tools/courses/te/<id>.json`, the shape of `te-mini-0100.json`:

```json
{
  "id": "te-mini-0200",
  "rankUpTo": 200,
  "level": "A1",
  "title": "The title from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["సీత", "వరంగల్"],
  "lessons": [
    {
      "title": "story title in Telugu",
      "summary": "One line in English on what happens.",
      "newWords": ["the band headwords this lesson introduces"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

`names` holds every person and place name the lessons use, in the nominative (వరంగల్, not
వరంగల్‌లో): the checker reads its case forms.

## The rules, checked by `python3 tools/check_course.py tools/courses/te/<id>.json`

- **Vocabulary**: 90% (ranks up to 100), 93% (to 300) or 95% (above) of a lesson's words are ranked
  within the course's `rankUpTo`; names count as known.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson. Reuse earlier courses' words freely.
- **Length** (story and questions together, in words as the reader cuts them): 80–180 to rank 100,
  120–240 to 300, 180–320 to 600, 220–380 to 1000, 260–450 to 2000, 300–520 to 3000, 320–550 to
  4000, 350–600 after. Telugu words carry their endings, so a Telugu lesson has fewer words than its
  English translation: the length is in Telugu words.
- **Coverage of the band**: teach 80–100 of the band's words; the checker's last line lists the band
  words not taught yet.
