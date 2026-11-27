# Writing a Tamil frequency course

Each course teaches one band of 100 words from the Tamil word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `ta-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 5,889 words: the last course is `ta-mini-5900`
(ranks 5801–5889).

## The list

The list is counted from web text and subtitles (wordfreq), film subtitles (OpenSubtitles) and news
(Leipzig Corpora Collection) by the analyser in `tools/frequency_tamil.py`: every corpus word is
read back to its dictionary form (a noun's nominative, a verb's root) by the tables of
`dictionaries/ta-en.sqlite` and the rules for the forms they lack, and the forms are listed after
it. So:

- **Every listed word has an entry in `dictionaries/ta-en.sqlite`** (but for a few dozen everyday
  words added by hand: தற்போது, ஏனெனில், ஏதாவது, வயது, அடிப்படை, பட்டியல்…). The pack is small
  (12,600 entries): common words it lacks are not in the list at all (நிமிடம் is there only as
  நிமிஷம்'s form). Check each word you teach against its entry (WRITER-BRIEF step 6). The list past
  rank 4000 was rebuilt: it opens with seven verbs made with இடு that the pack lacks (வெளியிடு
  "publish", திட்டமிடு, போட்டியிடு, உத்தரவிடு, பார்வையிடு, கையெழுத்திடு, மண்டியிடு), and leaves out
  the names, spoken words and pieces of other words the earlier bands had.
- **The ranks lean to the news.** Words of reports rank high (அரசு 47, நிறுவனம் 80, வழங்கு 91,
  தெரிவி 98, மாவட்டம் 105), everyday words lower than in speech (அம்மா 184, அப்பா 316). Write
  everyday stories anyway: a high-ranked formal word goes into an ordinary scene (a small company
  that gives children films, a district bus, the state government's new school).
- **Verbs are listed by their root** (செய், போ, வா, இரு, படி), the form of the singular
  imperative; nouns by the nominative (வீடு, மரம், பள்ளி). `newWords` holds the headword as the band
  has it. The checker reads every regular form back to its word by the same rules: the cases and the
  plural (வீட்டில், மரத்தை, பள்ளிக்கு, மக்களிடம்), the verb's tenses and persons (செய்கிறாள்,
  செய்தார், செய்வேன், செய்கின்றனர்), its participles and verbal nouns (செய்த, செய்து, செய்யும்,
  செய்வது, செய்தால்), its compounds with இரு, உள், விடு, கொள் and படு (செய்திருக்கிறார்,
  செய்துள்ளார், செய்துவிட்டான், செய்துகொண்டிருக்கிறாள், செய்யப்பட்டது), the clitics -உம், -ஏ, -ஆ,
  -ஓ, -தான் (வீட்டிலும், அவனே, வருகிறாயா, அதுதான்) and the doubled consonant before the next word
  (பள்ளிக்குப், அந்தப், தமிழ்ப்). Any correct written form of a known word counts.
- **Headwords to know**:
  - `என்` (20) is the verb "say" (என்கிறார், என்றார், என்பது's forms); "my" (என்) is a form of
    `நான்`. The quotative `என்று`, `என`, `என்ற`, `என்பது`, `எனும்` and `என்றால்` are words of their
    own.
  - `ஆ` (34) is "become": ஆகும், ஆன, ஆனது, ஆகி, ஆகிய, ஆனார். `ஆனால்` "but" is its own word.
  - `உள்` (13) holds உள்ளது, உள்ள, உள்ளனர், உள்ளன, உள்ளார் "there is, being".
  - `கொண்டு` "with, using", `பற்றி` "about", `போது` "when", `வரை` "until", `மூலம்` "through",
    `தொடர்ந்து`, `குறித்து`, `விட` "than", `கூட` "even, with" are words of their own, not the forms of
    the verbs they come from.
  - The pronouns are words of their own (நான், நாம், நாங்கள், நீ, நீங்கள், அவன், அவள், அவர்,
    அவர்கள், அது, இது, அவை, இவை, தான், தாம், தாங்கள்); their cases are their forms (என்னை,
    எனக்கு, உன், உங்களுக்கு, அவரது, தனது).
  - A person noun in -அன் is listed under its polite form in -அர் when that is the commoner
    (தலைவர், மாணவர், அமைச்சர், நண்பர்); the -அன் form (தலைவன், மாணவன்) is its form.
- **Names in the list**: Tamil has no capitals, so some names landed in the list as words (குமார்
  1057, முருகன் 1018, தேவி 1366, ராஜா 911, கோவை is left out). Never teach a name as a word and
  never use a list word as a character's name.
- **Spoken forms are not words**: the subtitles' spoken Tamil (போறேன், இருக்கு, அவங்க, இங்க, நல்லா)
  is mostly unread or filed under its written word; write the written standard (below).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *ரவியின் போன் எங்கே? அவன் வீட்டில் பார்க்கிறான். போன் அங்கே இல்லை.*
  Everyday life where Tamil is spoken: Chennai, Madurai, Coimbatore, Tiruchirappalli (திருச்சி),
  Salem, Thanjavur, Tirunelveli, Vellore, Kanyakumari, Ooty, Kodaikanal, Puducherry; the Kaveri, the
  Vaigai, the Western Ghats, the Coromandel coast, rice fields, temple towns; Jaffna, Colombo,
  Batticaloa and Trincomalee in Sri Lanka; Singapore and Malaysia; and now and then Bengaluru,
  Mumbai, Dubai, London or Toronto, where Tamil speakers live: home, family, food, shopping, work,
  school, the town, health, free time, travel, festivals (பொங்கல், தீபாவளி) and the stories of the
  themes.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, jumps in time, irony or open
  endings. Events in the order they happen. Few characters (one to three), named clearly; say who
  அவன் / அவள் / அவர் is whenever it could be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order
  subject, object, verb (*சுதா காபி குடிக்கிறாள்.*), the verb last. "X is Y" without a verb (*இது
  ஒரு நகரம். வீடு பெரியது.*); இருக்கிறது / இருக்கிறான் for "is there", அல்ல for "is not (a) Y",
  இல்லை for "is not there" and the past negative.
- **Politeness**: நீ (with the -ஆய் verb) among friends and family of the same age or younger;
  நீங்கள் (with -ஈர்கள்) to elders, strangers, in shops and offices. A respected adult is அவர் / இவர்
  with the -ஆர் verb all through the story (*பாலு வேலை செய்கிறார்.*); a child or a friend is அவன் /
  அவள். A small child or an animal may be அது with the -அது verb (*குழந்தை சிரிக்கிறது*). Keep a
  character's form the same in narration and questions.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present (*செய்கிறான், செய்கிறாள், செய்கிறார், செய்கிறது, செய்கிறேன்,
    செய்கிறார்கள்*); இரு (*இருக்கிறது, இருக்கிறான்*) and உள்ளது / உள்ளனர்; the cases: accusative
    *-ஐ*, dative *-க்கு / -உக்கு*, locative *-இல்*, genitive *-இன் / -உடைய* and the oblique
    (*வீட்டு, மரத்து*), *-உடன்*, *-இடம்* with persons; the plural *-கள்*; இல்லை and அல்ல;
    வேண்டும் with the dative (*சுதாவுக்குத் தண்ணீர் வேண்டும்*); questions with என்ன, யார், எங்கே,
    எப்போது, ஏன், எப்படி, எந்த, எத்தனை and *-ஆ* (*இருக்கிறதா?*); *-உம்* "also, and" and *-ஏ*
    (*வீட்டிலும், அவனே*);
  - ranks 301–600: the simple past (*செய்தான், வந்தாள், போனார், எழுதினார்*) and the future
    (*செய்வேன், வருவார், போகும்*); the verbal participle that joins actions (*வீட்டுக்கு வந்து
    சாப்பிடுகிறான்*); the infinitive with வேண்டும், முடியும் and *-லாம்* (*போகலாம்*); imperatives
    (*வா / வாருங்கள், செய் / செய்யுங்கள்*); *மேல், கீழ், முன், பின், அருகில், உள்ளே, வெளியே*;
  - ranks 601–1000: the negatives (*செய்யவில்லை, செய்யமாட்டேன், செய்யாதே, செய்யாமல், செய்யாத*);
    the relative participles (*படிக்கும் பையன், வந்த மக்கள்*); என்று after a quotation or a thought
    (*"வருகிறேன்" என்று சொன்னாள்*); comparison with *-ஐ விட* (*மதுரையை விடப் பெரியது*); the
    conditional *-ஆல்* (*மழை வந்தால்*) and *-போது* (*வரும்போது*);
  - ranks 1001–2000: the progressive (*செய்துகொண்டிருக்கிறான்*) and the perfect (*வந்திருக்கிறாள்,
    செய்துவிட்டார்*); verbal nouns in *-அது* (*படிப்பது பிடிக்கும்*); the concessive *-ஆலும்*; *-ஆக*
    and *-ஆன* (*அழகாக, அழகான*); causatives (*செய்வி, காட்டு*); கொள் and விடு after the participle
    (*கற்றுக்கொண்டாள், சாப்பிட்டுவிட்டான்*);
  - ranks 2001–3000: the written register of newspapers where Tamil prose uses it: the present in
    *-கின்ற-* (*செய்கின்றனர்*), the past plural *-அனர்* (*வந்தனர்*), the perfect with உள்
    (*செய்துள்ளார்*), the passive with படு (*கட்டப்பட்டது*); connectives (*எனவே, ஏனெனில், ஆனாலும்,
    இருப்பினும்*); reported speech with என்று / என;
  - ranks 3001–5000: a richer written register: the Sanskrit-derived and the pure Tamil words of
    newspapers and books beside their everyday ones, made plain by context; a column, a letter, an
    interview, a short essay that weighs two sides; long sentences that stay easy to parse;
  - ranks 5001–5889 (C1): the same, with the specialised words of the sciences, the arts and
    public life, each made clear by its context.
  After rank 1000 sentences get longer and dialogue can carry more of the story; a story may be told
  by a first-person narrator or as a letter, a message or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text, as long as it is calm and easy to follow.
- **Repetition on purpose**: the new words three times or more where it reads naturally (the
  checker needs two), the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `கேள்விகள்:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  கேள்விகள்:
  கீதாவின் வீடு எங்கே இருக்கிறது?
  கீதாவின் வீடு சென்னையில் இருக்கிறது.
  ```

  The header `கேள்விகள்:` is layout: the app and the checker do not count it as a word.
- **Standard written Tamil**: the narration, the dialogue and the questions use the written
  standard of books and newspapers (எழுத்துத் தமிழ்), never the spoken forms (*போகிறேன்*, not
  *போறேன்*; *இருக்கிறது*, not *இருக்கு*; *அவர்கள்*, not *அவங்க*; *இங்கே*, not *இங்க*), and no
  regional dialect. English loans Tamil has made its own (*பஸ், போன், டாக்டர், டிவி*) are fine in
  Tamil script; never write English in Latin letters.
- **Spelling and sandhi**: write the list's spelling (*கோயில்*, not *கோவில்*; *பிரச்சினை*; *ஆகஸ்டு*),
  and the doubled consonant of the written standard before a word in க, ச, த or ப: after the
  accusative and the dative (*வீட்டைப் பார்த்தான், பள்ளிக்குப் போனாள்*), after அந்த, இந்த, எந்த
  (*அந்தப் பெண்*), after the infinitive and the participle in -இ (*படிக்கப் போனான், தேடிப்
  பார்த்தாள்*) and after -ஆக (*நன்றாகப் பேசுகிறாள்*); never after a subject (*ரவி போனான்*). The
  checker reads a word with or without it.
- **Tamil typography**: a sentence ends with a full stop, `?` or `!`; direct speech in double
  quotation marks, each speaker's line a paragraph of its own. Write numbers as Western digits
  (*3 குழந்தைகள், 5 மணிக்கு, 2019-இல்*), never as Tamil digits (*௩*), except a number word that is in
  your band: spell that one out to teach it (*இரண்டு குழந்தைகள்*). A hyphen splits a word in two for
  the reader and the checker; write compounds as Tamil writes them.
- **Names are names**: a country, state or city name may be taught as what it is when the list has
  it (தமிழகம் "Tamil Nadu" 116, இந்திய "Indian"; இந்தியா itself is a name); language names are everyday words: teach them (தமிழ், ஆங்கிலம்,
  இந்தி). A list word that is also a person's name (தேவி "goddess", ராஜா "king") is taught only in its
  common sense or skipped, never through the name. Never put a list word in `names`, and never use
  one as a character's name. Common Tamil first names are fine (கீதா, பாலு, ரேவதி, கணேஷ்); check
  each against the list (மீனா is மீன்'s form, பிரியா பிரி's, கலா கல்'s).
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any everyday, cultural or story topic is fine
(festivals and temples, history, kings and queens, myths and folk tales, politics, cricket, films).
Avoid only explicit sexual content, slurs used as insults and gratuitous gore. Teach ordinary words
in their main sense. Skip list noise: fragments, abbreviations, names and words of other languages.

## The file

Write `tools/courses/ta/<id>.json`, the shape of `ta-mini-0100.json`:

```json
{
  "id": "ta-mini-0200",
  "rankUpTo": 200,
  "level": "A1",
  "title": "The title from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["கீதா", "சென்னை"],
  "lessons": [
    {
      "title": "story title in Tamil",
      "summary": "One line in English on what happens.",
      "newWords": ["the band headwords this lesson introduces"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

`names` holds every person and place name the lessons use, in the nominative (சென்னை, not
சென்னையில்): the checker reads its case forms (சென்னையில், பாலுவுக்கு, திருப்பரங்குன்றத்துக்கு).

## The rules, checked by `python3 tools/check_course.py tools/courses/ta/<id>.json`

- **Vocabulary**: 90% (ranks up to 100), 93% (to 300) or 95% (above) of a lesson's words are ranked
  within the course's `rankUpTo`; names count as known.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson. Reuse earlier courses' words freely.
- **Length** (story and questions together, in words as the reader cuts them): 80–180 to rank 100,
  120–240 to 300, 180–320 to 600, 220–380 to 1000, 260–450 to 2000, 300–520 to 3000, 320–550 to
  4000, 350–600 after. Tamil words carry their endings, so a Tamil lesson has fewer words than its
  English translation: the length is in Tamil words.
- **Coverage of the band**: teach 80–100 of the band's words; the checker's last line lists the band
  words not taught yet.
