# Writing a Gujarati frequency course

Each course teaches one band of 100 words from the Gujarati word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `gu-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 4,859 words: the last course is `gu-mini-4900`
(59 words).

## The list

The list is counted from Leipzig's Gujarati news (Gujarat Samachar, Divya Bhaskar, 2011 and 2020)
and Wikipedia sentences by `tools/frequency_gujarati.py`: every corpus word is read back to its
dictionary form in the `gu-en` pack, and its forms are listed after it. So:

- **The ranks lean to the news.** Words of reports rank high (સરકાર 49, પોલીસ 58, કેસ 89, રાજ્ય 52,
  ફિલ્મ 81), some everyday words lower than in speech (ઘર 161, ભાઈ 405, ભોજન 901). Write everyday
  stories anyway: a news word goes into an ordinary scene (a police officer who helps, a film the
  family sees, a state office where someone works).
- **Verbs are listed by the infinitive in -વું** (કરવું, જવું, આપવું, લેવું), with all their forms
  (કરું, કરે છે, કર્યું, કરી, કરીને, કરશે, કરતો, કરવામાં, કરાય); `newWords` holds the headword as the band
  has it. છે, છું, હતો, હતું, હશે, હોય and નથી are હોવું's forms; ગયો, ગઈ, જાય are જવું's.
- **Adjectives are listed by the neuter in -ું** (મોટું, નવું, સારું), with મોટો, મોટી, મોટા, મોટાં and
  their case forms (મોટાને, મોટામાં). A noun's postposition forms (ઘરમાં, ઘરનો, ઘરની, ઘરે, ઘરથી), its
  oblique and plural (છોકરો: છોકરા, છોકરાઓ, છોકરાને) and the pronouns' cases (મને, મારું, તેને, તેણે,
  તેમનું, આપણને) are its forms. The possessives મારું, તારું, તમારું, અમારું, આપણું are હું's, તું's,
  તમે's, અમે's, આપણે's.
- **Spelling variants are folded**: થઇ under થઈ, કોઇ under કોઈ, નહિ under નહીં, બન્ને under બંને,
  દરમ્યાન under દરમિયાન, ઉભું under ઊભું. Write the standard spelling: થઈ, ગઈ, કોઈ, લઈને, જોઈએ (ઈ after
  a letter), નહીં, બંને, ઊભું, ઊંચું, દૂર, શરૂ, જરૂર, રૂપિયો.
- **Function morphemes are entries too**: માં, નું, થી and ને (the postpositions, when a text writes
  them apart) and ના (also "no"). Teach ના as "no" and ને as "and" only where the story uses them so;
  skip માં, નું and થી.
- **Words without a dictionary entry**: the gu-en pack is small (about 6,000 words), so about 600
  everyday words it lacks were added to the list by hand (સાથે, દ્વારા, ઉપયોગ, ઘટના, યોજના, સમસ્યા,
  સંખ્યા, મુખ્ય, વગેરે). `rank.py` shows them; check their meaning in a general dictionary before you
  teach them, and teach the main sense.
- **List noise**: a few names rank as words (કુમાર, અશોક, પૂનમ, મમતા are also names; ભારત,
  ગુજરાત and the city names are not in the list), and some short words collect stray forms (ના, ને,
  પણ). Skip them.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *નીનાને કામ જોઈએ. પરંતુ તેની પાસે કોઈ કામ નથી.*
  Everyday life where Gujarati is spoken: Gujarat (Ahmedabad, Surat, Vadodara, Rajkot, Bhavnagar,
  Jamnagar, Junagadh, Gandhinagar, Anand, Bhuj and Kutch, Saurashtra, the Narmada, Somnath and Dwarka,
  the Gir forest, villages, farms and markets), Mumbai and Diu; and now and then London, Leicester,
  New Jersey, Nairobi or Dubai, where Gujaratis live: home, family, food, shopping, work, school,
  the town, health, free time, travel, festivals (Navratri and garba, Diwali, Uttarayan's kites,
  Holi, Eid) and the stories of the themes.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, jumps in time, irony or open
  endings. Events in the order they happen. Few characters (one to three), named clearly; say who
  તે / એ / તેઓ is whenever it could be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order
  subject, object, verb (*મીના ચા પીએ છે.*), the verb last. Present with છે (*તે કામ કરે છે*), the
  copula છે in "X is Y" (*અમદાવાદ મોટું શહેર છે*), નથી for its negative.
- **Agreement**: an adjective in -ું and a genitive agree with the noun (*મોટો દેશ, મોટી ફિલ્મ, મોટું
  શહેર; મીનાનો ભાઈ, મીનાની ફિલ્મ, મીનાનું ઘર*); a perfective verb agrees with its subject when it is
  intransitive (*મીના ગઈ, અનિલ ગયો*) and with its object, the subject in the ergative, when it is
  transitive (*મીનાએ ફિલ્મ જોઈ, તેણે કામ કર્યું, મેં કહ્યું*). Keep a character's gender the same.
- **Politeness**: તું among friends, family and children; તમે to elders, strangers, in shops and
  offices (and as the plural); આપ only in very formal speech. A respected elder is spoken of in the
  plural (*દાદા આવ્યા, પિતાજી કહે છે*) all through the story. Keep a character's form the same in
  narration and questions.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present (*કરું છું, કરે છે, કરીએ છીએ*); છે / હતું; નથી and નહીં; the postpositions
    written on: genitive *-નો / -ની / -નું / -ના* agreeing with the thing owned, *-ને* (to; the object),
    *-માં* (in), *-થી* (from, by), *-એ* (at, the ergative: *ઘરે, શાળાએ, મીનાએ*); postpositions apart
    after the genitive (*મીના પાસે, ઘરની સામે, મારા માટે, તેની સાથે*); the oblique (*છોકરો: છોકરાને*);
    yes/no questions by intonation (*મીના અમદાવાદમાં રહે છે?*) and questions with શું (145), કોણ,
    કયું, ક્યાં; the future (*કરીશ, કરશે*) where needed;
  - ranks 301–600: the simple past (*ગયો, ગઈ, આવ્યું; મેં કર્યું, તેણે કહ્યું*) with the ergative; the
    conjunctive *-ઈને* that joins actions (*ઘરે જઈને, ખાઈને*); the infinitive of purpose (*ફિલ્મ જોવા
    જાય છે*); *-વું પડે* "has to" (*જવું પડ્યું*); imperatives (*કર / કરો / કરજો*);
  - ranks 601–1000: the progressive and perfect (*કરી રહ્યો છે, ગયો છે*); જો … તો, જ્યારે … ત્યારે;
    comparison with *-થી / કરતાં વધુ*; જોઈએ "should, is needed" (*મને કામ જોઈએ*); શકવું "can";
  - ranks 1001–2000: the past continuous and habitual (*કરતો હતો, જતો*); compound verbs (*કરી
    નાખવું, લઈ લેવું, બેસી જવું, આવી જવું*); જે … તે, જેવું … તેવું; the verbal noun with postpositions
    (*કરવામાં, જવાનું, વાંચવાની*); the passive in *-આય* (*કહેવાય, દેખાય, જણાય*); causatives (*કરાવવું,
    બતાવવું, સમજાવવું*);
  - ranks 2001–3000: longer joined sentences (*કારણ કે, પરંતુ, છતાં, તેમજ, તથા*); the written
    passive (*કામ શરૂ કરવામાં આવ્યું*); reported speech with કે (*તેણે કહ્યું કે …*);
  - ranks 3001–4900 (C1 from 4100): a richer written register: the Sanskrit-derived (તત્સમ) words of
    newspapers and books beside their everyday ones, made plain by context; a column, a letter, an
    interview, a short essay that weighs two sides; long sentences that stay easy to parse.
  After rank 1000 sentences get longer and dialogue can carry more of the story; a story may be told
  by a first-person narrator or as a letter, a message or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text, as long as it is calm and easy to follow.
- **Repetition on purpose**: the new words three times or more where it reads naturally (the
  checker needs two), the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `પ્રશ્નો:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  પ્રશ્નો:
  મીના અમદાવાદમાં રહે છે?
  હા, મીના અમદાવાદમાં રહે છે.
  ```

  The header `પ્રશ્નો:` is layout: the app and the checker do not count it as a word.
- **Standard Gujarati**: the narration, the dialogue and the questions use today's standard
  written Gujarati of books and newspapers, no dialect (Kathiawadi *હાલો* for *ચાલો*, *સે* for *છે*)
  and no Hindi words where Gujarati has its own (*કામ*, not *કાર્ય* in speech; *ઘણું*, not *બહુત*).
  English loans Gujarati has made its own (*બસ, ફોન, ઓફિસ, સ્કૂલ, ડૉક્ટર, ટીવી*) are fine in Gujarati
  script; never write English in Latin letters.
- **Spelling and typing**: write the standard spelling (see "The list") in NFC, as the list and the
  dictionary have it: the checker flags text that is not. Never type a zero-width joiner or
  non-joiner: the reader cuts a word there. Avoid abbreviations with a full stop (ડૉ., પ્રો., શ્રી.):
  write the word out (ડૉક્ટર).
- **Gujarati typography**: a sentence ends with `.`, `?` or `!` (Gujarati writes the full stop, not
  the danda); direct speech in double quotation marks. Write numbers as digits, Western or Gujarati
  (*5 લોકો, 2024માં, ૧૦ વાગ્યે*): the checker counts a number and a postposition written on it as a
  number, but a number word that is in your band is spelled out to teach it (*બે દિવસ*). A hyphen
  splits a word in two for the reader and the checker; write compounds as Gujarati writes them.
- **Names are names**: Gujarati has no capital letters, so a name that is also a word reads as the
  word. Never put a list word in `names`, and never use one as a character's name (no કિરણ "ray",
  ઉષા "dawn", રાજ "rule", અમિત, ભૂમિ, આશા, પ્રેમ as names; check a name with `rank.py`). Common names
  that are no list words are fine (મીના, કેતન, નીના, દિલીપ, ગીતા, અનિલ, પ્રિયા, હેમા, જયેશ, સોનલ, મહેશ,
  હેતલ, નિખિલ, પારુલ, હિરેન, મનોજ, દક્ષા, ભાવિન, ચેતન, મેહુલ, નયના, ફાલ્ગુની); a country or city name may be
  used as what it is; language names are everyday words: teach them (ગુજરાતી, હિંદી, અંગ્રેજી).
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any everyday, cultural or story topic is fine
(festivals, history, myths and folk tales, politics, cricket). Avoid only explicit sexual content,
slurs used as insults and gratuitous gore. Teach ordinary words in their main sense. Skip list
noise: fragments, abbreviations and words of other languages.

## The file

Write `tools/courses/gu/<id>.json`, the shape of `gu-mini-0100.json`:

```json
{
  "id": "gu-mini-0200",
  "rankUpTo": 200,
  "level": "A1",
  "title": "The title from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["મીના", "અમદાવાદ"],
  "lessons": [
    {
      "title": "story title in Gujarati",
      "summary": "One line in English on what happens.",
      "newWords": ["the band headwords this lesson introduces"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

`names` holds every person and place name the lessons use, in the plain form (અમદાવાદ, not
અમદાવાદમાં): the checker reads the postpositions written on (અમદાવાદમાં, મીનાનો, કેતનને, સુરતથી). A name
of several words (નવી દિલ્હી, ન્યૂ યોર્ક) counts as one name where it is written whole.

## The rules, checked by `python3 tools/check_course.py tools/courses/gu/<id>.json`

- **Vocabulary**: 90% (ranks up to 100), 93% (to 300) or 95% (above) of a lesson's words are ranked
  within the course's `rankUpTo`; names count as known. A form the list lacks counts when the list
  builder's rules make it from a listed word (the checker reads it so: કરાવીશું, છોકરાઓમાં), or when
  `extra_forms.tsv` has it.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson. Reuse earlier courses' words freely.
- **Length** (story and questions together, in words as the reader cuts them): 80–180 to rank 100,
  120–240 to 300, 180–320 to 600, 220–380 to 1000, 260–450 to 2000, 300–520 to 3000, 320–550 to
  4000, 350–600 after.
- **Coverage of the band**: teach 80–100 of the band's words; the checker's last line lists the band
  words not taught yet.
