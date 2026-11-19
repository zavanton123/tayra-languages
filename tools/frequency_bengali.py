"""
The Bengali frequency list (build_frequency_list.py --language bn).

Bengali writes a noun with its case ending, classifier and plural joined on (দেশের, বইটা, ছেলেরা, বাড়িতে) and the clitics
-ই "only" and -ও "also" after them (দেশেও, আমিই). The pack's tables give the verbs' forms (করি, করছে, করেছিলেন) and some
nouns' and pronouns', but most nouns have none, nor does simplemma know Bengali. So each word of the pack is declined here by
its ending, and a corpus form is counted for the words it is a form of: by hand (BENGALI_FORMS: the pronouns, spellings the
pack lacks), by the pack's tables, or by these rules. A form that is a word of the pack itself keeps its count (থেকে
"from", not থাকা's "having stayed"), unless the word it is also a form of is more common than the form (করে goes to করা).
A form of several words is shared between them by how common each is by its forms of no other word.
"""
import json
import os
import re
import sqlite3
import unicodedata
from collections import defaultdict

# As the reader's Bengali wordCharacters, without the Bengali digits (numbers are no words).
BENGALI_LETTERS = re.compile("[ঀ-৥ৰৱ‌‍]+")
# A joiner belongs only after a virama (র‍্যাব); elsewhere it is a typing slip (ক‌রে).
STRAY_JOINER = re.compile("(?<!্)[‌‍]")
VOWEL_SIGNS = set("ািীুূৃেৈোৌ")
VOWELS = set("অআইঈউঊঋএঐওঔ")
WORD_KINDS = {"noun", "adj", "verb", "adv", "pron", "num", "conj", "det", "intj", "particle", "postp", "prep", "classifier"}
FORMS_LISTED = 400

# A noun's endings after a consonant (দেশ: দেশের, দেশকে, দেশে) and after a vowel (মা: মার, মায়ের, মাকে; বাড়ি: বাড়িতে;
# ঢাকা: ঢাকায়), with the classifiers and plurals that take the cases in turn.
CLASSIFIERS = ["টা", "টি", "টো", "খানা", "খানি", "গুলো", "গুলি", "গুলা", "টুকু", "জন"]
CLASSIFIER_CASES = ["", "র", "কে", "তে", "য়", "ও", "ই", "রই", "রও", "কেও", "কেই", "তেও", "তেই"]
CONSONANT_TAILS = ["ের", "কে", "ে", "েও", "েই", "েরা", "রা", "দের", "দেরকে", "েরও", "েরই", "কেও", "কেই", "দেরও", "দেরই", "রাও", "রাই",
                   "েতে"]
VOWEL_TAILS = ["র", "য়ের", "কে", "তে", "য়", "য়ে", "রা", "দের", "দেরকে", "রও", "রই", "য়েরও", "য়েরই", "কেও", "কেই", "তেও", "তেই",
               "য়ও", "য়ই", "দেরও", "দেরই", "রাও", "রাই"]
CLITICS = ["ই", "ও"]
# A verbal noun's (করা, যাওয়া) cases: করার, করাতে, করায়, করাটা.
VERBAL_NOUN_TAILS = ["র", "রই", "রও", "তে", "য়", "টা", "টাই", "টাও", "কে", "ই", "ও"]

# Forms the rules make wrongly or not at all, and spellings the pack lacks, by hand: form -> word.
PRONOUNS = {
    "আমি": "আমার আমাকে আমায়",
    "আমরা": "আমাদের আমাদেরকে",
    "তুমি": "তোমার তোমাকে তোমায়",
    "তোমরা": "তোমাদের তোমাদেরকে",
    "তুই": "তোর তোকে",
    "তোরা": "তোদের তোদেরকে",
    "আপনি": "আপনার আপনাকে",
    "আপনারা": "আপনাদের আপনাদেরকে",
    "সে": "তার তাকে",
    "তারা": "তাদের তাদেরকে",
    "তিনি": "তাঁর তাঁকে",
    "তাঁরা": "তাঁদের তাঁদেরকে",
    "এ": "এর একে",
    "এরা": "এদের এদেরকে",
    "ও": "ওর ওকে",
    "ওরা": "ওদের ওদেরকে",
    "উনি": "ওঁর ওঁকে ওনার ওনাকে",
    "ইনি": "এঁর এঁকে এনার এনাকে",
    "কে": "কার কাকে",
    "কারা": "কাদের কাদেরকে",
    "কেউ": "কারো কারও কাউকে",
    "যে": "যার যাকে",
    "যারা": "যাদের যাদেরকে",
    "যিনি": "যাঁর যাঁকে",
    "নিজে": "নিজের নিজেকে নিজেদের নিজেরা নিজেদেরকে",
    "সবাই": "সবার সবাইকে",
    "এটা": "এটার এটাকে এটাতে এটায়",
    "এটি": "এটির এটিকে এটিতে",
    "সেটা": "সেটার সেটাকে সেটাতে সেটায়",
    "সেটি": "সেটির সেটিকে সেটিতে",
    "ওটা": "ওটার ওটাকে ওটাতে ওটায়",
    "এগুলো": "এগুলোর এগুলোকে এগুলোতে এগুলি এগুলা",
    "সেগুলো": "সেগুলোর সেগুলোকে সেগুলোতে সেগুলি সেগুলা",
    "ওগুলো": "ওগুলোর ওগুলোকে ওগুলোতে",
}
BENGALI_FORMS = {form: word for word, forms in PRONOUNS.items() for form in [word] + forms.split()}
BENGALI_FORMS.update({
    # কোনটা "which one" is কোন's, not কি's.
    "কোনটা": "কোন", "কোনটার": "কোন", "কোনটায়": "কোন", "কোনটি": "কোন", "কোনটির": "কোন", "কোনগুলো": "কোন",
    # The perfect of যাওয়া as it is spoken and written (গেছে for গিয়েছে).
    "গেছে": "যাওয়া", "গেছি": "যাওয়া", "গেছ": "যাওয়া", "গেছো": "যাওয়া", "গেছেন": "যাওয়া", "গেছিল": "যাওয়া",
    "গেছিলাম": "যাওয়া", "গেছিলেন": "যাওয়া",
    # আসা's past and imperative, which its table lacks (এল, এসো).
    "এল": "আসা", "এলো": "আসা", "এলাম": "আসা", "এলেন": "আসা", "এলি": "আসা", "এসো": "আসা", "এসেছিল": "আসা",
    # আছে's forms, which the pack files under its root আছ- with থাকা's.
    "আছি": "আছে", "আছ": "আছে", "আছো": "আছে", "আছিস": "আছে", "আছেন": "আছে", "ছিলাম": "আছে", "ছিলি": "আছে", "ছিলে": "আছে",
    "ছিল": "আছে", "ছিলো": "আছে", "ছিলেন": "আছে", "ছিলুম": "আছে",
    # নয় "is not" in each person (নই, নন), and নেই "there is not" and নাই, words of their own, not নেওয়া's.
    "নই": "নয়", "নও": "নয়", "নস": "নয়", "নন": "নয়", "নেই": "নেই", "নাই": "নাই",
    # Imperatives the tables lack.
    "নিন": "নেওয়া", "নাও": "নেওয়া", "দাও": "দেওয়া", "দিও": "দেওয়া", "থেকো": "থাকা", "কোরো": "করা",
    "হোন": "হওয়া", "হও": "হওয়া", "হোক": "হওয়া", "বোসো": "বসা",
    # A time adverb's genitive (আজকের "today's", এখানকার "of here").
    "আজকের": "আজ", "কালকের": "কাল", "এখনকার": "এখন", "তখনকার": "তখন", "এখানকার": "এখানে", "সেখানকার": "সেখানে",
    "ওখানকার": "ওখানে", "আগেকার": "আগে",
    # এতে "in this", তাতে "in that".
    "এতে": "এ", "তাতে": "তা", "এতেই": "এ", "তাতেই": "তা", "এতেও": "এ", "তাতেও": "তা",
    # কেনো, as কেন "why" is often written, is no form of কেনা "to buy"; পুরা is পুরো "whole"; পরেও "even after" is পর's.
    "কেনো": "কেন", "পুরা": "পুরো", "পরও": "পর", "পরেও": "পর",
    # আগের "earlier, previous" is আগে's, not আগ "front part"'s.
    "আগের": "আগে", "আগেই": "আগে", "আগেও": "আগে", "থাক": "থাকা",
    # পা "foot", which the table of পাওয়া "to get" also lists as its imperative.
    "পা": "পা",
})
# Spellings the pack has the other way, or lacks (বেশি, the reformed spelling, as বেশী; ভেতর as ভিতর): each is declined as
# the word it is a spelling of, and its forms counted for it.
SPELLINGS = {
    "বেশি": "বেশী", "এখনো": "এখনও", "আরো": "আরও", "কখনো": "কখনও", "ওপর": "উপর", "কারন": "কারণ", "তৈরী": "তৈরি",
    "উচিৎ": "উচিত", "সাধারন": "সাধারণ", "ধরণ": "ধরন", "এতো": "এত", "হয়ত": "হয়তো", "ইচ্ছে": "ইচ্ছা", "ভেতর": "ভিতর",
    "পেছন": "পিছন", "নীচ": "নিচ", "গ্রহন": "গ্রহণ", "ঘোষনা": "ঘোষণা", "প্রমান": "প্রমাণ", "পরিমান": "পরিমাণ", "দারুন": "দারুণ",
    "ব্যাবহার": "ব্যবহার", "ব্যাক্তি": "ব্যক্তি", "দাবী": "দাবি", "কোম্পানী": "কোম্পানি", "পরিস্কার": "পরিষ্কার",
    "ভবিষ্যত": "ভবিষ্যৎ", "মুহুর্ত": "মুহূর্ত", "ধারনা": "ধারণা", "বেসরকারী": "বেসরকারি", "চাকরি": "চাকরী", "বিকেল": "বিকাল",
    "জানুয়ারী": "জানুয়ারি", "ফেব্রুয়ারী": "ফেব্রুয়ারি", "রোববার": "রবিবার", "ব্যাথা": "ব্যথা", "ভেঙ্গে": "ভাঙা", "ভাঙ্গা": "ভাঙা",
    "এখনি": "এখনই", "যেনো": "যেন", "কতো": "কত", "কিন্ত": "কিন্তু",
    "একসঙ্গে": "একসাথে", "এগোনো": "আগানো", "সরকারি": "সরকারী",
}
# Everyday words the pack lacks, with their kind as the rules decline them.
BENGALI_WORDS = {
    "আছে": "verb", "নেই": "adv",
    "তাহলে": "conj", "যেখানে": "adv", "নাকি": "particle", "এসব": "det", "সেসব": "det", "যেসব": "det", "এইসব": "det",
    "এমনকি": "adv", "একা": "adj", "যত": "adv", "তত": "adv", "তেমন": "adj", "এরকম": "adj", "যেভাবে": "adv",
    "সম্ভবত": "adv", "অন্তত": "adv", "এদিকে": "adv", "তাছাড়া": "adv", "এছাড়া": "adv", "সুতরাং": "conj", "একসাথে": "adv",
    "কাছাকাছি": "adv", "সেদিন": "adv", "যেদিন": "adv", "একদিন": "adv", "কিছুদিন": "adv", "অনেকদিন": "adv",
    "কয়েকদিন": "adv", "একসময়": "adv", "আজকাল": "adv", "মোটেও": "adv", "ভীষণ": "adv", "সাধারণত": "adv", "মূলত": "adv",
    "সম্প্রতি": "adv", "অবশেষে": "adv", "বারবার": "adv", "যেকোনো": "det", "অন্যদিকে": "adv", "শুধুমাত্র": "adv",
    "সারাদিন": "adv", "এতদিন": "adv", "যতক্ষণ": "conj", "অতঃপর": "adv", "এজন্য": "adv", "মোটামুটি": "adv",
    "ঘুম": "noun", "পাগল": "adj", "নেতা": "noun", "কয়েক": "adj", "ছোট্ট": "adj", "লোকজন": "noun", "মুহূর্ত": "noun",
    "চুপ": "adj", "নাচ": "noun", "দেয়াল": "noun", "টুকরো": "noun", "ক্লান্ত": "adj", "বৃদ্ধ": "adj", "প্রতিবেশী": "noun",
    "ভদ্র": "adj", "ভয়ানক": "adj", "টিভি": "noun", "অর্ধেক": "noun", "অসম্ভব": "adj", "বিরক্ত": "adj", "সাহসী": "adj",
    "হতাশ": "adj", "পড়াশোনা": "noun", "কোম্পানি": "noun", "পাশ": "noun", "জমা": "noun", "কাপ": "noun", "কল": "noun",
    "পিছন": "noun", "নিচ": "noun", "ঘন্টা": "noun", "চিন্তিত": "adj", "গোটা": "adj", "অন্যরকম": "adj", "উল্টো": "adj",
    "স্থানীয়": "adj", "নিহত": "adj", "আহত": "adj", "অবস্থান": "noun", "উল্লেখ": "noun", "উন্নয়ন": "noun", "উদ্ধার": "noun",
    "একমাত্র": "adj", "প্রতিষ্ঠান": "noun", "কেন্দ্রীয়": "adj", "আটক": "adj", "নিজস্ব": "adj", "পরিণত": "adj",
    "পরবর্তী": "adj", "সংস্থা": "noun", "সভাপতি": "noun", "নির্বাচিত": "adj", "আয়োজন": "noun", "সামরিক": "adj",
    "নিয়ন্ত্রণ": "noun", "প্রযুক্তি": "noun", "অবস্থিত": "adj", "বিক্ষোভ": "noun", "শক্তিশালী": "adj", "সংগ্রহ": "noun",
    "অভিযান": "noun", "মোট": "adj", "আবেদন": "noun", "চুক্তি": "noun", "পরিচালনা": "noun", "মন্তব্য": "noun", "মহান": "adj",
    "উপস্থিত": "adj", "জুড়ে": "postp", "পর্যায়": "noun", "নিরাপদ": "adj", "অর্থনৈতিক": "adj", "ভর্তি": "adj",
    "সর্বশেষ": "adj", "ব্যবহৃত": "adj", "ব্যর্থ": "adj", "পরিবেশ": "noun", "শিক্ষার্থী": "noun", "সম্পন্ন": "adj",
    "শ্রমিক": "noun", "সক্ষম": "adj", "কার্যকর": "adj", "পক্ষ": "noun", "বাধ্য": "adj", "প্রতিবেদন": "noun", "চূড়ান্ত": "adj",
    "বিখ্যাত": "adj", "সামান্য": "adj", "সহায়তা": "noun", "প্রশিক্ষণ": "noun", "প্রতিনিধি": "noun", "বক্তব্য": "noun",
    "অধ্যাপক": "noun", "নির্দিষ্ট": "adj", "প্রক্রিয়া": "noun", "প্রতিযোগিতা": "noun", "অতি": "adv", "নির্যাতন": "noun",
    "আর্থিক": "adj", "বিজয়ী": "adj", "বিবেচনা": "noun", "শ্রেষ্ঠ": "adj", "সীমান্ত": "noun", "চরিত্র": "noun",
    "দুর্নীতি": "noun", "বেশিরভাগ": "noun", "দায়ী": "adj", "বার্তা": "noun", "অসংখ্য": "adj", "ফেরত": "adj", "বেতন": "noun",
    "সক্রিয়": "adj", "অভাব": "noun", "একাধিক": "adj", "বিকল্প": "noun", "চলমান": "adj", "উন্নতি": "noun", "অপর": "adj",
    "বিরাট": "adj", "অসহায়": "adj", "গর্ব": "noun", "যাত্রী": "noun", "সংকট": "noun", "নায়ক": "noun",
    "উদযাপন": "noun", "নিখোঁজ": "adj", "সচেতন": "adj", "আগ্রহী": "adj", "চতুর্থ": "adj", "পঞ্চম": "adj", "ষষ্ঠ": "adj",
    "ভোট": "noun", "প্রদান": "noun", "অনুষ্ঠিত": "adj", "বেসরকারি": "adj", "প্রতিবাদ": "noun", "গ্রুপ": "noun",
    "ম্যাচ": "noun", "টিম": "noun", "কেজি": "noun", "জেল": "noun", "ব্যাংক": "noun", "কার্ড": "noun", "অফিসার": "noun",
    "মিডিয়া": "noun", "রিপোর্ট": "noun", "প্রথমবার": "adv", "বৈঠক": "noun", "আসন": "noun", "সূত্র": "noun",
    "বের": "adv", "সহ": "postp", "পুরো": "adj", "খাবার": "noun", "মানব": "noun",
}
# Words of their own that a verb's form is spelled as, which keep their count however common the verb is: সরল "simple" (not
# সরা's), চলতি "current", খাবার "food" (not খাওয়া's "to eat"), মানব "human".
KEEP = {"সরল", "চলতি", "খাবার", "মানব", "বের", "সহ", "বন"}
# Forms of verbs the pack has without a table: কাটা's, which the rules for such verbs (verb_forms) would make wrongly.
BENGALI_FORMS.update({f: "কাটা" for f in "কেটে কেটেছে কেটেছি কেটেছেন কেটেছিল কেটেছিলাম".split()})
BENGALI_FORMS.update({f: "বেরোনো" for f in "বেরিয়ে বেরোয় বেরোতে বেরিয়েছে বেরিয়েছি বেরিয়েছিল বেরোচ্ছে বেরোলো বেরোল বেরোবে বেরোবেন বেরোন বেরোই".split()})
# বাড়া "to grow", whose table the pack gives as বাড়ানো's.
BENGALI_FORMS.update({f: "বাড়া" for f in "বাড়ে বাড়ছে বাড়ছিল বেড়ে বেড়েছে বেড়েছিল বাড়বে বাড়তে বাড়ল বাড়লো বাড়েনি".split()})
BENGALI_FORMS.update({"দে": "দেওয়া", "ফিরিয়ে": "ফেরানো", "জড়িয়ে": "জড়ানো", "এগিয়ে": "আগানো", "পৌঁছে": "পৌঁছানো"})
# Links of the pack's tables that are wrong: হে "O!" is no form of সে, মণ "maund" none of মন, পা "foot" none of পাওয়া.
WRONG_LINKS = {("হে", "সে"), ("মণ", "মন"), ("পা", "পাওয়া"), ("মারা", "মা"), ("নর", "নারী"), ("নারী", "নর"), ("হাসান", "হাসানো")}
# Corpus spellings that are no words: letters, the genitive ending written apart after a number, foreign words.
NOT_WORDS = {"আল", "আলী", "আলি", "চৌধুরী", "চৌধুরি", "ইন", "র", "ই", "টি", "টা", "গুলো", "দের", "ের", "সা", "ড", "মো", "জি", "এস", "বি", "সি", "ডি"}
# The person endings a present form ends in, which the negative -নি joins (করিনি, করেনি, যায়নি, করেননি, যাইনি).
PERSON_ENDINGS = ("ি", "ে", "ো", "ন", "য়", "ও", "ই")


def vowel_final(word):
    return word[-1] in VOWEL_SIGNS or word[-1] in VOWELS


def nominal_forms(word):
    """A noun's (or adjective's, number's) case, classifier and plural forms, by its last letter."""
    tails = VOWEL_TAILS if vowel_final(word) else CONSONANT_TAILS
    # After -i the glide য় joins no ending (বাড়ির, বাড়িতে; বাড়িয়ে is বাড়ানো's).
    if word[-1] in "িী":
        tails = [t for t in tails if not t.startswith("য়")]
    forms = {word + t for t in tails}
    for c in CLASSIFIERS:
        forms |= {word + c + t for t in CLASSIFIER_CASES}
    return forms


def verb_forms(word, table):
    """
    A verb's forms its table lacks: its verbal noun's cases (করার, যাওয়ার), the negative of its present (করিনি, যায়নি), the
    spellings with a final -ো (করলো, হতো, যাবো) and without it, and the imperatives of a consonant stem (করুন, দেখো, করুক).
    """
    forms = {word + t for t in VERBAL_NOUN_TAILS}
    for variant in (word[:-len("ওয়া")] + "য়া",) if word.endswith("ওয়া") else ():
        forms |= {variant} | {variant + t for t in VERBAL_NOUN_TAILS}
    for form in table:
        # The future stem's genitive, the infinitive's (হবার, যাবার, করবার).
        if form.endswith("বে"):
            forms.add(form[:-1] + "ার")
        if form.endswith(PERSON_ENDINGS):
            forms.add(form + "নি")
        if form[-1] in "লতব":
            forms.add(form + "ো")
        elif form.endswith("ো") and form[-2] in "লতব":
            forms.add(form[:-1])
        if form.endswith("ি") and form[:-1] + "ে" in table and not vowel_final(form[:-1]):
            stem = form[:-1]
            forms |= {stem + "ুন", stem + "ো", stem + "ুক", stem + "িস"}
    return forms


# A verb's forms by its stem (কাট of কাটা, পৌঁছা of পৌঁছানো), for a verb the pack has no table for.
STEM_TAILS = ["ি", "ে", "ো", "েন", "িস", "ছি", "ছে", "ছ", "ছেন", "ছিল", "ছিলাম", "ছিলেন", "ল", "লো", "লাম", "লেন", "লে", "ব", "বো",
              "বে", "বেন", "বি", "তে", "ত", "তো", "তাম", "তেন", "ুন", "ুক"]
VOWEL_STEM_TAILS = ["ই", "য়", "ও", "ন", "স", "চ্ছি", "চ্ছে", "চ্ছ", "চ্ছেন", "চ্ছিল", "ল", "লো", "লাম", "লেন", "লে", "ব", "বো", "বে",
                    "বেন", "তে", "ত", "তো", "তাম", "তেন"]


def untabled_verb_forms(word):
    """A verb's present, past, future and infinitive forms by rule, when the pack has no table for it."""
    if word.endswith("ানো"):
        stem = word[:-2]
        return {stem + t for t in VOWEL_STEM_TAILS} | {word[:-3] + "িয়ে", word[:-3] + "িয়েছে"}
    if word.endswith("া") and len(word) > 2 and not vowel_final(word[:-1]):
        stem = word[:-1]
        return {stem + t for t in STEM_TAILS}
    return set()


def lexicon(dictionaries):
    """The pack's words by kind, its forms tables (form -> words), and the words that are only names."""
    db = sqlite3.connect(os.path.join(dictionaries, "bn-en.sqlite"))
    kinds = defaultdict(set)
    names = set()
    for word, pos, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        if " " in word or "-" in word:
            continue
        # A language's name is an everyday word (বাংলা); other names are left out.
        if pos == "name" and any("language" in g for s in json.loads(senses) for g in s.get("glosses", [])):
            pos = "noun"
        if pos == "name":
            names.add(word)
        elif pos in WORD_KINDS and word not in NOT_WORDS:
            kinds[word].add(pos)
    for word, kind in BENGALI_WORDS.items():
        kinds[word].add(kind)
    table = defaultdict(set)
    for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        # The pronouns' tables (and the demonstratives', which file এটা, এটি, এগুলো under one another) are BENGALI_FORMS'.
        if (" " in lemma or "-" in lemma or lemma not in kinds or form == lemma or (form, lemma) in WRONG_LINKS
                or lemma in PRONOUNS or form in BENGALI_FORMS):
            continue
        table[form].add(lemma)
    return kinds, table, names - set(kinds)


NOMINAL_KINDS = {"noun", "adj", "num", "classifier", "det"}


def word_forms(word, ks, table_of):
    forms = set()
    if ks & NOMINAL_KINDS:
        forms |= nominal_forms(word)
    if "verb" in ks:
        forms |= verb_forms(word, table_of.get(word, ())) if word in table_of else untabled_verb_forms(word) | verb_forms(word, ())
    return forms


def tail_cost(word, form):
    """How many endings [form] adds to [word]: none, a case or plural ending, or a classifier and its case (two)."""
    if not form.startswith(word):
        return 1
    tail = form[len(word):]
    return 0 if not tail else 2 if tail.startswith(tuple(CLASSIFIERS)) else 1


def with_clitics(forms):
    """The forms with the clitics -ই and -ও, which after a consonant is also written -ো (আবারো, এখনো)."""
    return set(forms) | {f + c for f in forms for c in CLITICS} | {f + "ো" for f in forms if not vowel_final(f)}


def unclitic(form):
    """The form without the clitic -ই or -ও it may end in (দেশেই, আমিও, যাবেই)."""
    return {form[:-1]} if len(form) > 2 and (form[-1] in "ইও" or form[-1] == "ো" and not vowel_final(form[:-1])) else set()


def build(found, size, label):
    """The list: the pack's words, by their forms' counts in [found]."""
    dictionaries = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "dictionaries")
    kinds, table, names = lexicon(dictionaries)
    counts = defaultdict(float)
    for _, source, _ in found:
        total = sum(source.values())
        for form, n in source.items():
            form = unicodedata.normalize("NFC", form)
            if BENGALI_LETTERS.fullmatch(form) and not STRAY_JOINER.search(form):
                counts[form] += n / total / len(found)

    table_of = defaultdict(set)
    for form, lemmas in table.items():
        for lemma in lemmas:
            table_of[lemma].add(form)
    # A verb's forms (its table's and the rules') and a noun's, adjective's or pronoun's (their tables' and the rules'),
    # apart: form -> words.
    verbal, nominal = defaultdict(set), defaultdict(set)

    def add(word, as_word, ks):
        made = word_forms(word, ks, table_of)
        nominal_made = nominal_forms(word) if ks & NOMINAL_KINDS else set()
        tabled = table_of.get(as_word, set()) if word == as_word else {word}
        for form in with_clitics(made | tabled):
            if form in counts and form != as_word:
                if "verb" in ks and (form not in with_clitics(nominal_made) or form in tabled - nominal_made):
                    verbal[form].add(as_word)
                else:
                    nominal[form].add(as_word)

    for word, ks in kinds.items():
        add(word, word, ks)
    for variant, word in SPELLINGS.items():
        if word in kinds:
            add(variant, word, kinds[word])

    def candidates(form):
        """(the word the form is itself, or None; the other words it is a form of)."""
        if form in BENGALI_FORMS:
            return None, {BENGALI_FORMS[form]}
        # A name's spellings are no words (বাংলাদেশ), unless a table has them as a word's form too (পারি, Paris, of পারা).
        if form in NOT_WORDS or form in names and not table.get(form):
            return None, set()
        own = form if form in kinds else None
        if own in KEEP:
            return own, set()
        for base in unclitic(form) if not own else ():
            if base in BENGALI_FORMS:
                return None, {BENGALI_FORMS[base]}
        verbs = verbal.get(form, set()) - {form}
        # A word of its own is taken for a verb's form (করে of করা, হয় of হওয়া), never for a noun's or adjective's (পানি
        # "water", not পা + নি; মাটি "soil", not মা + টি). A noun's form goes to the words it is the fewest endings away
        # from (মাটিতে is মাটি + তে, not মা + টি + তে).
        if own:
            return own, verbs
        nouns = nominal.get(form, set()) - {form}
        if nouns:
            fewest = min(tail_cost(w, form) for w in nouns)
            nouns = {w for w in nouns if tail_cost(w, form) == fewest}
        others = verbs | nouns
        if not others:
            for base in unclitic(form):
                if base in BENGALI_FORMS:
                    return None, {BENGALI_FORMS[base]}
                if base in kinds:
                    return None, {base}
        return own, others

    found_heads = {}
    for form in counts:
        found_heads[form] = candidates(form)
    # How common each word is by its forms of no other word.
    alone = defaultdict(float)
    for form, (own, others) in found_heads.items():
        heads = ({own} if own else set()) | others
        if len(heads) == 1:
            alone[next(iter(heads))] += counts[form]
    scores = defaultdict(float)
    forms = defaultdict(dict)
    for form, share in counts.items():
        own, others = found_heads[form]
        if own and others and max(alone.get(o, 0.0) for o in others) <= share:
            heads = {own}
        else:
            heads = others or ({own} if own else set())
        if not heads:
            continue
        weights = {h: alone.get(h, 0.0) for h in heads}
        whole = sum(weights.values())
        for head in heads:
            part = weights[head] / whole if whole else 1 / len(heads)
            if part <= 0:
                continue
            scores[head] += share * part
            forms[head][form] = forms[head].get(form, 0.0) + share * part

    ranked = sorted(scores, key=lambda w: -scores[w])[:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: -forms[word][f])[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label
