# Writing a Vietnamese frequency course

Each course teaches one band of 100 words from the Vietnamese frequency list
(`bands/<rankUpTo>.txt`: index, rank, word, its other spellings). Course `vi-mini-0300` teaches
ranks 201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the
band's words, so the course covers the band. `themes.tsv` gives each course its theme. The list has
5,579 words, so the last course is `vi-mini-5600` (ranks 5501–5579).

## Words are syllables

Vietnamese is written a syllable at a time, with a space between syllables, and the app's reader
shows every syllable as a word: *gia đình* ("family") is two words to the reader, *gia* and *đình*.
The list counts the same units, and so does the checker: a "word" in this brief, in the bands, in
`newWords` and in the lesson lengths is a syllable.

- **A free syllable** (one that stands as a word by itself: *nhà*, *ăn*, *đẹp*, *không*, *người*)
  is taught in its main free sense, used on its own at least twice. Uses inside compounds count
  for the checker too, but the lesson must show the word itself.
- **A bound syllable** (one that lives inside words of two syllables, mostly Sino-Vietnamese:
  *thể*, *gia*, *sử*, *quan*, *động*, *bất*, *nhiên*, *khả*) is taught through **one** common
  compound it appears in: *có thể*, *gia đình*, *lịch sử*, *quan trọng*, *hoạt động*, *bất ngờ*,
  *tự nhiên*, *khả năng*. The lesson uses that compound naturally, at least twice, and the gloss
  you give for the syllable is its meaning in that compound ("thể: in *có thể*, can, be able").
  Choose the compound a learner meets most; do not chase rare compounds to reach two uses.
- The other syllable of such a compound may be outside the course's range (*đình* is rank 374 when
  *gia* is taught at 197): it counts as an outside word for coverage, so pick compounds whose
  other half is already known or in the band where you can.
- **No word through another sense**: *thành* is "to become" (*trở thành*) and "city wall"; *thành
  phố* is a city. Teach a free syllable through its own sense, not only through a compound where it
  means something else. *đầu* is "head" and "beginning" (*bắt đầu*, *đầu tiên*): teach the sense
  you name, and use it in that sense.
- Spellings: the list files the spellings of one syllable together (*hòa* and *hoà*, *lý* and
  *lí*, *kỹ* and *kĩ*); write the list's headword. Tone marks go where modern spelling puts them:
  *hòa*, *khỏe*, *thủy*, *tòa*, *quý*.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad theme (its title), not one serial story; a
lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple ending. *Lan không biết điện thoại của cô ấy ở đâu. Cô ấy vào bếp. Không có. Cô ấy vào
  phòng khách...* Life where Vietnamese is spoken: Hà Nội, Thành phố Hồ Chí Minh (Sài Gòn), Huế,
  Đà Nẵng, Hội An, Hải Phòng, Cần Thơ, Nha Trang, Đà Lạt, Vũng Tàu, Quy Nhơn, Vinh, Ninh Bình, Hạ
  Long, Sa Pa, Phú Quốc, Buôn Ma Thuột; the Mekong Delta and the Red River; and now and then Paris,
  Berlin, Praha, Warszawa, Moskva, Sydney, Melbourne, Toronto or California, where many Vietnamese
  live: home, family, food, the market, work, school, the town, health, free time, travel, the
  motorbike, Tết with the family.
- **Nothing confusing**: no plot twists, flashbacks, jumps in time, irony, wordplay, unreliable
  narrators or open endings. Events in the order they happen. Few characters (one to three),
  named clearly. Give each story its own names: two Lans in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, ordinary word order
  (subject, verb, object; the adjective after its noun: *nhà mới*, *áo đẹp*). Refer to a character
  by name or by kin word plus *ấy* (*anh ấy*, *chị ấy*, *cô ấy*, *ông ấy*, *bà ấy*, *em ấy*), *nó*
  for a child or an animal, *họ* for several. Little dialogue; when there is some, short and
  simple, with the pronouns a Vietnamese speaker would choose (see below).
- **Grammar grows with the bands**, each band using what came before. Vietnamese does not
  inflect: the progression is in particles, sentence patterns and vocabulary.
  - ranks 1–300: plain statements; *là* before a noun (*Lan là giáo viên*), an adjective as the
    predicate without *là* (*Nhà này đẹp*), *rất* + adjective; *có* (to have, there is), *không*
    for "not"; yes/no questions with *có ... không?* and *... không?*, and *gì*, *ai*, *đâu*,
    *nào*, *sao*; the time words *đã* (past), *đang* (now), *sẽ* (future), *rồi* (already); *muốn*,
    *cần*, *phải*, *thích*, *có thể* + verb; *của* (*nhà của Lan*, often left out: *mẹ Lan*);
    plurals with *các* and *những*; the classifiers *cái* and *con* with numbers (*một cái bàn*,
    *hai con mèo*); *ở*, *trong*, *với*, *cho*, *đến*, *về*; *đi* + a place or an activity (*đi
    chợ*, *đi làm*);
  - ranks 301–600: *chưa* ("not yet") and *... chưa?*; *vừa*, *mới* ("just"), *sắp* ("about
    to"), *từng* ("ever"); comparison with *hơn*, *nhất*, *bằng*; requests with *hãy*, *đừng*,
    *nhé*, *đi* at the end; *được* and *bị* in plain cases (*được quà*, *bị ốm*); *để* for a
    purpose; *khi*, *lúc*, *trước khi*, *sau khi*; *vì ... nên*; more classifiers (*chiếc*,
    *quyển*, *cuốn*, *tờ*, *đôi*, *bức*);
  - ranks 601–1000: *nếu ... thì*; *mà* in relative clauses (*cái áo mà Lan mua*) and as "but";
    reported speech with *rằng* or *là* (*Mẹ nói rằng...*); *vẫn*, *cũng*, *đều*, *lại*, *nữa*,
    *cả*; *tuy ... nhưng*; *càng ... càng*; the sentence-final particles *à*, *nhỉ*, *chứ*, *mà*
    in dialogue; *được không?*;
  - ranks 1001–1300: *mặc dù*, *dù ... vẫn*, *cho nên*, *vì vậy*, *do đó*, *trong khi*;
    *bị* and *được* with an agent (*bị mẹ mắng*, *được cô giáo khen*); *đã ... bao giờ chưa?*;
  - ranks 1301–1600: *không những ... mà còn*, *vừa ... vừa*, *hễ ... là*, *trừ khi*, *giá mà*;
    reduplicated words in their common uses (*nho nhỏ*, *xanh xanh*, *từ từ*);
  - ranks 1601–2000: nouns made with *việc*, *sự*, *cuộc*, *niềm*, *nỗi* (*việc học*, *niềm vui*);
    *nhằm*, *để mà*; indirect questions (*Lan hỏi xem anh ấy có đến không*);
  - ranks 2001–3000: the written register: *tuy nhiên*, *ngoài ra*, *hơn nữa*, *mặt khác*,
    *bên cạnh đó*; more Sino-Vietnamese words; common idioms (*thành ngữ*) made clear by their
    context;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: the newspaper
    column (*tản văn*, a light reflection on everyday life), precise adjectives and clear images;
  - ranks 4001–5600 (C1): more abstract and nuanced content: a short essay that weighs two sides,
    an interview, a portrait, a review; precise vocabulary for feelings, ideas and processes;
    long sentences that stay easy to parse.
  After rank 1000 sentences get longer and joined, dialogue can carry more of the story, and a
  story may be told by a first-person narrator or as a letter, a message or a diary entry. From
  rank 2000 a lesson may also be a short informative or opinion text, calm and easy to follow.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `Câu hỏi:` with 4–6 simple
  questions about the story, each followed on its own line by a full-sentence answer that reuses
  the story's words:

  ```
  Câu hỏi:
  Lan không biết gì?
  Lan không biết điện thoại của cô ấy ở đâu.
  Điện thoại ở đâu?
  Điện thoại ở trong xe.
  ```

  The header `Câu hỏi:` is layout: the checker does not count it as two words.

## Pronouns, kin words and politeness

Vietnamese speakers address each other with kin words chosen by age and relation, and the same
word serves as "I" and "you": a child says *con* to a parent and calls the parent *mẹ*, *bố*;
the parent says *mẹ* or *bố* for "I" and *con* for "you". Siblings: *anh*/*chị* (older) and *em*
(younger). To a stranger: *anh*, *chị* (about one's own age or a bit older), *cô*, *chú* (a
generation older), *bác*, *ông*, *bà* (older still), *em* or *cháu* (younger). *tôi* is the
neutral "I" among adults who are not close; *mình* and *bạn* between friends of the same age;
*tớ* and *cậu* between children and young friends. Keep each pair consistent inside a story.

- Politeness: *ạ* at the end of what a younger person says to an older one (*Con chào mẹ ạ.*),
  *dạ* or *vâng* for "yes" to an older person; *xin* in polite requests (*xin lỗi*, *xin cảm
  ơn*); *chào* + kin word as a greeting (*Chào chị!*, *Cháu chào ông ạ.*).
- *mày* and *tao* are rude or very intimate: never in these stories. *hắn* is "he" with a cold or
  hostile tone (a villain, a thief): use it only where the story calls for it.

## Standard Vietnamese and spelling

- Write standard Vietnamese as books and the press use it, in the northern-based standard
  vocabulary: *không*, *vào*, *gì*, *vậy*, *này*, *bố* or *ba*, *mẹ*, *ngã*, *ốm* or *bệnh*.
  Southern and central dialect words (*vô* for *vào*, *hông*, *nè*, *dzậy*, *chi* for *gì*, *mô*,
  *răng*, *rứa*) and their spellings stay out of the narration; a character from Huế or Sài Gòn
  may say one in dialogue only where the story makes it plain. No slang and no teen spellings
  (*ko*, *k*, *đc*, *j*).
- **Every diacritic, always**: a syllable without its marks is another word (*ma* ghost, *má*
  mum, *mà* but, *mả* grave, *mã* code, *mạ* rice seedling). The checker compares them exactly.
- Numbers as digits (*3 người*, *lúc 7 giờ*, *năm 1995*), except a number word that is in your band:
  spell that one out to teach it (*hai người*). Dates as *ngày 5 tháng 3*; days and months are
  everyday words (*thứ hai*, *chủ nhật*, *tháng ba*): teach them, lowercase except at the start
  of a sentence.
- **Typography**: dialogue after a colon, in curly double quotes, each speaker's line its own
  paragraph: `Lan nói: “Con đói rồi.”` The questions and answers of the final `Câu hỏi:` block are
  plain lines, as above. Inside a sentence a dash is the en dash with spaces ( – ).

## Names

- Vietnamese names are made of ordinary syllables: *Lan* (orchid), *Mai* (apricot blossom;
  *mai* is also "tomorrow"), *Hoa* (flower), *Nam* (south), *Minh* (bright). The checker counts a
  name only where it is written with its capital, as listed in `names`: *Mai* is a name, *mai* is
  the word. So a name may be a list word, but then never start a sentence with that list word in
  its common sense, and do not use a name whose word you teach in the same lesson. Never list a
  name spelled as a word that often starts a sentence (*Anh* for England, *Em*, *Con*, *Ông*,
  *Bà*, *Cô*): write *nước Anh*, *tiếng Anh* without listing *Anh*; it then counts as the word.
- Put every name in `names` as written (*Lan*, *Minh*, *Nguyễn*, *Hà Nội*, *Đà Nẵng*, *Thành phố
  Hồ Chí Minh*). A name of several syllables counts as one known word wherever it is written whole.
- Address characters as Vietnamese do: kin word + given name (*chị Lan*, *anh Minh*, *bà Hoa*,
  *ông Tư*); the family name (*Nguyễn*, *Trần*, *Lê*, *Phạm*, *Hoàng*, *Vũ*, *Đặng*, *Bùi*, *Đỗ*)
  only in full names.
- Place adjectives and words for peoples and languages (*người Việt*, *tiếng Anh*, *người Pháp*)
  are names in this sense: *Việt*, *Anh*, *Pháp* are skipped as band words when they mean only a
  country. *tiếng* (language, sound) and *người* are ordinary words.

## Content

Content rules (2026-11-13): no topic rules. Any normal everyday, cultural or story topic is fine:
religion and holidays (Tết, Trung thu, the pagoda, ancestors' altars), history, kings and dynasties,
legends (*Sơn Tinh – Thủy Tinh*, *Thánh Gióng*), crime and detective stories, war in history,
alcohol, love, death, sport. Only avoid explicit sexual content, slurs used as insults, and
gratuitous gore. Teach ordinary words in their main sense.

## The file

Write `tools/courses/vi/<id>.json`:

```json
{
  "id": "vi-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Vietnamese, e.g. Ăn uống và đi chợ",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts"],
  "lessons": [
    {
      "title": "Story title in Vietnamese",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band syllables this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after that `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/vi/<id>.json`

- **Vocabulary**: at least 95% of a lesson's syllables are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names in `names` and numbers count as known.
  The checker lists every syllable outside the range with its rank: replace them, or keep a few
  when the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson (a bound syllable through its compound). Reuse earlier lessons' new words
  too.
- **Length** (story and questions together, counted in syllables): 80–180 per lesson in the first
  course, 120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000,
  300–520 up to 3000, 320–550 up to 4000, 350–600 after. Syllables run about one and a half to a
  word of English, so a Vietnamese lesson says a little less than a Galician one of the same count.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (an English word, a rude word, a dialect word, a name-only syllable); the
  checker's last line lists the band words not taught yet.

Things to know about the Vietnamese list:

- The courses are pinned to `tools/courses/vi/wordlist.tsv` (a copy of the app's `vi.tsv`), and
  the bands come from it. It is counted from film and TV subtitles (OpenSubtitles), so talk ranks
  high, and so do the words of action films (*giết* 146, *chết* 98, *súng* 359): use them in
  ordinary senses or skip them.
- Some entries are English words written like Vietnamese syllables, counted high by the English
  in subtitles: *i* 248, *the*, *in*, *so*, *can*, *to*, *may*, *a*, *u*, *bang*. Most are also
  Vietnamese words (*to* big, *in* to print, *can* to intervene, *may* lucky, *bang* state);
  teach those in their Vietnamese sense only if they fit, and skip *i* and *the*.
- Syllables the dictionary has only as parts of names are in the list too (*quốc*, *hoàng*,
  *nguyễn*): teach *quốc* through *quốc gia* or *quốc tế*, skip a pure family name.
