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
}

# Spellings with ម្ before a consonant where the pack has ំ (ចម្រើន, ចំរើន), or ំ where it has ម្ (កំលាំង, កម្លាំង), are
# found by build_khmer itself (KHMER_NASAL_SWAP_MIN): the texts write a word in either, the newer spelling mostly with ម្.
