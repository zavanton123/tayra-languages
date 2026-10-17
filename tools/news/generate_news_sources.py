#!/usr/bin/env python3
"""
Writes core/domain/.../news/NewsSources.kt from tools/news/feeds.json, the news feeds checked to
work for each language (a feed answers a plain request, has articles from the last month, and an
article's page holds its text). Run it after editing feeds.json.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SOURCE = os.path.join(ROOT, "tools", "news", "feeds.json")
TARGET = os.path.join(ROOT, "core", "domain", "src", "commonMain", "kotlin", "com", "tayra", "languages", "core", "domain", "news", "NewsSources.kt")


def kotlin_string(text: str) -> str:
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def main() -> None:
    with open(SOURCE, encoding="utf-8") as f:
        languages = json.load(f)
    lines = []
    for language in sorted(languages, key=lambda item: item["language"]):
        for source in language["sources"]:
            level = "NewsLevel.EASY" if source.get("level") == "easy" else "NewsLevel.STANDARD"
            # A source checked to carry its articles whole in the feed is read from it.
            in_feed = ", textInFeed = true" if source.get("extraction") == "feed" else ""
            lines.append(
                f'        NewsSource({kotlin_string(language["language"])}, {kotlin_string(source["name"])}, '
                f'{kotlin_string(source["feedUrl"])}, {kotlin_string(source.get("homepage", ""))}, {level}{in_feed}),'
            )
    body = "\n".join(lines)
    with open(TARGET, "w", encoding="utf-8") as f:
        f.write(f"""package com.tayra.languages.core.domain.news

// Written by tools/news/generate_news_sources.py from tools/news/feeds.json; edit those instead.

/** The news feeds the app knows, by language: easy-language outlets first, then general news. */
object NewsSources {{
    val all: List<NewsSource> = listOf(
{body}
    )
}}
""")
    print(f"{len(lines)} sources in {len(languages)} languages")


if __name__ == "__main__":
    main()
