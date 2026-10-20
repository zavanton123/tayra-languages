# Writing a Japanese frequency course

Each course teaches one band of 100 words from the Japanese word frequency list
(`bands/<rankUpTo>.txt`: index, rank, the word, its forms). Course `ja-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *さくらさんは猫がほしいです。さくらさんは店に行きます。店に猫が三匹います……*
  Everyday life: home, family, food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; say who 彼/彼女 is whenever it could be
  unclear (Japanese usually repeats the name, which is good for learners).
- **Plain narration**: third person, short sentences, one idea per sentence, common word order.
  Little dialogue; when there is some, short and simple. Grammar grows with the bands, each band
  using what came before:
  - ranks 1–500: the polite です/ます style in the narration (dialogue between family and friends may
    use short plain forms, as Japanese families speak: 「食べる？」「うん、食べる。」): AはBです, ます and ません, ました and ませんでした,
    the particles は, が, を, に, で, へ, と, も, の, から, まで, か, ね, よ, ある and いる, い- and
    な-adjectives before nouns and as predicates, この/その/あの, 何, どこ, だれ, いつ, counters with
    numbers (三人, 二つ), たい;
  - ranks 501–1000: the て-form (ています, てください, てから, てもいい), the plain forms in dialogue
    between friends and family, ない and なかった, から and ので for reasons, たことがある, より and
    のほうが for comparisons, と思う, ながら;
  - ranks 1001–1300: the potential (食べられる, 行ける), たら and ば conditionals, つもり, ようにする,
    なければならない, plain-form narration (a story may now be told in the plain style, だ/である
    left for later), relative clauses (昨日買った本);
  - ranks 1301–1600: the passive, the causative, そうだ, らしい, ようだ, はず, basic honorific and humble
    speech in dialogue (いらっしゃる, 申す, おっしゃる, ございます), reported speech with と言っていました;
  - ranks 1601–2000: the causative passive, ばかり, わけ, ところ, ために, のに, ても, かどうか, common
    four-character compounds and set phrases, each clear from its context;
  - ranks 2001–2500: the written plain style (である), connectors such as しかし, したがって, 一方,
    つまり, ところが, それに対して; a story may be told by a first-person narrator (私), or as a diary
    entry, a letter or an e-mail;
  - ranks 2501–3000: a richer, more formal register: によって, に関して, に対して, として, をもとに,
    common proverbs and set phrases explained by their context, full reported speech;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the 随筆 (a light reflection on everyday life), dialogue that marks polite and
    familiar speech;
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
  (*台所で探します。部屋で探します。庭で探します。*).
- **Questions at the end**: after the story, a paragraph starting `質問：` with 4–6 simple questions
  about the story, each followed on its own line by a full-sentence answer that reuses the story's
  words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  質問：
  さくらさんは何がほしいですか。
  さくらさんは猫がほしいです。
  さくらさんはどこに行きますか。
  さくらさんは店に行きます。
  ```

- **Standard Japanese**: neutral, correct and natural, as a native writer writes for adults; no
  slang, no dialect, no internet language. Normal Japanese spelling: kanji where Japanese writes a
  word in kanji (猫, 学校, 食べる), kana where it writes it in kana (ある, いる, ください, ありがとう);
  katakana for loanwords. Do not add furigana, readings, rōmaji or spaces: the app shows readings
  itself. Full-width Japanese punctuation (。、「」？！).
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/ja/<id>.json`:

```json
{
  "id": "ja-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Japanese, e.g. 食べ物と買い物",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, as written in them"],
  "lessons": [
    {
      "title": "Story title in Japanese",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the band file's third column writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by the checker

Run it with the Python that has MeCab:
`/private/tmp/claude-501/-Users-zavanton-TabbyProjects/67b397de-e54a-4339-a2c3-3acd7d846b0d/scratchpad/javenv/bin/python tools/check_course.py tools/courses/ja/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson, as a word the reader shows (see below). The checker also fails a lesson that
  uses a word a later lesson of the course teaches. Reuse earlier lessons' new words too.
- **Length** (story and questions together, in words as the checker cuts them): 80–180 words per
  lesson in the first course, 120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000,
  260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–700 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (see below); the checker's last line lists the band words not taught yet.

## Things to know about the Japanese list and the checker

- The courses are pinned to the list they were written against, `tools/courses/ja/wordlist.tsv`
  (a copy of the app's `ja.tsv`); the bands come from it.
- **How the reader cuts words**: the app cuts Japanese with MeCab's dictionary and joins the pieces
  of an inflected word into one word (食べ・まし・た is 食べました, 住んで・いる is 住んでいる), which
  it looks up under its dictionary form (食べる, 住む). Particles are words of their own (は, を, に,
  で). The checker cuts the text the same way and counts each word under the form the list ranks: a
  verb or adjective under its dictionary form, whatever ending it has. So write verbs in any form the
  story needs; they all count for the verb. Nouns joined by する (勉強する) are two words: the noun
  and する.
- **Band entries that are not words in the reader**: the list was counted from MeCab's pieces, so
  it has entries that the reader never shows on their own: endings and auxiliaries (ます, た, て,
  れ, さ, ば, ちゃう, せる), stems of する and other verbs (し, すれ), and pieces like き, ま, こ.
  They are grammar, not vocabulary: skip them as new words (use them freely in the text, they never
  count as words outside the range). Teach ない as the adjective (お金がない), です after nouns.
- The list comes from web text, so some everyday words rank later than you might expect, and the
  list has some odd entries. Check a word's rank in `wordlist.tsv` or the band files rather than
  guess it. A word may be listed in kana with its kanji spelling among its forms, or the other way
  round: write it as Japanese normally writes it, and put the band file's third column in newWords.
- The list has no names: people (田中, さくら, ケンさん), places other than the commonest (日本 and
  most cities are not in the list), shops and brands go in `names`, written exactly as in the text.
  A name listed whole is matched whole wherever it is written, so never list a name that contains a
  band word you teach, and never teach a band word only through a name. Keep only true names in
  `names`, never common words. さん, ちゃん, 様 and 君 after a name count with the name, so list
  the name alone (田中, not 田中さん).
- Numbers in kanji (三, 二十五) and digits count as known, and so does a number with its counter
  (三人, 五つ, 二時, 2024年, 5月3日); a number word or counter in your band can still be taught
  where it stands alone.
- Skip, as in the other languages' courses, surnames that are only names, vulgar or adult words,
  and political, military or violent words that cannot sit in a calm everyday story.
