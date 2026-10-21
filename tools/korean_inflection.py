"""
Yomitan's Korean inflection rules, as the app applies them (core/domain/.../morphology/LanguageTransformer.kt,
Hangul.kt): the words an inflected Korean word may be a form of ("먹었어요" of "먹다"). The rules are read
from the app's generated rules/KoTransforms.kt, so the two never differ.
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RULES = os.path.join(ROOT, "core", "domain", "src", "commonMain", "kotlin", "com", "tayra", "languages", "core", "domain",
                     "morphology", "rules", "KoTransforms.kt")

# Hangul.kt's tables: a port of Hangul.js (MIT, Copyright 2017 Jaemin Jo), which splits complex vowels and final clusters too.
CHO = ["ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"]
JUNG = ["ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅗㅏ", "ㅗㅐ", "ㅗㅣ", "ㅛ", "ㅜ", "ㅜㅓ", "ㅜㅔ", "ㅜㅣ", "ㅠ", "ㅡ", "ㅡㅣ", "ㅣ"]
JONG = ["", "ㄱ", "ㄲ", "ㄱㅅ", "ㄴ", "ㄴㅈ", "ㄴㅎ", "ㄷ", "ㄹ", "ㄹㄱ", "ㄹㅁ", "ㄹㅂ", "ㄹㅅ", "ㄹㅌ", "ㄹㅍ", "ㄹㅎ", "ㅁ",
        "ㅂ", "ㅂㅅ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"]
CONSONANTS = "ㄱㄲㄳㄴㄵㄶㄷㄸㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅃㅄㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
COMPLETE_CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
COMPLETE_JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
COMPLETE_JONG = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"
COMPLEX_CONSONANTS = {"ㄱㅅ": "ㄳ", "ㄴㅈ": "ㄵ", "ㄴㅎ": "ㄶ", "ㄹㄱ": "ㄺ", "ㄹㅁ": "ㄻ", "ㄹㅂ": "ㄼ", "ㄹㅅ": "ㄽ", "ㄹㅌ": "ㄾ",
                      "ㄹㅍ": "ㄿ", "ㄹㅎ": "ㅀ", "ㅂㅅ": "ㅄ"}
COMPLEX_VOWELS = {"ㅗㅏ": "ㅘ", "ㅗㅐ": "ㅙ", "ㅗㅣ": "ㅚ", "ㅜㅓ": "ㅝ", "ㅜㅔ": "ㅞ", "ㅜㅣ": "ㅟ", "ㅡㅣ": "ㅢ"}


def _is_cho(c):
    return c in COMPLETE_CHO


def _is_jung(c):
    return c in COMPLETE_JUNG


def _is_jong(c):
    return c in COMPLETE_JONG


def _jong_index(c):
    return COMPLETE_JONG.index(c) + 1


def disassemble(text):
    out = []
    for c in text:
        code = ord(c)
        if 0xAC00 <= code <= 0xD7A3:
            code -= 0xAC00
            jong = code % 28
            jung = (code - jong) // 28 % 21
            cho = (code - jong) // 28 // 21
            out.append(CHO[cho] + JUNG[jung] + (JONG[jong] if jong else ""))
        elif c in CONSONANTS:
            out.append(CHO[COMPLETE_CHO.index(c)] if _is_cho(c) else JONG[_jong_index(c)])
        elif _is_jung(c):
            out.append(JUNG[COMPLETE_JUNG.index(c)])
        else:
            out.append(c)
    return "".join(out)


def assemble(text):
    """Hangul.kt's assemble, step for step."""
    array = disassemble(text)
    result = []
    state = {"complete": -1, "merged": False}

    def syllable(cho, jung, jong):
        return chr((COMPLETE_CHO.index(cho) * 21 + COMPLETE_JUNG.index(jung)) * 28 + (_jong_index(jong) if jong else 0) + 0xAC00)

    def make(index):
        state["merged"] = False
        complete = state["complete"]
        if complete + 1 > index:
            return
        cho = jung1 = ""
        jong1 = None
        hangul = ""
        step = 1
        while True:
            c = array[complete + step]
            if step == 1:
                cho = c
                if _is_jung(cho):
                    if complete + step + 1 <= index and _is_jung(array[complete + step + 1]):
                        result.append(COMPLEX_VOWELS.get(cho + array[complete + step + 1], "\u0000"))
                    else:
                        result.append(c)
                    state["complete"] = index
                    return
                elif not _is_cho(cho):
                    result.append(c)
                    state["complete"] = index
                    return
                hangul = c
            elif step == 2:
                jung1 = c
                if _is_cho(jung1):
                    result.append(COMPLEX_CONSONANTS.get(cho + jung1, "\u0000"))
                    state["complete"] = index
                    return
                hangul = syllable(cho, jung1, None)
            elif step == 3:
                joined = COMPLEX_VOWELS.get(jung1 + c)
                if joined is not None:
                    jung1 = joined
                else:
                    jong1 = c
                hangul = syllable(cho, jung1, jong1)
            elif step == 4:
                jong1 = COMPLEX_CONSONANTS.get(jong1 + c) if jong1 is not None else c
                hangul = syllable(cho, jung1, jong1)
            elif step == 5:
                jong1 = COMPLEX_CONSONANTS.get(jong1 + c) if jong1 is not None else None
                hangul = syllable(cho, jung1, jong1)
            if complete + step >= index:
                result.append(hangul)
                state["complete"] = index
                return
            step += 1

    stage = 0
    previous = "\u0000"
    i = 0
    while i < len(array):
        code = array[i]
        if not _is_cho(code) and not _is_jung(code) and not _is_jong(code):
            make(i - 1)
            make(i)
            stage = 0
            i += 1
            continue
        if stage == 0:
            if _is_cho(code):
                stage = 1
            elif _is_jung(code):
                stage = 4
        elif stage == 1:
            if _is_jung(code):
                stage = 2
            elif previous + code in COMPLEX_CONSONANTS:
                stage = 5
            else:
                make(i - 1)
        elif stage == 2:
            if _is_jong(code):
                stage = 3
            elif _is_jung(code):
                if previous + code not in COMPLEX_VOWELS:
                    make(i - 1)
                    stage = 4
            else:
                make(i - 1)
                stage = 1
        elif stage == 3:
            if _is_jong(code):
                if not state["merged"] and previous + code in COMPLEX_CONSONANTS:
                    state["merged"] = True
                else:
                    make(i - 1)
                    stage = 1
            elif _is_cho(code):
                make(i - 1)
                stage = 1
            elif _is_jung(code):
                make(i - 2)
                stage = 2
        elif stage == 4:
            if _is_jung(code):
                if previous + code in COMPLEX_VOWELS:
                    make(i)
                    stage = 0
                else:
                    make(i - 1)
            else:
                make(i - 1)
                stage = 1
        elif stage == 5:
            if _is_jung(code):
                make(i - 2)
                stage = 2
            else:
                make(i - 1)
                stage = 1
        previous = code
        i += 1
    make(i - 1)
    return "".join(result)


def _rule_lines():
    source = open(RULES, encoding="utf-8").read()
    body = source[source.index("listOf(") + len("listOf("):source.rindex(")")]
    escapes = {"t": "\t", "n": "\n", "$": "$", '"': '"', "\\": "\\"}
    text = "".join(re.sub(r"\\(.)", lambda m: escapes[m.group(1)], s) for s in re.findall(r'"((?:[^"\\]|\\.)*)"', body))
    return [line for line in text.split("\n") if line]


class Transformer:
    """LanguageTransformer.kt for the Korean rules, whose patterns are all plain endings ("ㅇㅓㅇㅛ$")."""

    def __init__(self):
        conditions = []
        self.flags = {}
        self.dictionary_flags = 0
        # (transform id, rule index, ending, conditions in, conditions out, op), in the order the app tries them.
        self.rules = []
        transform = None
        index = 0
        pending = []
        for line in _rule_lines():
            f = line.split("\t")
            if f[0] == "C":
                conditions.append((f[1], f[2] == "1", [c for c in f[3].split(",") if c]))
            elif f[0] == "T":
                if not self.flags and conditions:
                    self._assign(conditions)
                transform, index = f[1], 0
            elif f[0] == "R":
                pending.append((transform, index, f))
                index += 1
        if not self.flags and conditions:
            self._assign(conditions)
        for transform, index, f in pending:
            pattern = f[1]
            assert pattern.endswith("$") and not f[2] and not f[3], pattern
            op = f[6:]
            if op[0] == "s":
                op = ("s", int(op[1]), int(op[2]), op[3] if len(op) > 3 else "", op[4] if len(op) > 4 else "")
            elif op[0] == "c":
                op = ("c", op[1] if len(op) > 1 else "")
            else:
                raise ValueError(f"unknown rule kind {op[0]}")
            self.rules.append((transform, index, pattern[:-1], self._flags_of(f[4]), self._flags_of(f[5]), op))
        self.by_ending = {}
        for order, rule in enumerate(self.rules):
            self.by_ending.setdefault(rule[2], []).append((order, rule))
        self.longest = max(len(e) for e in self.by_ending)

    def _assign(self, conditions):
        next_flag = 0
        targets = conditions
        while targets:
            waiting = []
            for name, _, subs in targets:
                if not subs:
                    self.flags[name] = 1 << next_flag
                    next_flag += 1
                elif all(s in self.flags for s in subs):
                    value = 0
                    for s in subs:
                        value |= self.flags[s]
                    self.flags[name] = value
                else:
                    waiting.append((name, _, subs))
            assert len(waiting) < len(targets)
            targets = waiting
        for name, is_dictionary_form, _ in conditions:
            if is_dictionary_form:
                self.dictionary_flags |= self.flags[name]

    def _flags_of(self, names):
        value = 0
        for name in names.split(","):
            if name:
                value |= self.flags[name]
        return value

    def transform(self, source, limit=4096):
        """(text, conditions, transforms) for the word and every word the rules turn it into, nearest first."""
        results = [(source, 0, ())]
        frames = [()]
        i = 0
        while i < len(results) and len(results) < limit:
            text, conditions, trace = results[i]
            path = frames[i]
            i += 1
            matching = []
            for n in range(0, min(self.longest, len(text)) + 1):
                matching.extend(self.by_ending.get(text[len(text) - n:] if n else "", ()))
            for _, (transform, index, _, cond_in, cond_out, op) in sorted(matching, key=lambda m: m[0]):
                if conditions and not conditions & cond_in:
                    continue
                if (transform, index, text) in path:
                    continue
                if op[0] == "s":
                    end = len(text) - op[2]
                    out = op[3] + (text[op[1]:end] if op[1] < end else "") + op[4]
                else:
                    out = op[1]
                if out:
                    results.append((out, cond_out, (transform,) + trace))
                    frames.append(((transform, index, text),) + path)
        return results


_transformer = []


def candidates(word):
    """The dictionary words [word] may be an inflection of, as LanguageMorphology.candidates gives them: (word, conditions, transforms)."""
    if not _transformer:
        _transformer.append(Transformer())
    seen = {}
    for text, conditions, trace in _transformer[0].transform(disassemble(word)):
        key = (assemble(text), conditions)
        if key not in seen and len(seen) < 4096:
            seen[key] = trace
    return sorted(((w, c, t) for (w, c), t in seen.items()), key=lambda x: len(x[2]))


def dictionary_forms(word):
    """The words [word] inflects, by at least one rule, ending in a dictionary form (a verb, adjective or the copula)."""
    if not _transformer:
        _transformer.append(Transformer())
    flags = _transformer[0].dictionary_flags
    return [(w, t) for w, c, t in candidates(word) if t and c & flags]
