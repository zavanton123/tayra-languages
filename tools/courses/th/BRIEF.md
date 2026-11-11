# Writing a Thai frequency course

Each course teaches one band of 100 words from the Thai word frequency list (`bands/<rankUpTo>.txt`: index,
rank, word, its forms). Course `th-mini-0300` teaches ranks 201–300 and may use every word ranked 1–300. Ten
lessons, each introducing about ten of the band's words, so the course covers the band; a learner who finishes
it knows the 300 most common words. `themes.tsv` gives each course its theme. The list has 8,157 words: the
courses are `th-mini-0100` to `th-mini-8100`.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini stories": plain,
clear and predictable, so the learner's attention goes to the language, not the plot. A course is ten separate
stories on one broad everyday theme (its title), not one serial story; a lesson never needs another lesson to
make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a simple happy
  ending. Everyday life where Thai is spoken: Bangkok (write กรุงเทพ, without ฯ), Chiang Mai, Chiang Rai,
  Lampang, Phuket, Krabi, Hua Hin, Pattaya, Rayong, Khon Kaen, Khorat, Ubon, Hat Yai, Ayutthaya, Sukhothai,
  Kanchanaburi, Pai, the islands and the beaches, the rice fields and the hills of the north; now and then Laos
  (Luang Prabang, Vientiane), Singapore, Tokyo or London, where Thais live and travel: home, family, food, the
  market, work, school, the town, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony, jokes that
  depend on wordplay, unreliable narrators or open endings. Events in the order they happen. Few characters
  (one to three), named clearly. Give each story its own names: two Nidas in two lessons blur together.
- **Plain narration**: third person, short sentences, one idea per sentence, the ordinary order (subject, verb,
  object). Thai drops subjects and objects freely, and before the pronouns are taught the narration simply
  repeats the name (*นิดาอยู่ที่เชียงใหม่ นิดาอายุ 9 ปี*); in dialogue a child or a close friend says their own
  name for "I" (*วิไลจะไม่ไป*), as Thais do. The pronouns come late in the list: *เขา* 125, *ผม* 284, *เธอ* 361,
  *คุณ* 434, *ฉัน* 1181; so do the kinship words (*แม่* 471, *ลูก* 453, *พ่อ* 858, *น้อง* 763, *พี่* 1167, *ยาย*
  2275) and the polite particles (*ครับ* 812, *นะ* 827, *ค่ะ* 1655, *คะ* 2178): use them where a story needs them,
  as outside words, until their band.
- **Grammar grows with the bands**, each band using what came before. Thai words do not inflect: what grows is
  the constructions.
  - ranks 1–300: *เป็น*, *อยู่*, *มี*; numbers with their classifier after the noun (*แมว 1 ตัว*, *คน 3 คน*,
    *มะม่วง 6 ผล*); *จะ* for what is going to happen, *แล้ว* for what is done, *ได้* after a verb for "can"
    (*ไปได้*) and before one for "got to" (*ได้ไปลำปาง*); *ไม่*; the direction verbs after a verb (*ขึ้นไป*,
    *ลงมา*, *ออกมา*); *ให้* "give, for"; *ของ* "of"; *ว่า* as the speech tag (*นิดาว่า "..."*); yes/no questions
    with *หรือไม่*, and the question words (*อะไร*, *ใคร*, *ที่ไหน*, *กี่*) as outside words;
  - ranks 301–600: *กำลัง* for what is going on, *เคย* "ever", *ยังไม่* "not yet"; comparisons with *กว่า* and
    *ที่สุด*; the pronouns and the particles *ครับ*, *ค่ะ*, *คะ*, *นะ* in dialogue (a man ends a polite sentence with
    *ครับ*, a woman with *ค่ะ*, and her question with *คะ*); requests with *ช่วย ... หน่อย*, *ขอ*, *อย่า*;
    *ทำให้*; the speech verbs *บอกว่า*, *ถามว่า*, *พูดว่า*;
  - ranks 601–1000: relative clauses with *ที่* and *ซึ่ง*; *เพราะ*, *ถ้า ... ก็*, *เมื่อ*, *ตอนที่*, *ก่อน*,
    *หลังจาก*; *ถูก* for a passive (something unpleasant: *ถูกฝนตก* is fine, nothing violent); *ให้* to make or let
    someone do something (*แม่ให้นิดาไปตลาด*); reported speech;
  - ranks 1001–2000: longer, joined sentences (*ทั้ง ... และ*, *จึง*, *ดังนั้น*, *แม้ว่า*, *ไม่เพียงแต่ ... แต่ยัง*);
    *คง*, *อาจจะ*, *น่าจะ*; the nouns made with *การ* and *ความ* (*การเดินทาง*, *ความสุข*), which the reader shows as one
    word; a first-person narrator, an e-mail, a letter or a diary entry;
  - ranks 2001–4000: a more formal written register where the text type asks for it (*ซึ่ง*, *โดย*, *ในขณะที่*,
    *อย่างไรก็ตาม*); short informative or opinion texts, everyday and calm;
  - ranks 4001–8100 (C1, C2): everything before, with more abstract and nuanced content: a short essay or column,
    an interview, a portrait, a review; precise vocabulary for feelings, ideas and processes; the specialised
    words of professions and sciences, each made clear by its context.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words three times or
  more where it reads naturally, the story's key sentence pattern several times (*บุญชูว่า "โมจิ ลงมา" แต่โมจิไม่ลงมา*).
- **Questions at the end**: after the story, a paragraph starting `คำถาม:` with 4–6 simple questions about the
  story, each followed on its own line by a full-sentence answer that reuses the story's words:

  ```
  คำถาม:
  นิดาอยู่กับใคร
  นิดาอยู่กับสุดาและวีระ
  ปิติอยู่ที่ไหน
  ปิติอยู่ที่ลำปางกับชูใจ
  ```

  The header `คำถาม:` is layout: the checker does not count it as a word.
- **Standard Thai**: the narration is standard central Thai as it is written today, correct, neutral and
  natural; no slang, no regional Thai (Isan, northern, southern) in the narration, and no English words that
  Thai has a common word for. Dialogue may be lightly colloquial in what any careful speaker says.
- **Thai typography**: no spaces between words; a space where English would put a full stop or a comma; no full
  stops and no commas; a question has no question mark (the question word or *หรือไม่* makes it a question).
  Direct speech in quotation marks, the speaker's line a paragraph of its own: *วิไลว่า "วิไลจะไม่ไป งานนี้วิไลต้องทำ"*.
  Numbers in Arabic digits (*อายุ 9 ปี*, *วันที่ 5 มีนาคม*, *2 ชั่วโมง*): the reader shows digits as no words, and
  the checker counts them neither way. Never Thai digits (๑๒๓). *ๆ* repeats the word before it (*เด็กๆ*,
  *ช้าๆ*): the reader shows it as no word; write it straight after the word. Never *ฯ* (*กรุงเทพฯ*): the reader
  shows it as a word of its own.
- **Names are names**: place names, words for peoples, nationalities and languages count as names: skip them as
  band words even when the list has them as entries (*ไทย* 24, *จีน* 475, *อังกฤษ* 482); a text may still use
  one, counted at its rank. Never use a list word as a character's name.
- **Original texts**: write your own stories; never copy or retell existing course material (LingQ's, school
  readers such as *มานี มานะ*, or any other), and not the courses of the other languages either.

## Content rules (strict)

The stories are for readers of every age and every country. Keep away from:

- **religion** in every form: monks, temples (*วัด*), merit-making (*ทำบุญ*), offerings, prayer, Buddha images,
  spirits, spirit houses, ghosts, amulets, fortune-tellers and every custom or holiday of religious origin, even
  as a date or a school break: Songkran, Loy Krathong, Makha Bucha, Visakha Bucha, Buddhist Lent, the Vegetarian
  Festival, ordinations; a market, a food fair, a flower festival, a kite festival, a birthday, New Year's Eve as
  a family evening, the school year and the holidays are fine;
- **the monarchy and politics**: the royal family and the royal words (*ราช*, *ทรง*, *พระราชทาน*, *พระองค์*,
  *เสด็จ*), the King's and Queen's birthdays, the government, parliament, elections, parties, ministers, the prime
  minister, protests, flags; everyday civic life (a library, a school, the district office's counter) is fine;
- **war, the army, weapons, violence, crime, police and courts**; Muay Thai and every other kind of boxing or
  fighting sport;
- **death**, killing, funerals, blood and injury beyond a scraped knee; an old person in a story is alive and
  well, and a pet does not die; **epidemics** (COVID, viruses, infection), and illness beyond a cold, a fever or a
  visit to the doctor;
- **vulgar or sexual words**, and words whose main sense is sexual;
- **alcohol** in every form (beer, whisky, rice spirits), **gambling** (the lottery, *หวย*, *สลาก*, betting),
  drugs and smoking;
- **names taught as words**: a band word that is a name, a part of names, a nationality, a people or a language is
  skipped. Common Thai first names are fine as characters, as long as they are not list words.

Skip band words that only fit these topics, and say so in your report. The skip list for the first 1000 ranks:

- politics: *รอง* 30, *พรรค* 34, *กระทรวง* 63, *รัฐบาล* 66, *นายกรัฐมนตรี* 74, *ประธาน* 91, *ประชาชน* 120,
  *รัฐมนตรี* 135, *คณะกรรมการ* 145, *แห่งชาติ* 149, *รัฐ* 157, *ว่าการ* 164, *ทำเนียบ* 175, *นายก* 187, *สภา* 204,
  *สมาชิก* 241 (except a club's members), *เลขาธิการ* 264, *การเลือกตั้ง* 267, *นายกฯ* 291, *รัฐธรรมนูญ* 304,
  *เลือกตั้ง* 325, *รัฐสภา* 337, *ชุมนุม* 348, *แกนนำ* 386, *คณะรัฐมนตรี* 387, *ผู้สมัคร* 390, *อำนาจ* 394,
  *ปลัด* 399, *กลาโหม* 404, *นโยบาย* 416, *การชุมนุม* 508, *ผู้ว่าราชการ* 571, *ประชาธิปไตย* 592,
  *ประชาธิปัตย์* 668, *มหาดไทย* 700, *อภิปราย* 777, *ฝ่ายค้าน* 795, *ม็อบ* 832;
- religion and royalty: *พระ* 41, *ราช* 138, *วัด* 215, *ทรง* 300, *บุญ* 331, *ธรรม* 424, *เจ้า* 425, *พระองค์* 584,
  *พระราชทาน* 597, *บรม* 617, *เกล้า* 627, *พุทธ* 650, *ศาสนา* 730, *สมเด็จพระ* 765, *พระบาทสมเด็จ* 977;
- police, military, crime, courts: *ตำรวจ* 51, *สอบสวน* 144, *คดี* 202, *ทหาร* 249, *ศาล* 321, *ยิง* 431,
  *จับกุม* 454, *สารวัตร* 469, *สืบสวน* 494, *ผู้บัญชาการ* 520, *ปราบปราม* 642, *ภูธร* 697, *ผู้ต้องหา* 714,
  *กองทัพ* 735, *อาญา* 766, *ฟ้อง* 778, *คนร้าย* 801, *กองบัญชาการ* 845, *ยึด* 867, *กองทัพบก* 883, *กล่าวหา* 905,
  *สงคราม* 948, *ศึก* 981, *อาวุธปืน* 989;
- death and disease: *โควิด* 131, *เสียชีวิต* 238, *ระบาด* 278, *ไวรัส* 307, *เชื้อ* 338, *การแพร่* 372,
  *บาดเจ็บ* 481, *ศพ* 572, *ติดเชื้อ* 628, *ตาย* 720, *โรคติดเชื้อ* 857;
- gambling and boxing: *สลาก* 611, *มวย* 799;
- names, nationalities, days: *ไทย* 24, *จันทร์* 43, *ศุกร์* 99, *อาทิตย์* 103, *อังคาร* 109, *พฤหัสบดี* 110,
  *พุธ* 146, *จีน* 475, *อังกฤษ* 482, *เอเชีย* 653, *อีสาน* 878;
- news formulas, not worth a lesson below rank 1000: *ราย* 78, *กรณี* 90, *เปิดเผย* 127, *ทั้งนี้* 140, *ณ* 153,
  *ดังกล่าว* 167, *ระบุ* 172, *แถลง* 599.

Further on, the same categories hold.

## The file

Write `tools/courses/th/<id>.json`:

```json
{
  "id": "th-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Thai, e.g. อาหารและการซื้อของ",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places, months and pets used in the texts"],
  "lessons": [
    {
      "title": "Story title in Thai",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, after `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/th/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93% up to rank 300,
  90% for the first course). Names you list in `names` count as known; digits are not counted. The checker lists
  every word outside the range with its rank: replace them, or keep a few when the text needs them.
- **New words**: each is in the course's band, introduced once in the course, and used at least twice in its
  lesson as the reader cuts the text (see below). Reuse earlier lessons' new words too.
- **No new word before its lesson**: a lesson must not use a band word that a later lesson of the course teaches
  (the checker fails it: "uses ... before lesson N teaches it"). Plan the lessons so the commonest little words
  (*ที่*, *ไม่*, *ได้*, *จะ*, *แล้ว*, *ก็*) are taught in the first lessons.
- **Length** (story and questions together, in the reader's words): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000,
  320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words, skipping the content rules' words, names
  and list noise; the checker's last line lists the band words not taught yet.

Things to know about the Thai list and the checker:

- The courses are pinned to the list they were written against, `tools/courses/th/wordlist.tsv` (a copy of the
  app's `th.tsv`); the bands come from it. The list is counted from Thai news and Wikipedia (the Leipzig corpora),
  so politics, police and dates rank high and everyday talk ranks low: *สวัสดี* 2623, *ขอบคุณ* 1077, *ข้าว* 1286,
  *แมว* 2526, *กาแฟ* 5198. Words of the content rules are skipped; everyday words outside the band are used as
  outside words where a story needs them.
- **How the reader, and so the checker, cuts Thai.** Thai is written without spaces, so the app cuts each stretch
  of letters into words itself: into the likeliest words by how common the list says they are, never inside a
  syllable, among the words of the list and of the Thai dictionary pack. The checker cuts exactly as the reader
  does. It follows that:
  - **two words the dictionary has together are one word**, counted at the pair's own rank: *ทำงาน* 229 (not *ทำ* +
    *งาน*), *วันนี้* 197, *มาจาก* 275, *เข้ามา* 369, *เข้าไป* 523, *ออกไป* 690, *ออกจาก* 625, *ด้วยกัน* 956,
    *ทุกคน* 779, *ทุกวัน* 365, *แล้วก็* 2266, *ในเมือง* 1874,
    *ที่มา* 1107 (*คนที่มา* is *คน* + *ที่มา*), *ผลไม้* 2341, *กลับบ้าน* 1830. Such a pair does not count as a use
    of either word. Write around it when you need the single words: *ไม่ไป*, *ไม่ต้อง*, *ขึ้นไป*, *ลงมา*, *ออกมา*
    are cut into their words.
  - **but some grammatical words always stand apart**: *ไม่*, *ก็* and *ตอน* join no word after them, *ว่า*, *อยู่*
    and the pronouns *เรา*, *ฉัน*, *เธอ* none before them, though the dictionary has the pair: *ไม่มี*, *ไม่ได้*,
    *ไม่ดี*, *ก็ได้*, *ตอนนี้*, *ตอนเย็น*, *คิดว่า*, *อาศัยอยู่*, *ของเรา* are two words each (*ไม่มีใคร* three), and
    such a pair is no band word to teach.
  - **a set phrase of the dictionary is one word too**, often outside the list: *คุณชื่ออะไรครับ*, *แม่ซื้อ* (in
    *แม่ซื้อปลา*), *กินข้าว* 5758. Rephrase.
  - **a word the list and the dictionary lack is cut into the pieces they have**, or kept as one unknown word: a
    name is usually cut into syllables (*นิดา* is *นิ* + *ดา*), which is why names go in `names`; the checker counts
    a listed name as one known word. But the reader knows no names: a name must start and end where the reader's
    words do, or it runs into the words around it, and the checker fails the lesson ("the reader runs the name ...
    into the words around it"). Then choose another name or rewrite the sentence around it.
  - *เท่าไหร่*, *เมื่อไหร่*: the dictionary has only *เท่าไร*, *เมื่อไร*; write those.
- Run `rank.py` (see INSTRUCTIONS) on a sentence to see how the reader cuts it before you build a lesson on it.
- **Words with many senses**: *ที่* 2 (that, at, place), *ได้* 11, *ให้* 15, *ของ* 9 (of, thing), *ว่า* 6 (that,
  says), *ถูก* 79 (cheap, correct, and the passive), *ผล* 96 (fruit, result, and the classifier for fruit), *ตัว* 76
  (body, and the classifier for animals and clothes), *งาน* 98 (work, and a party or fair). Use a new word in a
  sense the dictionary pack gives it.
- **Names** are not in the list: people, places, pets, shops, months (*มีนาคม*: the months are not in the list) go
  in `names`. Many Thai names are list words, because the dictionary has them as words: *มาลี*, *สมชาย*, *วิชัย*,
  *อรุณ*, *กมล*, *อนันต์*, *ดวงใจ*, *อนุชา*, *ปรีชา*, *กิตติ*, *นภา*, *มะลิ*, *น่าน*, *อุดร*, and most nicknames: never
  use them. Check a name with `rank.py`: a name shown as one word with a rank is a list word; one cut into pieces
  or shown with `-` is fine. Safe in the pilot: *นิดา*, *ปิติ*, *สุดา*, *วีระ*, *ประภา*, *ชูใจ*, *กานดา*, *มานพ*,
  *อำไพ*, *ธนา*, *จิรา*, *สมพร*, *สุรชัย*, *ดารณี*, *ไพลิน*, *รัตนา*, *วิไล*, *ชไมพร*, *บุญชู*, *ทองใบ*, *เพ็ญศรี*,
  *อรพิน*, *สมใจ*, *ยุพา*, *ศรีสุดา*, *ปกรณ์*, *ศิริพร*, *บุญเลิศ*; places *เชียงใหม่*, *ภูเก็ต*, *ลำปาง*, *ขอนแก่น*,
  *หัวหิน*, *โคราช*, *ปาย*, *หลวงพระบาง*, *เวียงจันทน์*. The days are list words (*จันทร์* 43, *ศุกร์* 99): never
  teach them; a story may use one at its rank.
- Loanwords Thai uses as its own (*ฟุตบอล*, *อินเทอร์เน็ต*, *คลิป*, *ออนไลน์*) are taught like any word; the Latin
  letters' names (*เอ*, *บี*, *ซี*) are not in the list.
