# News feeds: verification notes (2026-10-17)

## How the feeds were checked

Every source in `feeds.json` was checked on 2026-10-17 with curl, using User-Agent `TayraLanguages/1.0 (https://github.com/zavanton123/tayra-languages)` and a normal `Accept: text/html,…` header. For each one:

- The feed returned HTTP 200 and parsed as RSS or Atom (with feedparser).
- It had items from the last 30 days.
- The item titles and article text were in the target language (checked by eye from samples).
- The **three newest** article pages were fetched. Each had to return HTTP 200, show no Cloudflare, Incapsula, DataDome or PerimeterX challenge, and have no `isAccessibleForFree=false` paywall.

`extraction` records where the text was found, by majority over the three pages:

- `feed`: the item's `content:encoded` or `description` holds the full text.
- `jsonld`: the text is in a schema.org `articleBody`.
- `article`: the text is in `<p>` tags inside an `<article>` element.
- `paragraphs`: the text is only in `<p>` tags under a common parent or grandparent. All BBC World Service pages work this way, because each `<p>` sits in its own `<div>` under `<main>`.

`articleWords` is the median over the good samples, rounded to the nearest 10. For zh, yue and ja it is characters ÷ 2. For th, lo and km it is characters ÷ 5. For bo it counts syllables. Treat all of these as rough.

Scripts and raw results are in this folder: `check.py`, `disc.py`, `r*.jsonl` and `assemble.py`.

## Languages with no usable feed

| Lang | Why |
|---|---|
| br Breton | Ya! (yabzh.com) blocks curl with a challenge, and its feed also returns 403. Radio Kerne, RKB, Brezhoneg.bzh and Arvorig FM have no feed, or their feed is in French or unreachable. Ouest-France and Le Télégramme publish in French. |
| la Latin | Ephemeris moved to ephemeris.albinus.fr (the old alcuinus.net has a self-signed certificate and a meta-redirect). It is live (October 2026 items) but has no RSS feed; articles are at `nuntius.php?id=N`, so it could be scraped if needed. The Vatican News Latin page advertises `la.rss.xml`, but that returns 404. Radio Bremen's Nuntii Latini page returns 404. |
| gv Manx | No Manx-language news site with a feed was found. Manx Radio is in English, and pabyrseyr.com does not resolve. |
| kw Cornish | Nothing found. The Cornish-language organisations' sites carry English news. |
| yi Yiddish | forverts.forward.com does not resolve. yiddish.forward.com/rss is stale (2019). forward.com/yiddish/feed/ is live, but its items are in English and its article pages return 403. |
| arz Egyptian Arabic | Egyptian outlets write in Modern Standard Arabic, not the dialect. Mada Masr and Masrawy also block curl (403 or challenge). Use the `ar` feeds instead. |
| sa Sanskrit | Sudharma is published only as an e-paper. NewsOnAir has Sanskrit bulletins but no feed (and it often timed out). |
| Gothic, Ancient Greek, Old Irish, Toki Pona, Ainu, Okinawan, Navajo, Nahuatl, Hawaiian, Assyrian | No live news feeds; not searched in depth. |

Languages with only one source: ceb (SunStar Superbalita Cebu), fo (Kringvarp Føroya), he (Ynet), is (RÚV), mt (Illum), pa (BBC Punjabi), gd (BBC Naidheachdan).

## Notable failures, by reason

### Blocks non-browser clients (403 or challenge on the feed or article pages)

- **Feed blocked**: TVN24 (pl), Kompas (id), Walla, Maariv and Kan (he), T24 and Bianet (tr), Suspilne and Hromadske (uk).
- **Feed fine, articles blocked**: Maroela Media (af), Público (pt), iROZHLAS (cs), Denník N (sk), Bangla Tribune (bn), Matichon (th), AZERTAC (az), inmediahk (yue), Davar, Israel Hayom and Zman Yisrael (he), NV and Ukrainska Pravda (uk, partly), Bernardinai, Lrytas and Delfi.lt (lt), Fokus (mk), OFFNews (bg), mbl.is (is), NYT 中文网 (timed out).
- **LSM (lv)**, the Latvian public broadcaster: the feed works but every article page returns 403. This is the biggest public-broadcaster loss.
- **El País (es)**: 2 of 3 article pages returned 403, and the pages carry a paywall flag.
- **N12 / mako (he)**: PerimeterX "your activity and behavior…" page on 2 of 3 articles.

### Paywall or teaser only

- **Klar Tale (no)**, the main easy-Norwegian outlet: the feed works but articles show a paywall teaser only. This means there is **no easy Norwegian feed**.
- Also excluded for paywalls: Heimildin (is), Haaretz (he), Globes (he), El Mundo (es), Postimees and Õhtuleht (et), TVNET (lv; its feed also links to pmo.ee pages), Delo (sl), 444 (hu), OKO.press (pl, plus live blogs).
- **Kept despite partial paywalls, with a note**: Asahi and Mainichi (ja, 有料記事 show only the lead), Spiegel (SPIEGEL+), VG (VG+), Aftonbladet (Plus).
- **Not used but workable**: Ara (ca) and elDiario.es (es). Both set `isAccessibleForFree=false` but the full text was in the HTML. They weren't needed because each language already had enough sources.

### Client-rendered pages (text only in a JS or Next.js payload, not in the HTML)

- **NHK (ja)**: NHK moved to `news.web.nhk` ("NHK ONE") as a Next.js app, so article HTML has almost no text. The NHK News Web **Easy** `news-list.json` (LWT's source) now returns **HTTP 401**. There is **no working easy-Japanese source**.
- Others: Sveriges Radio Ekot (sv), Belsat (be), Vísir (is), NU.nl (nl), Sky News Arabia (ar), MIA (mk), RTP (pt; its feed also leads with radio bulletins), Dong-A and Chosun (ko).
- **Workarounds used**: RÚV (is) and Radio Sweden på lätt svenska (sv) are client-rendered too, but their feed descriptions carry the full text, so they are kept with `extraction: feed`.

### Video, audio or bulletin items with little text

- **VOA (en, am, km, vi, id, sq…)**: since the 2025 cuts, the `/api/` article feeds are frozen (last items March to April 2025). The live `/rss/` feeds are radio-programme podcasts, so VOA is excluded everywhere. This includes **VOA Learning English**: all its text feeds stopped in March 2025, and only the podcast is current.
- **DW learning feeds (de)**: DW "Langsam gesprochene Nachrichten" and "Top-Thema mit Vokabeln" point to learngerman.dw.com, a JS app with no text in the HTML. LWT's DKpodcast_lgn_de entry now fails.
- **Other easy-German candidates**:
  - nachrichtenleicht.de (Deutschlandfunk) has no text feed. Its podcast feed links to audio pages.
  - NDR Leichte Sprache items are videos.
  - ZDF logo! items are videos.
  - Tagesschau in Einfacher Sprache is a TV programme with no feed.
  - APA TopEasy's domain no longer resolves.
- **Mostly video or audio**: Radio Farda (fa), VOA 中文 (zh), the Azadliq site-wide feed (az), G1 (pt), and France 24 es/ar.
- **Observador (pt)**: the newest feed items are hourly radio bulletins, so it was swapped for Notícias ao Minuto.

### Stale or empty feeds

- **Stale**: Wablieft (nl; last item 2026-09-15, and the site now pushes the paid paper), Global Voices Esperanto (last 2026-09-13), EITB (eu; 2025), RFA Cantonese (2016), Watanoc (ja; 2016), VOA Tibetan `/api/` (March 2026).
- **Bengali timing**: DW Bangla posted only about 3 items in the last 30 days. It still passes, but expect a slow feed.
- **Empty**: RFE/RL Bulgarian (svobodnaevropa.bg) and Macedonian (slobodnaevropa.mk) feeds have **0 items**. These services look paused.
- **Moved**: RFE/RL Azerbaijani moved from radioazadliq.org (NXDOMAIN) to azadliq.org. Its feeds are mostly multimedia, so it is excluded.
- **Wrong or missing feed**: the old Yle Selkouutiset tag feed (`yle.fi/rss/t/18-205942/fi`) is stale; the working one is `yle.fi/rss/selkouutiset`. DW has no working feed for Hindi, Amharic, Bulgarian, Macedonian or French: `rss-<code>-all` returns 200 with an empty channel.

### Wrong language

- **Korea Times (ko)**, in LWT's list: the feed is English, so it was dropped.
- **GMA "nation"** (tl) is English. **Naiz** "eu" feed items are Spanish. **local.fo** and **Tibet Express** are English.

### Unreachable from here

- **Maltese**: tvmnews.mt, newsbook.com.mt and netnews.com.mt timed out at the TCP level, which may be geo or firewall related. ONE and Kullhadd use challenges. L-Orizzont and iNews have no feed. Only Illum works.
- **Mongolian**: Montsame (montsame.mn) timed out.

## Things the app should handle

1. **UN News feeds** (`news.un.org/feed/subscribe/<lang>/news/all/rss.xml`): item links like `/feed/view/en/story/…` return **HTTP 406**. Drop `/feed/view` (giving `/en/story/…`) and the pages work. With that rewrite, every UN language passed (en, fr, es, ru, ar, zh, pt, sw, hi). Only zh and ar are in `feeds.json`; the others are good spares.
2. **BBC Chinese**: `feeds.bbci.co.uk/zhongwen/simp/rss.xml` returns the same traditional-character titles as the `trad` feed, and its links end in `/trad`. Rewrite `/trad?` to `/simp?` for simplified article pages (verified).
3. **Illum (mt)**: its `pubDate` is `Fri,16 Oct 2026 …`, with no space after the comma. feedparser can't parse it, so use a lenient date parser.
4. **BBC World Service pages**: text sits in `<p>` elements, each in its own `<div>` under `<main>`. Extractors that only look at a single parent container miss it.
5. **RFE/RL `/api/` feeds**: the site-wide feed mixes in video and audio items. Section feeds (listed on each site's `/rssfeeds` page) can be cleaner. Kazakh uses the "Қазақстан" section feed for this reason.
6. **Proxy note**: this machine routes traffic through a local proxy on 127.0.0.1:9090. "CONNECT tunnel failed 502/504" errors were DNS failures or upstream timeouts, not blocks.

## Surprises

- **RFA is live**: Mandarin, Korean, Vietnamese, Khmer, Lao and Tibetan all have current `/RSS` feeds with full text in `content:encoded`. RFA Cantonese is dead.
- **Easy-language coverage is thin**: verified easy sources exist only for en, de, fi, fr, sv, es, nl, eo and cy.
  - **Lost since LWT's March list**: NHK Easy (ja), DW Langsam gesprochene Nachrichten (de) and VOA Learning English (en).
  - **Not usable**: Klar Tale (no) and Wablieft (nl).
  - **None found**: easy-news outlets for da, et, lv, lt, it (Due Parole has no feed), ru, ko or zh.
- **Fewer sources than expected**: Hebrew and Lithuanian have the fewest working sources among major languages, because most Israeli and Lithuanian sites block non-browser clients.
- **Cantonese**: no outlet publishes news in written colloquial Cantonese. The `yue` entries (RTHK, BBC traditional) are standard written Chinese as read in Hong Kong, and this is flagged in each note.

## After the research (2026-10-17)

- Four feeds redirected from https to plain http, which the app's HTTP client rightly refuses: Nació Digital,
  Pravda.sk, N1 Slovenija and RTHK. `feeds.json` now has the https addresses they lead to.
- Rechecked from the same machine an hour later, Radio Prague's article pages answered 403 to every client
  and Tibet Times answered 429, most likely rate limits after the many checks; both are kept.

## Global Voices (added 2026-10-17)

Global Voices publishes feature stories (CC BY 3.0) and volunteers translate them into dozens of
languages; each edition's feed (`<code>.globalvoices.org/feed/`) carries the full text. Of 42 editions
found, the 23 with an article since mid-2025 are listed; quieter ones (Albanian, Czech, Danish,
Hebrew, Hungarian, Korean, Polish, Swahili, Swedish, Turkish, Urdu, Filipino...) are left out. The
Chinese edition is in traditional characters.
