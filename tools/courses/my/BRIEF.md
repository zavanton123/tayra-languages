# Writing a Burmese frequency course

Each course teaches one band of 100 words from the Burmese word frequency list (`bands/<rankUpTo>.txt`: index,
rank, word, its forms). Course `my-mini-0300` teaches ranks 201–300 and may use every word ranked 1–300. Ten
lessons, each introducing about ten of the band's words, so the course covers the band; a learner who finishes
it knows the 300 most common words. `themes.tsv` gives each course its theme. The list has 7,183 words: the
courses are `my-mini-0100` to `my-mini-7200` (the last band has 83 words).

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini stories": plain,
clear and predictable, so the learner's attention goes to the language, not the plot. A course is ten separate
stories on one broad everyday theme (its title), not one serial story; a lesson never needs another lesson to
make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a simple happy
  ending. Everyday life where Burmese is spoken: Yangon, Mandalay, Bagan, Naypyidaw, Taunggyi and Inle Lake,
  Mawlamyine, Pathein, Pyay, Hpa-An, Sittwe, Myitkyina, Ngapali, the Ayeyarwady, the delta and the dry zone, the
  villages and their monasteries; now and then Bangkok, Singapore, Kuala Lumpur, Tokyo or London, where Burmese
  people live and work: home, family, food, the market, the tea shop, work, school, the town, health, free time,
  travel, the festivals.
- **Nothing confusing**: no plot twists, mysteries, flashbacks, jumps in time, irony, open endings. Events in
  the order they happen. Few characters (one to three), named clearly; give each story its own names.
- **Plain narration**: third person, short sentences, the ordinary order (subject, object, verb, then the
  particles). Burmese drops subjects freely; before a pronoun is taught the narration repeats the name.
- **Grammar grows with the bands**, each band using what came before. Burmese words do not inflect: what grows is
  the particles and the constructions.
  The pronouns and kinship words rank as follows: *သူ* 18, *ငါ* 21, *မင်း* 23, *ရှင်* 79, *ကျွန်တော်* 100, *ကျွန်မ*
  105, *ခင်ဗျား* 142, *နင်* 164, *ကလေး* 167, *အဖေ* 193, *အမေ* 204; *ငါတို့* 77, *သူတို့* 86 and *ကျွန်တော်တို့* 289
  are one word each; the politeness words *ဟုတ်ကဲ့* 316, *ကျေးဇူးတင်* 424, *မင်္ဂလာပါ* 1140. Use them where a story
  needs them, as outside words, until their band. A pronoun in the creaky tone (*သူ့*, *ငါ့*, *ကျွန်တော့်*) is a form
  of its word, and so is a noun's (*အမေ့*, *ကမ္ဘာ့*).
  - ranks 1–300: the sentence endings *တယ်* (what is or was), *မယ်* (what will be), *ပြီ* (what has come to be),
    *နေ...တယ်* (what is going on), *ခဲ့* (what happened); negation *မ ... ဘူး*; questions with *လား* (yes/no) and
    *လဲ* with the question words (*ဘာ*, *ဘယ်*, *ဘယ်သူ*); the noun particles *က* (who does it), *ကို* (whom, where
    to), *မှာ* (where), *ရဲ့* ('s), *နဲ့* (with, and), *တွေ* (plural); polite *ပါ* (*သွားပါတယ်*, *ထိုင်ပါ*); numbers
    with a classifier after the noun (*ကလေး နှစ်ယောက်*, *ခွေး တစ်ကောင်*); *ရှိ* "there is, have"; *ဖြစ်* "be".
  - ranks 301–600: the verb helpers *ချင်* (want to), *နိုင်* (can), *ရ* (get to, have to), *ပေး* (for someone),
    *လိုက်*, *ကြည့်* (try); requests *...ပါ*, *...ပေးပါ*, *မ...ပါနဲ့*; *ကြ* for several doers; comparisons
    *X ထက် ပို...*; *အ...ဆုံး* "the most"; *ပြီး* "and then"; *ရင်* "if, when".
  - ranks 601–1000: relative clauses with *တဲ့* (*ငါ ဝယ်တဲ့ စာအုပ်*); nouns made with *တာ* and *အ-*; reasons
    with *လို့* and *ကြောင့်*; *တဲ့အခါ* "when"; reported speech with *လို့ ပြော*; *ဖို့* "in order to".
  - ranks 1001–2000: longer, joined sentences; *ပေမဲ့* "but", *သလို* "as", *နေတုန်း* "while"; a first-person
    narrator, a letter, a diary; the written (literary) style of a story's narration where the text type asks for
    it, with dialogue in the spoken style, as Burmese fiction writes it.
  - ranks 2001–4000: the written style of notices, news and informative texts (*သည်*, *၏*, *၌*, *မည်*, *၍*, *၎င်း*,
    *သော*); short informative or opinion texts.
  - ranks 4001–7200 (C1): everything before, with more abstract and nuanced content: a short essay or column, an
    interview, a portrait, a review; precise vocabulary, each word made clear by its context.
- **Spoken and written Burmese**: Burmese has two registers with their own particles: the spoken (*တယ်*, *မယ်*,
  *ရဲ့*, *မှာ*, *နဲ့*, *တဲ့*) and the written (*သည်*, *မည်*, *၏*, *၌*, *နှင့်*, *သော*). The list has both near the
  top. Write a lesson in one register: the spoken one by default, the written one for a written text (a letter, a
  notice, a page of a storybook, a news item) or, from B1, for narration with spoken dialogue in quotes. Never mix
  the two in one sentence. A band word of the written style is taught in a lesson written in that style, even in
  the first courses. The questions at the end are always in the spoken style.
- **Repetition on purpose**: repeat the new words three times or more where it reads naturally, and the
  story's key sentence pattern several times.
- **Questions at the end**: after the story, a paragraph starting `မေးခွန်းများ:` with 3–6 simple questions about
  the story, each followed on its own line by a full-sentence answer that reuses the story's words:

  ```
  မေးခွန်းများ:
  မောင်မောင် ဘာ ဝယ်ချင်လဲ။
  မောင်မောင် လက်ဖက်ရည် ဝယ်ချင်တယ်။
  ```

  The header `မေးခွန်းများ:` is layout: the checker does not count it as a word.
- **Standard Burmese**: correct, neutral and natural standard Burmese as an educated Yangon or Mandalay speaker
  writes it; dialogue in what any careful speaker says, no slang. Spell as the dictionaries do, in Unicode.
- **Burmese typography**: no spaces inside a word; a space between phrases where a Burmese writer pauses (after
  the subject and its particle, between clauses), never inside a word. *။* ends a sentence and a question
  (a question may end in *?* only in quoted dialogue), *၊* separates clauses and list items, direct speech goes in
  « » or " ". Numbers in Burmese digits (*၁*, *၂၀*), which the reader shows as no words. Unicode only: never
  Zawgyi (the old font encoding, ေ typed before its letter), and never ၀ (zero) for ဝ (wa) or ၇ (seven) for ရ.
- **Names are names**: place names, peoples and nationalities count as names; a band word that is a name is
  skipped. Days of the week, months and language names are everyday words: teach them. Burmese given names are
  made of ordinary words (*အောင်* "success", *ကျော်* "famous", *မြ* "emerald", *ဖြူ* "white"), so a name is matched
  whole: list every name in `names` exactly as written, with its title (*ဦး*, *ဒေါ်*, *ကို*, *မ*, *မောင်*) outside
  it when the title is a list word (*ကို* is). Never name a character with a single list word (*အောင်*, *ဖြူ*,
  *မြ*, *လှ* alone): names of two or three syllables (*ဇော်ဝင်း*, *သီတာ*, *ကျော်ဇင်*, *နီလာ*, *မြင့်မြင့်*,
  *ဟိန်းထက်*, *ဝေဝေ*, *စန္ဒာ*, *မင်းခန့်*, *သူဇာ*, *ချိုချို*, *ရွှေရည်*, *ကောင်းထက်*, *ထက်ထက်*, *နန္ဒာ*, *သိန်းဟန်*)
  are matched whole.
- **Original texts**: write your own stories; never copy or retell existing course material.

## Content

WRITER-BRIEF step 4 (2026-11-13): no topic rules. Monks and monasteries, pagodas, Thingyan and Thadingyut, the
kings of Bagan, history, love, alcohol, crime stories and the rest are all fine. Avoid only explicit sexual
content, slurs used as insults and gratuitous gore. Teach ordinary words in their main sense, as the my-en
dictionary gives it.

## The file

Write `tools/courses/my/<id>.json`:

```json
{
  "id": "my-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Burmese, as themes.tsv gives it",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and pets used in the texts"],
  "lessons": [
    {
      "title": "Story title in Burmese",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the list writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, after `C1`.

## The rules, checked by `python3 tools/check_course.py tools/courses/my/<id>.json`

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo` (93% up to rank 300,
  90% for the first course). Names in `names` count as known; digits are not counted.
- **New words**: each is in the course's band, introduced once in the course, and used at least twice in its
  lesson as the reader cuts the text (see below).
- **No new word before its lesson**: a lesson must not use a band word that a later lesson of the course teaches.
- **Length** (story and questions together, in the reader's words): 80–180 words per lesson in the first course,
  120–240 up to rank 300, 180–320 up to 600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000,
  320–550 up to 4000, 350–600 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words; the checker's last line lists the band
  words not taught yet.

## How the reader, and so the checker, cuts Burmese

Burmese is written without spaces between words, so the app cuts each stretch of letters itself: into its
likeliest words, by how common the list says they are, among the words of the list, the app's own Burmese word
list (ICU's, `files/lexicon/my.txt`) and the my-en dictionary pack (a word the list lacks counts as rare), never
inside a syllable (not before a vowel sign, medial, tone mark or asat, nor after a virama). Letters no word covers
stay one unknown word. The text is read Unicode-normalised (*့* before *်*; ၀ between letters as ဝ). The checker
cuts exactly as the reader. It follows that:

- **two words the dictionary has together are one word**, counted at the pair's own rank, and not as a use of
  either word: *ငါတို့* 77, *သူတို့* 86, *နေရာ* 108, *ဘယ်လို* 136, *ဘယ်သူ* 151, *ဒီမှာ* 160, *ကလေး* 167, *ဘာလို့*
  175, *ပြီးတော့* 179, *ဒါပေမဲ့* 182, *ဟုတ်လား* 197, *ဒီလို* 206, *ကနေ* 215, *သူများ* 245, *ဒါဆို* 325, *နောက်ဆုံး*
  351, *လိုချင်* 429, *ဒီနေ့* 465. Write around a pair when you need its words, or put a space between them where a
  pause reads naturally.
- **but a verb and the particles and helper verbs after it are its words**: *ပြန်လာတယ်* is *ပြန်* + *လာ* + *တယ်*,
  *ရပါတယ်* *ရ* + *ပါ* + *တယ်*, *လုပ်ပေး* *လုပ်* + *ပေး*; and common words are not swallowed by rarer ones.
- **two nouns ICU's list joins that the dictionary lacks are those nouns**: *ခရုခွံ* is *ခရု* + *ခွံ*, *ခြင်ဆေး*
  *ခြင်* + *ဆေး*, and so are *အဖျားတက်* (*အဖျား* + *တက်*) and *လျှပ်ပြက်* (*လျှပ်* + *ပြက်*). A pair the dictionary
  has stays one word (*ဆန်မှုန့်* "rice flour" is no use of *မှုန့်*).
- **a name must start and end where the reader's words do**, or it runs into the words around it and the checker
  fails the lesson; list every name in `names`.
