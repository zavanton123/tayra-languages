"""
Khmer spellings the km-en pack lacks, filed under the word of the pack they spell (build_khmer): the list has each as that
word's form, so the reader, which reads Khmer by the list's words and forms, reads it as a word, and a course may write it.
"""

# Everyday spellings of a word the pack has in another: ឱ្យ and អោយ for ឲ្យ "to give, for", ម៉េច for ម្ដេច "how", ចឹង
# for អ៊ីចឹង "like that, so". The subtitles write them over 50,000 times.
KHMER_SPELLINGS = {
    "ឱ្យ": "ឲ្យ",
    "អោយ": "ឲ្យ",
    "ម៉េច": "ម្ដេច",
    "ចឹង": "អ៊ីចឹង",
    # លឺ for ឮ "to hear", as the subtitles mostly write it: the pack's លឺ, a Sino-Khmer "you", is rare.
    "លឺ": "ឮ",
}

# Spellings with ម្ before a consonant where the pack has ំ (ចម្រើន, ចំរើន), or ំ where it has ម្ (កំលាំង, កម្លាំង), are
# found by build_khmer itself (KHMER_NASAL_SWAP_MIN): the texts write a word in either, the newer spelling mostly with ម្.

# Pieces of the texts' loanwords and names the cut leaves where no word of the pack covers them (វី of វីដេអូ, ដា of កាណាដា,
# ឡាយ, គី, ឡូ), which the pack has only as rare words ("to spread out", "rock"): they are no words of the texts.
KHMER_EXCLUDE = {"ត្រ", "វី", "រា", "លប់", "ដា", "សេ", "បេ", "ឡាយ", "លី", "គី", "ឡូ", "ស្រ", "ធី", "រេ",
                 # And those the courses past rank 1900 met: ដឺ, ម៉, រ៉ of French and English loanwords, ភេ and វក of ភេរវករ
                 # "terrorist", គ្រ of គ្រវី "to wave", គម of សហគមន៍ "community", ព្រីង of place names (តាព្រីង), ថូ of
                 # ថូម៉ាស "Thomas", ណោះ of ប៉ុណ្ណោះ written ប៉ុនណោះ.
                 "ដឺ", "ម៉", "រ៉", "ភេ", "វក", "គ្រ", "គម", "ព្រីង", "ថូ", "ណោះ",
                 # And those past rank 2900 that no compound of the texts explains (build_khmer joins those): pieces of names
                 # (ខន, ខុន, ស្វរៈ, ទុង, សែល of មីសែល, ឥណ្ឌូ), of Pali and Sanskrit words (គរុ of គរុកោសល្យ, វីថិ, ធាន of
                 # អន្តរធាន, ថាន, ចម), of couplets (លៃ of លៃលក, រែក), and of misspellings (សាច of បីសាច, ពារ of កាពារ, ឆែ).
                 "ខន", "ខុន", "ស្វរៈ", "ទុង", "សែល", "ឥណ្ឌូ", "គរុ", "វីថិ", "ធាន", "ថាន", "ចម", "លៃ", "រែក", "សាច", "ពារ",
                 "ឆែ"}
