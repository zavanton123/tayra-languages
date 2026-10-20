# Writing a Mandarin Chinese frequency course

Each course teaches one band of 100 words from the Mandarin word frequency list
(`bands/<rankUpTo>.txt`: index, rank, the word in simplified characters, its forms). Course
`zh-mini-0300` teaches ranks 201–300 and may use every word ranked 1–300. Ten lessons, each
introducing about ten of the band's words, so the course covers the band; a learner who finishes it
knows the 300 most common words.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *小明想要一只猫。他去一个地方看猫。他看到三只猫……* Everyday life: home,
  family, food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who 他/她 is whenever it could be
  unclear.
- **Plain narration**: third person, short sentences, one idea per sentence, common word order
  (subject, time, place, verb, object). Little dialogue; when there is some, short and simple.
  Grammar grows with the bands, each band using what came before:
  - ranks 1–500: 是, 有, 在 (to be at), 很 + adjective, questions with 吗, 呢, 什么, 哪里, 谁,
    几, negation with 不 and 没, the measure words 个, 本, 杯, 只, 的 for possession and
    description, 想, 要, 喜欢, 会, 能, 可以, times and days before the verb;
  - ranks 501–1000: 了 in all its uses (a simple 了 for a finished action is fine from the start), 过 for experience, 在 / 正在 for
    an action in progress, 比 comparisons, 因为……所以, 还是 and 或者, 已经, 就 and 才, simple
    direction complements (回来, 出去, 上来), verb repetition (看看, 试一试);
  - ranks 1001–1300: result complements (看完, 听懂, 找到, 做好), the 把 sentence, the 被 passive,
    虽然……但是, 如果……就, 一……就, 先……然后, 又……又, longer sentences joined by these;
  - ranks 1301–1600: potential complements (看得懂, 来不及), degree complements (说得很快),
    the 的 / 地 / 得 contrast, 越来越, 连……都 / 也, 除了……以外, 不但……而且, reported speech
    (她说她很累);
  - ranks 1601–2000: 只要……就, 只有……才, 无论……都, 即使……也, 既然……就, 是……的 for when, where
    and how something happened, rhetorical questions (难道……吗), the most common four-character
    idioms, each clear from its context;
  - ranks 2001–2500: written connectors such as 然而, 因此, 于是, 尽管, 而且, 并且; 之所以……是因为;
    a story may be told by a first-person narrator, or as a diary entry, a letter or a message;
  - ranks 2501–3000: a richer, more formal register: 对于, 关于, 由于, 通过, 以……为……, 在……下,
    common 成语 explained by their context, full reported speech;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the 散文 (a light reflection on everyday life), dialogue that marks polite
    (您) and familiar speech;
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short essay
    that weighs two sides, an interview, a profile, a review; precise vocabulary for feelings, ideas
    and processes; long sentences that stay easy to parse; tone and register chosen to suit the text;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and the
    arts, each made clear by its context, and texts that explain a process or compare viewpoints;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images and
    rhythm, academic and journalistic registers, learned and classical expressions explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to a
  newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one clear
  idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*他在厨房找。他在房间找。他在客厅找。*).
- **Questions at the end**: after the story, a paragraph starting `问题：` with 4–6 simple questions
  about the story, each followed on its own line by a full-sentence answer that reuses the story's
  words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  问题：
  小明想要什么？
  小明想要一只猫。
  小明去哪里？
  他去一个地方看猫。
  ```

- **Standard Mandarin (普通话) in simplified characters**: neutral, correct and natural, as
  mainland China writes it; no slang, no dialect words, no 儿化 spellings (write 一点, 这里, 哪里,
  not 一点儿, 这儿, 哪儿), no pinyin, no Latin letters and no English words in the text. Full-width
  Chinese punctuation (，。？！：；“”《》、……), no spaces between words.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/zh/<id>.json`:

```json
{
  "id": "zh-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Chinese, e.g. 吃饭和买东西",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, as written in them"],
  "lessons": [
    {
      "title": "Story title in Chinese",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, in simplified characters as the band file writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by `python3 tools/check_course.py tools/courses/zh/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson, as a word of its own (see "How the reader cuts words" below). Reuse earlier
  lessons' new words too.
- **Length** (story and questions together, in words as the checker cuts them): 80–180 words per
  lesson in the first course, 120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000,
  260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–700 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (a surname, a name, a bound syllable that is no word on its own, a classical or
  very formal particle at a low level, a list error); the checker's last line lists the band words
  not taught yet.

## Things to know about the Chinese list and the checker

- The courses are pinned to the list they were written against, `tools/courses/zh/wordlist.tsv`
  (a copy of the app's `zh.tsv`); the bands come from it. The list writes its words in traditional
  characters with their simplified forms; the checker reads everything in simplified characters,
  and the band files show each word in simplified characters first. Write the courses in
  simplified characters only: the app shows them in traditional characters to a reader who chose
  those.
- **How the reader cuts words**: Chinese has no spaces, so the app cuts each run of characters into
  its likeliest words: the cut whose words are the most common in the list (不知道 is 不 + 知道,
  他从中国来 is 他 + 从 + 中国 + 来), and a character no word covers is a word of its own. The checker
  cuts the text the same way, and every count (coverage, length, new words) is of those words. Two
  words next to each other are still read as one where the list has a common word for them: 吃 + 饭
  is read as 吃饭, 你 + 好 as 你好, 很 + 多 as 很多, 三 + 个人 rather than 三个 + 人. That is fine
  where the longer word is right, but a new word must stand as a word of its own at least twice: if
  the checker counts a new word fewer times than you wrote it, a longer word swallowed it; rewrite
  those places or use the word where it stands alone. The checker's "outside" line shows every word
  as it was cut.
- The list comes from web text, so some everyday words rank later than you might expect (家 is 247,
  你好 2282, 吃饭 later than 吃). Check a word's rank in `wordlist.tsv` or the band files rather
  than guess it.
- One simplified character can stand for two traditional ones (发 for 發 "send" and 髮 "hair", 后
  for 後 "after" and 后 "queen", 干 for 幹 and 乾, 面 for 面 and 麵, 里 for 裡 and 里): the list
  counts them together under the commoner one. Teach such a word in that commoner sense.
- Single characters and the words built from them are separate entries: 学 and 学生 and 学习 are
  three words. Teach each in its own sense; a character that is only part of words in modern
  Mandarin (们, 民, 业 in most uses) is not a word to teach on its own, unless the band file's
  word really is used alone (们 after a noun for people, 孩子们, is fine to teach as the plural
  ending, used in several such words).
- The list has the common surnames as words (王, 张, 李, 陈, 刘): a surname is a name, never a word
  to teach, unless it is also a common word used in its own sense (王 "king"). The list has 中国
  but no other country or city names: 美国, 北京, 上海 and the like go in `names`.
- `names`: people (王小明, 李华, 安娜), places, shops and brands, written exactly as in the text.
  A name listed whole is matched whole wherever it is written, so never list a name that contains a
  band word you teach, and never teach a band word only through a name.
- Write numbers as Chinese words where Chinese prose writes them (一个人, 三本书, 两天, 十二点)
  and as digits for prices, years, long numbers and dates (2024年5月3日, 35块钱). A number in
  characters (三, 两, 二十五) counts as known, as digits do, but a number word in your band can
  still be taught. Write months and dates with digits (5月, 3日): 五月 and the like are list words
  of their own and would hide 月.
- Keep only true names in `names`, never common words.
