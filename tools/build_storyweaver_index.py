#!/usr/bin/env python3
"""
Builds the index of StoryWeaver's picture books that Find books offers, from the copy Kolibri
Studio keeps (Learning Equality's channel "Pratham Books' StoryWeaver", published 2018, every book
a PDF under CC BY 4.0).

The channel's catalog (an SQLite file) lists the books by theme with every one marked English, so
each book's PDF is read once with pdftotext (poppler) for what only the book itself says: its
language on the back cover ("(Portuguese)", "(Mandarin (Traditional))"), its level ("This is a
Level 2 book..."), its author and translator, and how many words its story has. Books in two
languages ("(English-Marathi)"), books in scripts whose text comes out of these PDFs broken
(Indic scripts and Arabic, their ligatures drawn rather than encoded; Hebrew, its words broken
into letters), books in languages the app does not know and books of fewer than --min-words words
(30, as for the Global Digital Library) are left out.

The index is one TSV file per language in data/storyweaver/ (file id, level, words, title,
authors, description, cover picture's file id), which the app fetches through jsDelivr from this
repository; the languages it has go into STORYWEAVER_LANGUAGES in BookDiscovery.kt. Downloads go
to a temporary folder and are deleted once read; each book's text is kept in
tools/books/.cache/storyweaver/books.jsonl, so a second run reads only the books not read yet and
--write reads the texts again for what they say.

Usage: python3 tools/build_storyweaver_index.py [--limit 200] [--workers 8]
       python3 tools/build_storyweaver_index.py --write     (the index from what was read)
"""
import argparse
import collections
import json
import os
import re
import sqlite3
import subprocess
import sys
import tempfile
import unicodedata
from concurrent.futures import ThreadPoolExecutor, as_completed

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools/books/.cache/storyweaver")
READ = os.path.join(CACHE, "books.jsonl")
OUT = os.path.join(ROOT, "data/storyweaver")
SOURCES = os.path.join(ROOT, "core/domain/src/commonMain/kotlin/com/tayra/languages/core/domain/books/discover/BookDiscovery.kt")
CHANNEL = "131e543dbecf5776bb13cfcfddf05605"
STUDIO = "https://studio.learningequality.org"
USER_AGENT = "TayraLanguages/1.0 (https://github.com/zavanton123/tayra-languages)"

# The back cover's language names, by the app's codes; a name not here is a language the app does not know.
LANGUAGES = {
    "Afrikaans": "af", "Albanian": "sq", "Basque": "eu", "Bulgarian": "bg", "Catalan": "ca", "Croatian": "hr",
    "Czech": "cs", "Danish": "da", "Dutch": "nl", "English": "en", "Esperanto": "eo", "Estonian": "et",
    "Finnish": "fi", "French": "fr", "Galician": "gl", "Georgian": "ka", "German": "de", "Greek": "el",
    "Hungarian": "hu", "Icelandic": "is", "Indonesian": "id", "Bahasa Indonesia": "id", "Irish": "ga", "Italian": "it",
    "Japanese": "ja", "Korean": "ko", "Latvian": "lv", "Lithuanian": "lt", "Macedonian": "mk",
    "Mandarin (Simplified)": "zh", "Mandarin (Traditional)": "zh", "Chinese (Simplified)": "zh", "Chinese (Traditional)": "zh",
    "Mongolian": "mn", "Norwegian": "no", "Polish": "pl", "Portuguese": "pt", "Portuguese (Brazilian)": "pt",
    "Romanian": "ro", "Russian": "ru", "Serbian": "sr", "Slovak": "sk", "Slovenian": "sl", "Spanish": "es",
    "Swahili": "sw", "Swedish": "sv", "Tagalog": "tl", "Filipino": "tl", "Turkish": "tr", "Ukrainian": "uk", "Kiswahili": "sw", "Cantonese (Traditional)": "yue",
    "Vietnamese": "vi", "Welsh": "cy",
}

# Scripts whose text pdftotext gives back whole in these PDFs; a title in another is not downloaded.
READABLE_SCRIPTS = {"LATIN", "CYRILLIC", "GREEK", "HANGUL", "CJK", "HIRAGANA", "KATAKANA", "GEORGIAN"}

LEVEL = re.compile(r"This is a Level (\d) book")
LABEL = re.compile(r"^\((.+)\)$")
CREDIT = re.compile(r"^(Author|Authors|Illustrator|Illustrators|Translator|Translators|Adapted by|Adaptation):\s*(.*)$")
# Where the story stops: StoryWeaver's note on the platform, or the attributions when the note is not there.
STORY_END = ("This book was made possible by Pratham Books", "Story Attribution:", "Illustration Attributions:")
CREDITS_WITHIN = 8


def scripts(text):
    found = set()
    for ch in text:
        if ch.isalpha():
            found.add(unicodedata.name(ch, "?").split()[0])
    return found


def books():
    """The channel's books once each: (file id, title, description), from its catalog."""
    path = os.path.join(CACHE, f"{CHANNEL}.sqlite3")
    if not os.path.exists(path):
        subprocess.run(["curl", "-fsSL", "-A", USER_AGENT, "-o", path + ".part", f"{STUDIO}/content/databases/{CHANNEL}.sqlite3"], check=True)
        os.replace(path + ".part", path)
    db = sqlite3.connect(path)
    rows = db.execute(
        """SELECT n.content_id, n.title, coalesce(n.description, ''), min(l.id)
           FROM content_contentnode n JOIN content_file f ON f.contentnode_id = n.id JOIN content_localfile l ON l.id = f.local_file_id
           WHERE n.kind = 'document' AND l.extension = 'pdf' GROUP BY n.content_id ORDER BY n.title"""
    ).fetchall()
    return [(file, title.strip(), description.strip()) for _, title, description, file in rows]


def read_book(file, title, description, work):
    """The book's text, as pdftotext gives it."""
    pdf = os.path.join(work, file + ".pdf")
    url = f"{STUDIO}/content/storage/{file[0]}/{file[1]}/{file}.pdf"
    try:
        subprocess.run(["curl", "-fsSL", "-A", USER_AGENT, "--retry", "3", "-o", pdf, url], check=True, timeout=300)
        text = subprocess.run(["pdftotext", "-q", "-enc", "UTF-8", pdf, "-"], capture_output=True, text=True, timeout=120).stdout
    except Exception as e:
        return {"file": file, "error": str(e)}
    finally:
        if os.path.exists(pdf):
            os.remove(pdf)
    return {"file": file, "title": title, "description": description, "text": text}


def described(book):
    """What a book's text says of it: its language on the back cover, just before its level, its credits and its story's length."""
    lines = [line.strip() for line in book["text"].splitlines() if line.strip()]
    level, label = None, None
    for i, line in enumerate(lines):
        found = LEVEL.search(line)
        if found:
            level = int(found.group(1))
            label = next((m.group(1) for back in reversed(lines[max(0, i - 8):i]) for m in [LABEL.match(back)] if m), label)
    end = next((i for i, line in enumerate(lines) if any(marker in line for marker in STORY_END)), len(lines))
    body = lines[:end]
    # The cover: the title, perhaps over a few lines, then the credits.
    credits = collections.defaultdict(list)
    cover = max((i for i, line in enumerate(body[:CREDITS_WITHIN]) if CREDIT.match(line)), default=-1)
    for line in body[:cover + 1]:
        found = CREDIT.match(line)
        if found:
            credits[found.group(1).rstrip("s")].append(found.group(2).strip())
    story = [line for line in body[cover + 1 if cover >= 0 else 1:] if not line.isdigit()]
    joined = " ".join(story)
    words = sum(1 for word in joined.split() if any(c.isalpha() for c in word))
    # Unspaced scripts: about two characters a word.
    if scripts(joined) & {"CJK", "HIRAGANA", "KATAKANA"}:
        words = sum(1 for c in joined if c.isalpha()) // 2
    return {
        "label": label, "level": level, "words": words,
        "author": ", ".join(credits.get("Author", [])), "translator": ", ".join(credits.get("Translator", [])),
    }


def scan(limit, workers):
    os.makedirs(CACHE, exist_ok=True)
    done = set()
    if os.path.exists(READ):
        with open(READ) as f:
            done = {json.loads(line)["file"] for line in f if line.strip()}
    wanted = [b for b in books() if b[0] not in done and scripts(b[1]) and scripts(b[1]) <= READABLE_SCRIPTS]
    if limit:
        wanted = wanted[:limit]
    print(f"{len(done)} books read before, {len(wanted)} to read", file=sys.stderr)
    with tempfile.TemporaryDirectory() as work, open(READ, "a") as out, ThreadPoolExecutor(workers) as pool:
        futures = [pool.submit(read_book, *b, work) for b in wanted]
        for i, future in enumerate(as_completed(futures), 1):
            out.write(json.dumps(future.result(), ensure_ascii=False) + "\n")
            out.flush()
            if i % 100 == 0:
                print(f"{i}/{len(wanted)}", file=sys.stderr)


def clean(text):
    return re.sub(r"\s+", " ", text).strip()


def covers():
    """Each book's cover picture, by the id of its PDF."""
    db = sqlite3.connect(os.path.join(CACHE, f"{CHANNEL}.sqlite3"))
    rows = db.execute(
        """SELECT pdf.local_file_id, min(thumbnail.local_file_id)
           FROM content_file pdf JOIN content_file thumbnail ON thumbnail.contentnode_id = pdf.contentnode_id
           WHERE pdf.preset = 'document' AND thumbnail.preset = 'document_thumbnail' GROUP BY pdf.local_file_id"""
    ).fetchall()
    return dict(rows)


def write(min_words):
    read = [json.loads(line) for line in open(READ) if line.strip()]
    cover = covers()
    by_language = collections.defaultdict(list)
    skipped = collections.Counter()
    for book in read:
        if "error" not in book:
            book.update(described(book))
        if "error" in book:
            skipped["download failed"] += 1
            continue
        code = LANGUAGES.get(book["label"] or "")
        if code is None:
            skipped[f"language {book['label']}"] += 1
            continue
        if book["words"] < min_words:
            skipped["too few words"] += 1
            continue
        authors = ", ".join(a for a in [book["author"], book["translator"] and f"tr. {book['translator']}"] if a)
        by_language[code].append([book["file"], str(book["level"] or ""), str(book["words"]), clean(book["title"]), clean(authors), clean(book["description"])[:300], cover.get(book["file"], "")])
    os.makedirs(OUT, exist_ok=True)
    for name in os.listdir(OUT):
        if name.endswith(".tsv"):
            os.remove(os.path.join(OUT, name))
    for code, rows in sorted(by_language.items()):
        # Easiest first, then by title.
        rows.sort(key=lambda r: (r[1] or "9", r[3].lower()))
        with open(os.path.join(OUT, f"{code}.tsv"), "w") as f:
            f.write("".join("\t".join(cell.replace("\t", " ") for cell in row) + "\n" for row in rows))
    # The languages Find books offers StoryWeaver for.
    text = open(SOURCES).read()
    listed = ", ".join(f'"{code}"' for code in sorted(by_language))
    text = re.sub(r"private val STORYWEAVER_LANGUAGES = setOf<String>\((.*?)\)", f"private val STORYWEAVER_LANGUAGES = setOf<String>({listed})", text)
    with open(SOURCES, "w") as f:
        f.write(text)
    print(", ".join(f"{code} {len(rows)}" for code, rows in sorted(by_language.items(), key=lambda kv: -len(kv[1]))))
    print("left out:", ", ".join(f"{reason} {n}" for reason, n in skipped.most_common(25)), file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--limit", type=int, help="read at most this many books not read yet")
    parser.add_argument("--workers", type=int, default=8, help="books read at once")
    parser.add_argument("--min-words", type=int, default=30, help="leave out books with fewer words")
    parser.add_argument("--write", action="store_true", help="write the index from the books read")
    args = parser.parse_args()
    if args.write:
        write(args.min_words)
    else:
        scan(args.limit, args.workers)


if __name__ == "__main__":
    main()
