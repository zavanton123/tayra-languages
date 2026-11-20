"""
The Bengali frequency list (build_frequency_list.py --language bn).

Bengali writes a noun with its case ending, classifier and plural joined on (দেশের, বইটা, ছেলেরা, বাড়িতে) and the clitics
-ই "only" and -ও "also" after them (দেশেও, আমিই). The pack's tables give the verbs' forms (করি, করছে, করেছিলেন) and some
nouns' and pronouns', but most nouns have none, nor does simplemma know Bengali. So each word of the pack is declined here by
its ending, and a corpus form is counted for the words it is a form of: by hand (BENGALI_FORMS: the pronouns, spellings the
pack lacks), by the pack's tables, or by these rules. A form that is a word of the pack itself keeps its count (থেকে
"from", not থাকা's "having stayed"), unless the word it is also a form of is more common than the form (করে goes to করা).
A form of several words is shared between them by how common each is by its forms of no other word.

A form no word of the pack explains may be a word's other spelling (variants: শ্রেণি, শ্রেণির of শ্রেণী), a slip of a form
spelled right (SLIPS: আক্রমন of আক্রমণ), or the form of a word the pack lacks, which the corpus shows (induce: চারপাশ,
চারপাশে; ভিড়); names and English of names are left out by hand (INDUCED_NOT_WORDS).
"""
import heapq
import json
import math
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
# A name's gloss that makes it a word: a month's (বৈশাখ "the first month of the Bengali calendar") or a festival's.
OCCASION = re.compile(r"\bmonth\b|\bEid\b|Christmas|festival", re.I)

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
    "উনি": "ওঁর ওঁকে ওনার ওনাকে উনার উনাকে",
    "উনারা": "উনাদের উনাদেরকে ওনারা ওনাদের",
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
    # যাওয়া's future imperative, made on the participle যেয়ে its table lacks.
    "যেয়ো": "যাওয়া", "যেও": "যাওয়া",
    # Spoken and dialect forms (দিছে for দিয়েছে, আমারে for আমাকে, এইডা for এটা), and মা's and লোক's plurals the rules lack.
    "দিছে": "দেওয়া", "দিছি": "দেওয়া", "দিমু": "দেওয়া", "করবা": "করা", "যাইতে": "যাওয়া", "হয়া": "হওয়া",
    "হইছে": "হওয়া", "এইডা": "এটা", "ঐটা": "ওটা", "ওইসব": "ওসব", "তুমার": "তুমি", "তোমারে": "তুমি", "আমারে": "আমি",
    "তোরে": "তুই", "আমারা": "আমরা", "ক্যামন": "কেমন", "মায়েরা": "মা", "মায়েদের": "মা", "লোকেদের": "লোক",
    "খুবি": "খুব", "নিজেরে": "নিজে", "তুমিতো": "তুমি", "এটাতো": "এটা", "সেতো": "সে", "আসতেছি": "আসা", "করতেছি": "করা",
    "পাইছি": "পাওয়া", "খাইছে": "খাওয়া", "হইসে": "হওয়া", "হইতাছে": "হওয়া", "পারবা": "পারা", "আমাগো": "আমরা",
    "কিন্তুু": "কিন্তু", "তাহাদের": "তারা", "ধইরা": "ধরা", "করমু": "করা", "যামু": "যাওয়া", "যাবা": "যাওয়া",
    "খাইয়া": "খাওয়া", "দেইখা": "দেখা", "এইগুলো": "এগুলো",
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
    # Two spellings of one word that the pack has as two words: the less common folded into the other.
    "ভাল": "ভালো", "ভালবাসা": "ভালোবাসা", "ঘন্টা": "ঘণ্টা", "ইসলামি": "ইসলামী", "দূর্ঘটনা": "দুর্ঘটনা", "বাঙ্গালী": "বাঙালি",
    "জগৎ": "জগত", "পুরোনো": "পুরনো", "গুন": "গুণ", "প্লীজ": "প্লিজ", "সাক্ষি": "সাক্ষী", "আঙ্গুল": "আঙুল", "দেশিয়": "দেশীয়",
    "মুরগী": "মুরগি", "পল্লি": "পল্লী", "বাদামি": "বাদামী", "পেশি": "পেশী", "গন্ডার": "গণ্ডার", "বেগুনী": "বেগুনি",
    "হাঙ্গর": "হাঙর", "রেলগাড়ী": "রেলগাড়ি", "ডিকশনারী": "ডিকশনারি", "ডাঙ্গা": "ডাঙা", "আমলকি": "আমলকী",
    "জিন্দেগি": "জিন্দেগী", "পেশিবহুল": "পেশীবহুল", "অকুল": "অকূল", "ফনী": "ফণী", "হাবশি": "হাবশী", "হরিত": "হরিৎ",
    "পাখী": "পাখি", "কুমীর": "কুমির", "ফকীর": "ফকির", "এমনকী": "এমনকি",
    "তাইলে": "তাহলে",
}
# Everyday words the pack lacks, with their kind as the rules decline them.
BENGALI_WORDS = {
    "আছে": "verb", "নেই": "adv",
    "তাহলে": "conj", "যেখানে": "adv", "নাকি": "particle", "এসব": "det", "সেসব": "det", "যেসব": "det", "এইসব": "det",
    "এমনকি": "adv", "একা": "adj", "যত": "adv", "তত": "adv", "তেমন": "adj", "এরকম": "adj", "যেভাবে": "adv", "ওসব": "det",
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
    # Words a shorter word's rule forms would take (দায় as দা + য়, আদায় as আদা + য়).
    "দায়": "noun", "আদায়": "noun", "বসতি": "noun", "কলেরা": "noun", "সরি": "intj", "শো": "noun",
}
# Words of their own that a verb's form is spelled as, which keep their count however common the verb is: সরল "simple" (not
# সরা's), চলতি "current", খাবার "food" (not খাওয়া's "to eat"), মানব "human".
KEEP = {"সরল", "চলতি", "খাবার", "মানব", "বের", "সহ", "বন", "বসতি",
        # বেলুন "balloon" and ফোঁটা "drop", not বেলা's and ফোটা's forms; বাড়ানো "to raise", a verb of its own, not বাড়া's.
        "বেলুন", "ফোঁটা", "বাড়ানো"}
# Forms of verbs the pack has without a table: কাটা's, which the rules for such verbs (verb_forms) would make wrongly.
BENGALI_FORMS.update({f: "কাটা" for f in "কেটে কেটেছে কেটেছি কেটেছেন কেটেছিল কেটেছিলাম".split()})
BENGALI_FORMS.update({f: "বেরোনো" for f in "বেরিয়ে বেরোয় বেরোতে বেরিয়েছে বেরিয়েছি বেরিয়েছিল বেরোচ্ছে বেরোলো বেরোল বেরোবে বেরোবেন বেরোন বেরোই".split()})
# বাড়া "to grow", whose table the pack gives as বাড়ানো's.
BENGALI_FORMS.update({f: "বাড়া" for f in "বাড়ে বাড়ছে বাড়ছিল বেড়ে বেড়েছে বেড়েছিল বাড়বে বাড়তে বাড়ল বাড়লো বাড়েনি".split()})
BENGALI_FORMS.update({"বসে": "বসা", "দে": "দেওয়া", "ফিরিয়ে": "ফেরানো", "জড়িয়ে": "জড়ানো", "এগিয়ে": "আগানো", "পৌঁছে": "পৌঁছানো"})
# Links of the pack's tables that are wrong: হে "O!" is no form of সে, মণ "maund" none of মন, পা "foot" none of পাওয়া.
WRONG_LINKS = {("হে", "সে"), ("মণ", "মন"), ("পা", "পাওয়া"), ("মারা", "মা"), ("নর", "নারী"), ("নারী", "নর"), ("হাসান", "হাসানো")}
# Corpus spellings that are no words: letters, the genitive ending written apart after a number, foreign words.
NOT_WORDS = {"রা", "আল", "আলী", "আলি", "চৌধুরী", "চৌধুরি", "ইন", "র", "ই", "টি", "টা", "গুলো", "দের", "ের", "সা", "ড", "মো", "জি", "এস", "বি", "সি", "ডি"}
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
    # A last ৎ is ত before a vowel (বিদ্যুতের, বিদ্যুতে; বিদ্যুৎকে).
    forms = {(word[:-1] + "ত" if word.endswith("ৎ") and t[0] in VOWEL_SIGNS else word) + t for t in tails}
    for c in CLASSIFIERS:
        forms |= {word + c + t for t in CLASSIFIER_CASES}
    # জন ends in a consonant: দুজনের, দুজনে, দুজনেই.
    forms |= {word + "জন" + t for t in CONSONANT_TAILS}
    return forms


def verb_forms(word, table, words=()):
    """
    A verb's forms its table lacks: its verbal noun's cases (করার, যাওয়ার), the negative of its present (করিনি, যায়নি), the
    spellings with a final -ো (করলো, হতো, যাবো) and without it, the imperatives of a consonant stem (করুন, দেখো, করুক), and
    the future imperative made on the participle (পেয়ো of পেয়ে, থেকো of থেকে), unless that is a word of [words] (তুলো
    "cotton", not তুলে's).
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
        if form.endswith("ে") and form + "ছে" in table and form[:-1] + "ো" not in words:
            forms.add(form[:-1] + "ো")
    return forms


# A verb's forms by its stem (কাট of কাটা, পৌঁছা of পৌঁছানো), for a verb the pack has no table for.
STEM_TAILS = ["ি", "ে", "ো", "েন", "িস", "ছি", "ছে", "ছ", "ছেন", "ছিল", "ছিলাম", "ছিলেন", "ল", "লো", "লাম", "লেন", "লে", "ব", "বো",
              "বে", "বেন", "বি", "তে", "ত", "তো", "তাম", "তেন", "ুন", "ুক"]
VOWEL_STEM_TAILS = ["ই", "য়", "ও", "ন", "স", "চ্ছি", "চ্ছে", "চ্ছ", "চ্ছেন", "চ্ছিল", "ল", "লো", "লাম", "লেন", "লে", "ব", "বো", "বে",
                    "বেন", "তে", "ত", "তো", "তাম", "তেন"]


# The perfective's tails, on a consonant stem (কেটে, কেটেছে) and on a causative's (-িয়ে: পাঠিয়ে, পাঠিয়েছিল).
PERFECTIVE_TAILS = ["ে", "েছে", "েছেন", "েছি", "েছ", "েছো", "েছিস", "েছিল", "েছিলো", "েছিলাম", "েছিলেন", "েছিলে"]
# A stem's first vowel as the perfective (and, after ো and ে, most forms) raise it: কাটা কেটে, খোলা খুলে খুলছে, লেখা লিখে
# লিখছে, ওঠা উঠে.
RAISED = {"া": "ে", "ো": "ু", "ে": "ি", "আ": "এ", "ও": "উ", "এ": "ই"}


def first_vowel(stem):
    """Where [stem]'s first vowel is written, if RAISED raises it (not the inherent vowel of কর, বল, nor ু of ঘুম), else None."""
    if stem[0] in VOWELS:
        return 0 if stem[0] in RAISED else None
    i = 1
    while i < len(stem) and stem[i] in "\u09bc\u09cd":
        i += 1 if stem[i] == "\u09bc" else 2
    return i if i < len(stem) and stem[i] in RAISED else None


def raised(stem):
    """[stem] with its first vowel raised (RAISED)."""
    i = first_vowel(stem)
    return stem if i is None else stem[:i] + RAISED[stem[i]] + stem[i + 1:]


def untabled_verb_forms(word):
    """A verb's present, past, future, infinitive and perfective forms by rule, when the pack has no table for it."""
    if word.endswith("ানো"):
        stem = word[:-2]
        base = word[:-3]
        # A causative raises ো and ে only (মেলানো মিলিয়ে, ফেরানো ফিরিয়ে; পাঠানো পাঠিয়ে).
        bases = {base, raised(base)} if first_vowel(base) is not None and base[first_vowel(base)] in "োেওএ" else {base}
        perfective = {b + "িয়" + t for b in bases for t in PERFECTIVE_TAILS}
        return {stem + t for t in VOWEL_STEM_TAILS} | perfective
    if word.endswith("া") and len(word) > 2 and not vowel_final(word[:-1]):
        stem = word[:-1]
        high = raised(stem)
        forms = {stem + t for t in STEM_TAILS} | {high + t for t in PERFECTIVE_TAILS}
        # After ো and ে the raised stem goes with every tail but -ে, -ো, -েন (খুলি, খুলছে, খুলবে; খোলে, খোলো).
        if high != stem and stem[first_vowel(stem)] in "োেওএ":
            forms |= {high + t for t in STEM_TAILS if t not in ("ে", "ো", "েন")}
        return forms
    return set()


def lexicon(dictionaries):
    """The pack's words by kind, its forms tables (form -> words), and the words that are only names."""
    db = sqlite3.connect(os.path.join(dictionaries, "bn-en.sqlite"))
    kinds = defaultdict(set)
    names = set()
    for word, pos, senses in db.execute("SELECT word_lc, pos, senses FROM entries"):
        if " " in word or "-" in word:
            continue
        # A language's name is an everyday word (বাংলা), and so is a month's or a festival's (বৈশাখ, ঈদ, বড়দিন "Christmas");
        # other names are left out.
        if pos == "name" and any("language" in g or OCCASION.search(g) for s in json.loads(senses) for g in s.get("glosses", [])):
            pos = "noun"
        if pos == "name":
            names.add(word)
        elif pos in WORD_KINDS and word not in NOT_WORDS:
            kinds[word].add(pos)
    for word, kind in BENGALI_WORDS.items():
        kinds[word].add(kind)
    # A spelling the pack has as a word of its own is folded into the word it spells (ভাল into ভালো), its table too.
    folded = {variant: word for variant, word in SPELLINGS.items() if variant in kinds and word in kinds}
    for variant in folded:
        del kinds[variant]
    table = defaultdict(set)
    for form, lemma in db.execute("SELECT form_lc, lemma FROM forms"):
        lemma = folded.get(lemma, lemma)
        # The pronouns' tables (and the demonstratives', which file এটা, এটি, এগুলো under one another) are BENGALI_FORMS'.
        if (" " in lemma or "-" in lemma or lemma not in kinds or form == lemma or (form, lemma) in WRONG_LINKS
                or lemma in PRONOUNS or form in BENGALI_FORMS):
            continue
        table[form].add(lemma)
    return kinds, table, names - set(kinds)


NOMINAL_KINDS = {"noun", "adj", "num", "classifier", "det"}


def word_forms(word, ks, table_of, words=()):
    forms = set()
    if ks & NOMINAL_KINDS:
        forms |= nominal_forms(word)
    if "verb" in ks:
        forms |= (verb_forms(word, table_of.get(word, ()), words) if word in table_of
                  else untabled_verb_forms(word) | verb_forms(word, (), words))
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


def variants(kinds, made):
    """
    Other spellings of the pack's words that texts write: variant -> word. Each word written with ি or ী in the other
    letter (শ্রেণি of শ্রেণী, দাদি of দাদী, বাড়ী of বাড়ি), as the spelling reform and older texts write it, and a verb
    with its first vowel raised as Bangladesh writes it (বুঝা of বোঝা, উঠানো of ওঠানো). Not when the variant is a word of
    the pack, a spelling or form of SPELLINGS and BENGALI_FORMS, or another word's form ([made]: form -> words, as জমির,
    জমি's genitive, is no spelling of জমীর "heart"); a word with both letters has none.
    """
    out = defaultdict(set)
    for word, ks in kinds.items():
        spelled = {other_i(word)} if other_i(word) else set()
        at = first_vowel(word) if "verb" in ks else None
        if at is not None and word[at] in "োেওএ":
            spelled.add(raised(word))
        for variant in spelled:
            if (variant not in kinds and variant not in SPELLINGS and variant not in BENGALI_FORMS
                    and not made.get(variant, set()) - {word}):
                out[variant].add(word)
    return {variant: next(iter(words)) for variant, words in out.items() if len(words) == 1}


# Letters texts write for one another, by slip or habit: ন for ণ (আক্রমন), ু for ূ (রুপ), ি for ী, ত for ৎ (বিদ্যুত),
# স্ট for ষ্ট (পোস্ট), ংক for ঙ্ক (আতংক), no chandrabindu (খুজে), অ + া for আ (অামি), and a visarga for a colon after a
# word (বলেছেনঃ).
SLIPS = [("অা", "আ"), ("ণ", "ন"), ("ূ", "ু"), ("ী", "ি"), ("ঈ", "ই"), ("ঊ", "উ"), ("ৎ", "ত"), ("ষ্ট", "স্ট"), ("ঙ্ক", "ংক"),
         ("ঙ্গ", "ংগ"), ("ঁ", "")]


def slipped(form):
    """[form] as written with every slip of SLIPS, the one spelling of all its slips."""
    for a, b in SLIPS:
        form = form.replace(a, b)
    return form[:-1] if form.endswith("ঃ") else form


def slip_of(form, right):
    """
    Whether [form] is [right] written with slips (SLIPS): either way for the vowels, ৎ and ষ্ট (ভুল and ভূল, পোস্ট and
    পোষ্ট), but only ন for ণ, no chandrabindu for one, and অ + া for আ (করুণ "pitiful" is no slip of করুন).
    """
    return (form != right and slipped(form) == slipped(right) and form.count("অা") >= right.count("অা")
            and form.count("ণ") <= right.count("ণ") and form.count("ঁ") <= right.count("ঁ"))


def other_i(word):
    """[word] with ি for ী or ী for ি, if it has only one of them."""
    for a, b in (("ী", "ি"), ("ি", "ী")):
        if a in word and b not in word:
            return word.replace(a, b)
    return None


# A word the corpus has and the pack lacks (induce): a noun (or adjective) when written bare INDUCED_BARE times (of the
# corpus's words), or INDUCED_LONE times when the corpus has it in no other form; a verb when INDUCED_FORMS of its telling
# forms by rule, which no noun has (a participle, a progressive, a future, an infinitive), are written INDUCED_VERB times in
# all. At least INDUCED_LENGTH letters and signs (not এম, বি).
INDUCED_BARE = 1e-6
INDUCED_LONE = 4e-6
INDUCED_FORMS = 3
INDUCED_VERB = 2e-6
INDUCED_LENGTH = 3
# The tails that make a verb's telling forms, on a consonant stem (জমছে, জমতে, জমবে) and a causative's (পাল্টিয়ে, পাল্টাচ্ছে).
STEM_TELLING = ("ছে", "ছেন", "ছিল", "ছিলেন", "ছিলাম", "তে", "বে", "বেন", "লো", "লেন", "লাম", "তাম", "তেন", "েছে", "েছেন", "েছিল")
CAUSATIVE_TELLING = ("িয়ে", "িয়েছে", "িয়েছেন", "িয়েছিল", "াচ্ছে", "াচ্ছেন", "াচ্ছিল", "াতে", "াবে", "াবেন", "ালো", "ালেন",
                     "ানো", "ানোর")
# A form ending so is a verb's, no noun's.
NOT_NOUN = ("িয়ে", "েছে", "েছেন", "েছিল", "েছিলেন", "চ্ছে", "চ্ছেন", "চ্ছিল", "ছেন", "ছিলেন", "ছিলাম", "বেন", "লেন", "তেন",
            "লাম", "তাম")
# Names, English words of names (অফ, দ্য) and abbreviations the corpus has that the pack does not: no words for induce. Their
# forms (হাসিনার, ফেসবুকে) are theirs.
INDUCED_NOT_WORDS = set("""
বিএনপি আহমেদ সৌদি মোহাম্মদ আওয়ামী নিউ আবু এন্ড ইয়র্ক অ্যান্ড জামায়াত জর্জ হযরত আবুল আমিন ইন্টারন্যাশনাল টাইমস ওবামা
ন্যাশনাল টুইটার ইউটিউব বিবিসি দ্য দ্যা ওয়া উদ্দিন মাহমুদ আবদুল মির্জা জেমস মুজিবুর তারেক রিয়াল ইউপি নারায়ণগঞ্জ এইচ রানা
টাঙ্গাইল মুম্বাই মোঃ হাসিনা হাসিনার আওয়ামীলীগ মাইকেল নিউইয়র্ক আবদুল্লাহ সেলিম ইউনাইটেড চাঁদপুর মাওলানা মেসি মায়ানমার
পিটার জামাত হোয়াইট কোরআন মাদ্রিদ রবার্ট আক্তার ইসরাইল ওবায়দুল সাল্লাল্লাহু ইন্ডিজ বার্সেলোনা গাজা ফখরুল ডটকম ফরিদপুর জ্যাক
ডাঃ কিম ফেনী কিং স্টেট সুনামগঞ্জ গ্রেট আলাইহি রাঃ মুহাম্মাদ রাইট বিজিবি ছাত্রদল নর্থ মিরপুর রাসূল ওয়েস্ট ওয়ার্ল্ড সান ইবন
ওয়ান ম্যান রেড ব্ল্যাক ইয়া গাজীপুর হুমায়ূন ইসি আইসিসি দুদক আইপিএল ইউনেস্কো নাসা আইএস বিডি ইউএস এমএনএ সাউথ রাজীব হেনরি
শর্মা আসাদ ফেইসবুক হাজী ব্লু বেঙ্গল সেন্ট্রাল মেরি রুবেল এক্সপ্রেস দুবাই শামীম শাকিব সিদ্দিকী জর্ডান জাকির কাপুর লিটন
লক্ষ্মীপুর জিয়াউর নরেন্দ্র তামিম মুফতি ডেইলি জামালপুর কার্ল নাটোর সিরাজগঞ্জ শেরপুর গোপালগঞ্জ সানি রাসূলুল্লাহ নুরুল
রাজবাড়ী বাবুল লাহোর স্টিভ মোশাররফ করাচি টনি ইস্ট স্মিথ দিল্লী ফিলিপ রিও চাঁপাইনবাবগঞ্জ জেরুজালেম গাইবান্ধা থ্রি মাইক্রোসফট
কিশোরগঞ্জ মেহেদী মার্টিন হায়দার জনি অ্যাপল হবিগঞ্জ মাশরাফি ক্রিস মাহবুব মুসা হোসাইন নীলফামারী খালিদ শাহজালাল রোনালদো উত্তরা
আব্দুর নাসিম নিক ফ্রাঙ্ক মুহম্মদ রিজভী জলিল অজয় রাজু আবদুর রফিক কোহলি মাগুরা আশরাফুল মঞ্জু খাতুন হাফিজ আহমাদ বরগুনা
এডওয়ার্ড মেহেরপুর ঝালকাঠি শামসুল বাশার সঞ্জয় এরিক জেন মুন্সীগঞ্জ আল্লামা আর্থার সজীব ওয়েন নেইমার ব্রায়ান মুন টোয়েন্টি
লিটল হিউম্যান গোল্ডেন গ্রিন বিগ আল্লাহ্‌ আল্লাহ্‌র আল্লাহ্ আল্লা হুসাইন বিসিবি ফক্স ম্যানচেস্টার টোকিও জোসেফ সনি সিনহা
মানিকগঞ্জ তিব্বত প্র আলিয়া সিলভা মাউন্ট টেক্সাস হোমস সিনা মান্না গীতা কেভিন শাহজাহান মন্ডল রাজ্জাক স্যামসাং উল্লাহ বুখারী
আলিম আমর সুনীল বারাক মিয়া মিয়ার পটুয়াখালী বিসিএস হালিম ভিক্টর কিউ পিপলস ড্যানিয়েল রিয়েল রনি মিশেল নরসিংদী নিকোলাস
ব্যানার্জী লালমনিরহাট নিউটন চার্লি উইথ জাকার্তা স্টিফেন সারাহ ক্যাটরিনা নাজমুল ভিক্টোরিয়া হাকিম ফরিদ স্কট আওয়ার ববি আনিসুল
রণবীর জাহিদ হুসেইন জিবি ম্যাক্স জ্যাকসন মল্লিক মান্নান ফ্রান্সিস নিও সিসি মাসুম জব্বার বেনাপোল মিসর কোরান ত্রিনিদাদ সাদিক
ডিন ফজলুল রফিকুল উমা আরা ডেনিস জাজিরা চোপড়া এলিজাবেথ অপু নাসের ব্রাউন কাসেম মেলবোর্ন বিএসএফ নড়াইল মাদারীপুর কায়েদা এএফসি
মরক্কো আইনস্টাইন শিকাগো রাঙ্গামাটি আলহাজ্ব সাঃ পিরোজপুর সৌরভ আলাউদ্দিন মোহাম্মাদ প্রিয়া সাব্বির পটার টুডে দীপিকা দোহা মিরাজ
অভিজিৎ রিক য়ে কর্ণফুলী স্যাম আমিনুল ওয়াজেদ সামাদ লিওনেল ইনু ডিএমপি রজার প্রিয়াঙ্কা শরীয়তপুর চুয়াডাঙ্গা আজিজুল মনিরুল
রিয়া ইভান মিজানুর মিমি রঞ্জিত কালীগঞ্জ মুরসি ইইউ স্টুয়ার্ট শ্রীমঙ্গল মির্জাপুর ফাই ওয়ে কুইন ফাইভ লাস্ট আরিফুল আকরাম ম্যাক
সিদ্ধার্থ গ্রে আমজাদ সাইমন মিনা রউফ মালেক জিমি ম্যারি সিইসি হুসেন তারিক আবেদীন বিবেকানন্দ ডেল ভুট্টো মিলস ডিবি মুশফিক সাংহাই
যুবদল টেকনাফ রায়পুর মেহের কঙ্গো লরা নাইন সেভেন ফোর সিক্স নওয়াজ তোফায়েল রবিন তাআলা সুভাষ বিপিএল রুহুল জামান এনামুল সালাহ
এমা পটিয়া তায়ালা মাহমুদুর হিলারি রুনি টমাস হান্নান রামু মনু মতিউর বকর সাত্তার সৈয়দপুর ক্যাথরিন হায়াত লাস আসসালামু শাহ্
সালাহউদ্দিন গৌরীপুর নাহার রুমি জসিম কোলকাতা খাঁ কাও বাচ্চু বচ্চন শওকত তাহের লিয়াকত রেহমান গোমেজ মতিঝিল রামপাল গ্রামীণফোন
নাছির জ্যাকব কুলাউড়া নেলসন ড্যান রাজাপুর খলিল জোন্স রিপন সিজার ঐশ্বরিয়া রাঙামাটি মোহাম্মদপুর অ্যান্ডি আবূ মশিউর লিও এডি
ব্র্যাক জ্যামাইকা সালাউদ্দিন আনু দুলাল সাইদ আলতাফ লাইগা ওয়েলস প্যাট্রিক আতাউর টেড নূরুল রাফায়েল চকরিয়া হান এরদোগান
ওয়ালটন লিন রঞ্জন উইমেন ইডেন ঢাবি উমর পলি ফুলবাড়ী বার্ন ক্রুজ রাইটস মহেশখালী জাসদ ডিউক খিত জাস্টিন বিডিআর সুজন অ্যালেক্স
দৌলতপুর তানিয়া টেন বুশ জেফ আনাস অ্যালান ইমন জুলিয়ান কিংস স্টিভেন ওয়াং সিরাজুল গর্ডন ইবি মোশারফ নোমান চেলসি ম্যাট জেএমবি
মওদুদ সিএনএন শহীদুল্লাহ জুলফিকার শেখর সিআইএ জহির নিসা স্টার্ক আহলে সাং সিয়েরা ইউকে কামরান জেদ্দা মুস্তাফা কাপ্তাই ফিতর শচীন
ইভা টিপু রায়ান কাবা টোবাগো তপন রাব্বি আইরিন ওয়াকা হাবিবুর ভিনসেন্ট আন্দ্রে বোস্টন ইউনিসেফ মেনন ফ্রান্সিসকো জায়েদ এফবিআই
গেইল রিমা গনি কেরি শ্রীপুর ভালুকা ক্লিনটন আর্সেনাল ছাতক তিমুর রাউল এথেন্স লাদেন ওয়ার্নার সিউল হাটহাজারী মজিদ হেলাল প্রণব
বেঞ্জামিন পাবলো ওয়ার্ডপ্রেস জীবনানন্দ জগন্নাথপুর বেনজীর অ্যা ওয়াহিদ হাওয়ার্ড আলবার্ট তসলিমা জেসন নাহিদ পিট মুকুল আনোয়ারা
মোয়াজ্জেম ফজলুর এমিলি উইলসন হিমু ইউএন মুজিবনগর ব্যাঙ্গালোর সান্তা রাবি আফজাল ইন্না মোরালেস টেলিগ্রাফ আমাজন এলিস মিন্টু
জয়পুরহাট ইন্দিরা কারেন ফোর্ড মুস্তফা আব্দুস সেইন্ট ড্যানি আবাহনী ফরিদা পেকুয়া শ্যামল নিলয় জাপা আফ্রিদি শিকদার সোনিয়া
কামালের হাসিনাকে কোপা কার্লোস খায়ের শাকিল শারমিন বনি হাম জ্যাকি রাহমান হুয়ান সার্ক নেদারল্যান্ড ক্রিসেন্ট ভিলেজ ফ্রেড
কাদির সজল ক্যামেরন চ্যান হোসে ম্যারাডোনা মুহিত শহিদুল টেইলর জালালাবাদ আদনান শেরিফ শীরা হান্ট জুয়েল আলেপ্পো ফাতিমা হাসানুল
হাশিম রুহানি বিশ্বজিৎ কাতালান শফিউল জেরেমি বেইলি ক্রিস্টোফার শায়খ গ্রেস কার্টার মুন্নী আত্রাই শাজাহান ইমরুল মনিকা
ভার্জিনিয়া কামিল তেজগাঁও হামাস হামাসের মার্কস কুপার অ্যান্ডারসন পার্সি বিটিভি গোলাপগঞ্জ লর্ডস জামিল মাহমুদুল বিডিনিউজ তিতাস
মতিন উইলিয়ামস এটিএন নিজামী আমান ডেভ নিজাম আনহু ইয়াহু হিউজ কাই টঙ্গী জম্মু জাবি বায়ার্ন ওয়াল্টার মিথিলা রূপগঞ্জ ইবনু
বদরুল গেটস সেলিনা বেলুচিস্তান বার্সা খানম ইনস্টাগ্রাম হার্ভার্ড ব্যারি আসানসোল ইবরাহীম মনজুর আশুগঞ্জ মুজাহিদ আমু রকি সাবা
আকবার যাদবপুর মুস্তাফিজ তেহরিক বিলাল কামরুজ্জামান নয়াপল্টন নকিয়া আহম্মেদ পার্বতীপুর এনটিভি ওয়াসিম শ্রীরামপুর ম্যা মুন্না
রেঙ্গুন গ্যারি লুসি কুরাইশ ইউয়ান গোবিন্দগঞ্জ লরেন্স জহিরুল মোজাম্মেল শেন আদিল দাউদকান্দি কসবা পিটার্সবার্গ জুলিয়া তেরেসা
চয়ন লিলি রাকিব রবিউল হিউস্টন বিয়ানীবাজার লিজ শাইখ মাহমুদউল্লাহ শফিকুল ভারতী ওয়ালেস ম্যানহাটন শফী সুজান ইথান আনন্দবাজার
অনিক এএফপি এ্যান্ড সৌম্য বৃটেন হকিং ড্যারেন জেসিকা রোডস উম্মে বায়তুল মুস্তাফিজুর তাসকিন সিবিআই কমলাপুর লিংকন জোনস কুন সাঈদী
ফাহিম মার্গারেট দেল মিলান জলপাইগুড়ি গোবিন্দ যাদব জেরি টমি শ্রীকৃষ্ণ জেনি নোভা বরুণ সাইফুল হাছান ছাত্রশিবির রাজিউন বিল্লাহ
টুটুল তাপস পাওলো আল্লাহু ইয়েস ডাবলিন তালিবান মার্সেল ফিশার চট্রগ্রাম মাওয়া বায়েজিদ আফরোজ মঈন পপি আখাউড়া রামগড় উখিয়া
অ্যাডাম পার্কার সিংড়া কেট দিয়েগো পিপিপি আরএসএস অলিভার স্টেটস খাঁন সাদি কেশবপুর পুয়ের্তো রুনা কোস্টা ইসলামিয়া নবীনগর
জয়নাল মনোজ তনু জলি বানু এমি ম্যান্ডেলা নন্দীগ্রাম হাসনাত আন্না ইদ্রিস ম্যালকম নাদিয়া আনসারী খোরশেদ কায়েস কুয়াকাটা
ইমতিয়াজ হাওলাদার মিঠু গ্যাব্রিয়েল ডেভিস ফারিয়া আযহা রামগঞ্জ মাজিদ হালদা সলোমন পারভীন মাধব সাবাহ সুরমা প্যাটেল ইসলামপুর
ভাসানী বাংলালিংক অ্যামাজন খায়রুল পাউবো উসমান কর্মকার এয়ারটেল কপিল সুচিত্রা সালিম ম্যানিলা জিউস ইয়াসির শিবগঞ্জ ম্যানুয়েল
মার্কাস আজহা চৌগাছা নেহা গিয়াস কানাইঘাট জুনাইদ বাঁকুড়া এবিসি নেওয়াজ ডায়ানা স্যামুয়েল দারুল রাজেশ জিপি স্কাইপ মাধবপুর
রিলায়েন্স রোমিও লিমা কলম্বাস জোলি ইন্দো হোপ স্কাই সৌদ মিখাইল অরুণাচল পাইকগাছা হাফিজুর রেন কাউখালী সোবহান ক্যারল অ্যারন আহাদ
ঘাটাইল ভিকি মিকি নেতানিয়াহু অ্যানি বিটিআরসি মিতু ডারউইন নাজনীন সবুর শাহাবুদ্দিন দিপু ডোরেমন কোয়েল লারা মহেশ জামিয়া
জেনিফার লাকসাম এডিসন জেসি সুশান্ত আলফ্রেড আইএমএফ ঐশী আকিব বাপ্পি ইন্টেল কারিনা অদিতি জিন্নাহ ডব্লিউ রিকা দীপক নাজিম মুকেশ
কেরানীগঞ্জ রাফি মিয়াকে মহম্মদ শ্যামনগর বর্মন কলিন্স জকিগঞ্জ সিদ্দিকুর বিআরটিএ ল্যারি নাইক্ষ্যংছড়ি গুগুল জেমি আরাবিয়া
সাবিনা আহম্মদ বাঁশখালী ওয়ারেন মোসাদ্দেক গাবতলী পায়েল সুসান এন্ডারসন লিন্ডা কাদেরের টোয়েন্টিফোর রাদিয়াল্লাহু পিন্টু
মুরাদনগর লিসা ঋত্বিক রামপুর সাম সান্তোস আলাল দিরাই ফেলিক্স সাজু পান্ডে লুলু টেরি হাদী বার্ট লিওনার্দো ফিল্মফেয়ার কুমারখালী
গোপালপুর বিরামপুর রসূলুল্লাহ গ্র্যামি হিউ হগওয়ার্টস লুথার একনেক নাঙ্গলকোট খের সুয়ারেজ শাবানা শামস ডেরেক কেজরিওয়াল রুবি
ডিক ওআইসি ক্রেমলিন মোরশেদ তাহিরপুর সোনাগাজী জিদান নিকোলা প্রসেনজিৎ তাহসান নাজিব আনান সোলায়মান হলদিয়া আনুশকা এলজি লোহাগড়া
কিবরিয়া নরম্যান কেইন নানক শ্যামপুর মোহনবাগান যাত্রাবাড়ী অ্যাডামস রমিজ সুজা নন্দী অমি সুবোধ ব্রুকলিন আলবদর ভার্মা ডগলাস
গাংনী শ্যামলী ফেলুদা নবাবগঞ্জ মুশফিকুর মিয়ামি পল্লবী নাসিরনগর আজিম কৃষ্ণা মালদহ ইউরি শ্রীকান্ত শেরেবাংলা সুলেমান মোস্তাফিজ
অভি সিপিবি জোনাথন জুভেন্টাস ইয়েল কোচিন মমিন হারুনুর বিজিএমইএ ইশতিয়াক মধুপুর গ্রেগরি অ্যামনেস্টি মিচেল ক্রিস্টিনা নোকিয়া
দীপা ইউএসএ টিএসসি রিকার্ডো মোনা নিনা অ্যাঞ্জেলিনা কলিন ভবানী কিথ চায়নার চিটাগাং মাহিয়া গজারিয়া পিসিবি কিন কলারোয়া ইসরা
শ্রাবন্তী ফুয়াদ রমেশ রমনা রাইডার্স ব্রাসেলস মিল্টন ডিজনি ব্যোমকেশ ট্রেভর লাব্বাইক শার্লি সান্তিয়াগো টাইটানিক পীরগঞ্জ
বাঘাইছড়ি হ্যামিল্টন মফিজ মহাবীর দীঘিনালা বাহা টেরা কারীম বহরমপুর সদরঘাট নোয়া তিশা রুথ ম্যাথু কাশিমপুর ডাকসু সাতকানিয়া
ঈশ্বরদী এলাহী বাসুদেব এমিল কাস্ত্রো ন্যান্সি বারমুডা কায়সার চাটমোহর ফারাক্কা মাহবুবুর মুমিনুল টার্নার আমিনা ভেগাস লক্ষীপুর
শিন সাদাত আইলা ওয়াদুদ হোসনি সুরঞ্জিত হেলেনা সুফিয়ান ফ্র্যাঙ্ক ইশা বর্ধন পাপুয়া ত্রিশাল হুরায়রা হোসেইন নবীগঞ্জ গ্রেগ
হাবিবুল্লাহ সমীর ক্লেয়ার হাডসন উইঘুর হাসিব ভুবনেশ্বর শৈলকুপা সুন্দরগঞ্জ বদিউল রেমন্ড সাহারার স্যান কোম্পানীগঞ্জ লালমোহন
সাকা মাসউদ মোস্তাফা সাদেক ক্যারোলিনা বিষ্ণুপুর কঙ্গনা ফায়ারফক্স এসএ এবি এমআই জিএম য়া ইউজ ইউর দ্যাট হাউ উইন নাউ মেন ব্লা
ব্রো ওতে থেক আন্ত সাথ আফরোজা ঢামেক মজিবুর নিক্সন পাভেল আর্নল্ড এফবিসিসিআই আগা ভলডেমর্ট হিরোশিমা নেশন দাই ফিরোজা গফুর ইউজিন
গালিব শিবা অজিত এলান ভেনাস অস্টিন আসলান পিয়ের এবিএম গয়েশ্বর জিয়াং নিয়েন রিকি হুগো নেপোলিয়ন ডাম্বলডোর এডওয়ার্ডস নলছিটি
নাসিরউদ্দিন গিল বীরেন্দ্র টেসলা জাং টিনা চেচেন জেফারসন জুয়ান জাহানারা অশ্বিন কিংডম আলজাজিরা স্ট্যানফোর্ড নাইরোবি মাদানী
চৈতন্য মুলার মিশিগান কেটি সন্দ্বীপ শীতলক্ষ্যা কুইন্স জুরিখ তপু সিড শার্লক শুভশ্রী ম্যাক্সওয়েল কামারুজ্জামান প্রবীর পতেঙ্গা
নন্দ আলেকজান্দ্রিয়া কোয়েটা গুহ মাহমুদুল্লাহ সেরেনা কোস্টারিকা জুলিয়াস বাটা রামপুরা সোহান গুপ্তা বাবলু ওয়াহ কাউর টিটু
রিচি সানিয়া মোকাররম থম্পসন এলিয়ট বলকান কেপলার কবিগুরু ইয়াঙ্গুন ভেনিস আনাম হল্যান্ড রত্না ক্যারি পাকিস্থান এস্তোনিয়া
জয়নুল আটলান্টা কলোরাডো কেসি প্যাট শিবপুর নাথান অ্যানা সাসেক্স নাতাশা আইএসআইএস শামসুদ্দিন ডিজি সোমালি টুঙ্গিপাড়া হ্যারিস
খালেক রকিব ক্রিকইনফো ভাইবার রবার্তো সুরেশ স্টকহোম শিবচর এঞ্জেলস গৌরী তুরাগ সাইফুল্লাহ পেন্টাগন ক্রিস্টি চৌদ্দগ্রাম রোমা
সুফিয়া কাইয়ুম তিতুমীর ভেনিজুয়েলা কারওয়ান অমল মার্থা সিরাজদিখান বাফুফে আলিপুর কলাপাড়া হৃতিক হুথি থমসন রাজউক
বন্দোপাধ্যায় রিতা অভ্র নাদিম ভানু রামকৃষ্ণ হাজীগঞ্জ শিভা তাওহীদ বিসিসিআই মনপুরা কোকা স্নোডেন অপর্ণা উবুন্টু লালু মসুল
ওয়াটার্স সালেম লিউ কুতুব পের জেইন বুড়িগঙ্গা মায়াবতী এলেক্স তাজউদ্দীন কাফি নেভিল পেদ্রো হালুয়াঘাট তালহা এফডিসি উয়েফা
সুদীপ লুনা লিজা সুস্মিতা পবন রাগীব অর্চনা কেন্ট মমিনুল মগবাজার এলিজা রোহান ঈশ্বরচন্দ্র ম্যাকডোনাল্ড শিবাজী নান্নু গার্সিয়া
ফিঞ্চ এহসান সাইবেরিয়া ম্যাকাও পাথরঘাটা জুলি স্কারলেট মালালা রোনাল্ড অ্যালিস রাধিকা ইজরায়েল মারমা আরিয়ান শ্যারন মারাদোনা
জবস কচুয়া কল্লোল লুসিয়া ওসামা লোগান জুলিয়েট নফিস ইউএই ব্লেক জেপি হেরাল্ড সুরাইয়া খসরু মোহিত মিসবাহ এনডিটিভি সাঁথিয়া
ময়নামতি বাকু ওমা দীনেশ অনুষ্কা ম্যাথিউ হরিপুর শিল্পা আকসা বার্নস দৌলত মেগান এমসিসি তারানা গ্রান্ট টেট ডেসটিনি নিশা মধুসূদন
ফটিকছড়ি কমলনগর সুন মায়ামি মন্টি ফার্নান্দেজ শোলাকিয়া রবিনসন নিকি বাবরি দাগনভূঞা আদমজী জহুর লিনা গুস্তাভ লোকনাথ পিটিআই
চিশতী সাবরিনা হাজারা কুবরা স্টেফান হুগলি আতিয়া সাজ্জাদ হারমান ভ্যালেন্সিয়া কোতোয়ালী আইজ্যাক আরজু বেনি খাত্তাব ইস্টবেঙ্গল
লোপেজ লালবাগ উইজলি অ্যান্থনি দৌলতদিয়া শুক্লা আসিয়ান মোহামেডান সঞ্জীব এলজিইডি আজমল ফিলিপস মল্লিকা ভেঙ্কটেশ লাগোস ফ্লয়েড
ক্যাথি জোশ জেসাস সানজিদা হিমেল ডিসকভারি আযম অনুপ মনিরুজ্জামান দ্র ল্য ক্রে ওল রাস তুম মাইয়ার হোন্ডা সোয়াত কুর্দিস্তান
উদীচী ফিদা হাকিমপুর ক্যাপিটল স্টেইন রিনা ডিমলা মাহাথির উইলি সারদা ইয়েরেভান চন্দ সোয়ান চৌরঙ্গী লিয়ন এসেক্স লেভি রাজনগর গৌর
পিটারসন এলসি আব্দ ওয়াহাব অলিভিয়া রূপপুর শাহিদ কোয়া নান্দাইল জিল বারবারা রাব্বানী উডি হাতিরঝিল সিরি রজার্স মিতা মিশা আমতলী
গিনেস খোকসা রোকন মলি আরিচা আন্তোনিও হুয়া রাবেয়া ফ্রান্সিস্কো মাউ মজনু মাদুরাই হুয়াওয়ে বড়লেখা জাবের উলিপুর এডিবি টেলিটক
মঠবাড়িয়া মোসাদ খাইবার এঞ্জেলেস স্টোকস জয়েস স্বপ্না তামান্না রাকেশ ন্যাট বুলু মাবুদ স্যামি চিলমারী জবি নাবিল আগুয়েরো
ক্লারা ইউএনডিপি সোফি বিবার সিকদার হামাদ মুকসুদপুর ইউনিলিভার অ্যান্ড্রু ব্লেয়ার নাজমা বিএফইউজে হুন নুনু লোহাগাড়া সিকান্দার
বুবলী বুদ্ধদেব পেরেজ রোজি শশাঙ্ক নির্ঝর হাজ্জাজ মোর্শেদ চরমোনাই রেড্ডি বরিস এজে
""".split())


def induce(unexplained, claimers):
    """
    The words the corpus's forms no word of the pack explains ([unexplained]: form -> count) are of: word -> its forms. A
    name's forms ([claimers]: the pack's names and the words that are none) are its, and no word's. Each form goes to one
    word, the one with the most forms' count of those left (a noun's forms are its nominal_forms and clitics, and its
    spellings alike but for slips: ভিড়'s ভীড়).
    """
    # The forms spelled alike but for slips (SLIPS), as one: আক্রমণ's আক্রমন.
    alike = defaultdict(set)
    for form in unexplained:
        alike[slipped(form)].add(form)

    def nominal(word):
        return {f for w in with_clitics(nominal_forms(word) | {word}) for f in alike.get(slipped(w), ())}

    claimed = set()
    for name in claimers:
        # A name ending in a consonant takes -র too (আল্লাহর).
        claimed |= nominal(name) | {name} | {f for f in with_clitics({name + t for t in VOWEL_TAILS + ["সহ"]}) if f in unexplained}
    left = {f: n for f, n in unexplained.items() if f not in claimed}
    paradigms, verbs = {}, set()
    for form, n in left.items():
        # Of the spellings alike but for slips, the one written most often (ভিড়, not ভীড়).
        if max(alike[slipped(form)], key=lambda f: (left.get(f, 0), f)) != form:
            pass
        elif n >= INDUCED_BARE and len(form) >= INDUCED_LENGTH and not form.endswith(NOT_NOUN):
            paradigms[form] = {f for f in nominal(form) if f in left}
        for tails, back in ((CAUSATIVE_TELLING, "ানো"), (STEM_TELLING, "া")):
            for tail in tails:
                stem = form[:-len(tail)]
                if not form.endswith(tail) or len(stem) < 2:
                    continue
                if back == "া" and (tail.startswith("ে") or vowel_final(stem) or stem[-1] == "্"):
                    continue
                word = stem + back
                if word in paradigms or word in claimed:
                    continue
                made = with_clitics(untabled_verb_forms(word) | verb_forms(word, ()))
                forms = {f for f in made if f in left}
                endings = CAUSATIVE_TELLING if back == "ানো" else STEM_TELLING
                telling = {f for f in forms if any(f.endswith(t) for t in endings)}
                if (len(telling) >= INDUCED_FORMS and math.fsum(left[f] for f in telling) >= INDUCED_VERB
                        and math.fsum(left[f] for f in forms) >= INDUCED_VERB):
                    paradigms[word] = forms
                    verbs.add(word)
    # Lazy greedy: a word's count only falls as others take its forms, so one popped with its count unchanged is the best left.
    chosen = {}
    heap = [(-math.fsum(left[f] for f in forms), word) for word, forms in paradigms.items()]
    heapq.heapify(heap)
    while heap:
        negative, word = heapq.heappop(heap)
        forms = paradigms[word] - claimed
        mass = math.fsum(left[f] for f in forms)
        if mass < -negative:
            paradigms[word] = forms
            if mass > 0:
                heapq.heappush(heap, (-mass, word))
            continue
        if word in verbs:
            if len(forms) < INDUCED_FORMS or mass < INDUCED_VERB:
                continue
        elif word in claimed or left.get(word, 0) < INDUCED_BARE or forms <= {word} and left[word] < INDUCED_LONE:
            continue
        chosen[word] = forms
        claimed |= forms | {word}
    return chosen


def build(found, size, label):
    """The list: the pack's words and those the corpus shows (induce), by their forms' counts in [found]."""
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
    # The words a form no word explains may be of, spelled otherwise (variants): form -> words.
    loosely = defaultdict(set)

    def add(word, as_word, ks, loose=False):
        made = word_forms(word, ks, table_of, kinds)
        nominal_made = nominal_forms(word) if ks & NOMINAL_KINDS else set()
        tabled = table_of.get(as_word, set()) if word == as_word else {word}
        written = with_clitics(made | tabled | {word})
        for form in written:
            if form in counts and form != as_word:
                if loose:
                    loosely[form].add(as_word)
                elif "verb" in ks and (form not in with_clitics(nominal_made) or form in tabled - nominal_made):
                    verbal[form].add(as_word)
                else:
                    nominal[form].add(as_word)

    for word, ks in kinds.items():
        add(word, word, ks)
    for variant, word in SPELLINGS.items():
        if word in kinds:
            add(variant, word, kinds[word])
    made = {form: verbal.get(form, set()) | nominal.get(form, set()) for form in set(verbal) | set(nominal)}
    for variant, word in variants(kinds, made).items():
        add(variant, word, kinds[word], loose=True)

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
            # Of those, the longest (মেয়ের is মেয়ে + র, not মে + য়ের; রাতে is রাত + ে, not রা + তে).
            longest = max(len(w) for w in nouns)
            nouns = {w for w in nouns if len(w) == longest}
            # A verb's form is no short word with a classifier and a case on it (মেটাতে is মেটানো's, not মে + টা + তে).
            if verbs and fewest == 2:
                nouns = set()
        others = verbs | nouns
        if not others:
            for base in unclitic(form):
                if base in BENGALI_FORMS:
                    return None, {BENGALI_FORMS[base]}
                if base in kinds:
                    return None, {base}
            # A vowel-final word's genitive the rules do not make (সামনের of সামনে, বাইরের of বাইরে).
            if not own and form.endswith("র") and form[:-1] in kinds and vowel_final(form[:-1]):
                return None, {form[:-1]}
            # A verb's form with না "not" written on (হয়না, জানিনা, করেনা).
            if not own and form.endswith("না") and verbal.get(form[:-2]):
                return None, verbal[form[:-2]] - {form[:-2]} or verbal[form[:-2]]
            if not own and form.endswith("না") and "verb" in kinds.get(BENGALI_FORMS.get(form[:-2]), ()):
                return None, {BENGALI_FORMS[form[:-2]]}
            if not own and loosely.get(form):
                return None, loosely[form]
        return own, others

    found_heads = {}
    for form in counts:
        found_heads[form] = candidates(form)
    # A form no word explains is a slip (SLIPS) of the form spelled right that is written at least as often, and goes where
    # that goes (আক্রমন as আক্রমণ, অামার as আমার; not আই "I" as আঁই).
    slips = defaultdict(list)
    for form, (own, others) in found_heads.items():
        if own or others:
            slips[slipped(form)].append(form)
    for form, (own, others) in list(found_heads.items()):
        if not own and not others:
            right = [f for f in slips.get(slipped(form), ()) if counts[f] >= counts[form] and slip_of(form, f)]
            # And a form with সহ "with" written on (পরিবারসহ) goes where the form without it goes.
            if form.endswith("সহ") and any(found_heads.get(form[:-2], ())):
                right.append(form[:-2])
            heads = {h for f in right for h in found_heads[f][1] or {found_heads[f][0]}}
            if heads:
                found_heads[form] = (None, heads)
    # How common each word is by its forms of no other word.
    alone = defaultdict(float)
    for form, (own, others) in found_heads.items():
        heads = ({own} if own else set()) | others
        if len(heads) == 1:
            alone[next(iter(heads))] += counts[form]
    scores = defaultdict(float)
    forms = defaultdict(dict)
    unexplained = {}
    for form, share in counts.items():
        own, others = found_heads[form]
        if own and others and max(alone.get(o, 0.0) for o in others) <= share:
            heads = {own}
        else:
            heads = others or ({own} if own else set())
        if not heads:
            unexplained[form] = share
            continue
        weights = {h: alone.get(h, 0.0) for h in heads}
        whole = sum(weights.values())
        for head in heads:
            part = weights[head] / whole if whole else 1 / len(heads)
            if part <= 0:
                continue
            scores[head] += share * part
            forms[head][form] = forms[head].get(form, 0.0) + share * part

    for word, made in induce(unexplained, names | NOT_WORDS | INDUCED_NOT_WORDS).items():
        for form in sorted(made, key=lambda f: (-counts[f], f)):
            scores[word] += counts[form]
            forms[word][form] = forms[word].get(form, 0.0) + counts[form]

    ranked = sorted(scores, key=lambda w: -scores[w])[:size]
    lines = [f"# source: {label}", f"# words: {len(ranked)}"]
    for word in ranked:
        listed = sorted(forms[word], key=lambda f: -forms[word][f])[:FORMS_LISTED]
        if word not in listed:
            listed = [word] + listed[:FORMS_LISTED - 1]
        lines.append(word + "\t" + " ".join(listed))
    return "\n".join(lines) + "\n", label
