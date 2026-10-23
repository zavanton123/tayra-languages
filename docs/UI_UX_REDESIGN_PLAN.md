# Tayra Languages UI/UX Redesign Plan

Status: implementation-ready proposal  
Date: 23 October 2026  
Scope: Android, iOS, desktop and web Compose UI, with mobile as the first implementation target

## 1. Decision summary

The redesign should be driven by learning flows, not by a new visual style. Tayra already has a coherent colour system, typography scale, icon library and adaptive Compose architecture. The work should therefore:

1. Make the next useful learning action obvious.
2. Connect reading, vocabulary, flashcards and listening into one loop.
3. Reduce controls shown before content.
4. Make state, filters, downloads and recovery actions explicit.
5. Keep mobile layouts compact and touch-friendly.
6. Harmonise desktop with the same interaction model without turning desktop tables and panes into phone cards.

No mandatory generated image assets are needed. The redesign should primarily use existing vector icons, theme tokens and Compose components.

## 2. Product experience to optimise

The intended learning loop is:

```text
Choose a text -> Read or listen -> Inspect and save words -> Review cards -> Continue reading
```

Every primary screen should either advance this loop or make the next step clear.

### Primary journeys

#### Returning learner

```text
Home -> Continue the highest-priority activity -> Completion -> Recommended next activity
```

Priority order:

1. A due or interrupted activity.
2. Course or book in progress.
3. Flashcards due today.
4. Playlist in progress.
5. Discovery when nothing is underway.

#### New learner

```text
Choose learning language -> Set an approximate vocabulary level -> Get required offline resources -> Start a course or text
```

The setup should explain consequences in plain language and allow non-essential downloads to be postponed.

#### Reading and vocabulary

```text
Open text -> Select a word -> Hear or translate it -> Set knowledge status -> Continue from the same position
```

Opening a word, examples, conjugations or reader settings must not lose the reading position or make the text jump.

#### Flashcard review

```text
Review due cards -> See the next interval before rating -> Complete session -> Continue the last reading activity
```

Word knowledge and flashcard scheduling must be presented as separate concepts.

#### Playlist listening

```text
Create playlist -> Add texts -> Play queue -> Recover from missing voice or text -> Resume or complete
```

## 3. Experience principles

### One primary action per view

A screen may contain many actions, but only one should be visually dominant. Filled buttons are reserved for the next likely step.

### Content before controls

Continue cards and content should appear before search, sort and advanced filters. On phones, advanced filters belong in bottom sheets.

### Stable controls

Controls used repeatedly during an activity must remain in the same location. Examples include flashcard ratings and reader playback.

### Progressive disclosure

Show the common path first. Advanced scheduling fields, resource diagnostics and large filter sets should be expandable or placed in dedicated sheets/dialogs.

### Explicit state

Do not hide important state behind a small badge. Show what is filtered, downloading, unavailable, selected or scheduled, with a nearby way to change it.

### Preserve context

Sheets and dialogs should be preferred when a learner needs temporary detail and should return to the current text, list position or playback position afterward.

### Consistent language

Use separate vocabularies for separate concepts:

- Word knowledge: `New`, `Learning`, `Known`, `Ignored`.
- Review scheduling: `First time`, `In steps`, `Due`, `Scheduled`, `Relearning`.
- Download state: `Not downloaded`, `Downloading`, `Downloaded`, `Failed`.

## 4. Information architecture

### Mobile bottom navigation

Keep the existing five destinations:

1. Home
2. Courses
3. Books
4. Flashcards
5. More

Do not add another permanent tab until usage data proves it is needed. Vocabulary, News, Playlists and Settings remain reachable from More, while Home exposes contextual shortcuts to due vocabulary and active playlists.

### More

More should be a concise destination hub rather than a long miscellaneous menu.

Recommended groups:

- **Read and listen:** News, Playlists, Find books.
- **Words and progress:** Vocabulary, Word frequency, Statistics.
- **Offline resources:** Dictionaries, Speech, Translation, Course packs, Book packs.
- **Preferences and data:** Settings, Languages, Backups, About.

Destructive `Clear data` remains inside Settings/Data rather than as a first-level More action.

### Desktop navigation

Keep the top navigation and persistent side panes. Align names and grouping with mobile, but preserve desktop advantages:

- tables;
- multi-column layouts;
- persistent dictionary panes;
- hover and keyboard interactions;
- inline filters when they fit.

## 5. Shared UI foundation

Implement these primitives before duplicating patterns across features.

### CompactContinueCard

For Home, Courses, Books and Playlists:

- title and context;
- optional icon or cover;
- one progress measure;
- one primary action;
- entire card may open details, while the button performs the immediate action.

### DenseContentRow

For books, vocabulary, flashcards, downloads and backup history:

- minimum 48dp touch target;
- one title, one supporting/meta line;
- optional leading icon;
- trailing state or action;
- no duplicated footer separated by a divider.

### CompactFilterBar

- Search on its own row when necessary.
- One visible primary segment or scope.
- Filter and Sort buttons open sheets.
- Labels must not clip at 130% system font size.

### ActiveFilterSummary

Shows the applied state in plain language, for example:

```text
Learning 1–4 · 12 terms                    Show all
```

The number on the Filters button must equal the number of visible filter groups, not the number of individual statuses hidden inside a group.

### StableBottomActions

For reader, flashcards and practice:

- fixed above system navigation;
- respects navigation-bar insets;
- action positions do not move when state changes;
- content scrolls behind or above it with sufficient bottom padding.

### StatusNotice

One shared pattern for warnings, downloads and recoverable errors:

- correct icon and semantic colour;
- plain headline;
- short consequence;
- next action inside the notice;
- optional details link, not an always-visible diagnostic dump.

### Shared files

- `core/ui/.../components/AppTopBar.kt`
- `core/ui/.../components/ContentCards.kt`
- `core/ui/.../components/Controls.kt`
- `core/ui/.../components/PickerSheets.kt`
- `core/ui/.../components/PhoneNavigation.kt`
- `core/ui/.../theme/Tokens.kt`

Existing uncommitted work in shared components must be reviewed and preserved before editing.

## 6. Screen plans

### 6.1 Home

Goal: answer “What should I do next?” without requiring scrolling.

Changes:

- Replace the sequence of equally weighted large cards with one primary continue card and compact secondary rows.
- Put due flashcards above non-urgent activities.
- Keep today/streak statistics small and secondary.
- Do not offer `Continue book` for a completed book.
- Add contextual playlist continuation when a queue is active.
- End cards with a single action; avoid a second competing card-level CTA.

Primary file:

- `feature/home/.../HomeScreen.kt`

Acceptance:

- At least the primary activity, due flashcards and two statistics fit above the bottom navigation at 400x860dp.
- The first filled action reflects the highest-priority available activity.
- Empty/new-learner state makes `Browse courses` the obvious next step.

### 6.2 Courses

Goal: resume learning before browsing and make discovery compact.

Changes:

- Move Continue learning directly beneath the toolbar/subtitle.
- Place search and browsing controls after Continue.
- Use a compact status scope plus Filter and Sort sheet buttons.
- Group recommended courses by level, but reduce the height of level headers.
- Reduce course-card metadata to title, topic, lesson count/common-word range and one progress state.
- Hide redundant built-in source tags.
- Use `Common words 1–100`, not unexplained internal rank language.

Primary file:

- `feature/courses/.../PhoneCourses.kt`

Acceptance:

- Two complete course cards and part of a third are visible on a typical phone after the continue card.
- `Not started` and other status labels do not clip at 130% text.
- Continue remains ahead of filters.

### 6.3 Books

Goal: make the library a scannable reading queue.

Changes:

- Keep the continued book out of the repeated library list while its continue card is visible.
- Combine search, filters and sort into fewer rows.
- Replace tall book cards with dense rows/cards.
- Use one metadata line: word count, source and last opened.
- Show status only when it provides information such as Finished.
- Keep Read and Known terminology consistent across list and continue card.

Primary files:

- `feature/books/.../BooksScreen.kt`
- `feature/books/.../PhoneBooks.kt`

### 6.4 Reader

Goal: make the text the dominant object and preserve reading context.

Changes:

- On phones, show the title once in the toolbar.
- Remove redundant breadcrumb/title space above the text.
- Hide the pager for a one-page text; otherwise keep a compact page indicator in the toolbar.
- Remove the outer card and render the text on the reading background with 16dp gutters.
- Move sentence playback controls into a paragraph/sentence gutter so icons never split text.
- Keep a stable bottom playback/action bar.
- Open reader appearance as a partial-height, non-obscuring sheet.
- Open word details as a sheet with word, pronunciation, translation, status and first dictionary sense visible immediately.
- Open examples and conjugations in large dialogs/sheets over the reader and restore the same scroll position on close.

Primary files:

- `feature/reading/.../ReadingScreen.kt`
- `feature/reading/.../ReaderPane.kt`
- `feature/terms/.../form/ConjugationDialog.kt`
- `feature/terms/.../examples/ExamplesSearchScreen.kt`

Acceptance:

- The first paragraph begins within the upper quarter of a phone screen.
- Speaker controls never appear between words.
- Selecting a term does not reflow the text.
- Closing any reader sheet/dialog preserves page and scroll position.
- Reader remains usable at 130% font scale and in RTL scripts.

### 6.5 Vocabulary

Goal: make the current result set and filter state unambiguous.

Changes:

- Collapse a contiguous default filter into `Learning 1–4`.
- Display `shown of total` or the filtered count beside the filter summary.
- Put `Show all` next to the active filter.
- Use dense term rows with speaker, term, translation, knowledge status, date and overflow action.
- Prefer continuous loading on phones; if pagination remains, simplify it and keep it below the list.
- Keep bulk-selection affordances out of the normal browsing state.

Primary files:

- `feature/terms/.../list/PhoneTerms.kt`
- `feature/terms/.../list/TermsScreen.kt`

Acceptance:

- A default-filtered list cannot look like vocabulary has disappeared.
- At least five normal term rows fit on a typical phone.
- Filter count and visible filter summary agree.

### 6.6 Flashcards

Goal: keep review controls predictable and explain scheduling outcomes.

Changes:

- Reserve a fixed bottom slot for Show answer and rating buttons.
- Show the computed next interval under Again, Hard, Good and Easy.
- Keep Previous and Edit visible; move Suspend and Delete into overflow.
- Distinguish word knowledge from review scheduling language.
- At session completion, provide `Continue reading` as the main action and Previous as an undo/supporting action.

Primary files:

- `feature/flashcards/.../FlashcardsScreen.kt`
- `feature/flashcards/.../PhoneBrowseFlashcards.kt`

### 6.7 News, Wikis and Find books

Goal: show content before advanced discovery controls.

Changes:

- Keep search, content type and one Filters entry visible.
- Keep source, topic, period, difficulty and length in a sheet.
- Show an ActiveFilterSummary after the sheet closes.
- Make the whole result card open content.
- Use a quiet secondary card action rather than a filled button on every item.
- Use known percentage, length and difficulty as one metadata line.

Primary files:

- `feature/news/.../NewsScreen.kt`
- `feature/news/.../Wikipedia.kt`
- `feature/books/.../FindBooksScreen.kt`

Note: News filters are already sheet-based on compact layouts. This work should build on that implementation rather than replace it.

### 6.8 Playlists

Goal: complete the new listen-later flow and make playback resilient.

Changes:

- Preserve the existing playlist library, detail/edit, add-text, player, empty, delete, completion and interruption states.
- Add playlist continuation to Home when relevant.
- Keep the mini-player above phone navigation.
- Preserve queue and playback position across navigation.
- Missing voice: offer Download voice, Use system voice and Skip text.
- Missing/deleted text: explain what disappeared and offer Remove from playlist or Skip.

Primary files:

- `feature/playlists/.../PlaylistsScreen.kt`
- `feature/playlists/.../PlaylistDetail.kt`
- `feature/playlists/.../PlayerScreen.kt`
- `feature/playlists/.../PlayerBar.kt`
- `feature/playlists/.../AddToPlaylist.kt`

### 6.9 More, Settings and offline resources

Goal: separate preferences from resources and recovery tasks.

Changes:

- Regroup More according to the information architecture above.
- Add an Offline resources destination or clear group for Dictionaries, Speech and Translation.
- Keep learning-language changes in Languages rather than duplicating native-language controls.
- Use one download state vocabulary and one StatusNotice pattern.
- Put Install/Download in the notice that explains why it is required.
- Make Settings an index of preference areas with current values.
- Keep destructive actions inside a dedicated Data section.

Primary files:

- `core/ui/.../components/PhoneNavigation.kt`
- `feature/settings/.../PhoneSettings.kt`
- `feature/settings/.../PhoneTranslation.kt`
- `feature/settings/.../PhoneSpeech.kt`
- `feature/settings/.../PhoneDictionaries.kt`
- `feature/languages/.../PhoneLanguages.kt`

## 7. Desktop redesign alignment

Desktop should share the same hierarchy and language, while retaining density.

Required desktop changes:

- Put Continue before search and filters on Courses and Books.
- Keep tables for Vocabulary and dense resource lists.
- Simplify filters without hiding common controls that fit.
- Keep dictionary and reader panes persistent.
- Use the same StatusNotice and terminology as mobile.
- Add every settings destination to the Settings index.
- Standardise page headers, back behaviour and status placement.
- Keep desktop-only hover, keyboard shortcuts and context menus.

Do not mechanically scale phone cards across desktop widths.

## 8. Asset strategy

### Required

No new raster assets.

Use:

- `AppIcons` and Material vectors;
- `IconTile`, `CodeTile`, `LevelTile` and playlist cover colours;
- theme surfaces and status colours;
- existing app logo and flags.

### Optional

- A small vector set of course-topic glyphs.
- One reusable empty-state illustration family.
- Subtle vector patterns for playlist covers.

These should be SVG/vector or Compose paths, not generated PNG screenshots.

### News thumbnails

The current `NewsArticle` model has no image URL. Do not ship AI-generated static images for real news.

If genuine thumbnails are later required:

1. Add an optional image URL to `NewsArticle`.
2. Extract feed enclosures or Open Graph images.
3. Add asynchronous loading, caching, placeholders and failure states.
4. Verify publisher usage and attribution requirements.

This is a separate product/data feature and is not required for the first redesign.

## 9. Adaptive and accessibility requirements

Every redesigned screen must be checked at:

- 360dp, 400dp and 420dp phone widths;
- compact landscape where applicable;
- medium and expanded desktop widths;
- 100% and 130% system font scales;
- Default, Sepia and at least one dark theme;
- long translated labels;
- RTL text in the reader;
- keyboard navigation on desktop;
- reduced motion.

Minimum requirements:

- 48dp touch targets, even when the drawn icon is smaller.
- No clipped primary labels at 130% text.
- Visible focus indicators.
- Semantic descriptions for icon-only actions.
- Content never hidden by bottom navigation, player bars or gesture insets.
- Do not rely on colour alone for status.

## 10. Testing strategy

### Component tests

Add tests for:

- active filter grouping and clearing;
- continue-card priority;
- stable flashcard action positions;
- reader page/title rules;
- bottom inset handling;
- StatusNotice actions and states.

### Screenshot tests

For each redesigned screen capture:

- phone light;
- phone dark;
- phone at 130% font scale;
- desktop light where the shared component changes;
- empty, populated, loading and error states where relevant.

### Journey tests

Add or extend end-to-end Compose navigation tests for:

1. Home -> continue course -> finish lesson -> next action.
2. Reader -> select word -> set status -> close sheet without losing position.
3. Flashcards -> show answer -> rate -> session complete -> continue reading.
4. Vocabulary -> see active filter -> show all -> reopen filters.
5. Playlist -> missing voice -> use system voice -> resume.

## 11. Delivery phases

### Phase 0 — Baseline and safeguards

- Preserve and review existing uncommitted work.
- Capture current reference screenshots.
- Confirm terminology and mobile navigation labels.
- Add missing test fixtures for key states.

### Phase 1 — Mobile reader

- Compact phone toolbar.
- Remove duplicate header/card chrome.
- Move sentence audio to the gutter.
- Add stable bottom reader controls.
- Preserve context across term, settings, examples and conjugation overlays.

### Phase 2 — Shared compact patterns

- CompactContinueCard.
- DenseContentRow.
- CompactFilterBar.
- ActiveFilterSummary.
- StatusNotice.
- Shared bottom inset behaviour.

### Phase 3 — Home, Courses and Books

- Apply task priority and continue-first hierarchy.
- Reduce card height and repeated metadata.
- Consolidate mobile filters.

### Phase 4 — Vocabulary and Flashcards

- Make filtering explicit.
- Stabilise review controls and show rating intervals.
- Improve completion next steps.

### Phase 5 — Discovery

- Harmonise News, Wikis and Find books.
- Apply shared filter summary and result-card patterns.

### Phase 6 — More, Settings and resources

- Regroup destinations.
- Standardise resource state and recovery flows.
- Complete the Settings index.

### Phase 7 — Desktop harmonisation

- Apply shared hierarchy, terminology and states.
- Retain desktop tables, panes and high information density.

## 12. First implementation slice

Start with the mobile Reader because it has the highest impact and is largely contained in files that are currently not modified in the working tree.

Initial slice:

1. Add a compact phone reader toolbar with back, one-line title, optional page count and reader-settings action.
2. Remove the duplicate phone breadcrumb/title area.
3. Remove phone text-card margins and use 16dp reading gutters.
4. Place sentence play controls in a fixed gutter rather than inline with words.
5. Keep existing playback behaviour and term selection unchanged.
6. Update/add phone screenshots at normal and 130% font scale.
7. Run reading feature tests and shared UI tests affected by the toolbar.

Expected primary files:

- `feature/reading/src/commonMain/kotlin/com/tayra/languages/feature/reading/ReadingScreen.kt`
- `feature/reading/src/jvmTest/kotlin/com/tayra/languages/feature/reading/ReadingHoverTest.kt`
- `feature/reading/src/jvmTest/kotlin/com/tayra/languages/feature/reading/ReadingTextTest.kt`

Only touch shared core UI components if the reader cannot implement the behaviour locally without duplication. If shared files are required, reconcile their current uncommitted changes first.

## 13. Definition of done

The redesign is complete when:

- Core journeys lead naturally to a next activity.
- Every top-level phone screen exposes useful content above the fold.
- Reader text is the dominant phone experience.
- Important filter and download state is explicit.
- Flashcard actions remain stable and explain their result.
- Mobile and desktop use the same terminology and status model.
- No primary label clips at 130% font scale.
- All interactive controls meet the 48dp touch-target requirement.
- Existing themes remain coherent without screen-specific hard-coded colours.
- Updated component, screenshot and journey tests pass.

## 14. Design references

Mobile redesign mockups:

- `design/mobile-redesign-2026-10-23/01-home.png`
- `design/mobile-redesign-2026-10-23/02-courses.png`
- `design/mobile-redesign-2026-10-23/03-reader.png`
- `design/mobile-redesign-2026-10-23/04-vocabulary.png`
- `design/mobile-redesign-2026-10-23/05-news.png`

Existing audits:

- `docs/ui-audit-2026-10-19/index.html`
- `docs/ui-audit-2026-10-20/index.html`
