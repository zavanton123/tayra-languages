#!/usr/bin/env python3
"""
Fetches the texts of a book pack (tools/books/<code>/books.json) from their free sources and
writes them, cleaned, to tools/books/<code>/text/<id>.txt for tools/build_books.py.

Sources a book can name:
  {"source": "machado", "file": "contos/macn005.txt", "story": "A cartomante"}  the Machado de Assis
      corpus of machado.mec.gov.br (public domain) as packaged by NLTK; "story" picks one story of a
      collection by its title, "part": "romance" takes a whole novel after its front matter
  {"source": "wikisource", "pages": ["Triste Fim de Policarpo Quaresma/I/I", ...]}  pt.wikisource.org
  {"source": "wikibooks", "pages": [...]}  pt.wikibooks.org (CC BY-SA 4.0)
  {"source": "wikipedia", "pages": [...]}  pt.wikipedia.org leads (CC BY-SA 4.0)
  {"source": "machado-collection", "stories": [["contos/macn005.txt", "A cartomante"], ...]}  several corpus stories as one book
Several pages are joined with a blank line between them, each under its heading.

Usage: python3 tools/fetch_books.py --language pt [--only id,id...]
"""
import argparse
import io
import json
import os
import re
import subprocess
import sys
import time
import urllib.parse
import zipfile
from html.parser import HTMLParser

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", "books", ".cache")
MACHADO_ZIP = "https://raw.githubusercontent.com/nltk/nltk_data/gh-pages/packages/corpora/machado.zip"
USER_AGENT = "TayraLanguagesBookPacks/1.0 (https://github.com/zavanton123/tayra-languages)"


def fetch(url, retries=4):
    """Through curl, whose certificate store is the system's; the Wikimedia sites want a User-Agent."""
    for attempt in range(retries):
        result = subprocess.run(["curl", "-sS", "-L", "-m", "90", "-A", USER_AGENT, url], capture_output=True)
        if result.returncode == 0 and result.stdout:
            return result.stdout
        if attempt == retries - 1:
            raise RuntimeError(f"could not fetch {url}: {result.stderr.decode(errors='replace').strip()}")
        time.sleep(2 * (attempt + 1))


def cached(url):
    os.makedirs(CACHE, exist_ok=True)
    name = re.sub(r"[^A-Za-z0-9_.-]+", "_", url)[-150:]
    path = os.path.join(CACHE, name)
    if os.path.exists(path):
        return open(path, "rb").read()
    data = fetch(url)
    open(path, "wb").write(data)
    return data


class WikiText(HTMLParser):
    """The readable text of a MediaWiki page's HTML: no headers, navigation, references, tables or edit links."""

    SKIP_CLASSES = ("ws-noexport", "noprint", "mw-editsection", "reference", "mw-ref", "navbox", "toc", "headertemplate", "navigation", "mw-headline-number", "printfooter", "catlinks", "thumb", "gallery", "wikitable", "infobox", "metadata", "ambox", "hatnote", "mbox", "mw-jump", "shortdescription", "mw-cite")
    SKIP_TAGS = ("style", "script", "table", "sup", "figure", "img", "noscript")
    BLOCKS = ("p", "div", "h1", "h2", "h3", "h4", "h5", "li", "br", "blockquote", "dd", "dt", "tr", "center", "section")

    def __init__(self):
        super().__init__()
        self.out = []
        self.stack = []  # (tag, skipped)

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        classes = (a.get("class") or "").split()
        ids = a.get("id") or ""
        skipped = tag in self.SKIP_TAGS or any(c.startswith(self.SKIP_CLASSES) for c in classes) or ids in ("toc", "navigationHeader")
        if tag in ("br", "img"):
            if tag == "br" and not self.skipping():
                self.out.append("\n")
            return
        self.stack.append((tag, skipped))
        if tag in self.BLOCKS and not self.skipping():
            self.out.append("\n")

    def handle_endtag(self, tag):
        for i in range(len(self.stack) - 1, -1, -1):
            if self.stack[i][0] == tag:
                del self.stack[i:]
                break
        if tag in self.BLOCKS and not self.skipping():
            self.out.append("\n")

    def skipping(self):
        return any(skipped for _, skipped in self.stack)

    def handle_data(self, data):
        if not self.skipping():
            self.out.append(data)

    def text(self):
        text = "".join(self.out)
        text = text.replace(" ", " ")
        text = re.sub(r"[ \t]+", " ", text)
        text = re.sub(r" *\n *", "\n", text)
        text = re.sub(r"\n{3,}", "\n\n", text)
        return text.strip()


def wiki_api(site, params):
    query = urllib.parse.urlencode({**params, "format": "json"})
    return json.loads(cached(f"https://{site}/w/api.php?{query}"))


def wiki_page_text(site, page, heading=True):
    reply = wiki_api(site, {"action": "parse", "page": page, "prop": "text|displaytitle", "disabletoc": 1})
    if "error" in reply:
        raise RuntimeError(f"{site}: {page}: {reply['error'].get('info')}")
    parser = WikiText()
    parser.feed(reply["parse"]["text"]["*"])
    text = parser.text()
    if not heading:
        return text
    title = re.sub(r"<[^>]+>", "", reply["parse"].get("displaytitle") or page).split("/")[-1].strip()
    if text.split("\n", 1)[0].strip().casefold() == title.casefold():
        return text
    return f"{title}\n\n{text}"


def wikipedia_intro(page):
    reply = wiki_api("pt.wikipedia.org", {"action": "query", "prop": "extracts", "explaintext": 1, "exintro": 1, "redirects": 1, "titles": page})
    p = list(reply["query"]["pages"].values())[0]
    text = (p.get("extract") or "").strip()
    if not text:
        raise RuntimeError(f"pt.wikipedia.org: {page}: no extract")
    text = re.sub(r"\n{3,}", "\n\n", text)
    return f"{p['title']}\n\n{text}"


def machado_corpus():
    data = cached(MACHADO_ZIP)
    return zipfile.ZipFile(io.BytesIO(data))


def machado_file(name):
    with machado_corpus() as z:
        # Windows-1252: the corpus writes its dashes and curly quotes above Latin-1.
        return z.read("machado/" + name).decode("cp1252", errors="replace").replace("\r\n", "\n")


def unwrap(text):
    """The corpus wraps lines inside paragraphs; paragraphs are blank-line separated, so lines are joined back."""
    paragraphs = re.split(r"\n\s*\n", text)
    joined = []
    for paragraph in paragraphs:
        lines = [line.strip() for line in paragraph.split("\n") if line.strip()]
        if not lines:
            continue
        joined.append(" ".join(lines))
    return "\n\n".join(joined)


def machado_story(file, story):
    """
    One story of a collection. The file opens with an index of the stories, one title each
    (broken over lines when long), and each story then begins with its title again.
    """
    raw = machado_file(file)
    lines = raw.split("\n")

    def norm(text):
        return re.sub(r"\s+", " ", text.strip().upper())

    def is_heading(text):
        """An upper-case line short enough to be a title or a piece of one ("A" / "CARTOMANTE", "D." / "PAULA")."""
        t = re.sub(r"\s+", " ", text.strip())
        return bool(t) and t == t.upper() and any(c.isalpha() for c in t) and len(t) < 80 and not t.endswith(",") and (not t.endswith(".") or len(t) <= 3)

    index_start = next((i for i, line in enumerate(lines) if norm(line) == "ÍNDICE"), None)
    if index_start is None:
        raise RuntimeError(f"{file}: no index")
    titles = []
    i = index_start + 1
    while i < len(lines):
        if not lines[i].strip():
            i += 1
            continue
        if not is_heading(lines[i]):
            break
        parts = []
        while i < len(lines) and lines[i].strip() and is_heading(lines[i]):
            parts.append(lines[i].strip())
            i += 1
        titles.append(norm(" ".join(parts)))
    index_end = i
    wanted = norm(story)
    if wanted not in titles:
        raise RuntimeError(f"{file}: story {story!r} is not in the index {titles}")

    def heading_at(j):
        """The index title starting at line j, over consecutive heading lines, with the line after it."""
        parts, k = [], j
        while k < len(lines) and lines[k].strip() and is_heading(lines[k]) and len(parts) < 4:
            parts.append(lines[k].strip())
            k += 1
            if norm(" ".join(parts)) in titles:
                return norm(" ".join(parts)), k
        return None, j

    body_from = None
    j = index_end
    while j < len(lines):
        title, after = heading_at(j)
        if title == wanted:
            body_from = after
            break
        j += 1
    if body_from is None:
        raise RuntimeError(f"{file}: story {story!r} has no text after the index")
    stop = len(lines)
    j = body_from
    while j < len(lines):
        title, _ = heading_at(j)
        if title and title != wanted:
            stop = j
            break
        j += 1
    body = "\n".join(lines[body_from:stop])
    return f"{story}\n\n{strip_chapter_index(unwrap(body))}"


def strip_chapter_index(text):
    """A story with chapters lists them first; the list runs up to the second time the first chapter's heading appears."""
    paragraphs = text.split("\n\n")
    if not paragraphs or paragraphs[0].strip().upper() != "ÍNDICE" or len(paragraphs) < 3:
        return text
    first = paragraphs[1].strip()
    later = [k for k in range(2, len(paragraphs)) if paragraphs[k].strip() == first]
    if not later:
        return text
    return "\n\n".join(paragraphs[later[0]:])


def machado_novel(file):
    raw = machado_file(file)
    # Front matter ends where the first chapter heading starts.
    match = re.search(r"^(CAPÍTULO|CAPITULO|PRÓLOGO|AO LEITOR|ADVERTÊNCIA|I\n)", raw, re.MULTILINE)
    body = raw[match.start():] if match else raw
    body = re.sub(r"\n\s*FIM\s*$", "", body.strip())
    return unwrap(body)


def attempt(fetcher, *args, **kwargs):
    """A page that cannot be read is left out, with a warning, so one missing chapter does not stop the bundle."""
    try:
        return fetcher(*args, **kwargs)
    except Exception as e:  # noqa: BLE001
        print(f"  warning: {e}", file=sys.stderr)
        return None


def text_for(book):
    source = book["source"]
    if source == "machado":
        if "story" in book:
            return machado_story(book["file"], book["story"])
        return machado_novel(book["file"])
    if source == "machado-collection":
        return "\n\n".join(machado_story(file, story) for file, story in book["stories"])
    if source in ("wikisource", "wikibooks"):
        site = "pt.wikisource.org" if source == "wikisource" else "pt.wikibooks.org"
        parts = [found for found in (attempt(wiki_page_text, site, page, heading=book.get("headings", True)) for page in book["pages"]) if found]
        return "\n\n".join(parts)
    if source == "wikipedia":
        return "\n\n".join(found for found in (attempt(wikipedia_intro, page) for page in book["pages"]) if found)
    raise RuntimeError(f"unknown source {source!r}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--language", required=True)
    parser.add_argument("--only", default="")
    args = parser.parse_args()
    folder = os.path.join(ROOT, "tools", "books", args.language)
    manifest = json.load(open(os.path.join(folder, "books.json"), encoding="utf-8"))
    only = {s for s in args.only.split(",") if s}
    os.makedirs(os.path.join(folder, "text"), exist_ok=True)
    for book in manifest["books"]:
        if only and book["id"] not in only:
            continue
        text = text_for(book)
        if book.get("append"):
            text = text.rstrip() + "\n\n" + book["append"]
        path = os.path.join(folder, "text", f"{book['id']}.txt")
        open(path, "w", encoding="utf-8").write(text.strip() + "\n")
        words = len(re.findall(r"\w+", text))
        print(f"{book['id']}: {words} words -> {os.path.relpath(path, ROOT)}")


if __name__ == "__main__":
    main()
