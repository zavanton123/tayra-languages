# Writing a Hindi frequency course

Each course teaches one band of 100 words from the Hindi word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `hi-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has 9,665 words: the last course is `hi-mini-9600`.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *राहुल का फ़ोन कहाँ है? वह घर में देखता है। फ़ोन वहाँ नहीं है। वह बाहर देखता
  है...* Everyday life where Hindi is spoken: Delhi, Mumbai, Jaipur, Lucknow, Pune, Bhopal, Indore,
  Kanpur, Agra, Shimla, Chandigarh, Dehradun, Nainital, Udaipur, Jodhpur, Gwalior, Raipur, Ranchi;
  the Himalayas, the Ganga and the Yamuna as rivers, the Thar desert, hill stations and village fields;
  and now and then London, Toronto, Dubai, Singapore or Nairobi, where Hindi speakers live: home,
  family, food, shopping, work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *वह* is whenever it could be unclear.
  Give each story its own names: the learner reads the course in one go, and two Rahuls in two
  lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, object, verb: *अनीता चाय बनाती है।*), the verb last. The narration uses the written
  pronouns *वह* and *यह* (singular), *वे* and *ये* (plural); dialogue may use the spoken *वो* for *वह*.
  Little dialogue; when there is some, short and simple, with *तुम* among family and friends of the
  same age or younger, and *आप* (with the plural verb: *आप कहाँ जा रहे हैं?*) with parents and
  elders, strangers, in shops and offices; a name with *जी* for respect (*शर्मा जी*, *माँ जी*).
  Never *तू* (intimate or rude) below rank 1000, and after that only between very close friends or
  to a small child.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *है*, *हैं*, *हूँ*, *हो* and *था*, *थे*, *थी*, *थीं*; the present habitual (*वह
    काम करता है*, *वह पढ़ती है*); the present progressive (*वह खा रहा है*, *वे जा रहे हैं*); the
    simple past (perfective) of verbs without an object (*वह घर गया*, *अनीता आई*) and, with *ने*,
    of verbs with one (*राहुल ने चाय बनाई*, *उसने कहा*), the verb agreeing with the object;
    gender and number agreement in every form (*अच्छा लड़का*, *अच्छी बात*, *अच्छे लोग*); the
    postpositions *का/की/के*, *में*, *पर*, *से*, *को*, *तक*, *के लिए*, *के साथ*, *के बाद*, *के बारे
    में*, and the oblique forms before them (*घर में*, *उस दिन*, *लड़के को*); *मुझे ... चाहिए*;
    *सकना* (*वह आ सकता है*); the future in *-गा* (*वह आएगा*, *हम चलेंगे*); the imperative with
    *तुम* (*आओ*, *देखो*, *बताओ*) and *आप* (*आइए*, *बैठिए*, *कीजिए*); questions with *क्या*, *कौन*,
    *कहाँ*, *कब*, *क्यों*, *कैसे*, *कितना*;
  - ranks 301–600: the dative-subject phrases (*मुझे पता है*, *उसे पसंद है*, *मुझे लगता है कि*,
    *उसे भूख लगी*); compound verbs with *लेना*, *देना*, *जाना* (*खा लिया*, *बता दो*, *सो गया*); the
    conjunctive participle *-कर* / *-के* (*खाना खाकर वह सोता है*); comparatives with *से* (*से
    बड़ा*, *सबसे अच्छा*); *चाहना* + infinitive (*वह जाना चाहती है*); the habitual past (*वह पढ़ता
    था*) for background and habits;
  - ranks 601–1000: the past progressive (*वह खा रहा था*); obligation (*मुझे जाना है*, *उसे जाना
    पड़ा*, *तुम्हें आना चाहिए*); the relative pair *जो ... वह* (*जो लड़का आया, वह मेरा भाई है*);
    *जब ... तब*, *अगर ... तो* with the present or future for real plans; reported speech with *कि*
    (*उसने कहा कि वह आएगा*); the infinitive with *वाला* (*जाने वाला है*);
  - ranks 1001–1300: the present and past perfect (*मैंने देखा है*, *वह जा चुका था*); *लगना* +
    oblique infinitive (*वह हँसने लगी*); the passive with *जाना* in plain cases (*यहाँ हिंदी बोली
    जाती है*); *-ते हुए* and *-ते ही* (*घर पहुँचते ही*);
  - ranks 1301–1600: the subjunctive (*शायद वह आए*, *मैं चाहता हूँ कि तुम आओ*, *ताकि*); the
    presumptive (*वह घर पर होगा*); causative verbs (*बनवाना*, *दिखाना*, *पिलाना*);
  - ranks 1601–2000: unreal conditions with the past habitual form (*अगर समय होता तो मैं आता*),
    indirect questions (*पता नहीं वह कहाँ है*), participles as adjectives (*बैठा हुआ आदमी*, *खुली
    खिड़की*), *जैसे ही*, *जब तक ... नहीं*;
  - ranks 2001–2500: longer joined sentences (*क्योंकि*, *इसलिए*, *फिर भी*, *बल्कि*, *जबकि*),
    reported speech with shifts of person, the more formal words of newspapers and books
    (*प्रयोग*, *आवश्यक*, *उपलब्ध*) beside their everyday ones (*इस्तेमाल*, *ज़रूरी*, *मिलना*);
  - ranks 2501–3000: a richer, more formal register: connectors such as *यद्यपि*, *अतः*, *परंतु*,
    *अर्थात*, *इसके बावजूद*; idioms (*मुहावरे*) explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (a light reflection on everyday life), dialogue that
    marks *तुम* and *आप*;
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short essay
    or reflective column that weighs two sides, an interview, a portrait, a review; precise
    vocabulary for feelings, ideas and processes; long sentences that stay easy to parse;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and
    the arts, each made clear by its context;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely and words
    chosen for their shades of meaning;
  - ranks 8001–9600 (C2): texts close to what native readers meet: literary prose, academic and
    journalistic registers, Sanskrit-derived (*तत्सम*) and Persian-derived words explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story
  may be told by a first-person narrator or as an e-mail, a letter or a diary entry. From rank 2000 a
  lesson may also be a short informative or opinion text, as long as it is everyday, calm and easy to
  follow. Still one clear idea at a time.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*वह रसोई में देखता है। फ़ोन वहाँ नहीं है। वह बाहर देखता है। फ़ोन वहाँ भी नहीं है।*).
- **Questions at the end**: after the story, a paragraph starting `सवाल:` with 4–6 simple questions
  about the story, each followed on its own line by a full-sentence answer that reuses the story's
  words (after rank 1000 they may also ask *क्यों* and *कैसे*, with fuller answers):

  ```
  सवाल:
  राहुल क्या ढूँढता है?
  राहुल अपना फ़ोन ढूँढता है।
  फ़ोन कहाँ था?
  फ़ोन गाड़ी में था।
  ```

  The header `सवाल:` is layout: the checker does not count it as a word.
- **Standard Hindi**: the narration is the standard Hindi of good books and newspapers (Devanagari as
  the Central Hindi Directorate writes it), neutral, natural and everyday: Hindustani words where
  people use them (*ज़रूरी*, *मदद*, *दोस्त*, *किताब*), neither heavily Sanskritised nor heavily
  Urdu. English loanwords Hindi has made its own (*फ़ोन*, *बस*, *स्कूल*, *डॉक्टर*, *टीवी*,
  *कंप्यूटर*) are fine; never write English in Latin letters, and never Hinglish (*मैं बहुत busy
  हूँ*). No slang and no vulgar words (the list has some: skip them).
- **Spelling**: write the standard spelling. Nukta in the Persian, Arabic and English loans that
  have it (*ज़रूर*, *ख़ुश*, *ग़लत*, *फ़ोन*, *क़दम*, *ज़्यादा*) and always in *ड़* and *ढ़* (*लड़का*,
  *पढ़ना*, *बड़ा*): without the dot they are other letters. Chandrabindu for a nasal vowel where no
  vowel sign sits above the line (*हाँ*, *यहाँ*, *माँ*, *पाँच*, *आँख*, *हूँ*), anusvara where one does
  (*में*, *नहीं*, *हैं*, *कहीं*) and for a nasal consonant before a consonant (*हिंदी*, *ठंडा*,
  *संबंध*). The list keeps the other spellings as forms (*ज्यादा*, *यहां*, *हिन्दी*), so they would
  count, but the courses write the standard ones. Type the nukta as its own sign after the letter
  (क + ़), as every Hindi keyboard does: the single-character nukta letters (U+0958–U+095F) are not
  read as the same word, by the app or the checker.
- **Hindi typography**: a sentence ends with the danda `।`, never a full stop (the app's reader
  keeps a full stop after some words as part of the word, and the checker flags it); `?` and `!` as
  usual. Direct speech in double quotation marks “…”, each speaker's line a paragraph of its own:

  ```
  “नमस्ते, सुनील जी,” अनीता कहती है।
  “आप कैसे हैं?” सुनील पूछते हैं। “आप चाय पिएँगी?”
  ```

  The questions and answers of the final `सवाल:` block are plain lines, as above. Write numbers as
  Western digits (*3 भाई*, *5 बजे*, *2019 में*), never as Devanagari digits (*३*, which the reader
  reads as words), except a number word that is in your band: spell that one out to teach it (*दो
  भाई*). A hyphen splits a word in two for the reader and the checker (*माता-पिता* is *माता* +
  *पिता*); write compounds as Hindi writes them.
- **Names are names**: place names, adjectives made from place names (*भारतीय*, *अमेरिकी*), and
  words for peoples or languages (*हिंदी*, *अंग्रेज़ी*, *पंजाबी*) count as names: skip them as band
  words even when the list has them as entries; a text may still use one (*वह हिंदी बोलती है*),
  counted at its rank. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God and gods (*भगवान*, *ईश्वर*, *अल्लाह*), temples, mosques, churches
  and gurdwaras, priests, prayer and *पूजा*, heaven and hell, souls and spirits, pilgrimages, holy
  rivers and holy cities as such, and every festival or custom of religious origin, even as a date, a
  school break or a gift season: Diwali, Holi, Eid, Christmas, Navratri, Durga Puja, Ganesh
  Chaturthi, Janmashtami, Raksha Bandhan, Karva Chauth, Chhath, Lohri, Makar Sankranti, Pongal, Onam,
  Baisakhi; wedding rites (a wedding as a family party with food and music is fine, with no rite,
  priest or ceremony named). Fine: a birthday, a family dinner, a fair or market (*मेला*, *हाट*)
  with no festival named, New Year's Eve as a family evening, the start of the school year, the
  summer holidays, a kite in the sky on a windy afternoon. *जी* is an honorific, *श्री* only as
  "Mr" before a surname (*श्री वर्मा*). Prefer vegetarian dishes in food scenes (dal, roti, rice,
  sabzi, paneer, fruit, chai), and never beef or pork;
- **politics**: the state, the government (*सरकार*), parliament and assemblies, elections, parties,
  ministers, presidents, kings and rulers, nationalism, independence and national days, flags,
  language politics, caste (*जाति*, *आरक्षण*), communities set against each other, and borders as
  disputes; everyday civic life (a library, a school, the post office, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  crossing guard at a school is fine; theft, arrests, prisons and trials are not);
- **death**, killing, funerals, blood and injury beyond a scraped knee; an old person in a story is
  alive and well, and a pet does not die;
- **vulgar or sexual words**, insults, and words whose main sense is sexual;
- **alcohol** in every form, **gambling** (lotteries, betting, cards for money), drugs, *paan* and
  *gutka*, and smoking;
- **deliberate misspellings**: no nuktaless or Hinglish spellings, not even in a child's speech or
  a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people or a language is skipped. Common Indian first names are fine as characters.

The themes keep to this: the history courses (*इतिहास, जिसे छू सकते हैं*, *पुरातत्व और अतीत*,
*खोजी और यात्री*) tell the history of everyday life, towns, crafts, trade, science and travel, never
of wars, rulers, religion or politics; *मेले और हाट* are fairs and markets with no festival named;
*सार्वजनिक सेवाएँ* is the library, the health centre and the post office, never politics; *हिंदी
भाषा* and *शब्द और उनकी कहानी* are about words, dialects and learning, never about language politics;
*लोककथाएँ* tells folk tales of clever animals and wise villagers, without gods, demons or miracles.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks:

- religion: *भगवान* 138, *जय* 221 (*जय हो*, slogans), *हिंदू* 259, *मंदिर* 264, *धर्म* 265, *गुरु* 600
  (except a teacher of music or a craft, made plain), *आत्मा* 628, *प्रभु* 632, *देवी* 635, *मुस्लिम*
  651, *मुसलमान* 678, *देवता* 699, *पूजा* 746, *प्रार्थना* 766, *अल्लाह* 869, *दर्शन* 873 (also
  "philosophy": teach it only so, later), *परमेश्वर* 977, *नरक* 552;
- politics: *सरकार* 109, *प्रदेश* 194 (holds Uttar Pradesh's name), *पार्टी* 206 (a party with
  friends is *दावत*; teach *पार्टी* only as a birthday party, made plain), *राज्य* 230, *चुनाव* 241,
  *कांग्रेस* 280, *नेता* 298, *राजा* 332, *मंत्री* 352, *राष्ट्रीय* 445, *अध्यक्ष* 446, *मुख्यमंत्री*
  475, *राष्ट्रपति* 477, *विधायक* 518, *राजनीति* 543, *वोट* 554, *विधानसभा* 565, *जाति* 653, *संघ*
  685, *दलित* 723, *आंदोलन* 749, *राजनीतिक* 759, *शासन* 893, *नीति* 887, *सांसद* 892, *आरक्षण* 901,
  *रानी* 921, *सत्ता* 951;
- war, crime, police, courts: *पुलिस* 190, *सेना* 313, *युद्ध* 322, *हमला* 327, *गोली* 336 (also a
  pill: teach it only so, made plain), *लड़ना* 374, *लड़ाई* 422, *हत्या* 441, *जेल* 472, *बंदूक़* 572,
  *आरोप* 577, *सैनिक* 606, *हथियार* 616, *दुश्मन* 623, *गिरफ़्तार* 674, *कोर्ट* 681, *चोरी* 727,
  *अपराध* 757, *चोर* 776, *शिकार* 834, *परमाणु* 875, *बम* 942;
- death: *मारना* 143, *मरना* 152, *मार* 184, *मौत* 283, *ख़ून* 490, *मृत्यु* 609, *मृत* 788, *रक्त* 950;
- vulgar and insults: *बकवास* 330, *पागल* 369, *कमबख़्त* 439, *गधा* 495 (the animal is fine, made
  plain), *मूर्ख* 625, *कमीना* 676, *भाड़* 679, *साला* 819, *सेक्स* 852, *सूअर* 874;
- alcohol: *शराब* 809, *नशा* 948;
- names, place adjectives, peoples and languages: *भारतीय* 218, *हिंदी* 274, *अंग्रेज़ी* 464,
  *अमेरिकी* 865, *बंगाल* 924;
- list noise: *कराहना* 753 and *हाँक* 771 (the subtitles' sound notes), *व* 170 (an abbreviation of
  *और*), *ओ* and *रे* (calls), *डॉ* (an abbreviation).

Further on, the same categories hold; for example *स्वर्ग*, *ईश्वर*, *मस्जिद*, *त्योहार* (when a
religious festival is meant), *दीवाली*, *होली*, *ईद*, *पुजारी*, *प्रधानमंत्री*, *संसद*, *झंडा*,
*अदालत*, *जज*, *क़ैदी*, *लाश*, *सिगरेट*, *तंबाकू*, *जुआ*, *लॉटरी*.

## The file

Write `tools/courses/hi/<id>.json`:

```json
{
  "id": "hi-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Hindi, e.g. खाना और ख़रीदारी",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Hindi",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 9600 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/hi/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *गया* or *जाता* for *जाना*, *अच्छी* for *अच्छा*, *मेरी* for
  *मेरा*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it: the
  infinitive of a verb (*देखना*), the masculine singular of an adjective (*अच्छा*), the direct
  singular of a noun (*लड़का*).
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after. Hindi writes postpositions and auxiliaries
  as words of their own (*घर में*, *जा रहा है*), so a Hindi story has more words than the same story
  in most languages: keep it short.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name, a form of another
  word, a place adjective, a word of the content rules); the checker's last line lists the band
  words not taught yet.

Things to know about the Hindi list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/hi/wordlist.tsv`
  (a copy of the app's `hi.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it. The list is counted from web
  text and film subtitles (OpenSubtitles), so talk ranks high, politics and film words rank high,
  and some everyday written words rank low.
- **How the checker cuts words**, as the app's reader does: at spaces, punctuation, the danda and
  hyphens; vowel signs, virama and nukta are part of the word. A word is matched exactly, nukta and
  chandrabindu included (the list holds the usual other spellings as forms).
- **Postpositions are entries of their own**: *का* 2 holds *की* and *के* (so *के लिए*, *के बाद*,
  *के साथ*, *के बारे में* count *के* as *का*; *की* is *करना*'s form too, "did"); *में* 4, *से* 10,
  *को* 13, *पर* 19 ("on" and "but"), *ने* 35, *तक* 56; *लिए* 18, *साथ* 39, *बाद* 64, *बारे* 79,
  *पहले* 69, *बाहर* 98 are entries too.
- **The copula**: *है* 1 holds *हैं*, *हूँ* (and *हूं*); *था* 16 holds *थे*, *थी*, *थीं*; *हो*, *होगा*,
  *हुआ*, *होता* are *होना* 5. A lesson with *है* and *था* has not taught *होना*.
- **Pronouns**: the oblique forms are their pronoun's: *मुझे*, *मैंने*, *मुझसे* of *मैं* 6; *उसे*,
  *उस*, *उसने*, *वे*, *उन्हें*, *उन्होंने* of *वह* 12; *इसे*, *इस*, *इन* of *यह* 8; *ये* 44 and *वो* 65
  are entries of their own; *किसी* of *कोई* 32; *किसे*, *किस* of *कौन* 96; *हमें* of *हम* 25; *तुम्हें*
  of *तुम* 22; *आपको* of *आप* 17. **Every possessive is an entry of its own** with its *-ी* and *-े*
  forms: *अपना* 24, *मेरा* 28, *उसका* 51, *हमारा* 57, *उनका* 66, *इसका* 73, *आपका* 76, *तुम्हारा* 81,
  then *तेरा*, *जिसका*, *किसका*, *इनका*, *जिनका*. So *मेरी माँ* is a use of *मेरा*, not of *मैं*.
- **Verbs** are listed by their infinitive in *-ना*, with their forms: the stem too (*कर*, *जा*,
  *दे*, *चल*, *मिल*, *बन*), as in *कर दिया*, *चल पड़ा*. But **a noun spelled like a stem is an entry of
  its own and keeps that spelling**: *समझ* "understanding", *सोच* "thought", *खेल* "game", *जीत*
  "victory", *हार* "defeat", *भूल* "mistake", *मार*, *डर* "fear", *जान* "life", *बोल* "words", *रोक*,
  *माँग*, *पहचान*, *खोज*, *भाग* "part". So *समझ गया* counts for the noun *समझ*, *डर गया* for *डर*:
  teach the verb (*समझना*, *डरना*) through its other forms (*समझा*, *समझता*, *समझो*; *डरता*, *डरी*).
- **Homographs**: some entries hold two words: *खाना* "food" and "to eat"; *सोना* "gold" and "to
  sleep"; *गाना* "song" and "to sing"; *कल* "yesterday" and "tomorrow"; *बस* "bus" and "just,
  enough"; *पर* "on" and "but"; *जी* (respect, "yes") and *जीना*'s stem; *आम* "mango" and "common";
  *हार* "defeat" and "garland"; *बदला* "revenge" and *बदलना*'s past; *खाता* "account" and "eats".
  Use a new word in its own sense, never through its homograph, and count a new word's uses in
  forms that are its own.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). It is
  generated by `hi-writers/hi_extra.py`: every verb's regular forms (perfective, subjunctive, future,
  polite imperative, *-कर*), the *-ी* and *-े* forms of adjectives in *-ा*, and by hand the irregular
  polite imperatives (*कीजिए*, *दीजिए*, *लीजिए*, *पीजिए*). Every headword in it must be a headword of
  the list (the checker warns otherwise). If a true form is missing, write around it and name the
  word and form in your report.
- **Names** are not in the list: people, places (*दिल्ली*, *जयपुर*, *लखनऊ*), shops and brands go in
  `names`. Keep only true names in `names`, never common words, and never a name that is also a list
  word: as characters avoid *प्रिया* (1511), *किरण* (1805), *मोहन* (1845), *माया*, *गीता*, *कविता*
  ("poem"), *पूजा*, *आशा* ("hope"), *सीमा* ("border"), *विकास* ("development"), *अजय*, *मीरा*, *रवि*,
  *अमन* ("peace"), *ललित*, *सुमन*, *विनोद*, *ईशा*, *अंजलि*; they count at their own rank. Check a
  name's rank with `rank.py` before you use it. Safe: *राहुल*, *अनीता*, *सुनील*, *रीना*, *नेहा*,
  *अर्जुन*, *दीपा*, *राधा*, *सोनू*, *पिंकी*, *रमेश*, *सुरेश*, *अमित*, *तन्वी*, *आरव*, *कमला*,
  *शीला*; *दिल्ली*, *मुंबई*, *जयपुर*, *लखनऊ*, *पुणे*, *भोपाल*, *इंदौर*, *कानपुर*, *आगरा*, *शिमला*,
  *चेन्नई*, *नागपुर*.
- Everyday words rank later than you would think: *घर* 89, *बड़ा* 90, *पानी* 216, *माँ* 118, *पिता*
  145, *भाई* 133, *बहन* 326, *खाना* 150, *दोस्त* 151, *लड़की* 171, *लड़का* 200, *पैसा* 173, *सुबह*
  273, *शाम* 527, *चाय* 701, *दूध* 822, *किताब* 778.
- The list's form lists are noisy (look-alikes, misspellings): a new word must appear as itself or
  a true inflection, never through such a form. Some entries are names, letters or abbreviations:
  skip them.
