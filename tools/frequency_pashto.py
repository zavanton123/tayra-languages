"""
Pashto for tools/build_dictionary.py, tools/build_frequency_list.py and tools/check_course.py.

The reader (PashtoParser) matches a word in Pashto letters without marks, its spellings of one letter folded together, as texts
write them in Afghanistan and Pakistan: ك as ک, گ as ګ, ۀ, ە, ہ and ھ as ه, Urdu's ٹ, ڈ and ڑ as ټ, ډ and ړ, and the yes ی,
ى, ې, ے, ۍ, ئ and ۓ all as ي (چې, چي and چی are one word, as are ډېر and ډير, کېږي and کيږي, سړی and سړي). The other
letters Pashto has of its own stay apart (ښ, ږ, ځ, څ, ڼ beside ش, ژ, ز, ص, ن). The vowel signs, tatweel and the
non-joiner go. Keep pashto_fold and PashtoParser.fold the same.

The dictionary pack and the list keep each word as it is spelled (سړی, ډېر); only its matching (word_lc, forms) is folded.
"""
import re
import unicodedata

ZWNJ = "\u200c"
# A long vowel or hamza seat written as a letter and a combining sign.
_COMPOSED = (
    ("\u0627\u0653", "\u0622"), ("\u0627\u0654", "\u0623"), ("\u0627\u0655", "\u0625"), ("\u0648\u0654", "\u0624"),
)
_LETTERS = str.maketrans({
    "\u0643": "\u06a9",  # ك  ک
    "\u06af": "\u06ab",  # گ  ګ
    "\u06c0": "\u0647", "\u06d5": "\u0647", "\u06c1": "\u0647", "\u06be": "\u0647",  # ۀ ە ہ ھ  ه
    "\u0679": "\u067c", "\u0688": "\u0689", "\u0691": "\u0693",  # ٹ ڈ ڑ  ټ ډ ړ
    # ی ى ې ے ۍ ئ ۓ  ي
    "\u06cc": "\u064a", "\u0649": "\u064a", "\u06d0": "\u064a", "\u06d2": "\u064a", "\u06cd": "\u064a",
    "\u0626": "\u064a", "\u06d3": "\u064a",
})
# The vowel signs, shadda, sukun, the hamza and madda signs, superscript alef, tatweel and the non-joiner.
_MARKS = re.compile("[\u064b-\u065f\u0670\u0640\u200c]")
# The letters of a Pashto word as the reader reads it (PashtoParser's word characters, folded).
LETTERS = re.compile("[\u0621-\u063a\u0641-\u064a\u0679-\u06d3]+")


def pashto_fold(word):
    """A word as the reader matches it (PashtoParser.fold)."""
    for spread, composed in _COMPOSED:
        word = word.replace(spread, composed)
    return _MARKS.sub("", word).translate(_LETTERS)


# A table cell of the English Wiktionary: the word, then its romanisation and notes ("کوم (kawә́m)"), some letters in their
# presentation forms (کوﺉ).
_RUN = re.compile("[\u0600-\u06ff\u200c]+")
# The vowel signs and tatweel a pack spelling leaves out, as texts do.
_SPELLING_MARKS = re.compile("[\u064b-\u065f\u0670\u0640]")


def clean_pashto(word):
    """A pack spelling: the Pashto word a cell or headword starts with, without vowel signs, ك as ک and ۀ as ه."""
    word = unicodedata.normalize("NFKC", word.strip())
    if re.search(r"[A-Za-z(/,]", word) or "\u0259" in word or "\u04d9" in word:
        run = _RUN.match(word)
        word = run.group(0) if run else ""
    for spread, composed in _COMPOSED:
        word = word.replace(spread, composed)
    return _SPELLING_MARKS.sub("", word).replace("\u0643", "\u06a9").replace("\u06c0", "\u0647").strip(ZWNJ)


def spelling_rank(word):
    """
    How Afghan a spelling of a folded word is, to show the commonest one: the more of Pashto's own yes it writes (ې, ۍ, ی, ئ
    for the ي of Pakistani and older spellings) and of its own letters (ګ for گ), the more; of two alike, the pack's first.
    """
    return sum(c in "\u06d0\u06cd\u06cc\u0626" for c in word), sum(c in "\u067c\u0681\u0685\u0689\u0693\u0696\u069a\u06ab\u06bc" for c in word)


# A word as the reader cuts it: a run of its word characters for Pashto (AddedLanguages), the non-joiner and marks inside.
READER_WORD = re.compile("[\u200c\u0600-\u0608\u0610-\u061a\u0620-\u065f\u066e-\u06d3\u06d5-\u06ef\u06fa-\u06ff"
                         "\ufb50-\ufdff\ufe70-\ufefc]+")
# Leipzig's Pashto corpora hold Dari and Persian sentences too (its 2011 news crawl a quarter, and some Bosnian). A Pashto sentence
# has one of Pashto's own letters, or one of these words, which Persian has not.
_OWN_LETTERS = re.compile("[\u067c\u0681\u0685\u0689\u0693\u0696\u069a\u06ab\u06bc\u06cd\u06d0]")
_OWN_WORDS = {"د", "په", "کي", "چي", "يي", "دی", "ده", "دي", "ته", "لري", "شوی", "وو", "يو", "دا", "هم", "سره", "پر"}


# But the Dari sentences of Afghan sites are often typed with ګ and ې too (ديګر, ګرديده, بشناسېد), and have هم: a sentence with
# two of Persian's own words (است, از, این; not را, which Pashto writes too) and more of them than of Pashto's, or one and none
# of Pashto's, is Dari.
_PERSIAN_WORDS = {pashto_fold(w) for w in (
    "است این از با برای بود شده کرد میشود های در خود شد کند دیگر دارد نیز اند یک هست هستند کرده میکند باشد بودند کردند "
    "شود").split()}
_PASHTO_WORDS = {pashto_fold(w) for w in (
    "د په کې چې ته دی ده دي وو شو شوه شول شوي شوی لري سره څخه یې دا هغه دغه پر له لپاره وي کړي کړه کول وکړ وکړه وکړل "
    "ورته دې نو خو زه موږ مونږ تاسو کوي کېږي شي باندې کښې کوم ټول").split()}


def is_dari(words):
    """Whether the folded [words] of a sentence are Dari's rather than Pashto's."""
    persian = sum(w in _PERSIAN_WORDS for w in words)
    pashto = sum(w in _PASHTO_WORDS for w in words)
    return persian >= 2 and persian > pashto or persian >= 1 and not pashto


def is_pashto(sentence):
    if is_dari([pashto_fold(w) for w in READER_WORD.findall(sentence)]):
        return False
    if _OWN_LETTERS.search(sentence):
        return True
    return any(pashto_fold(w) in _OWN_WORDS for w in READER_WORD.findall(sentence))


# The Pashto sentences' words of each corpus counted (sentence_counts), for fold.
SENTENCES = {}


def sentence_counts(path):
    """A Leipzig corpus's word counts over its Pashto sentences, the words cut and folded as the reader cuts and matches them."""
    import io
    import tarfile
    from collections import Counter
    counts = Counter()
    sentences = SENTENCES[path] = []
    with tarfile.open(path) as tar:
        member = next(m for m in tar.getmembers() if m.name.endswith("-sentences.txt"))
        for line in io.TextIOWrapper(tar.extractfile(member), encoding="utf-8"):
            sentence = line.rstrip("\n").partition("\t")[2]
            if is_pashto(sentence):
                words = tuple(w for w in map(pashto_fold, READER_WORD.findall(sentence)) if LETTERS.fullmatch(w))
                sentences.append(words)
                counts.update(words)
    return counts


# Common words the English Wiktionary has no Pashto entry for (له, هم, خپل, سهار, ليکل), or has only in a rare sense (سر
# "secret", not "head"; کس, تنه, باره, مالي): word | part of speech | English gloss | forms no rule makes. tools/build_dictionary.py
# adds them to the pack as entries of their own, glossed so.
_HAND = """
له|prep|from; by; with (له ... سره)
هم|adv|also, too; even
تر|prep|until, up to; than; under (تر ... لاندې)
څخه|postp|from (له ... څخه)
خو|conj|but; though
خپل|adj|own, one's own|خپله خپلې خپلو
باید|adv|must, should
اړه|noun|relation (په اړه: about)
ورته|adj|similar, alike
توګه|noun|manner, way (په ... توګه)
خوا|noun|side, direction (له خوا: by)|خواوې خواوو
لکه|prep|like, as
نن|adv|today
ما|pron|me (oblique of زه)
هېڅ|det|no, none; nothing
همدا|pron|this very, this same
اوسه|noun|now (تر اوسه: until now)
امله|noun|reason (له امله: because of)
حال|noun|state, condition
مهال|noun|time, period
ترسره|adv|accomplished (ترسره کول: to carry out)
تړاو|noun|connection (په تړاو: concerning)
کولای|adv|able (کولای شي: can)
پیدا|adj|found; born (پیدا کول: to find)
چا|pron|whom (oblique of څوک)
شته|verb|there is, exists
نشته|verb|there is not
مشر|noun|leader, elder; head|مشران مشرانو مشري
ترمنځ|postp|between
ضد|noun|opposite; against (د ... ضد)
مخکې|adv|before, earlier; in front
پاتې|adj|remaining, left
مخنیوی|noun|prevention
یعني|conj|that is, namely
آیا|particle|question particle (whether)
حتی|adv|even
خورا|adv|very, most
پورته|adv|up, above
بلکې|conj|but rather
سربېره|prep|besides, in addition to
کوم|det|which; some|کومه کومې کومو
نیم|adj|half
چمتو|adj|ready, prepared
خوشې|adj|released, free (خوشې کول: to release)
تازه|adj|fresh, new
روان|adj|current, ongoing; flowing|روانه روانې روانو
دوام|noun|continuation
ثبت|noun|registration, record
ملاتړ|noun|support
حل|noun|solution
پام|noun|attention, care
کچه|noun|level, rate
حق|noun|right; truth
اړتیا|noun|need
مخه|noun|front, way (مخه نیول: to stop)
وروستی|adj|last, latest, recent
اړوند|adj|related, concerned
راتلونکی|adj|coming, next, future
کم|adj|little, few, less
توکی|noun|item, article (pl. توکي: goods)
وضعیت|noun|situation
ولاړ|adj|standing|ولاړه ولاړې ولاړو
جدي|adj|serious
عملي|adj|practical
مقام|noun|position, authority; official
مبارزه|noun|fight, struggle, campaign
پوره|adj|full, complete, enough
عام|adj|public, general, common|عامه عامې عامو
ټینګار|noun|emphasis, insistence
هکله|noun|matter (په دې هکله: about this)
حواله|noun|reference (په حواله: citing)
اساس|noun|basis (پر اساس: according to)
مخامخ|adv|facing, face to face
مهم|adj|important
ترڅ|noun|course (په ترڅ کې: during)
بېلابېل|adj|various, different
سرچینه|noun|source
اندازه|noun|amount, size, measure
پلان|noun|plan
ټپي|adj|wounded|ټپیان ټپیانو
همدارنګه|adv|likewise, also
پوځي|adj|military
څېر|noun|likeness (په څېر: like)
اصلي|adj|main, original, real
نېټه|noun|date
ملکي|adj|civilian
تړون|noun|agreement, treaty
بنسټ|noun|foundation, basis
اساسي|adj|basic, fundamental
غیر|adj|non-, other than
چاپ|noun|print, printing
مشري|noun|leadership
بهیر|noun|process, course
ناسته|noun|sitting, meeting
نظام|noun|system, order; regime
اوسنی|adj|present, current
سلنه|noun|percent
لیک|noun|letter
عمومي|adj|general
لامل|noun|cause, reason
فساد|noun|corruption
عمل|noun|action, deed
تن|noun|person (counting people)|تنه تنو
خاطر|noun|sake (د ... له خاطره: for the sake of)|خاطره
لومړنی|adj|primary, first, initial
کرنه|noun|agriculture, farming
ټاکل|verb|to appoint, to choose, to set|ټاکي وټاکي ټاکم ټاکو وټاکل ټاکلی ټاکلي ټاکلو ټاکنه ټاکنې ټاکنو
بدلون|noun|change
پلاوی|noun|delegation
موجود|adj|present, existing
عملیات|noun|operations|عملیاتو
بحث|noun|discussion, debate
پراختیا|noun|expansion, development
قومي|adj|tribal, national
موده|noun|period, length of time
قوماندان|noun|commander
دیني|adj|religious
ادبي|adj|literary
پخوا|adv|before, formerly, earlier
پلی|adj|on foot; carried out (پلی کول: to implement)
بدل|adj|changed (بدل کول: to change)
اوربند|noun|ceasefire
مشهور|adj|famous
ځایي|adj|local
خوراکي|adj|food (adj.), edible
مواد|noun|materials, substances|موادو
تولید|noun|production
قانوني|adj|legal, lawful
مانا|noun|meaning
سهار|noun|morning
سیستم|noun|system
بریالی|adj|successful
نظامي|adj|military
همداسې|adv|just so, likewise
کلن|adj|years old (لس کلن: ten years old)
خلاف|noun|opposite; against (د ... خلاف)
لرونکی|adj|having, owning
بشپړ|adj|complete, whole
غبرګون|noun|reaction, response
تراوسه|adv|so far, until now
زیان|noun|harm, damage, loss
تګ|noun|going
یادونه|noun|mention; note
مثبت|adj|positive
راتګ|noun|coming, arrival
سرتېری|noun|soldier
فلم|noun|film
ګواښ|noun|threat
پخپله|adv|oneself, by oneself
استفاده|noun|use
لړۍ|noun|series, chain
لیدنه|noun|visit, meeting
زمینه|noun|ground, background; opportunity
مراسم|noun|ceremony|مراسمو
باب|noun|chapter; matter
فشار|noun|pressure
جمهوري|adj|republican (of a republic)
پوهاوی|noun|awareness
وسیله|noun|means, tool
ترتیب|noun|order, arrangement
مختلف|adj|different, various
تطبیق|noun|implementation
انتقال|noun|transfer
اقدام|noun|action, step
کمېټه|noun|committee
هماغه|pron|that same, that very
ټولګی|noun|class, classroom
هټۍ|noun|shop
ستړی|adj|tired
خوشحاله|adj|happy, glad
خفه|adj|sad, upset; annoyed
ناروغ|adj|ill, sick|ناروغه ناروغې ناروغو ناروغان ناروغانو
روغ|adj|healthy, well|روغه روغې روغو
یخ|adj|cold|یخه یخې یخو
سبزي|noun|vegetables, greens
جامې|noun|clothes|جامو
کمیس|noun|shirt (kameez)
پیزار|noun|shoe, shoes
ریل|noun|train
پخلنځی|noun|kitchen
کټ|noun|bed (charpoy)
بستره|noun|bed, bedding
کمپله|noun|blanket
میره|noun|husband
ناست|adj|sitting, seated|ناسته ناستې ناستو
ویده|adj|asleep
هېڅکله|adv|never
خرڅ|adj|spent, sold (خرڅ کول: to spend, to sell)
خبر|adj|aware, informed (خبر کول: to inform)
سر|noun|head; top, end|سرونه سرونو
کس|noun|person (counting people)|کسان کسانو
باره|noun|matter (په باره کې: about)
مالي|adj|financial
خالي|adj|empty
کافي|adj|enough, sufficient
ګروپ|noun|group
بوره|noun|sugar
لا|adv|still, yet
دین|noun|religion
پوسته|noun|post, checkpost
مړ|adj|dead|مړه مړې مړو
وړاندې|adv|forward, ahead; before (له ... وړاندې)
نوموړی|adj|the said, the aforementioned|نوموړې نوموړي نوموړو
امنیت|noun|security
امنیتي|adj|security (adj.)
چاره|noun|affair, matter (pl. چارې: affairs)|چارې چارو
یوازې|adv|only, alone|یواځې
موخه|noun|aim, purpose
بهرنی|adj|foreign, outside
ځانګړی|adj|special, particular
کبله|noun|reason (له کبله: because of)
وینا|noun|speech
ریاست|noun|directorate, presidency
ورسره|adv|with him, with her, with them
چارواکی|noun|official, authority
متحده|adj|united
رامنځته|adv|into being (رامنځته کول: to create)
تګلاره|noun|policy, course
نامه|noun|name (په نامه: named)
لخوا|postp|by (د ... لخوا)
ترلاسه|adv|in hand (ترلاسه کول: to obtain)
مرستیال|noun|deputy, assistant
وای|particle|would (marks the conditional)
ترڅنګ|postp|beside, alongside; besides
روغتیایي|adj|health (adj.)
تاوتریخوالی|noun|violence
ولایتي|adj|provincial
کمېسیون|noun|commission
شامل|adj|included
کوونکی|noun|doer, one who does (زده کوونکی: student)
ټولنیز|adj|social
پروسه|noun|process
پاملرنه|noun|attention, care
بهر|adv|outside, abroad
چاپېریال|noun|environment, surroundings
لګښت|noun|cost, expense
پیاوړی|adj|strong, powerful
ګډ|adj|joint, common, mixed
اختر|noun|Eid, festival
تایید|noun|confirmation, approval
چاودنه|noun|explosion
شخصي|adj|personal, private
مذهبي|adj|religious
لیکنه|noun|writing, article
خوندي|adj|safe, protected
دنیا|noun|world
مذاکرات|noun|negotiations|مذاکراتو
پټ|adj|hidden, secret
خبرداری|noun|warning
معلوم|adj|known, clear
دمخه|adv|before, beforehand
تورن|adj|accused
مقابل|adj|opposite, facing
همغږي|noun|coordination
مدني|adj|civil
قوي|adj|strong
تمه|noun|expectation, hope
پرمهال|postp|during, at the time of
عادي|adj|ordinary, normal
دغسې|adj|such, like this
شرایط|noun|conditions|شرایطو
بیان|noun|statement, expression
همدغه|pron|this very, this same
ژوبله|noun|injury, casualties
توپیر|noun|difference
منفي|adj|negative
امکان|noun|possibility
کمزوری|adj|weak
دننه|adv|inside
کمښت|noun|shortage, decrease
ورڅخه|adv|from him, from her, from them
رهبري|noun|leadership
اطلاعات|noun|information, intelligence|اطلاعاتو
زیاتره|adv|mostly, most
همداراز|adv|likewise, also
مدیریت|noun|management
داخلي|adj|internal, domestic
واکمني|noun|rule, reign
ادامه|noun|continuation
اړ|adj|compelled, in need (اړ کېدل: to have to)
ناوړه|adj|bad, harmful
میلادي|adj|AD, of the Christian era
وېش|noun|division, distribution
وړتیا|noun|ability, capacity
پلوه|noun|side, respect (له دې پلوه: in this respect)
مړینه|noun|death
وده|noun|growth, development
خلاص|adj|open; free, finished
بندیز|noun|ban, sanction
معرفي|noun|introduction
ممکن|adj|possible
قوم|noun|tribe, people
روزنه|noun|training, upbringing
اندېښنه|noun|worry, concern
غوندې|postp|like, as
خوښ|adj|pleased, liked, chosen
غوښتونکی|noun|seeker, one who demands
مطلب|noun|point, meaning, aim
اغېز|noun|effect, influence
مبارکي|noun|congratulations
کنټرول|noun|control
ثبات|noun|stability
یوځای|adv|together
ماتې|noun|defeat
هرڅه|pron|everything
کیسه|noun|story
هرکلی|noun|welcome
لارښوونه|noun|guidance, instruction
ماموریت|noun|mission
البته|adv|of course
مسوول|adj|responsible; official
ورباندې|adv|on it, on him, on them
انرژي|noun|energy
جوړښت|noun|structure
کمپاین|noun|campaign
واسطه|noun|means (په واسطه: by means of)
ونډه|noun|share, contribution
ورځنی|adj|daily
درناوی|noun|respect
خراب|adj|bad; broken, spoiled
آزاد|adj|free|ازاد
غور|noun|consideration, attention
حضور|noun|presence
حالات|noun|conditions, circumstances|حالاتو
هسې|adv|just, for nothing, in vain
ولسوال|noun|district governor
توان|noun|ability, strength
تخنیکي|adj|technical
حقوق|noun|rights; law|حقوقو
کارکوونکی|noun|worker, employee
استعمال|noun|use
ستونزه|noun|problem, difficulty
ولسي|adj|national, of the people
تجارتي|adj|commercial, trade (adj.)
هوايي|adj|air (adj.), aerial
تا|pron|you (oblique of ته)
راته|adv|to me, to us
درته|adv|to you
روښانه|adj|bright, clear
وار|noun|turn, time
علیه|prep|against
دفاعي|adj|defence (adj.), defensive
خپور|adj|published, spread|خپره خپرې خپاره خپرو
پکې|adv|in it, among them
پرې|adv|on it, on them
ترې|adv|from it, from him, from them
پاره|noun|sake (له ... پاره: for)
ګاونډ|noun|neighbourhood
پکار|adj|needed, necessary
خطرناک|adj|dangerous
مه|particle|don't (before an imperative: مه ځه "don't go")
پای|noun|end (پای ته رسېدل: to come to an end)
بجه|noun|o'clock (درې بجې: three o'clock)|بجې بجو
وزګار|adj|free, not busy|وزګاره وزګارې وزګارو
غښتلی|adj|strong, powerful|غښتلې غښتلي غښتلو غښتلیو
"""
# Forms of the pack's words that its tables lack, irregular or often written: word | forms.
_MORE_FORMS = """
ويل|وايي وايم وايې وايو وايئ ووايي ووايم ووايې ووايو ووايئ وايه ووايه وويل وويلې وويله وويلو وويلم ويلي ويلی ويلې ويلو
کول|وکړ وکړو وکړم وکړې وکړي وکړل وکړه وکړئ کړی کړې کړي کړل کړه کړ کړو کړلی کړلې کړلي کوه کوئ
ورکول|ورکوي ورکوم ورکوو ورکوې ورکړي ورکړم ورکړو ورکړې ورکړل ورکړ ورکړه ورکړی ورکولو
کېدل|شو شوه شول شوي شوې شوی شوم شوو وشو وشوه وشول وشوي وشي کېدای کیدای کېږي کېږم شي شم شې نشي
لرل|درلود درلوده درلودل درلودې نلري
رسېدل|ورسېد ورسېده ورسېدل ورسېدو ورسېږي رسېږي رسېږم رسېدو
تلل|لاړ لاړه لاړل لاړې لاړم ولاړ ولاړه ځي ځم ځو ځئ
راتلل|راغی راغلی راغلي راغلې راغله راغلل راځي راشي راځم راشم
يم|و وه وې وو ول وئ ياست ؤ
يو|يوه يوې يوو
همدا|همدې همدغه
کال|کاله کلونو
منځ|منځه
ياد|ياده يادې يادو
ټول|ټوله ټولې ټولو
دغه|دغې دغو
کتل|کوت وکوت کاته وکاته
ښودل|ښووه وښووه ښووله وښووله وښوول
پېژندل|پېژاند وپېژاند پېژانده وپېژانده
بيول|بوت بوته بوتله بوتلل بوتلم بوتلې بوتلئ بیوه بیوله
راوستل|راووست راووسته راووستل راووستله راووستلې راووستم راووستلم راووستو راووستلو
رسېدل|راورسېد راورسېده راورسېدو راورسېدل راورسېدله راورسېدم
ختل|خوت وخوت خاته وخاته
وړل|یووړ یووړه یووړل یووړله یوړ یوړه یوړل یوسي یوسم یوسو یوسه
راوړل|راووړ راووړه راووړل راووړله
ګڼل|وګاڼه
منل|مانه ومانه
تړل|تاړه وتاړه
ګټل|ګاټه وګاټه
مېلمه|مېلمانه مېلمنو مېلمنه
نجلۍ|نجونې نجونو
دواړه|دواړو
ځای|ځایونه ځایونو
غوښتل|غوښت
موندل|موند
"""
# Spellings the pack has as words of their own that are another word's, as Pakistani and older texts write it (چه for چې,
# کښې for کې, وایروس for ویروس), as regions write it (روځ for ورځ, دلې for دلته) or misspelt (جکړه for جګړه), or forms filed
# under a rare word spelled alike (کړي "does", not کړۍ "ring"; لري "has", not لرې "far"; شي "becomes", not شی "thing";
# پخو "baked", of پوخ, not "to cook").
_SPELLED_AS = """
چه|چې
کښې|کې
پکښې|پکې
وايروس|ويروس
لاره|لار
پاته|پاتې
لیرې|لرې
ان|آن
اووني|اونۍ
قومندان|قوماندان
ډاکتر|ډاکټر
بلکه|بلکې
ایا|آیا
دپاره|لپاره
سو|کېدل
سي|کېدل
سوي|کېدل
مځکه|ځمکه
خلګ|خلک
ږغ|غږ
اخستل|اخيستل
روځ|ورځ
روځې|ورځ
روځو|ورځ
دلې|دلته
جکړه|جګړه
جکړې|جګړه
جکړو|جګړه
پخو|پوخ
"""
# Verbs the pack lacks, with the stem their present and imperative are made of, and the prefix of their perfective ("و" but for
# verbs with a prefix of their own): infinitive | present stem | perfective prefix | English gloss.
_HAND_VERBS = """
خپرېدل|خپرېږ||to be published, to spread
پرېوتل|پرېوځ||to fall
وهل|وه|و|to hit, to beat (زنګ وهل: to phone)
زیاتېدل|زیاتېږ||to increase, to grow
کارول|کارو|و|to use
رسول|رسو|و|to deliver, to bring
سپارل|سپار|و|to hand over, to entrust
وېشل|وېش|و|to divide, to distribute
ساتل|سات|و|to keep, to protect
لیکل|لیک|و|to write
څښل|څښ|و|to drink
نیول|نیس|و|to hold, to seize, to catch
ښودل|ښی|و|to show
ښکارېدل|ښکارېږ||to seem, to appear
جوړېدل|جوړېږ||to be made, to be built
رسېدل|رسېږ|و|to arrive, to reach
ګرځېدل|ګرځ|و|to walk, to wander; to turn
اوښتل|اوړ|وا|to turn, to cross over
پوښتل|پوښت|و|to ask
تښتېدل|تښت|و|to flee, to run away
غږول|غږو|و|to call; to play (an instrument)
لګېدل|لګېږ|و|to be hit; to be spent; to seem
لګول|لګو|و|to apply, to put on; to spend
خلاصول|خلاصو||to open, to free
پرانیستل|پرانیز||to open
پېښېدل|پېښېږ||to happen
کېناستل|کېن||to sit down
پاڅېدل|پاڅ||to get up, to stand up
ځغاستل|ځغل|و|to run
لمبل|لامب|و|to bathe, to swim
وینځل|وینځ|و|to wash
لېږل|لېږ|و|to send
خندل|خاند|و|to laugh
ژړل|ژاړ|و|to cry, to weep
تېرېدل|تېرېږ||to pass, to go by
تېرول|تېرو||to spend (time), to pass
لټول|لټو|و|to search, to look for
"""


def more_forms():
    """(form, word) pairs of _MORE_FORMS and _SPELLED_AS, as spelled."""
    pairs = []
    for line in _MORE_FORMS.strip().splitlines():
        word, forms = line.split("|")
        pairs += [(form, word) for form in forms.split()]
    for line in _SPELLED_AS.strip().splitlines():
        form, word = line.split("|")
        pairs.append((form, word))
    return pairs


def hand_entries():
    """The _HAND and _HAND_VERBS words: (word, pos, gloss, forms)."""
    out = []
    for line in _HAND.strip().splitlines():
        word, pos, gloss, *forms = line.split("|")
        out.append((word, pos, gloss, forms[0].split() if forms else []))
    for line in _HAND_VERBS.strip().splitlines():
        verb, stem, prefix, gloss = line.split("|")
        out.append((verb, "verb", gloss, verb_forms(verb, stem, prefix)))
    return out


# The present stems of the pack's verbs that no rule makes from the infinitive (کتل: ګوري, لیدل: ویني, اخيستل: اخلي), as their
# tables, where they have one, write them: verb | present stems.
_STEMS = """
کتل|ګور
ليدل|وین
اخيستل|اخل
نیول|نیس
غوښتل|غواړ
لوستل|لول
پېژندل|پېژن
وتل|وځ وز
ننوتل|ننوځ
پرېوتل|پرېوځ
کېناستل|کېن
راوستل|راول
بيول|بیای بوځ
ښودل|ښی
اورېدل|اور
موندل|موم
خوړل|خور
وژل|وژن
ختل|خېژ
اغوستل|اغوند
پرانیستل|پرانیز
پرېښودل|پرېږد
ايستل|باس
وړل|وړ
راوړل|راوړ
ځغاستل|ځغل
خندل|خاند
ژړل|ژاړ
لمبل|لامب
ټاکل|ټاک
لیکل|لیک
ساتل|سات
سپارل|سپار
لېږل|لېږ
ګټل|ګټ
ګڼل|ګڼ
منل|من
تړل|تړ
وهل|وه
پوښتل|پوښت
څښل|څښ
وینځل|وینځ
وېشل|وېش
پلورل|پلور
اوښتل|اوړ
الوتل|الوځ
زغمل|زغم
څېړل|څېړ
غندل|غند
لمانځل|لمانځ
ژباړل|ژباړ
"""
# Verbs whose forms the rules would get wrong, which _MORE_FORMS and their tables give: کول (کړ-), کېدل (ش-), تلل (ځ-, لاړ;
# و with its stem would be وتل, another verb), the verbs of راتلل and ورکول, ویل, لرل.
_IRREGULAR = set("کول کېدل شول تلل راتلل درتلل ورتلل ويل لرل يم ورکول درکول راکول".split())
# A verb with a prefix of its own takes no و in its perfective: کېناستل, پرېښودل, ننوتل, پرانیستل, and را, در or ور with a verb
# (راوړل, ورکول) or these (راوستل), whose perfectives with و inside (راووست) _MORE_FORMS gives.
_PREFIXES = tuple(pashto_fold(p) for p in ("کېن", "کښېن", "پرې", "ننو", "پران"))
_DIRECTIONS = tuple(pashto_fold(p) for p in ("را", "در", "ور"))
_PREFIXED = {pashto_fold(v) for v in "راوستل دروستل وروستل".split()}


def own_prefix(key, verbs):
    """Whether the folded verb [key] has a prefix of its own, [verbs] the folded verbs."""
    return key.startswith(_PREFIXES) or key in _PREFIXED or key.startswith(_DIRECTIONS) and key[2:] in verbs


_PRESENT = ("م", "ې", "ي", "و", "ئ")
# The past's person endings: I, you, she, we, you (plural), they (masculine); "he" is the stem itself, or with ه or و.
_PAST = ("م", "ې", "ه", "و", "ئ", "")


def verb_stems():
    """The pack's verbs' present stems that no rule makes, folded: verb -> [stems]."""
    out = {}
    for line in _STEMS.strip().splitlines():
        verb, stems = line.split("|")
        out[pashto_fold(verb)] = stems.split()
    return out


def verb_paradigm(verb, stems=(), prefix="و"):
    """
    A verb's forms by the regular rules, from its infinitive (لیکل) and the present stems it has (لیک; the -ېدل verbs' -ېږ
    and the -ول verbs' -و the rules make): the present (لیکم, لیکې, لیکي, لیکو, لیکئ) and imperative (لیکه); the past on the
    infinitive (لیکلم, لیکلې, لیکله, لیکلو, لیکلئ, لیکل) and on its short stem (لیکم, لیکه, لیکو; the -ول verbs' لټاوه); each
    also with the perfective [prefix] (ولیکي, ولیک, واخیست, ورسېد), "" for a verb with a prefix of its own (کېناستل) or one
    made of an adjective (زیاتېدل, whose perfective is زیات شو); the participle (لیکلی, لیکلې, لیکلي, لیکلو), the
    infinitive's oblique (لیکلو) and the potential (لیکلای). The short stem stands alone as the past of an -ېدل verb (رسېد)
    and of one without the prefix (کېناست); another's is written with it (ولید, not لید, which is "sight").
    """
    if " " in verb or not verb.endswith("\u0644") or len(verb) < 3:
        return []
    short = verb[:-1]
    stems = list(stems)
    # An -ول verb (لټول: لټوي, لټاوه) but نیول and بیول, whose stems _STEMS gives.
    causative = verb.endswith("\u0648\u0644") and not stems
    if verb.endswith("\u06d0\u062f\u0644") and not stems:
        stems = [verb[:-3] + "\u06d0\u0696"]
    elif causative:
        stems = [verb[:-1]]
    present = [stem + e for stem in stems for e in _PRESENT + ("\u0647",)]
    past = [verb + e for e in _PAST] + [short + e for e in _PAST if len(short + e) >= 3] + [short + "\u0648"]
    # The short stem alone, as above; an -ول verb's past is لټاوه, not لټو (لټ's oblique).
    if causative or not (verb.endswith("\u06d0\u062f\u0644") or not prefix):
        past = [f for f in past if f != short]
    if causative:
        past.append(verb[:-2] + "\u0627\u0648\u0647")
    participle = [verb + e for e in ("\u06cc", "\u06d0", "\u064a", "\u0648", "\u06cc\u0648")] + [verb + "\u0627\u06cc"]
    forms = present + past + participle
    if prefix:
        forms += [prefix + f for f in present + past]
        if not causative:
            forms.append(prefix + short)
    return list(dict.fromkeys(f for f in forms if f != verb))


def verb_forms(verb, stem, prefix="و"):
    """
    A verb's forms by the regular rules, from its infinitive (لیکل) and present stem (لیک): the present (لیکم, لیکې, لیکي,
    لیکو, لیکئ) and imperative (لیکه), each also with the perfective prefix (ولیکي, ولیکه); the past (لیکلم, لیکلې, لیکه,
    لیکله, لیکلو, لیکلئ, لیکل), also perfective (ولیکل); the participle (لیکلی, لیکلې, لیکلي, لیکلو), the infinitive's oblique
    (لیکلو). Its yes are written once, folded (frequency_pashto.pashto_fold folds ې, ی, ئ as ي).
    """
    past = verb[:-1]
    present = [stem + e for e in ("م", "ې", "ي", "و", "ئ", "ه")]
    simple_past = [verb + e for e in ("م", "ې", "ه", "و", "ئ")] + [verb, past + "ه"]
    participle = [verb + e for e in ("ی", "ې", "ي", "و", "یو")]
    forms = present + simple_past + participle
    if prefix:
        forms += [prefix + f for f in present + simple_past]
    return list(dict.fromkeys(f for f in forms if f != verb))


# Nouns for people that make a woman's word with -ه (ډاکټر: ډاکټره, ډاکټرې), which no rule tells from a thing's.
_PERSONS = "ډاکټر استاد کډوال وکیل همکار لیکوال خبریال نرس شاعر افغان"


def rule_forms(entries):
    """
    The forms the rules make of the pack's words beyond nominal_forms, (word, part of speech) pairs as spelled: each verb's
    (verb_paradigm, with the present stems of _STEMS or _HAND_VERBS) but the irregular ones', and each noun's plurals
    (noun_forms) and a person's feminine (ډاکټره, ډاکټرې), but those spelled as an entry is: (form, word) pairs.
    """
    words = {pashto_fold(word) for word, _ in entries}
    hand = {pashto_fold(verb): (verb, stem, prefix) for verb, stem, prefix, _ in (line.split("|") for line in _HAND_VERBS.strip().splitlines())}
    irregular = {pashto_fold(v) for v in _IRREGULAR}
    persons = {pashto_fold(w) for w in _PERSONS.split()}
    verbs = {pashto_fold(word) for word, pos in entries if pos == "verb"}
    stems = verb_stems()
    pairs = []
    for word, pos in entries:
        key = pashto_fold(word)
        if pos == "verb" and key in hand:
            verb, stem, prefix = hand[key]
            pairs += [(form, verb) for form in verb_paradigm(verb, [stem], prefix)]
        elif pos == "verb" and key not in irregular:
            prefix = "" if own_prefix(key, verbs) else "\u0648"
            pairs += [(form, word) for form in verb_paradigm(word, stems.get(key, ()), prefix)]
        elif pos == "noun":
            made = noun_forms(word) + ([word + "\u0647", word + "\u06d0"] if key in persons else [])
            pairs += [(form, word) for form in made if pashto_fold(form) not in words]
    return pairs


def noun_forms(word):
    """
    A noun's plurals nominal_forms leaves out: a final ye's animate plural -یان, -یانو (ګاونډیان, ګاونډیانو) and a final
    consonant's Arabic plural -ات (حیوانات, تاثیرات).
    """
    if " " in word or len(word) < 2 or word[-1] in "\u0647\u0627\u0648":
        return []
    if word[-1] in "\u064a\u06cc\u06d0\u06cd\u0626":
        return [word[:-1] + "\u06cc\u0627\u0646", word[:-1] + "\u06cc\u0627\u0646\u0648"]
    return [word + "\u0627\u062a"]


def gaps(pairs, words, formed):
    """The (form, word) [pairs] whose form is neither one of [words] nor among the forms [formed] already has (folded)."""
    return [(form, word) for form, word in pairs if pashto_fold(form) not in words and pashto_fold(form) not in formed]


def mend(pos, lemmas, common):
    """
    Adds the hand words, their forms, _MORE_FORMS and the forms the rules make (rule_forms) where the pack has the form
    neither as a word nor as a form (gaps) to the list builder's [pos], [lemmas] and [common], folded, so the list has them
    whether or not the pack on disk was built with them.
    """
    def add(form, word):
        form, word = pashto_fold(form), pashto_fold(word)
        if form != word and word not in lemmas[form]:
            lemmas[form].append(word)

    added = []
    for word, kind, _, forms in hand_entries():
        key = pashto_fold(word)
        if kind not in pos[key]:
            pos[key].add(kind)
            common.setdefault(key, word)
            added.append((word, kind))
        for form in forms:
            add(form, word)
    for form, word in more_forms():
        add(form, word)
    # The pack gives each of its words the forms nominal_forms makes, but those spelled as an entry is.
    for word, kind in added:
        for form in nominal_forms(word, kind) if kind in ("noun", "adj", "det", "num", "pron") else ():
            if pashto_fold(form) not in pos:
                add(form, word)
    entries = [(common.get(key, key), kind) for key, kinds in pos.items() for kind in sorted(kinds)]
    formed = {form for form, of in lemmas.items() if of}
    for form, word in gaps(rule_forms(entries), set(pos), formed):
        add(form, word)


def nominal_forms(word, pos):
    """
    The regular forms of a noun or adjective the pack has no table for, by its ending: a consonant's plural in -ونه (ولایتونه)
    and -ان (مشران) with their obliques (-ونو, -انو) and the oblique plural -و (نورو); an adjective's feminine -ه, -ې (خپله,
    خپلې); -ه's -ې and -و (سولې, سولو); a final ye's oblique plural -یو and -و (سړیو, پیسو); -ا's -وې, -وو, -ګانې, -ګانو.
    """
    if " " in word or len(word) < 2:
        return []
    last = word[-1]
    if pos == "pron" and last in "\u0647\u064a\u06cc\u06d0\u06cd\u0626\u0627\u0648":
        out = []
    elif last in "\u0647":
        out = [word[:-1] + "\u06d0", word[:-1] + "\u0648"]
    elif last in "\u064a\u06cc\u06d0\u06cd\u0626":
        out = [word[:-1] + "\u064a\u0648", word[:-1] + "\u0648"]
    elif last == "\u0627":
        out = [word + "\u0648\u06d0", word + "\u0648\u0648", word + "\u06ab\u0627\u0646\u06d0", word + "\u06ab\u0627\u0646\u0648"]
    elif last == "\u0648":
        out = [word + "\u06ab\u0627\u0646\u06d0", word + "\u06ab\u0627\u0646\u0648"]
    elif pos == "pron":
        out = [word + "\u0648"]
    elif pos == "noun":
        out = [word + "\u0648\u0646\u0647", word + "\u0648\u0646\u0648", word + "\u0627\u0646", word + "\u0627\u0646\u0648", word + "\u0648"]
    else:
        out = [word + "\u0647", word + "\u06d0", word + "\u0648"]
    return out


# The list's corrections, as frequency_fixes.OVERRIDES and EXCLUDE have them, folded: the other spellings with their word; the
# verb forms the pack also has as a rare word spelled alike (کړي "does" and کړۍ "ring", لري "has" and لرې "far", شي "becomes"
# and شی "thing", وو "were" and "wind", مني "accepts" and "semen") with their verb.
OVERRIDES = {pashto_fold(form): [pashto_fold(word)] for line in _SPELLED_AS.strip().splitlines() for form, word in [line.split("|")]}
OVERRIDES.update({pashto_fold(form): [pashto_fold(verb)] for form, verb in (
    ("کړي", "کول"), ("کړه", "کول"), ("کړ", "کول"), ("کړې", "کول"), ("لري", "لرل"), ("شي", "کېدل"), ("شو", "کېدل"),
    ("وو", "يم"), ("وه", "يم"), ("مني", "منل"), ("کړم", "کول"), ("کړو", "کول"),
    # And the forms filed under a rare word spelled alike: لارې "ways" (not لارۍ "lorry"), ډېرې, کلي "villages", ملګري
    # "friends", تنه and کسان "persons".
    ("لارې", "لار"), ("ډېرې", "ډېر"), ("کلي", "کلی"), ("ملګري", "ملګری"), ("تنه", "تن"), ("کسان", "کس"), ("غونډې", "غونډه"), ("بولي", "بلل"), ("خوري", "خوړل"))})
# اوسي "lives" (of اوسېدل), not the -ې form of اوسه "now"; غښتلي "strong" (غښتلی), no form of غښتل "to plait".
OVERRIDES[pashto_fold("اوسي")] = [pashto_fold("اوسېدل")]
OVERRIDES[pashto_fold("غښتلی")] = [pashto_fold("غښتلی")]
# ماتو "defeats" (د تاريخي ماتو), the oblique of ماتې, not the pack's ماتو "housewife".
OVERRIDES[pashto_fold("ماتو")] = [pashto_fold("ماتې")]
# How a word is shown where the pack's spellings would show another word's (ترۍ for ترې, مېنه "home" for مینه "love"), or
# a Kandahari or Pakistani spelling (تاسې, ستاسې) for the Afghan standard one.
DISPLAY = {pashto_fold(w): w for w in "چې دی ترې مینه رئیس امریکایي زوی نوی چای".split()}
DISPLAY.update({pashto_fold("تاسې"): "تاسو", pashto_fold("ستاسې"): "ستاسو"})
# Persian words of the Dari sentences among the Pashto ones (برای "for", کشور "country", مردم "people", است "is"), and بين, the
# piece of بين المللي ("international").
EXCLUDE = {pashto_fold(w) for w in "برای کشور مردم من کند اند بين کرد است از با این خود".split()}
# And names the pack has as words (کندهار, the province; حسين "handsome", the name Hussain).
EXCLUDE |= {pashto_fold(w) for w in "کندهار حسين".split()}
# And قابو "control" (قابو کول), which the pack lacks, no form of قاب "plate".
EXCLUDE.add(pashto_fold("قابو"))


# A word of the pack that is two words written as one (دهري for د هرې, نوله for نو له, فکرکوي for فکر کوي) is those two
# words where the sentences write them apart JOINED_APART times as often as joined.
JOINED_APART = 5
# And these, written joined more often than apart, are mostly two words (ورشو: ور شو, not "meadow"; مايي: ما یې, not مایه's).
_JOINED = {pashto_fold(w) for w in "ورشو مايي".split()}
# The words before a name (ښاغلي الهام هوتکي, انجنير ...) and after one (خان, جان, نصر الله; a magazine's مجله: ګوربت مجله).
_TITLES = {pashto_fold(w) for w in (
    "ښاغلي ښاغلی ښاغلو محترم محترمه مولوي مولوی مولانا حاجي الحاج قاري ملا سید شهید مرحوم مرحومه اغلې اغلی مېرمن ډاکټر "
    "داکتر انجنير انجینر جنرال ډګرمن ډګروال ډګرجنرال شيخ حضرت سردار نواب حافظ مفتي").split()}
_AFTER_NAME = {pashto_fold(w) for w in "خان جان صاحب صیب خېل خیل الله الدین مجله مجلې ورځپاڼه ورځپاڼې".split()}


def read_usage(known):
    """
    How the Pashto sentences counted use each word (folded): the share of its uses that are a piece of a word of the pack a space
    split (زيږ يدو, عا لم, و خت: the word whole at least as often) or beside a title or name (ښاغلي هوتکي, ګوربت مجله); and the
    words of the pack that are two words written as one (joined: دهري, نوله, فکرکوي).
    """
    from collections import Counter
    seen, pairs = Counter(), Counter()
    for path in sorted(SENTENCES):
        for words in SENTENCES[path]:
            seen.update(words)
            for a, b in zip(words, words[1:]):
                if a + b in known:
                    pairs[a, b] += 1
    lost = Counter()
    joined = set(_JOINED)
    for (a, b), n in sorted(pairs.items()):
        whole = a + b
        if seen[whole] >= n:
            lost[a] += n
            lost[b] += n
        elif a in known and b in known and n >= JOINED_APART * seen[whole]:
            joined.add(whole)
    for path in sorted(SENTENCES):
        for words in SENTENCES[path]:
            for i, word in enumerate(words):
                if i and words[i - 1] in _TITLES or i + 1 < len(words) and words[i + 1] in _AFTER_NAME:
                    lost[word] += 1
    return {w: min(1.0, n / seen[w]) for w, n in lost.items() if seen[w]}, joined


# Names, titles, dynasties, tribes and places the pack has as words (بېګم, هوتکي "Hotak", اپريدی, پارس, نصر of نصرالله,
# وېلېنګټن), and words of Dari (اسم, نان, صحبت, نوشته) or Arabic (يوم "day", not "spade") whose list rank is their other use.
NOT_WORDS = {pashto_fold(w) for w in (
    "بېګم هوتکي بودا پيمان ګهيځ پېچ اپريدی پارس پلوشه ګوربت نصر وېلېنګټن اسم نان پېروزي روزنامه صحبت نګاه نوشته اعتقاد "
    "يوم").split()}


# The letters regional and loan spellings of one word write for each other (ټ, ت: لاټرۍ, لاترۍ; ډ, د; ړ, ر: دړد, درد; ښ, ش:
# ښوروا, شوروا; ږ, ژ; ځ, ز; ڼ, ن; ن, م before ب: تنباکو, تمباکو; آ, ا: آهنګ, اهنګ), and the ه a vowel swallows (شاهزاده,
# شازاده).
_VARIANT_LETTERS = str.maketrans({"\u067c": "\u062a", "\u0689": "\u062f", "\u0693": "\u0631", "\u069a": "\u0634",
                                  "\u0696": "\u0698", "\u0681": "\u0632",
                                  "\u0622": "\u0627", "\u06bc": "\u0646"})


def variant_key(word):
    """A folded word as its regional and loan spellings write it too (_VARIANT_LETTERS)."""
    word = word.translate(_VARIANT_LETTERS).replace("\u0646\u0628", "\u0645\u0628")
    return re.sub("(?<=[\u0627\u0648\u064a])\u0647(?=.)", "", word)


_STOP_GLOSS = set("the and for with from that this one who which any its his her their person thing used being of".split())
# A gloss naming a man of a tribe (an Afridi man, a Barakzai man), or a given name or surname.
_TRIBE_GLOSS = re.compile(r"^(?:an? )?[A-Z][a-z]+ man$|\b(?:given name|surname)\b")


def read_glosses(pack):
    """The pack's words' gloss words (folded word -> set) and its tribes and names (a word all of whose glosses name one)."""
    import json
    import sqlite3
    glosses, kinds = {}, {}
    db = sqlite3.connect(pack)
    for word, pos, senses in db.execute("SELECT word_lc, pos, senses FROM entries ORDER BY id"):
        key = pashto_fold(word)
        heads = [g for s in json.loads(senses) for g in s.get("glosses", [])]
        glosses.setdefault(key, set()).update(
            w[:5] for g in heads for w in re.findall("[a-z]+", g.lower()) if len(w) > 2 and w not in _STOP_GLOSS)
        named = pos == "name" or bool(heads) and all(_TRIBE_GLOSS.search(g) for g in heads)
        kinds[key] = kinds.get(key, True) and named
    db.close()
    return glosses, {w for w, named in kinds.items() if named}


def fold(scores, forms, known, pack):
    """
    After the counting: a word's forms count only their uses as that word (read_usage), none of a form that is two words
    written as one; a regional or loan spelling of a word of a like sense (variant_key, a gloss word in common) is the
    commoner spelling's; NOT_WORDS, the pack's tribes and names and the words with no use left are left out.
    """
    lost, joined = read_usage(known)
    glosses, named = read_glosses(pack)
    for word in sorted(scores):
        counted = forms[word]
        for form in sorted(counted):
            counted[form] *= 0.0 if form in joined else 1 - lost.get(form, 0.0)
    for word in sorted(scores):
        scores[word] = sum(forms[word].values())
        if word in NOT_WORDS or word in named or scores[word] <= 0:
            del scores[word]
    keyed = {}
    for word in sorted(scores, key=lambda w: (-scores[w], w)):
        first = keyed.setdefault(variant_key(word), word)
        if first != word and glosses.get(first, set()) & glosses.get(word, set()):
            for form, share in sorted(forms[word].items()):
                forms[first][form] = forms[first].get(form, 0) + share
            scores[first] += scores.pop(word)
