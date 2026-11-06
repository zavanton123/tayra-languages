# Cantonese mini-story courses: adapted from the Mandarin courses

Each Cantonese course is an adaptation of the Mandarin course with the same number:
`tools/courses/zh/zh-mini-NNNN.json` becomes `tools/courses/yue/yue-mini-NNNN.json`. Keep the
stories, their order and their people. Don't invent new plots; the work is making each lesson read
as natural Cantonese.

## Language
- Colloquial written Cantonese as used in Hong Kong: 係, 唔, 佢, 佢哋, 嘅, 咗, 緊, 喺, 冇, 嘢, 睇, 食,
  飲, 諗, 講, 話, 畀, 嚟, 咁, 呢, 嗰, 乜嘢, 點解, 邊度, 幾時, 啲, 晒, and 畀 (never 俾),
  mm4 written 唔, and particles (呀, 喎, 啦, 㗎, 咩, 囉) where a speaker would really use them.
- Rewrite the grammar, not just the words: 我去咗學校, 佢高過我, 你食咗飯未呀？, 畀本書我, 食飯先.
  A word-for-word swap of Mandarin characters is not acceptable.
- Traditional characters in the Hong Kong standard: 裡, 着 as 著, 為, 衛. No simplified characters.
- One spelling per word across all courses: 嘅, 咗, 喺, 哋, 嗰, 啲, 咁, 冇, 畀, 嘢, 唔, 佢.
- Never write deliberately wrong language.

## Grading
Keep the difficulty of the Mandarin course: same level, about the same length, sentences as simple
or as rich as the source. High-band formal topics may lean more formal, as a Hong Kong speaker
would when explaining such a topic, but stay Cantonese (佢, 係, 嘅).

## File
Same JSON shape as the source: `id` yue-mini-NNNN, `rankUpTo`, `level`, `topic` unchanged; `title`,
`description`, `names` and each lesson's `title` and `text` in Cantonese. Keep `summary` in the
language the source uses. The question block keeps its place, with the header 問題： and questions
and answers in Cantonese.

`newWords` per lesson: the Cantonese word that the story actually uses for each word the Mandarin
lesson taught (所以 stays 所以; 他 becomes 佢; 没有 becomes 冇). Every word in `newWords` must appear
in that lesson's text. Don't teach a word again if an earlier yue course already teaches it: drop
it and go on. Fewer words than the source is normal. Don't list names.

## Content rules
As in /Users/zavanton/TabbyProjects/tayra-writers/WRITER-BRIEF.md step 4; if a Cantonese word
brings a vulgar sense into the text, choose another. Cantonese has several common words that are
vulgar in one tone or sense: when in doubt, use a plainer word.
