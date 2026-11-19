"""
Lao corrections for tools/build_frequency_list.py (build_lao), from reading the list's top 2,300 by hand (2026-11-19).

The reader cuts Lao with ICU's word-break list (files/lexicon/lo.txt), which has the syllables of names and foreign words as
entries of their own so that it can cut them: a name the dictionary lacks (ສີສະຫວັນ, ອິນເຕີເນຊັນແນລ) comes out as its
syllables. Lao Wikipedia, the list's only source, is full of names, so those syllables would rank as common words.
FRAGMENTS are the syllables that are no word alone in ordinary Lao (prefixes of Pali words, pieces of names and foreign
words): they are not counted, and a word next to one is not counted there either, as next to letters no word covers.
SPELLINGS are misspellings and older spellings common in the source, counted under the word they spell.
"""

FRAGMENTS = set("""
ສະ ທະ ຕະ ມະ ຍະ ຊະ ຄະ ວະ ບະ ງະ ດະ ຜະ ຣະ ອະ ຂະ ສາດ ເສດ ສຳ ສົງ ເທບ ກິດ ອິດ ສັນ ບັນ ລາດ ອຳ ພົງ ໄລ ນ່າ
ເນ ເຕີ ລີ ລິ ເອ ດາ ສຸ ກິ ໂມ ເຊ ມິ ຄາ ບາ ວາ ໂນ ໂຊ ຈີ ຊູ ໂກ ເລ ອີ ວິ ຈິ ບູ ເປີ ພີ ກີ ພິ ດູ ເມ ໂຄ ບີ ອຸ ເຊີ ໂບ ຍູ ໄມ ເຕ
ເກ ເຈ ແອ ເບ ເດ ຮາ ເວ ໂຂ ໂຮ ໂພ ໄບ ແວ ໂຮ່ ເຈີ ໄນ ຟາ ເທ ເຄ ໂຈ ຕຸ ຈູ ຟູ ຍຸ ປໍ ບິ ຮິ ມໍ ວໍ ໄຮ ໄຄ ເຮ ຊຸ ຮໍ ເນີ ມຸ ຍາ ຊີ ຈາ
ລູ ຮ່າ ເດິ ປິ ໂປ ຕ້າ ເຊຍ ເຕີຍ ໂນ້ຍ ບ້ຳ ຊຸບ ດິດ ຣຸດ
ອິນ ຊັນ ຄອນ ກຸນ ມອນ ເວດ ກຸດ ມິນ ມຸງ ລາມ ທັມ ເຊັນ ເຈນ ຮາມ ລິນ ເລນ ມັງ ຊອບ ນາດ ເກດ ເອັນ ນັນ ຊິນ ວິນ ແອນ ແມນ
ອອນ ອັບ ນິກ ປິນ ອັງ ຕິກ ຊຶມ ຍົດ ເວນ ອານ ຂອນ ຮຽງ ເດນ ໂມນ ເຊນ ແຊນ ເບນ ຮິນ ຮັນ ເຄນ ຊອນ ລຽນ ວິດ ໂນນ ບອນ ປາງ ຕົງ
ນົນ ທອນ ພາມ ພວນ ຊ່າ ທັດ
""".split())

SPELLINGS = {
    "ຂື້ນ": "ຂຶ້ນ", "ເຖີງ": "ເຖິງ", "ທັ້ງ": "ທັງ", "ຫຼີ້ນ": "ຫຼິ້ນ", "ເຊີ່ງ": "ເຊິ່ງ", "ຊຶ່ງ": "ເຊິ່ງ", "ຊື່ງ": "ເຊິ່ງ",
    "ເລິ່ມ": "ເລີ່ມ", "ໜື່ງ": "ໜຶ່ງ", "ໄກ້": "ໃກ້", "ເງີນ": "ເງິນ", "ຮວມ": "ຮ່ວມ", "ຊ່ອຍ": "ຊ່ວຍ", "ຈິ່ງ": "ຈຶ່ງ",
}
