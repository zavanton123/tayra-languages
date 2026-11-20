# Writing a Nepali frequency course

Each course teaches one band of 100 words from the Nepali word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its forms). Course `ne-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 5,009 words: `ne-mini-0100` to `ne-mini-5100`
(the last band has 9 words).

## The register

**Standard written Nepali of Nepal (मानक नेपाली)**, plain and correct, as graded readers, children's
books and the newspapers (कान्तिपुर, गोरखापत्र) write it. Dialogue may be everyday spoken Nepali as
stories write it (*के छ?*, *हजुर*, *ठीक छ*, *हो र?*, *धन्यवाद*), but no Hindi words or endings (*है,
हैं, नहीं, में, का घर*), no romanised Nepali and no English mixed into a sentence beyond the loanwords
Nepali writes in Devanagari (*फोन*, *बस*, *डाक्टर*, *कम्प्युटर*).

| Point | Write | Not |
|---|---|---|
| "you" | *तपाईं* to an adult, a stranger, a parent or anyone you address politely (verb *-नुहुन्छ*: *तपाईं जानुहुन्छ*); *तिमी* to a friend, a child or a younger sibling (*तिमी जान्छौ*); *तँ* only to a small child or in anger; *हजुर* to an elder in the family; one form per pair of speakers | mixing *तपाईं* and *तिमी* to the same person |
| Respect | a parent, a teacher or an older person takes the honorific in narration too: *आमा आउनुहुन्छ*, *हरि काम गर्नुहुन्छ*, pronoun *उहाँ*; a peer *उनी* (*उनी आउँछन्*, *उनी आउँछिन्*); a child or a friend *ऊ* (*ऊ आउँछ*, *ऊ आउँछे*) | *आमा आउँछ* |
| Postpositions | written joined to the noun, pronoun or name: *घरमा*, *रामको*, *मलाई*, *केटाहरूसँग*, *पोखराबाट*, *घरभित्र* | *घर मा*, *राम को* |
| Postposition after a number | apart, as the news write it: *२०८१ मा*, *मंसिर १५ मा*, *५ बजे* | |
| The ergative | *ले* on the subject of a transitive verb in the past (and often the present): *सीताले भात खाई*, *रामले भन्यो* | *सीता भात खाई* |
| Numbers | Devanagari digits: *१० वर्ष*, *७ बजे*, *२०८१ साल*; small numbers in words when they are band words | Western *10* |

**Spelling as the 2003 standard and the news write it (and as the reader needs it):**

- **Long and short vowels** as the dictionary has them: *हरू* (not *हरु*), *तपाईं*, *नयाँ*, *ठूलो*, *दुई*,
  *महिना*, *विद्यालय*, *सुरु*, *विभिन्न*. The list keeps the other spellings as forms (*ठुलो*, *दुइ*, *शुरु*),
  but write one spelling per word, the standard one.
- **ं and ँ**: *तपाईं*, *गरौं*, *छौं*, *यहाँ*, *हुँदैन*, *जाँदै*, *आउँछ*: the nasal on a verb's ending as the
  examples, never dropped (*हुदैन*, *जादै* are slips).
- **र्य**: write it without the joiner, as most text does: *गर्यो*, *पर्यो*, *गर्यौं*. The reader reads
  *गर्‍यो* (with the zero-width joiner) as another word.
- **Nukta letters** typed as letter + nukta (the checker asks for NFC); Nepali seldom needs them.
- **Punctuation**: the danda *।* ends a sentence; *?* and *!* as usual; the comma *,*. Speech after a comma
  or colon in straight double quotes: *सीता भन्छे, "म ठीक छु।"* Each speaker's line is a paragraph of its own
  (or *Name: "..."* lines in a dialogue).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. Everyday life in Nepal: Kathmandu, Lalitpur, Bhaktapur, Pokhara, Chitwan,
  Biratnagar, Dharan, Butwal, Janakpur, Nepalgunj, Ilam, a village in the hills or the Tarai, the
  mountains of Solukhumbu or Mustang; and now and then Nepali-speaking families in Darjeeling,
  Sikkim, Doha, London or Sydney.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony or open endings. Events in
  the order they happen. Few characters (one to three), named clearly; repeat the name wherever *ऊ*,
  *उनी* or *उहाँ* could be unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, Nepali order: subject,
  object, verb at the end (*सीता किताब पढ्छे।*). Present tense up to rank 300; from rank 400 the story
  may be told in the past. Dialogue short and simple.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300 (A1): *छ / छन् / छु / छौं* and *हो / हुन् / हुँ / हौं* (and their negatives *छैन*, *होइन*),
    *थियो / थिए*, the simple present (*गर्छ, गर्छे, गर्छन्, गर्छु, गर्छौ*) and its honorific (*गर्नुहुन्छ*),
    the progressive (*गर्दै छ*), the possessive *को / का / की* agreeing with what is owned (*रामको घर, रामका
    साथी, रामकी दिदी*), *मेरो / हाम्रो / तिम्रो / तपाईंको / आफ्नो*, the joined postpositions *मा, बाट, लाई, ले,
    सँग, सम्म, भन्दा*, the plural *हरू*, the imperative (*गर*, *गर्नुहोस्*), *सक्नु* and *चाहनु* with the
    infinitive (*जान सक्छ*, *जान चाहन्छे*), questions with *के*, *को*, *कहाँ*, *किन*, *कहिले*, *कसरी*, *कति*,
    *कस्तो*; the adjective in *-ो* agreeing (*राम्रो केटो, राम्री केटी, राम्रा मान्छे*); numbers with *जना* and
    *वटा* (*दुई जना*, *तीनवटा*);
  - ranks 301–600 (A2): the simple past with the ergative (*सीताले भात खाई*, *रामले भन्यो*, *हामी गयौं*), the
    negative present and past (*गर्दैन*, *गरेन*), comparison (*रामभन्दा ठूलो*, *सबैभन्दा राम्रो*), *जब ... तब*,
    *यदि ... भने*, *किनभने*, *त्यसैले*, the future in *-नेछ* and *-ला* (*जानेछ*, *आउला*); days, months and the time;
  - ranks 601–1000 (A2): the perfect (*गरेको छ*, *आएको थियो*), the conjunctive (*घर गएर*, *खाना खाएर*),
    obligation (*जानुपर्छ*, *गर्नुपर्ने*), *-पछि* and *-अघि* on a verb (*खाएपछि*, *जानुअघि*), the helpers
    *दिनु, सक्नु, रहनु, लिनु* on the conjunctive (*गरिदियो*, *खाइसक्यो*, *गरिरहेको*);
  - ranks 1001–1300 (B1): participles before a noun (*आएको मान्छे*, *गर्ने काम*), the passive (*भनिन्छ*,
    *बनाइयो*), *जो ... त्यो* relative clauses, *लाग्नु* with an infinitive (*रुन लागी*);
  - ranks 1301–2000 (B1): reported speech with *भनेर* and *भन्ने* (*उसले आउँछु भनेर भन्यो*), the past
    conditional (*गएको भए*, *गरेको भए हुन्थ्यो*), longer sentences joined with *तर*, *बरु*, *यद्यपि*, *जबकि*; a
    story may be told by a first-person narrator or as a letter, a message or a diary entry;
  - ranks 2001–3000 (B2): the learned (tatsam) words of everyday writing (*व्यवस्था*, *अवस्था*, *प्रयास*),
    participles as adjectives (*लेखिएको*, *फुटेको*); a lesson may also be a short informative or opinion
    text, as long as it is everyday, calm and easy to follow;
  - ranks 3001–4000 (B2): connectors such as *यसका बाबजुद*, *अर्कोतर्फ*, *यसैले*, *फलस्वरूप*; the light column
    (*स्तम्भ*) on everyday life, common proverbs (*उखान*) and idioms explained by context;
  - ranks 4001–5100 (C1): formal written style, verbal nouns in place of clauses (*घर पुगेपछि*, *काम
    गर्नुअघि*), a short essay or reflective column that weighs two sides, an interview, a portrait, a
    review; the specialised words of professions, sciences and the arts, each made clear by its context.
  A learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: the new words three times or more where it reads naturally, the story's
  key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `प्रश्नहरू:` with 3–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words (after rank 1000 they may also ask *किन* and *कसरी*, with fuller answers):

  ```
  प्रश्नहरू:
  सीता पोखरामा बस्छे?
  हो, सीता पोखरामा बस्छे।
  ```

  The header `प्रश्नहरू:` is layout: the checker does not count it as a word.
- **Original texts**: write your own stories; never copy or retell existing course material.
- **Topics**: any everyday, cultural or story topic is fine (WRITER-BRIEF.md step 4: no topic
  rules); avoid only explicit sexual content, slurs used as insults and gratuitous gore.

## The file

Write `tools/courses/ne/<id>.json`, the same shape as the other languages':

```json
{
  "id": "ne-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Nepali, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Nepali",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ne/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *गर्छ* or *गरेको* for *गर्नु*, *घरमा* for *घर*, *राम्री* for *राम्रो*).
  `newWords` holds the word as the list writes it: the infinitive of a verb in *-नु* (*जानु*, *गर्नु*), the
  bare noun, the masculine of an adjective in *-ो*.
- **Length** (story and questions together): 80–180 words per lesson in the first course, 120–240 up
  to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550
  up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words; the checker's last line lists the
  band words not taught yet.

Things to know about the Nepali list and the checker:

- The courses are pinned to `tools/courses/ne/wordlist.tsv` (a copy of the app's `ne.tsv`, counted from
  Leipzig's Nepali news of 2020 and web pages of 2015 (the court judgments left out), OpenSubtitles and
  Leipzig's Wikipedia sentences); the `bands/*.txt` come from it, and `extra_forms.tsv` adds the forms the
  corpus never showed: verbs' table forms (the feminine *-छे*, *-छिन्*, the honorific), and nouns' plural and
  postpositions. The checker reads words as the reader does: runs of Devanagari letters and signs, cut at
  spaces, punctuation, the danda and digits.
- **A word with its postpositions is that word**: *घरमा, घरबाट, घरहरूलाई, घरभित्रको* all count as *घर*;
  *मलाई, मेरो, मैले* as *म*; *आफ्नो* as *आफू*; *यस, यसको, यसले* as *यो*; *उसको* as *ऊ*; *उनको* as *उनी*;
  standalone *का, की* as *को*. A name takes them too (*रामको*, *पोखरामा*, *सीतालाई*): list the bare name in
  `names` and every joined form of it counts as known.
- **The copulas are words of their own**: *छ* (with *छन्, छु, छौं, छिन्, छे*), *छैन* (with *छैनन्*), *हो* (with
  *हुन्, हुँ, हौं*), *होइन* (with *हैन*); *थियो, थिए, भयो, भएको, हुन्छ, हुने, हुँदैन, हुन्न* are *हुनु*'s.
- **Verbs are listed by the infinitive in -नु** with their forms under it, the helpers written on included:
  *गरिरहेको, गरिदियो, गर्नुपर्छ, गरेपछि* count as *गर्नु*. A passive that is a word of the pack is its own word
  (*देखिनु*, *भनिनु*, *पाइनु*), otherwise it is its verb's (*गरिन्छ* is *गर्नु*'s).
- **The news shows in the first ranks**: *सरकार, कम्पनी, जिल्ला, विकास, निर्माण, कार्यक्रम, प्रतिशत, बैंक* are in
  the first hundred, and everyday nouns come later than you might expect: *नाम* 121, *आमा* 150, *साथी* 195,
  *विद्यालय* 263, *पानी* 264, *बुबा* 301, *खाना* 352, *भाइ* 606, *दिदी* 1215, *हजुरआमा* 1777. Check a word's rank
  (`rank.py` in the writer folder) rather than guess.
- **Most words have no entry in the app's ne-en dictionary**: the pack has 2,200 entries, so the list also
  takes the words the corpus uses that the English Wiktionary's Hindi entries spell alike (*कारण*, *अवस्था*,
  *जानकारी*). They are Nepali words all the same; teach them as usual.
- **List noise**: a few names that are also words rank high (*श्रेष्ठ*, *सिंह*, *आचार्य*), and some titles and
  party names (*जी*, *श्री*, *कांग्रेस*, *माओवादी*): skip them, a band of 80–100 taught words is normal.
