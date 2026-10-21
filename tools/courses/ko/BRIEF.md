# Writing a Korean frequency course

Each course teaches one band of 100 words from the Korean word frequency list
(`bands/<rankUpTo>.txt`: index, rank, the word, its forms). Course `ko-mini-0300` teaches ranks
201–300 and may use every word ranked 1–300. Ten lessons, each introducing about ten of the band's
words, so the course covers the band; a learner who finishes it knows the 300 most common words.

## What the lessons are: mini stories

Each lesson is a short, standalone story made for learning, in the spirit of graded "mini
stories": plain, clear and predictable, so the learner's attention goes to the language, not the
plot. A course is ten separate stories on one broad everyday theme (its title), not one serial
story; a lesson never needs another lesson to make sense.

- **One simple situation per story**: a person, what they want or need, a few ordinary steps, a
  simple happy ending. *서연은 초코가 안 보여요. 서연은 방에서 찾아요. 부엌에서 찾아요……*
  Everyday life: home, family, food, shopping, work, school, the city, health, free time, travel.
- **Nothing confusing**: no plot twists, mysteries, surprises, flashbacks, jumps in time, irony,
  jokes that depend on wordplay, unreliable narrators or open endings. Events in the order they
  happen. Few characters (one to three), named clearly; Korean repeats names and drops pronouns,
  which is good for learners (그 and 그녀 are rare in speech: use the name).
- **Plain narration**: third person, short sentences, one idea per sentence, common word order.
  Little dialogue; when there is some, short and simple. Grammar grows with the bands, each band
  using what came before:
  - ranks 1–500: narration in the polite 해요체 (-아요/어요, -았어요/었어요, N이에요/예요, 아니에요);
    dialogue between family members and friends in 반말 (-아/어, -았어, -자, -(으)ㄹ게, -지?), to
    adults and strangers in 해요체. The particles 이/가, 은/는, 을/를, 에, 에서, 도, 의, 와/과, 에게/한테,
    부터, 까지, (으)로, 만; 있다/없다; 안 and -지 않다, 못; -고 싶다 (for someone else -고 싶어 하다);
    이/그/저, 뭐, 누구, 어디, 언제, 왜, 어떻게, 몇; native numbers with counters (세 명, 두 시, 다섯 살).
    수 is rank 23, so 할 수 있어요/없어요 may be used from the first course as a set phrase;
  - ranks 501–1000: -고 (and), -아서/어서 (so, and then), -(으)면, -지만, -(으)러 가다, -아/어 주다,
    -고 있다, -(으)ㄹ 거예요 (future), -(으)ㄹ까요, -(으)ㄹ게요, comparisons with 보다 and 더, 제일;
  - ranks 1001–1300: -는데/-(으)ㄴ데, -(으)ㄹ 수 있다/없다 freely, -아/어야 하다, -아/어도 되다,
    -(으)면 안 되다, relative clauses with -는/-(으)ㄴ/-(으)ㄹ (어제 산 책), -기 때문에, -(으)니까,
    -기 전에, -(으)ㄴ 후에;
  - ranks 1301–1600: honorifics: -(으)시-, 께서, 께, 드시다, 계시다, 주무시다, 말씀; -(으)ㄹ 때, -는
    동안, -게 되다, -아/어 보다, -(으)ㄴ 적이 있다; reported speech with -다고 하다, -냐고 하다;
  - ranks 1601–2000: -는 것 같다, -(으)ㄹ 것 같다, -기로 하다, -도록, -(으)려고, -거든요, -잖아요,
    -네요, -군요; the formal 합니다체 where a speaker would use it (an announcement, a speech, a shop);
  - ranks 2001–2500: the written plain style (한다체: 간다, 먹었다, 이다) may tell a story; connectors
    such as 그러나, 따라서, 한편, 즉, 그런데, 반면에; a story may be told by a first-person narrator (나),
    or as a diary entry, a letter or an e-mail;
  - ranks 2501–3000: a richer, more formal register: -에 대해, -에 의해, -을/를 통해, -(으)로서,
    -에 따르면, -기 위해, common 사자성어 and proverbs explained by their context, full reported speech;
  - ranks 3001–4000: everything before, in a more literary, descriptive style: precise adjectives
    and clear images, the 수필 (a light reflection on everyday life), dialogue that marks polite and
    familiar speech;
  - ranks 4001–5000 (C1): everything before, with more abstract and nuanced content: a short essay
    that weighs two sides, an interview, a profile, a review; precise vocabulary for feelings, ideas
    and processes; long sentences that stay easy to parse; tone and register chosen to suit the text;
  - ranks 5001–6000 (C1): the same, with the more specialised words of professions, sciences and the
    arts, each made clear by its context, and texts that explain a process or compare viewpoints;
  - ranks 6001–8000 (C1): the same, with figurative language and idioms used more freely, words
    chosen for their shades of meaning, and topics that need some background, which the text gives;
  - ranks 8001–10000 (C2): texts close to what native readers meet: literary prose with images and
    rhythm, academic and journalistic registers, Sino-Korean learned expressions explained by their
    context. Still standalone, calm and easy to follow for a reader at the level.
  From rank 2000 a lesson may also be a short informative or opinion text (a column, a letter to a
  newspaper, a short report), as long as it is everyday, calm and easy to follow. Still one clear
  idea at a time: a learner at the level should follow every sentence on first reading.
- **Repetition on purpose**: repeat key words and phrases the way a teacher would — the new words
  three times or more where it reads naturally, the story's key sentence pattern several times
  (*방에서 찾아요. 부엌에서 찾아요. 마당에서 찾아요.*).
- **Questions at the end**: after the story, a paragraph starting `질문:` with 4–6 simple questions
  about the story, each followed on its own line by a full-sentence answer that reuses the story's
  words (after rank 1000 they may also ask why and how, with fuller answers):

  ```
  질문:
  서연은 초코가 보여요?
  아니요, 서연은 초코가 안 보여요.
  초코는 어디 있어요?
  초코는 상자 안에 있어요.
  ```

- **Standard Korean**: neutral, correct and natural, as a native writer writes for adults; no
  slang, no dialect, no internet language. Hangul only (no hanja, no romanisation, no readings).
  Standard spacing (띄어쓰기): particles and the copula join the word before them (학교에서,
  학생이에요), 수, 것, 때, 적 and other bound nouns stand apart (갈 수 있어요, 먹을 때), a native
  number stands apart from its counter (세 명, 두 시), a digit joins it (3개, 5월, 2024년). Use who
  calls whom what as Korean families do: younger siblings call older ones 오빠, 언니, 형, 누나, not
  by name (twins and friends of the same age use names); children say 엄마 and 아빠.
- **Original texts**: write your own stories; never copy or retell existing course material
  (LingQ's or any other).
- Suitable for all ages.

## The file

Write `tools/courses/ko/<id>.json`:

```json
{
  "id": "ko-mini-0300",
  "rankUpTo": 300,
  "level": "A1",
  "title": "The theme, in Korean, e.g. 음식과 장보기",
  "description": "One sentence in English: what the stories are about.",
  "topic": "A short English topic, e.g. Food and shopping",
  "names": ["Names of people, places and brands used in the texts, as written in them, without particles"],
  "lessons": [
    {
      "title": "Story title in Korean",
      "summary": "One line in English on what happens.",
      "newWords": ["the ~10 band words this lesson introduces, as the band file's third column writes them"],
      "text": "The story, then the questions. Paragraphs separated by a blank line (\n\n); each question and each answer on its own line (\n)."
    }
  ]
}
```

Levels: up to rank 300 `A1`, up to 1000 `A2`, up to 2000 `B1`, up to 4000 `B2`, up to 8000 `C1`, up to 10000 `C2`.

## The rules, checked by the checker

Run it with `python3 tools/check_course.py tools/courses/ko/<id>.json` (any Python 3; it reads
`dictionaries/ko-en.sqlite` and imports `tools/build_frequency_list.py`, which need nothing else).

- **Vocabulary**: at least 95% of a lesson's words are ranked within the course's `rankUpTo`
  (93% up to rank 300, 90% for the first course). Names you list in `names` and numbers count as
  known. The checker lists every word outside the range with its rank: replace them, or keep a few
  when the text needs them (they are why the threshold is not 100%).
- **New words**: each is in the course's band, introduced once in the course, and used at least
  twice in its lesson, as a word the reader shows (see below). The checker also fails a lesson that
  uses a word, or a form of a word, that a later lesson of the course teaches. Reuse earlier
  lessons' new words too.
- **Length** (story and questions together, in words as the checker cuts them, particles
  included): 80–180 words per lesson in the first course, 120–240 up to rank 300, 180–320 up to
  600, 220–380 up to 1000, 260–450 up to 2000, 300–520 up to 3000, 320–550 up to 4000, 350–700 after.
- **Coverage of the band**: aim to teach at least 90 of the 100 words. Skip a word that is not
  worth a lesson (see below); the checker's last line lists the band words not taught yet.

## How the checker reads Korean (the reader's cut)

- Only runs of Hangul are words: spaces, punctuation, digits and Latin letters are not. In 3개 the
  word is 개; in TV and KTX there is none.
- Each run is cut as the app's reader cuts it (`KoreanParser`, ported as `korean_pieces` in
  `tools/build_frequency_list.py`): the particles at its end come off as words of their own
  (학교에서 is 학교 + 에서, 집에도 is 집 + 에 + 도, 학생이에요 is 학생 + 이에요), when the word before them
  is one the app knows; of the possible cuts and the word left whole, the commoner by the app's list
  wins, so a word the list has whole stays whole (사과, 정도, 앞으로, 나가) and verbs keep their
  endings (먹었어요). The cut uses the list the app ships (`ko.tsv`), the ranks then come from the
  pinned `wordlist.tsv`.
- Each piece counts at the rank of the list's word that lists it: particles are words of their own
  (을 1, 이 2, 에 4, 가 5, 를 7, 는 8, 은 11, 도 12, 에서 16 …), the copula's forms (예요, 이에요, 이야,
  입니다, 였어요) count for 이다, a verb or adjective in any form the list has counts for it. A verb
  form the list lacks counts for the list verb Yomitan's rules give for it (먹었구나 for 먹다), also
  through the spoken 잖아 endings and without a final 요.
- **Names**: a word that starts with a name you list counts as known, with the particles after it
  counted as words (민수가, 서울역에서), and so does a name called (민수야, 민준아) or with the 이 a
  given name ending in a consonant takes (민준이가, 서연이는). A name of several words is matched
  word by word. List each name once, without particles. A name is matched at the start of a
  word whenever the rest reads as particles, so never pick one that spells a word of the text with
  them (a name 서 would swallow 서로, 수 would swallow 수도), and never one that is a word you teach.
- **Numbers**: a word right after digits counts as known in its first piece (3개, 5월, 2024년에 —
  the 에 still counts); so do a number word and the counter after it (한 잔, 두 시, 세 명, 다섯 살,
  세 사람, 오 분, 십 년). The counters are a fixed set (개, 명, 분, 시, 시간, 살, 번, 마리, 잔, 병,
  권, 장, 대, 층, 원, 년, 월, 일, 주, 달, 가지, 사람 …). A band counter used so does not count as a
  use: teach 명 with 몇 명, 시간 with 시간이 없어요. 몇 is a word, not a number.
- `질문:` is not counted as a word.

## Things to know about the Korean list

- The courses are pinned to the list they were written against, `tools/courses/ko/wordlist.tsv`
  (a copy of the app's `ko.tsv`); the bands come from it.
- The list was counted from film subtitles and a 2019 news corpus, so news words rank high
  (대통령 73, 밝히다 82, 지난 80, 등 32; 검찰, 의원, 장관, 혐의, 수사 in the first 300). Place the
  usable ones in plain everyday settings (a child who wants to be president, a news item someone
  reads aloud, a notice); skip crime, court and political words that cannot sit in a calm story.
- **Band entries that are not worth a lesson**: 고 (a quotative and connective piece the reader
  shows alone), and at high ranks pieces of longer words. Particles are words in the reader: teach
  them like any word (use each twice in its lesson). 요 is the 요 after a particle (나도요, 저도요,
  집에서요).
- **One spelling, one headword**: 보다 "see" and 보다 "than" are one entry; 하고 "with" counts for
  하다; 네 is "yes" and "your"; 이 is the subject particle and "this"; 잘 is "well" and a form of
  자다 (잘 시간): use each new word in the sense of its headword, never only through a homograph.
- **The names**: people's names and brands are not in the list; 미국, 중국 and 일본 are not either:
  list them in `names`. 한국, 서울, 부산, 대구 and 홍콩 are list words: never list them as names.
  Check any place in `wordlist.tsv` before listing it.
- **Cuts to write around** (the reader reads these differently from what you mean):
  - 오와, 오도, 오나 are read as forms of 오다; 자는, 자라도 as forms of 자다 (not 자 + 는);
  - noun + particle words the list keeps as other nouns: 사도 (사 + 도), 주가 (주 + 가), 분만
    (오 분만 is "childbirth": write 5분만 or 오 분 동안), 그날 is one word (rank 1627);
  - 마십시오 is read as 말다 and 마다 (write 마시세요); 나와요 is cut 나 + 와 + 요 (write 나왔어요,
    나가요 or another verb); 라면 is read as the copula's 라면 (avoid the noodles); a lone 가 (어디 가?)
    and 와 (이리 와) are read as the particles;
  - 오월 and other months in Hangul are not in the list: write 5월;
  - 드세요 counts for 들다, honorific forms in general for the plain verb's entry where the list has
    them.
- Third person: Korean says what someone else wants or likes with -고 싶어 하다 and 좋아하다
  (서연은 공을 좋아해요), not 싶어요 or 좋아요; the questions follow the same rule.
- Skip, as in the other languages' courses, surnames that are only names, vulgar or adult words,
  and political, military or violent words that cannot sit in a calm everyday story.
