# Writing an Amharic frequency course

Each course teaches one band of 100 words from the Amharic word frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its common forms). Course `am-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.
`themes.tsv` gives each course its theme. The list has about 2,900 words (the English Wiktionary's
Amharic is small), so the last course is `am-mini-2900`.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *ሳራ ስልኳን ትፈልጋለች። ወደ ኩሽና ትሄዳለች። ስልኩ የለም። ...* Everyday life where Amharic
  is spoken: Addis Ababa (Piassa, Merkato, Bole, Arat Kilo, Entoto), Bahir Dar and Lake Tana, Gondar,
  Lalibela, Dessie, Debre Birhan, Debre Markos, Adama, Hawassa, Dire Dawa, Harar, Jimma, Arba Minch,
  the Simien mountains, the Blue Nile falls; now and then Washington, London or Tel Aviv, where many
  Amharic speakers live: home, family, coffee, injera, the market, work, school, the town, health,
  free time, travel, holidays (Meskel, Timket, Enkutatash, Genna, Fasika, Eid). Any everyday, cultural
  or story topic is fine (WRITER-BRIEF step 4); only explicit sexual content, slurs used as insults and
  gratuitous gore stay out.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, wordplay, unreliable
  narrators or open endings. Events in the order they happen. Few characters (one to three), named
  clearly; say who *እሱ* / *እሷ* is whenever it could be unclear. Give each story its own names.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary Amharic
  order: subject, object, verb, the verb last (*አበበ ቡና ይጠጣል።*); a modifier before its noun
  (*ትልቅ ቤት*, *የአበበ ቤት*, *ይህ ልጅ*). The verb carries its subject: write *እሱ*, *እሷ*, *እኔ* only for
  contrast or emphasis. Little dialogue; when there is some, short and simple.
- **The verb**: the perfective tells what happened (*መጣ*, *መጣች*, *መጡ*, *መጣሁ*), the imperfective with
  *-ኣል* what happens or will happen (*ይመጣል*, *ትመጣለች*, *እመጣለሁ*), the gerund links actions (*መጥቶ
  ተቀመጠ*), the infinitive follows *ፈለገ*, *ጀመረ*, *ቻለ* (*መብላት ይፈልጋል*). The list files every form under
  its verb (*ሰበረ* holds *ይሰብራል*, *የሚሰብሩት*, *አልሰበረም*, *ሰብሮ*, *መስበር*). A verb is listed by its
  perfective, or by its infinitive where the dictionary pack has only that: *መምጣት* "come" holds *መጣ*,
  *ይመጣል*, *ና*; *መቻል* "can" holds *ቻለ*, *ይችላል*; *ማወቅ* "know" holds *አወቀ*, *ያውቃል*; *ማድረግ* "do"
  holds *አደረገ*, *ያደርጋል*. *ነው* "is" holds *ናት*, *ነኝ*, *ናቸው*; *ነበረ* "was" holds *ነበር*,
  *ነበሩ*; *አለ* is "there is, have" and "say" both (*አለ*, *አሉ*, *አለኝ*, *አለበት*; *ብሎ*, *ይላል*,
  *ማለት*), *የለም* "there isn't" is a headword of its own.
- **Grammar grows with the bands**, each band using what came before:
  - ranks 1–300: *ነው* / *አይደለም* (*አበበ አስተማሪ ነው*), *አለ* / *የለም* "there is, have" (*ቤት አለው*,
    *መኪና የለኝም*), the present-future (*ይሄዳል*, *ትበላለች*) and its negative (*አይሄድም*), the past
    (*ሄደ*, *በላች*) and its negative (*አልሄደም*); the prepositions on the word (*በቤት*, *ለልጁ*, *ከከተማ*,
    *የአበበ*, *ወደ ገበያ*) and the postpositions (*ቤት ውስጥ*, *ጠረጴዛ ላይ*); the article *-ኡ* / *-ዋ*
    (*ልጁ*, *ልጅቷ*), the possessives (*ቤቴ*, *ቤትህ*, *ቤቷ*, *ቤታችን*), the plural *-ኦች* (*ልጆች*,
    *ቤቶች*), the object *-ን* (*ቡናውን ጠጣ*); *ይህ*, *ይቺ*, *ያ*, *እነዚህ*; numbers one to ten; questions
    with *ምን*, *ማን*, *የት*, *መቼ*, *ለምን*, *እንዴት*, *ስንት*;
  - ranks 301–600: the infinitive (*መሥራት ይፈልጋል*, *ለመማር*), the gerund chain (*ተነሥቶ ታጠበ*), the
    imperative and jussive (*ና*, *ተቀመጥ*, *ይምጣ*, *እንሂድ*), the relative verb (*የሚሠራ ሰው*, *የገዛችው
    ቤት*), *ሲ-* "when" and *ቢ-* "if" (*ሲመጣ*, *ቢዘንብ*), *እንዲ-* "so that" (*እንዲመጣ ጠየቀ*), the present
    perfect (*መጥቷል*, *ደርሳለች*), object suffixes (*አየው*, *ሰጠችኝ*, *ነገራቸው*);
  - ranks 601–1000: the passive *ተ-* (*ተሰበረ*, *ይታወቃል*), the causative *አ-* / *አስ-* (*አሳየ*,
    *አስተማረ*), *ስለ* + verb "because" (*ስለታመመ*), *እስከ*, *ከ ... በፊት*, *ከ ... በኋላ* with verbs,
    comparison (*ከአበበ ይበልጣል*), the past progressive and past perfect with *ነበር* (*ይሠራ ነበር*, *ሄዶ
    ነበር*);
  - ranks 1001–2000: reported speech (*... እንደሆነ ነገረችው*, *መጣሁ አለ*), longer joined sentences
    (*ምንም እንኳ*, *ስለዚህ*, *ነገር ግን*, *ቢሆንም*), the reciprocal and repeated verbs (*ተነጋገሩ*,
    *ተለያዩ*);
  - ranks 2001–2900: a richer register: a newspaper column, a letter, an interview, a short essay,
    proverbs explained by their context.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a story
  may be told by a first-person narrator or as a letter or a diary entry.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `ጥያቄዎች፦` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses the
  story's words:

  ```
  ጥያቄዎች፦
  ሳራ ምን ትፈልጋለች?
  ሳራ ስልኳን ትፈልጋለች።
  ስልኩ የት ነው?
  ስልኩ ኩሽና ውስጥ ነው።
  ```

  The header `ጥያቄዎች፦` is layout: the checker does not count it as a word.
- **Standard Amharic**: the narration is today's standard written Amharic as in Addis Ababa papers and
  schoolbooks. No English where Amharic has its own word (*ስልክ*, not *ፎን*). No swearing (skip such list
  words).
- **Amharic typography**: words are parted by spaces (not the old ፡); a sentence ends with ። and a
  question with ? (or ፧); the comma is ፣, the semicolon ፤. Never end a sentence with "." (the reader
  reads one letter before a full stop as an abbreviation). Direct speech in
  «…» or "…", each speaker's line a paragraph of its own. Write numbers as Arabic digits (*3 ልጆች*,
  *ከሰዓት 9 ሰዓት*) except a number word in your band: spell that one out to teach it (*ሁለት ልጆች*).
  Never write Ethiopic numerals (፩, ፲): the reader reads them as words. No abbreviations with dots
  (*ዓ.ም.*, *ወ/ሮ*): write them out (*ወይዘሮ*).
- **Spelling**: Amharic writes some sounds with two or three letters (ሀ ሐ ኀ, ሰ ሠ, አ ዐ, ጸ ፀ). Write
  each word as the list's headword writes it, or as its forms in the band show; the checker reads any
  of the spellings as the same word, the reader does not.
- **Names are names**: Ethiopian names (*አበበ*, *ሳራ*, *ዮናስ*, *ማርታ*, *ቤተልሔም*, *ዳዊት*, *ሔለን*,
  *ኤርሚያስ*, *ሩት*, *ሰላማዊት*, *ሙሉጌታ*, *ፍሬሕይወት*, *አልማዝ*, *ከበደ*, *መሐመድ*, *ፋጡማ*, *ሐና*, *ዮሐንስ*,
  *ሜሮን*, *ናትናኤል*) and places go in `names`; a name with a preposition or ending on it (*የሳራ*,
  *ለአበበ*, *ከዮናስ*) counts as the name. Many Ethiopian names are list words: *ሰላም*, *ተስፋ*, *ፍቅር*,
  *ብርሃን*, *ደስታ*, *ታደሰ*, *አበራ*, *በረከት*, *ጸጋ*, *አበባ*, *ገነት*, *ውበት*, *ወርቅ*, *መስፍን*: never use one
  as a character's name. Words for peoples and languages (*ኢትዮጵያዊ*, *አማርኛ*, *ኦሮሞ*) count as words
  at their rank: never teach them as band words, a text may still use one.
- **Original texts**: write your own stories; never copy or retell existing course material, and not
  the courses of the other languages either.

## Things to know about the Amharic list

- Headwords are written as the English Wiktionary writes them: verbs by the perfective's third
  person masculine (*ሰበረ*, *ሄደ*), or by the infinitive where the pack has only that (*መምጣት*, *ማወቅ*,
  *ማድረግ*, *መቻል*); nouns and adjectives by their bare form. Teach a verb through its forms: *ይሄዳል*,
  *ሄደች*, *መሄድ* all teach *ሄደ*; *መጣ*, *ይመጣል* teach *መምጣት*.
- Words of the list hold their prepositions and endings: *ቤት* holds *በቤት*, *ቤቱ*, *ቤቶች*, *የቤታችን*.
  The checker reads a form the list lacks by the rules the list was built with (*ለልጆቻችን* counts as
  *ልጅ*, *የሚሰብሩት* as *ሰበረ*); only words with no such reading are outside words.
- About 500 list words are no entry of the dictionary pack (the English Wiktionary lacks *ወደ*, *ግን*,
  *ሌላ*, *ሄደ*, *መጣ*): the learner sees them unglossed, so teach them in a sentence that makes their
  sense clear.
- The list is web text, much of it news and church writing: *እግዚአብሔር*, *ቅዱስ*, *ክርስቲያን*, *መንግሥት*,
  *ፓርቲ*, *ሚኒስትር* rank high. They are ordinary words, fine in a story; skip noise and anything that
  is only a name.
