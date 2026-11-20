# Writing a Marathi frequency course

Each course teaches one band of 100 words from the Marathi word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `mr-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 4,480 words: the last course is `mr-mini-4500`
(80 words).

## The list

The list is counted from Leipzig's Marathi news (1.4 million sentences) and, at a fifth of the weight, its
Wikipedia's sentences, by `tools/frequency_marathi.py`: every corpus word is read back to its dictionary
form in the `mr-en` pack (a noun's direct singular, a verb's infinitive in -णे), and its forms are listed
after it. So:

- **The ranks lean to the news.** Words of reports rank high (पोलीस 29, जिल्हा 60, पक्ष 78, निवडणूक 88,
  मुख्यमंत्री 145, सरकार 164), everyday words lower than in speech (शाळा 153, मुलगी 230, पुस्तक 232,
  आई 362, मित्र 542, आवडणे 655). Write everyday stories anyway: a news word goes into an ordinary scene
  (a police officer who shows the way, a village that asks the district for water, a film made in a village).
- **Verbs are listed by the infinitive in -णे** (करणे, जाणे, येणे, देणे, घेणे, होणे), with all their
  forms (करतो, केला, केलेल्या, करणार, करायला, करून, करण्यासाठी); `newWords` holds the headword as the
  band has it. आहे, आहेत, होता, होते are असणे's; नव्हता is नसणे's; नाही "no, not" is a word of its own
  (with नाहीत, नाहीये). A noun's case forms and postpositions written on its oblique (घरात, घराला, घराचा,
  घरासाठी; शाळेत, शाळेपासून; पाण्यात) or on the noun as it is (सरकारने, रेल्वेत, सकाळचा, प्रकारचे), an adverb's
  genitive (सध्याचा, आतापर्यंत) and the pronouns' (मला, माझा, त्याला, त्याच्याबरोबर, तिच्या, आमच्या, यांनी) are its
  forms.
- **Adjectives in -ा are cited in the neuter -े**, as the pack has them: मोठे (मोठा, मोठी, मोठ्या), नवे,
  जुने, काळे, दुसरे, पहिले. Teach the headword as the band gives it.
- **हा, ही, हे and तो, ती, ते are six words**: हा "this (m)" holds या, याचा, यासाठी; हे "these, this (n)"
  holds यांनी, यांना (the honorific "he, she" of the news: *पाटील यांनी सांगितले*); तो holds त्या, त्याला,
  त्याच्या; ते holds त्यांना, त्यांनी. The headwords' ranks are 3, 4, 9, 7, 19, 25.
- **List noise**: some words collect stray forms (रोज holds रोजी "on (a date)", मध्ये holds मध्या), a few
  ranks are lifted by names in the news (राज 562, प्रकाश 484, किरण 776, यश 292 are also first names); a
  few shared spellings are split by how common each word is (गावी "in the village" and गाणे's गावी "should
  sing"). Teach a word in its main sense; skip a word that is only noise.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *अजयला एका कंपनीत नवे काम मिळाले आहे. आज कामाचा पहिला दिवस आहे.*
  Everyday life where Marathi is spoken: Maharashtra (Mumbai, Pune, Nagpur, Nashik, Kolhapur,
  Chhatrapati Sambhajinagar, Solapur, Satara, Sangli, Ratnagiri, Amravati, Latur, Jalgaon; the Konkan
  coast, the Sahyadri forts, the Godavari and the Krishna, villages, sugar-cane and cotton fields, wadas
  and chawls) and Goa, Belagavi and Indore where Marathi speakers live; and now and then Delhi,
  Bengaluru, London or the Gulf: home, family, food (पोळी, भाकरी, वरण-भात, पोहे, मोदक), shopping, work,
  school, the town, health, free time, travel, festivals (गणेशोत्सव, दिवाळी, गुढीपाडवा, होळी,
  संक्रांत, पंढरपूरची वारी) and the stories of the themes.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, jumps in time, irony or open
  endings. Events in the order they happen. Few characters (one to three), named clearly; say who
  तो / ती / ते is whenever it could be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order
  subject, object, verb, the verb last (*रमेश पाणी आणतो.*). The verb agrees with the subject in the
  present and future, and with the object in the past of a transitive verb (*सुनीताने भाजी आणली*,
  *रमेशने पुस्तक वाचले*), the agent then in -ने: bring that in with the past (see below), and keep it right.
- **Politeness**: तू among friends, family and children; तुम्ही to elders, strangers, in shops and
  offices; आपण for a polite "you" and the inclusive "we". An elder or a respected person takes the
  plural verb all through the story (*आजी म्हणतात, बाबा येतात, अधिकारी सांगतात*); a child or a friend
  the singular (*तो म्हणतो*). Keep a character's form the same in narration and questions.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: the present (*करतो, करते, करतात*); आहे / आहेत and नाही; the cases on the oblique
    (*घराला, घराचा, घरात, मुंबईत, शाळेत*) and the commonest postpositions written on (*-साठी, -मध्ये,
    -वर, -कडे, -बरोबर*); the genitive agreeing with its noun (*माझा भाऊ, माझी आई, माझे घर*); questions with
    काय, कोण, का, कुठे (243), किती (310); the future in -णार (*करणार आहे*) and the clitics -च "just",
    -ही "also" (*आजच, मीही*);
  - ranks 301–600: the simple past and its agreement (*तो गेला, ती गेली; मी पुस्तक वाचले*), the agent
    in -ने (*त्याने, सुनीताने*); the future (*करेन, करशील, करेल, करू, कराल, करतील*); the conjunctive
    participle that joins actions (*जेवून झोपतो, घरी जाऊन पाहते*); the infinitive of purpose (*पाहायला
    जातो, खेळायला येते*); imperatives (*ये / या, बस / बसा, करा*); पाहिजे and हवा / नको (*मला चहा हवा*);
  - ranks 601–1000: the progressive (*करत आहे, करत होता*); the perfect with the participle (*केलेले काम,
    आलेले लोक*); the conditional (*पाऊस पडला तर, वेळ मिळाला तर*); जेव्हा … तेव्हा, जर … तर, जो … तो;
    comparison with -पेक्षा (*त्याच्यापेक्षा मोठा*); the subjunctive / "should" (*करावे, जावे*);
  - ranks 1001–2000: the habitual past (*करायचा, जायची*); compound verbs (*खाऊन टाकणे, सांगून देणे,
    निघून जाणे*); the infinitive with its cases (*करण्यासाठी, जाण्याआधी, आल्यानंतर, केल्यामुळे*); reported
    speech with की and म्हणून; the passive of news prose where Marathi uses it (*काम पूर्ण करण्यात आले*);
  - ranks 2001–3000: longer joined sentences (*कारण, त्यामुळे, म्हणून, तरीही, उलट*); causatives
    (*करवणे, दाखवणे, शिकवणे*); relative clauses with जो / जी / जे, जिथे … तिथे; idioms of everyday speech;
  - ranks 3001–4500 (B2–C1): a richer written register: the Sanskrit-derived (तत्सम) words of
    newspapers and books beside their everyday ones, made plain by context; a column, a letter, an
    interview, a short essay that weighs two sides; long sentences that stay easy to parse.
  After rank 1000 sentences get longer and dialogue can carry more of the story; a story may be told
  by a first-person narrator or as a letter, a message or a diary entry. From rank 2000 a lesson may
  also be a short informative or opinion text, as long as it is calm and easy to follow.
- **Repetition on purpose**: the new words three times or more where it reads naturally (the
  checker needs two), the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `प्रश्न:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  प्रश्न:
  रमेश कुठे राहतो?
  रमेश शिरूर गावात राहतो.
  ```

  The header `प्रश्न:` is layout: the app and the checker do not count it as a word.
- **Standard Marathi (प्रमाण मराठी)**: the narration, the dialogue and the questions use the standard
  written Marathi of today's books and newspapers; a character may speak colloquially (*करतोय, आलंय,
  काय झालं?*), but no regional dialect (Varhadi, Malvani, Ahirani). English loans Marathi has made its
  own (*बस, फोन, ऑफिस, शाळा, डॉक्टर, टीव्ही*) are fine in Devanagari; never write English in Latin letters.
- **Spelling and typing**: write today's standard spelling (*महत्त्व, पोलीस, सुद्धा, येथे / इथे*). Write
  the eyelash ra as ऱ + virama (*दुसऱ्या, करणाऱ्या, शेतकऱ्यांना*), as the list and the dictionary have it:
  never type a zero-width joiner or non-joiner (र् + joiner + या is another word to the reader; the
  checker flags it). English vowels are ॲ / ऑ (*बॅग, ऑफिस*). Type a nukta letter as the letter and the
  nukta (NFC).
- **Marathi typography**: a sentence ends with the full stop `.`, `?` or `!` (Marathi prose does not use
  the danda); direct speech in double quotation marks “ ”, each speaker's line a paragraph of its own
  when the dialogue grows. Write numbers as digits, Western or Devanagari (*५ मुले, 2024 साली, १०वी*):
  the checker counts a number and an ending written on it as a number, but a number word that is in your
  band is spelled out to teach it (*दोन मुले*). A hyphen splits a word in two for the reader and the
  checker; write compounds as Marathi writes them.
- **Names are names**: Marathi has no capital letters, so a name that is also a word reads as the
  word. Never put a list word in `names`, and never use one as a character's name (no विजय "victory",
  सीमा "border", आशा "hope", प्रकाश "light", किरण "ray", यश "success" as names). Common names are fine
  (रमेश, सुनीता, अजय, ओंकार, गौरी, प्रिया, नेहा, अमित, रोहन, सायली, संजय, राहुल, अदिती, सचिन, मानसी,
  निखिल, स्वाती); a country or city name may be taught as what it is; language names are everyday words:
  teach them (मराठी, हिंदी, इंग्रजी).
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Any everyday, cultural or story topic is fine
(festivals, history, myths and folk tales, politics, cricket). Avoid only explicit sexual content,
slurs used as insults and gratuitous gore. Teach ordinary words in their main sense. Skip list
noise: fragments, abbreviations and words of other languages.

## The file

Write `tools/courses/mr/<id>.json`, the shape of `mr-mini-0100.json`:

```json
{
  "id": "mr-mini-0200",
  "rankUpTo": 200,
  "level": "A1",
  "title": "The title from themes.tsv",
  "description": "One sentence in English: what the stories are about.",
  "topic": "The English topic from themes.tsv",
  "names": ["रमेश", "पुणे"],
  "lessons": [
    {
      "title": "story title in Marathi",
      "summary": "One line in English on what happens.",
      "newWords": ["the band headwords this lesson introduces"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

`names` holds every person and place name the lessons use, in the plain form (पुणे, not पुण्यात): the
checker reads its case endings, postpositions and clitics, on the name and on its oblique (पुण्यात,
रमेशला, नाशिकमध्ये, गौरीचा). A name of several words (नवी मुंबई) counts as one name where it is written
whole, its last word's endings too.

## The rules, checked by `python3 tools/check_course.py tools/courses/mr/<id>.json`

- **Vocabulary**: 90% (ranks up to 100), 93% (to 300) or 95% (above) of a lesson's words are ranked
  within the course's `rankUpTo`; names count as known. A form the list lacks counts when
  `extra_forms.tsv` has it (each word's regular cases, postpositions and verb forms the corpus lacked:
  शाळेपासून, करशील, पाहिल्यानंतर), or when it is a listed form with -च, -ही or -सुद्धा on it (घरीच,
  आजही, मीसुद्धा).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson. Reuse earlier courses' words freely.
- **Length** (story and questions together, in words as the reader cuts them): 80–180 to rank 100,
  120–240 to 300, 180–320 to 600, 220–380 to 1000, 260–450 to 2000, 300–520 to 3000, 320–550 to
  4000, 350–600 after.
- **Coverage of the band**: teach 80–100 of the band's words; the checker's last line lists the band
  words not taught yet.
