# Writing a Georgian frequency course

Each course teaches one band of 100 words from the Georgian word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `ka-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *ნინოს ტელეფონი არ აქვს. ის სამზარეულოში შედის. ტელეფონი იქ არ არის...*
  Everyday life where Georgian is spoken: Tbilisi (its districts: ვაკე, საბურთალო, ძველი თბილისი as
  plain places), Kutaisi, Batumi, Rustavi, Zugdidi, Gori, Telavi, Poti, Zestaponi, Ozurgeti, Kobuleti,
  Akhaltsikhe, Sighnaghi, Mestia, Lagodekhi, Dusheti; the regions (კახეთი, იმერეთი, სვანეთი, რაჭა,
  გურია, სამეგრელო), the Black Sea coast, the Mtkvari and the Rioni, the Caucasus villages; and now
  and then London, Berlin, Athens or New York, where Georgians live: home, family, food, shopping,
  work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who *ის* is whenever it could be unclear
  (Georgian has no he/she: *ის* is both). Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, object, verb or subject, verb, object, as Georgian writes it plainly: *ნინო წიგნს
  კითხულობს*, *ნინომ წიგნი წაიკითხა*). Georgian drops subject pronouns: write *მე*, *შენ*, *ის*,
  *ჩვენ* only where Georgian would (contrast, emphasis, a new subject). Little dialogue; when there is
  some, short and simple, with *შენ* among family and friends and *თქვენ* with strangers, older people
  and in shops or offices, and *ბატონო* / *ქალბატონო* + first name in address (*ქალბატონო ნანა*,
  *ბატონო გიორგი*).
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–200: the present (*ნინო სახლშია* → write *ნინო სახლში არის*; *წერს*, *კითხულობს*,
    *ცხოვრობს*, *მიდის*, *მოდის*), *არის* and its persons (*ვარ*, *ხარ*, *არის*, *ვართ*, *ხართ*,
    *არიან*), possession with the dative: *აქვს* for things, *ჰყავს* for people and animals (*ნინოს
    სახლი აქვს*, *ნინოს ორი და ჰყავს*: never *ორი და აქვს*), *უნდა* "wants" with the dative (*ნინოს
    წყალი უნდა*); the cases nominative, dative and genitive and the postpositions written onto
    them (*-ში*, *-ზე*, *-თან*, *-დან*, *-თვის*, *-ისკენ*); a number takes the singular (*ორი წიგნი*,
    *5 ადამიანი*); questions with *რა*, *ვინ*, *სად*, *როგორ*, *რომელი*;
  - ranks 201–400: the **aorist**, the past of events, with its ergative subject (*ნინომ წიგნი
    წაიკითხა*, *გიორგიმ თქვა*, *ისინი სახლში მივიდნენ*: an intransitive verb keeps the nominative);
    the future, the present with a preverb (*დაწერს*, *გააკეთებს*, *წავა*, *მოვა*); the possessives
    *ჩემი*, *შენი*, *მისი*, *ჩვენი*, *თქვენი*, *მათი*; the instrumental and adverbial cases (*მანქანით*,
    *მასწავლებლად*);
  - ranks 401–700: the **imperfect** (*წერდა*, *ცხოვრობდა*, *იყო*) for background and habits; the
    **optative** after *უნდა* and *რომ* (*უნდა წავიდეს*, *მინდა, რომ მოხვიდე*); the imperative
    (*მოდი*, *დაჯექი*, *მითხარი*, *წაიკითხე*); the relatives *რომელიც*, *ვინც*, *რაც*, *სადაც*,
    *როდესაც*; comparison (*უფრო დიდი*, *ვიდრე*, *ყველაზე*);
  - ranks 701–1000: the verbs of feeling and having with a dative subject in every tense
    (*უყვარს* / *უყვარდა*, *მოსწონს*, *სჭირდება*, *ახსოვს*, *ჰგონია*); *თუ* with the future for real
    plans (*თუ წვიმს, სახლში დავრჩებით*); reported speech with *რომ* (*თქვა, რომ მოვა*);
  - ranks 1001–1300: the passive and the change-of-state verbs in *-ება* (*კეთდება*, *შენდება*,
    *ითვლება*), the verbal noun (*კითხვა*, *წასვლა*) with postpositions (*წასვლამდე*,
    *კითხვისას*), *იმიტომ, რომ*, *თუმცა*, *მიუხედავად*;
  - ranks 1301–1600: the **perfect** (evidential: *უთქვამს*, *წასულა*, *ყოფილა*) for what one
    learns or finds out, not for plain events; the conditional (*წავიდოდა*, *ვიყიდდი*) with *რომ* /
    *თუ* (*რომ მქონდეს დრო, წავიდოდი*); purpose with *რომ* + optative (*რომ ისწავლოს*);
  - ranks 1601–2000: the participles (*დაწერილი*, *მომავალი*, *საჭმელი*), the pluperfect of
    narration where it reads naturally, indirect questions (*არ იცის, მოვა თუ არა*), the
    quotative particles *-ო* and *-მეთქი* in dialogue only;
  - ranks 2001–3000: a richer, more formal register: connectors such as *ამრიგად*, *შესაბამისად*,
    *ანუ*, *უფრო მეტიც*, *ერთი მხრივ ... მეორე მხრივ*; longer sentences with participles; common
    idioms explained by their context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the newspaper column (a light reflection on everyday life), dialogue that marks
    *შენ* and *თქვენ*;
  - ranks 4001–6000 (C1): more abstract and nuanced content: a short essay or reflective column that
    weighs two sides, an interview, a portrait, a review; precise vocabulary for feelings, ideas and
    processes; the specialised words of professions, sciences and the arts, each made clear by its
    context;
  - ranks 6001–8000 (C1): figurative language and idioms used more freely, words chosen for their
    shades of meaning, topics that need some background, which the text gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images and
    rhythm, academic and journalistic registers, learned words explained by their context. Still
    standalone, calm and easy to follow for a reader at the level.
  After rank 1000 sentences get longer and joined (*რადგან*, *როდესაც*, *სანამ*, *თუმცა*, *ამიტომ*),
  dialogue can carry more of the story, and a story may be told by a first-person narrator or as an
  e-mail, a letter or a diary entry. From rank 2000 a lesson may also be a short informative or
  opinion text (a column, a letter, a short report), as long as it is everyday, calm and easy to
  follow.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `კითხვები:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words (after rank 1000 they may also ask *რატომ* and *როგორ*, with fuller answers):

  ```
  კითხვები:
  რა არ აქვს ნინოს?
  ნინოს ტელეფონი არ აქვს.
  სად არის ტელეფონი?
  ტელეფონი მანქანაში არის.
  ```

  The header `კითხვები:` is layout: the checker does not count it as a word.
- **Standard Georgian**: the narration is the modern literary standard, neutral, correct and
  natural as written today: the standard verb forms (*მოვიდა*, not *მოვიდნენ* for one person;
  *ვყიდულობ*; *იმიტომ, რომ*), *დიახ* or *კი* for "yes" (*ჰო* only in relaxed dialogue), no dialect
  forms (Gurian, Megrelian, Svan, Kakhetian speech), no Russian loans of the street (*ნორმალნო*,
  *დავაი*, *კაროჩე*), no slang. Georgian has no capital letters: never use the Mtavruli capitals
  (*ᲜᲘᲜᲝ*), not even for a title or a shout.
- **Write the copula out**: write *ნინო სახლში არის*, not *ნინო სახლშია*, and *ასევე* or *კიდევ* for
  "also", not *-ც* joined to a word (*ნინოც*, *სახლიც*), until the list has the joined form (check it
  with `rank.py`): the reader reads *სახლშია* and *სახლიც* as words of their own, which the list
  mostly lacks, so they count as outside words. Names may take *-ც* and *-ა* (*ნინოც*, *ნინოა*).
- **Georgian typography**: direct speech in the quotation marks „…“ (low opening, high closing), each
  speaker's line a paragraph of its own; the narrator's comma goes after the closing mark, a
  question or exclamation mark of the quoted sentence inside it:

  ```
  „გამარჯობა, ნინო“, ამბობს გიორგი.
  „როგორ ხარ?“ ეკითხება ნინო.
  ```

  Inside a sentence a dash is the en dash with spaces ( – ). Write numbers as digits (*3 წიგნი*,
  *5 საათზე*, *1992 წელს*), centuries in Roman numerals (*XIX საუკუნე*), except a number word in your
  band: spell that one out to teach it (*სამი და*). Never join an ending to a number or an
  abbreviation with a hyphen (*2020-ში*, *მე-5*, *5-ჯერ*, *აშშ-ის*): the reader cuts the ending off as a
  word of its own (*ში*), which is no word. Write *2020 წელს*, *მეხუთე*, *ხუთჯერ*, or around it.
- **Names are names**: place names, adjectives made from place names (*ქართული*, *რუსული*,
  *ამერიკული*, *ინგლისური*) and words for peoples or languages (*ქართველი*, *სომეხი*, *ინგლისელი*)
  count as names: skip them as band words even when the list has them as entries; a text may still
  use one (*ქართულად ლაპარაკობს*), counted at its rank. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: God, saints, churches, monasteries and cathedrals (*ეკლესია*, *ტაძარი*,
  *მონასტერი*, *სამება*), priests and monks, crosses (*ჯვარი*), icons, prayer, candles in church,
  souls and spirits (*სული*), and every holiday of religious origin, even as a date or a school
  break: *შობა*, *აღდგომა*, *გიორგობა*, *მარიამობა*, *ალავერდობა*, *ნათლობა*, saints' and name days.
  New Year (*ახალი წელი*) is fine as a family evening, with *ჩურჩხელა*, *გოზინაყი* and a fir tree.
  Towns are names, but never visit a church or a monastery, not even as sightseeing (so not Mtskheta's
  churches, Gergeti, Gelati, Jvari, Svetitskhoveli); *ციხე* only as an old fortress seen from outside
  (*ნარიყალა*), never as a prison, and better not at all below rank 1000;
- **politics**: the state (*სახელმწიფო*), government, parliament, ministers, presidents, parties,
  elections, kings, queens and rulers (*მეფე*, *დედოფალი*, *თამარ მეფე*, *დავით აღმაშენებელი*), the
  Soviet period (*საბჭოთა*), independence, occupation, Abkhazia and South Ossetia as disputes,
  flags and national symbols, nationalism; everyday civic life (a library, a school, the town hall's
  counter, a residents' meeting) is fine;
- **war, the army, weapons, violence, crime, police and courts** (a lost-property office or a
  crossing guard at a school is fine; theft, arrests, prisons and trials are not);
- **death**, killing, funerals, graves, blood and injury beyond a scraped knee; an old person in a
  story is alive and well, and a pet does not die;
- **vulgar or sexual words**, and words whose main sense is sexual;
- **alcohol** in every form: wine (*ღვინო*, *ქვევრი*, *მარანი*, *რთველი* as wine-making), *ჭაჭა*,
  beer, the *სუფრა* with its toasts and *თამადა*; **gambling**, drugs and smoking. Georgian food is
  fine: *ხაჭაპური*, *ხინკალი*, *ლობიო*, *მჭადი*, *ფხალი*, *ბადრიჯანი ნიგვზით*, *მწვადი*, *ტყემალი*,
  *ჩურჩხელა*, fruit, bread from the *თონე*, tea, mineral water;
- **deliberate misspellings**: no spellings in Latin letters (*gamarjoba*), no shortened chat
  spellings, not even in a text message;
- **names taught as words**: a band word that is a name, a part of names, a place adjective or a
  word for a people or a language is skipped. Common Georgian first names are fine as characters.

The themes keep to this: the history courses (*ისტორია ახლოდან*, *არქეოლოგია და წარსული*,
*მკვლევრები და მოგზაურები*) tell the history of everyday life, towns, crafts, trade, science and
travel, never of wars, kings or politics; *სახალხო ზეიმები და ბაზრობები* and *ხალხური ხელოვნება და
ადათები* are folk and family celebrations with no church holiday or saint named and no wine
(Tbilisoba as a city fair with music and food, a harvest of apples or nuts, a craft fair);
*საჯარო სერვისები* is the library, the clinic and the public service hall, never politics; *ქართული
ენა და დამწერლობა* and *სიტყვები და მათი წარმოშობა* are about words, the alphabet, dialects and
learning, never about language politics; *ზღაპრები და ლეგენდები* tells folk tales (clever animals,
giants, a poor man's luck) without saints, devils or miracles; *საქართველოს სამზარეულო* is food,
never the wine of the *სუფრა*.

Skip band words that only fit these topics, and say so in your report. The skip list for the first
1000 ranks and a little beyond (ranks of the pinned list):

- religion: *ეკლესია* 106, *ტაძარი* 285, *წმინდა* 340, *მონასტერი* 600, *რელიგიური* 742, *ღმერთი* 844,
  *ჯვარი* 932, *სული* 938, *რელიგია* 872, *სასულიერო* 1199, *საეკლესიო* 1228, *პაპი* 1156;
- politics: *სახელმწიფო* 82, *საბჭო* 94, *მუნიციპალიტეტი* 107, *ეროვნული* 113, *მეფე* 125, *პარტია* 160,
  *მთავრობა* 155, *პოლიტიკური* 171, *სამეფო* 185, *პრეზიდენტი* 161, *მინისტრი* 226, *არჩევნები* 268,
  *ხელისუფლება* 292, *პარლამენტი* 363, *სამინისტრო* 389, *იმპერია* 411, *პოლიტიკა* 466, *ლიდერი* 496,
  *მმართველობა* 548, *თავმჯდომარე* 551, *საარჩევნო* 594, *ტახტი* 606, *იმპერატორი* 652,
  *დემოკრატიული* 658, *დამოუკიდებლობა* 599, *მმართველი* 681, *პოლიტიკოსი* 712, *ოპოზიცია* 724,
  *დედოფალი* 739, *დინასტია* 795, *ფედერაცია* 811, *რეზოლუცია* 884, *კონსტიტუცია* 887, *დეპუტატი* 937,
  *რევოლუცია* 1019, *ძალაუფლება* 970, *ნაციონალური* 1084, *საპარლამენტო* 1111, *გუბერნატორი* 1230;
- war, crime, police, courts: *ომი* 115, *ბრძოლა* 176, *სამხედრო* 184, *სასამართლო* 346, *არმია* 384,
  *ციხე* 396, *ჯარი* 482, *იარაღი* 626, *დაამარცხებს* 787, *შეიარაღებული* 964, *თავდაცვა* 994,
  *ჯარისკაცი* 955, *თავდასხმა* 1059, *მტერი* 908, *საბრძოლო* 1167, *ძალადობა* 765, *მსხვერპლი* 1168;
- death: *სიკვდილი* 588, *გარდაცვალება* 693;
- alcohol: *ღვინო* 683;
- names, place adjectives, peoples and languages: *ქართული* 111, *ქართველი* 174, *რომი* 412, *აჭარა* 583, *რუსული* 530, *ევროპული* 703, *ამერიკელი* 274, *ამერიკული* 381, *კავკასია* 798, *რუსი* 808,
  *ინგლისური* 634, *ფრანგი* 806, *გერმანული* 899, *ფრანგული* 427, *ბერძნული* 1029, *სომხური* 1171,
  *გერმანელი* 1032, *სომეხი* 1236, *თურქი* 1176, *ინგლისელი* 1180;
- news and list noise: *კორონავირუსი* 476 and *პანდემია* 1113 (teach *ვირუსი* only as an ordinary
  cold), *ძე* 560 and *ასული* 1223 (old words for son and daughter, from names), *სახელობა* 485 (only in
  "named after"), *ეგ* 665 (holds the abbreviation *მაგ.*).

Further on, the same categories hold; for example *ბერი*, *მღვდელი*, *ხატი*, *შობა*, *აღდგომა*,
*ეშმაკი*, *სამოთხე*, *ჯოჯოხეთი*, *მეფობა*, *ომის*, *ქვევრი*, *მარანი*, *ჭაჭა*, *ლუდი*, *სიგარეტი*,
*პოლიცია*, *დანაშაული*, *ქურდი*, *მოკლა*, *დაკრძალვა*, *საფლავი*, *დროშა*.

## The file

Write `tools/courses/ka/<id>.json`:

```json
{
  "id": "ka-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Georgian, e.g. საჭმელი და საყიდლები",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Georgian",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/ka/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (any form counts: *ვარ* or *იყო* for *არის*, *დიდ* for *დიდი*, *ქალაქში* for
  *ქალაქი*). Reuse earlier lessons' new words too. `newWords` holds the word as the list writes it:
  a verb by its 3rd person singular (*წერს*, *მიიღებს*), a noun and an adjective by the nominative
  singular.
- **Length** (story and questions together): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an abbreviation, a vulgar word, a list error such as a name, a form of another
  word, a place adjective, a word of the content rules); the checker's last line lists the band
  words not taught yet.

Things to know about the Georgian list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/ka/wordlist.tsv`
  (a copy of the app's `ka.tsv`); the checker reads it instead of the app's list, so a rebuilt app
  list does not move the bands. The `bands/*.txt` files come from it and from `extra_forms.tsv`.
- **The list is counted from news and Wikipedia, with everyday sentences blended in** (Tatoeba's and
  Common Voice's Georgian, a quarter of the weight). There is no Georgian subtitle corpus, so the list
  still leans bookish: encyclopedic words rank early (*მოსახლეობა* 57, *მუნიციპალიტეტი* 107, *საუკუნე*
  137, *კედელი* 189, *ფართობი* 319), while everyday words now come early too: *შენ* 15, *ჩვენ* 20,
  *ძალიან* 32, *სად* 78, *უყვარს* 79, *რატომ* 88, *ვინ* 130, *ბავშვი* 150, *დედა* 166, *მიდის* 180,
  *ძაღლი* 181, *ჭამს* 195, *მამა* 196, *ფული* 205, *მეგობარი* 206, *ხვალ* 233, *პატარა* 278, *კატა* 342,
  *გუშინ* 349, *ექიმი* 358, *ოთახი* 405, *ლამაზი* 446, *ყავა* 581, *ტელეფონი* 662, *მადლობა* 674,
  *ბიჭი* 697, *გოგო* 910, *გამარჯობა* 1197, *დიახ* 1978, *ბებია* 2327. In the early bands lean on names,
  digits and the band's own words, and spend the outside words on what the story cannot do without
  (*ჰყავს* 103 for people, *ოჯახი* 109, *ქალი* 110). Some band words are bookish: teach them in a plain
  everyday use (a school report, a map, a town's numbers).
- **How the checker reads Georgian**, as the app's reader does: a word is a run of Georgian letters.
  A hyphen, an apostrophe, a digit or a Latin letter ends it, so *ერთ-ერთი* is *ერთ* + *ერთი* (both
  list words, fine to write) and *2020-ში* leaves a word *ში* (no word: never write it). Latin
  letters and Roman numerals are not words to the reader and are not counted at all. A word with
  *-ც*, *-ვე* or the copula *-ა* joined to it (*სახლიც*, *იქვე*, *სახლშია*) is a word of its own, in
  the list only where `rank.py` finds it. A name in `names` counts with the case endings and
  postpositions written onto it (*ნინომ*, *ნინოს*, *ნინოსთან*, *ნინოსთვის*, *თბილისში*,
  *თბილისიდან*, *ლევანმა*, *ნინოც*, *თბილისსა და გორს*); the name goes in `names` in its
  nominative (*თბილისი*, *ლევანი*, *ნინო*).
- **Verbs**: a verb's forms are filed by its preverb. *წერს* holds the present (*წერს*, *წერენ*,
  *წერდა*), *დაწერს* the future and the aorist (*დაწერს*, *დაწერა*, *დავწერე*): the two are separate
  entries, and so are *აკეთებს* / *გააკეთებს*, *იღებს* / *მიიღებს* / *აიღებს*, *ასრულებს* /
  *დაასრულებს*. A verb whose preverb changes its meaning is its own entry too (*იცავს* "defends",
  *მოიცავს* "includes"; *ადგენს* / *წარმოადგენს*). The verbs of motion keep their preverbs apart
  (*მიდის*, *მოდის*, *შედის*, *გადის*, *გადადის*, *ჩადის*, *წავა*). Teach the entry the band gives and
  count its own forms; a suppletive form is its verb's (*თქვა*, *იტყვის* of *ამბობს*; *ვარ*, *იყო*,
  *იქნება* of *არის*). The regular present, imperfect and subjunctive forms the pack lacked
  (*ამბობენ*, *ვცხოვრობ*, *ვხედავდით*) were added by rule (`extra_forms.tsv`); other missing forms
  you report.
- **Pronouns and little words**: *ის* 4 holds *მას*, *მან*, *მის*, *იმ*; *ისინი* 14 holds *მათ*;
  *მისი* 16 "his/her" and *მათი* 97 "their" are entries; *მე* 5 holds *ჩემთვის*; *ჩემი* 28,
  *ჩვენ* 20, *შენ* 15, *შენი* 59 and *თქვენ* 21 are in the first band, *ჩვენი* 156 in the second;
  *ეს* 3 holds *ამ*, *ამის*, *ამას* and *ესე* (of *ესე იგი* "that is"); *რომელი* 27 and *რომელიც* 10 are two entries, *კი* 23 is both "yes" and "but, and"
  (*ეს კი...*); *და* 1 is "and" and "sister" (*დის*, *დას*, *დები*).
- **Homographs and words filed under another**: *წელს* 13 "this year" and *დღეს* 75 "today" are
  entries of their own beside *წელი* 8 and *დღე* 83; *თავი* 41 is "head", "self" (*თავისი* 43 "his
  own") and "chapter"; *მხარე* 122 "side, region" and *მხარი* "shoulder" share *მხრის*; a verbal noun is
  often its verb's (*მიღება* of *მიიღებს*, *დახმარება*, *შეცვლა*), so teach the verb through its other
  forms. Use a new word in its own sense, never through its homograph.
- Common forms the list misses go in `extra_forms.tsv` (headword, a tab, the forms). Every headword
  in it must be a headword of the list (the checker warns otherwise). If a true form is missing,
  write around it and name the word and form in your report.
- **Names** are not in the list: people, places and brands go in `names`. Never a name that is also
  a list word: as characters avoid *ია*, *ლალი*, *თინა*, *ნატო*, *გია*, *ირაკლი*, *მანანა*, *ეთერი*
  (list words or forms of them); as places *ბორჯომი* (the mineral water, 6858), *აჭარა* 583 and
  *რომი* 412 are list words. Check a name with `rank.py` before you use it. Safe in the pilot:
  *ნინო*, *თამუნა*, *ეკა*, *ლევანი*, *მაკა*, *მზია*, *ქეთი*, *ნანა*, *დათო*, *ნათია*, *საბა*,
  *ელენე*, *ლაშა*, *სოფო*, *სალომე*, *ბექა*, *ნათელა*, *გიორგი*, *ლუკა*, *ანა*, *ნიკა*; places
  *თბილისი*, *ქუთაისი*, *ბათუმი*, *რუსთავი*, *თელავი*, *გორი*, *მესტია*, *ახალციხე*, *კახეთი*. The
  months are in the list (*მარტი* 218, *იანვარი* 246, *მაისი* 236, *დეკემბერი* 234, *აგვისტო* 290 ...),
  the days of the week late (*კვირა* 457 is "week" and "Sunday"; *ორშაბათი* 6829, *შაბათი* 4063).
- The list's form lists are noisy (old and rare spellings, look-alikes); a new word must appear
  as itself or a true inflection, never through such a form. Some entries are names, abbreviations
  or fragments: skip them. Loanwords Georgian uses as its own (*ინტერნეტი*, *ვიდეო*, *ფეხბურთი*,
  *კომპიუტერი*) are taught like any word.
