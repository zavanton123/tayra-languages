# Tayra Languages: synchronisation backend design

Status: proposal, 2026-10-26, decisions settled the same day. Nothing here is implemented yet.

## 0. Decisions

Settled with the owner on 2026-10-26; the rest of the document follows them.

| Question | Decision |
|---|---|
| Scope of the subscription | Sync and cloud backup only; one `active_until` flag per account |
| Business entity | The owner is a Georgian individual entrepreneur. Google Play's merchant program does not cover Georgia and Stripe does not onboard Georgian businesses (verify both). The owner will **form a company in Uruguay or Paraguay** (both on Google Play's merchant list, as far as known) and put Paddle, the Apple and Google developer accounts and the app listings under that one entity. **The paid release waits for the company**; the client-side sync work does not |
| Stores and billing | Both apps will be published. Three billing sources feed one entitlement: **Paddle** (merchant of record, handles VAT) on the web and desktop, **StoreKit** on iOS (Apple requires in-app purchase in any app that unlocks a subscription bought elsewhere), **Google Play Billing** on Android once the company's merchant account exists |
| Devices per account | Unlimited; the Account screen lists signed-in devices and can sign one out remotely |
| Pricing | One plan, monthly and yearly, no trial |
| Web app | Sync later (Phase 3); desktop and phones first |
| Settings that sync | Language choices (native, my languages, current, Chinese script, per-language dictionaries) and learning setup (vocabulary levels, flashcard limits, practice setup and presets). Reading and speech preferences stay per device |
| Removing a language | Removes it everywhere after sync; the dialog says so |
| Hand-made and imported texts | Sync whole, with a size cap of a few MB per text; larger ones sync progress only |
| Practice examples | Sync (small, chosen per word); the translation cache does not |
| Triggering | Manual (button, pull-to-refresh) in Phase 2; automatic in Phase 4 |
| First sign-in on a device with data | Combine, after a dialog explaining what merges; replacing a device goes through cloud backup restore |
| Hosting | A platform (Fly.io or Railway) with managed Postgres (Neon or the platform's), EU region |
| Sign-in methods in Phase 1 | Email + password (transactional mail through Resend or Postmark on the owner's domain) and Google; Apple Sign In before the App Store listing |
| Account deletion and export | Both in Phase 1; export is the existing backup file |
| Repository | This one, as a `backend` module excluded from app builds and the release workflow |

## 1. What the current code gives us

Facts that shape the design, taken from the repository and a real library:

- **Every platform runs the same SQLDelight schema** (`core/data/.../db/*.sq`), with foreign keys
  on, and `BackupRepositoryImpl` already moves a whole library between platforms as one SQLite
  file plus a `backup_settings` table. Cross-platform compatibility is solved; sync does not need
  to re-solve it.
- **Primary keys are `INTEGER PRIMARY KEY AUTOINCREMENT`** on every user table (terms, books,
  pages, playlists, reviews, …), and foreign keys reference those integers. Two devices that each
  create a book both get `id = 137`. Integer ids therefore cannot be the identity of a row on the
  wire.
- **Terms already have a natural key**: `UNIQUE (text_lc, language_id)`. The same word saved on two
  devices is the same term and must merge into one row, not become two.
- **Only a few tables carry change times** (`terms.status_changed_at`, `terms.created_at`,
  `flashcard_reviews.reviewed_at`, `pages.read_date`). There are no `updated_at` columns and no
  tombstones, so today nothing records *what changed since when*.
- **Languages are a catalog**: `LanguageService.syncWithCatalog` recreates the rows, so a
  language's integer id differs between devices. On the wire a language must be named by its
  ISO code, never its id.
- **Much of the database is content, not user data**: course packs seed `courses` and `lessons`
  (8,000 lessons in the sample library), book packs seed `books` with `source_uri = bookpack:…`,
  `sentence_translations` is a one-week cache, `news_articles` is a cache. None of it should sync;
  the other device downloads the same pack.
- **Size**: a heavy library is 62 MB on disk with 35k terms, 136 books, 2.6k pages. As records
  the user data is roughly 10–15 MB of JSON once, and kilobytes per day after that.
- **The client already has** Ktor client 3.6, kotlinx-serialization, a `SecureStore` for secrets
  and an `HttpClientFactory` per platform. The 58-field `UserSettings` lives outside the database
  in a key-value store and is mostly device-specific (theme, fonts, speech engine, paths).
- `terms.sync_status` is an unused column inherited from LWT. It can be dropped or ignored.

## 2. Synchronisation strategy

### 2.1 The options

| Strategy | How | Merges two devices' edits? | Transfer | Verdict |
|---|---|---|---|---|
| **Whole file** | Upload the backup file; download replaces the local file | No: last upload wins, the other device's edits are lost | 15–60 MB every sync | Fine as *cloud backup*, not as sync |
| **Whole records, no history** | Upload every row each time; server compares | Only with per-row timestamps, which the schema lacks today | Full library each sync | Worse than incremental for the same work |
| **Incremental changes (change log)** | Each row has a stable id, an `updated_at` and a tombstone when deleted; a device pushes rows changed since its last push and pulls rows others changed since its last pull | Yes, row by row, by rules per table | Kilobytes after the first sync | **Recommended** |
| **CRDT library** (Automerge, cr-sqlite, PowerSync, ElectricSQL) | A library tracks every column change | Yes, automatically | Small | More machinery than the data needs; cr-sqlite has no wasm/iOS story that fits SQLDelight, PowerSync/Electric are hosted services with their own client stacks |
| **Hybrid** | Incremental sync for data, whole-file cloud backup as a safety net | Yes | Small, plus an optional backup upload | **Recommended**: backup is nearly free given `BackupRepositoryImpl` |

Why not whole-file sync even though the file is cross-platform: the user's scenario in §5 of the
brief (edit on phone, edit on desktop, sync both) cannot work without merging, and the file format
has no way to merge. Keep the file as "Cloud backup" (upload the file the app already makes;
restore on another device), which is useful on its own, ships in days, and is the escape hatch if a
sync bug ever corrupts a library.

### 2.2 Row identity on the wire

Every synced row gets a **sync key** that is the same on every device:

| Table | Key on the wire |
|---|---|
| `terms` | `language code + "/" + text_lc` (the natural key; merges the same word from two devices) |
| `term_parents`, `level_terms` | not records of their own: a word's record carries its parents' texts and whether it is on the vocabulary level |
| `flashcards`, `practice_progress` | the term's key (one row per term) |
| `flashcard_reviews`, `words_read` | append-only: `term key + reviewed_at` / `language + read_at + page key`; never updated, never deleted |
| `practice_examples` | `term key + sentence` (already `UNIQUE (term_id, sentence)`) |
| `books` | a new `sync_id TEXT` (UUID v4, generated at creation, migration fills existing rows) |
| `pages` | `book sync_id + page_order` |
| `sentences` | not synced: rebuilt from the page text on arrival (they are derived) |
| `page_bookmarks`, `book_stats` | book sync_id + page_order / book sync_id |
| `book_tags`, `book_tag_map` | tag name; map by book sync_id + tag name |
| `playlists` | a new `sync_id` |
| `playlist_items` | playlist sync_id + book sync_id |
| `vocabulary_levels` | language code |
| `languages` + `language_dictionaries` + `language_scripts` | language code (only the user's settings on the row: dictionaries, script, show_romanization) |
| settings (subset) | the setting key |

Locally the integer ids stay as they are; the sync layer translates keys to ids when applying a
pull (look up the term by `(language_id, text_lc)`, the book by `sync_id`) and ids to keys when
pushing. No repository or screen changes its queries.

Excluded from sync: `courses`, `lessons`, `course_tags`, `course_tag_map`, `seeded_courses`,
`course_lessons`, `news_articles`, `sentence_translations`, and `books` whose `source_uri` starts
with `bookpack:` (their progress still syncs: see §2.6).

### 2.3 Tracking changes locally: triggers, not repository edits

Add to each synced table `updated_at INTEGER NOT NULL DEFAULT 0` and, for books and playlists,
`sync_id TEXT`. Add two tables:

```sql
CREATE TABLE sync_tombstones (kind TEXT NOT NULL, key TEXT NOT NULL, deleted_at INTEGER NOT NULL, PRIMARY KEY (kind, key));
CREATE TABLE sync_state (id INTEGER PRIMARY KEY CHECK (id = 1), applying INTEGER NOT NULL DEFAULT 0, last_seq INTEGER NOT NULL DEFAULT 0, last_push_at INTEGER);
```

Then SQLite triggers in a SQLDelight migration keep `updated_at` current and write tombstones,
for every write path in the app at once:

```sql
CREATE TRIGGER terms_touched AFTER UPDATE ON terms
WHEN (SELECT applying FROM sync_state) = 0
BEGIN UPDATE terms SET updated_at = (strftime('%s','now') * 1000) WHERE id = NEW.id; END;

CREATE TRIGGER terms_deleted AFTER DELETE ON terms
WHEN (SELECT applying FROM sync_state) = 0
BEGIN INSERT OR REPLACE INTO sync_tombstones VALUES ('term', OLD.language_code || '/' || OLD.text_lc, strftime('%s','now') * 1000); END;
```

Notes:

- SQLite's `recursive_triggers` is off by default, so the `UPDATE` inside an `AFTER UPDATE`
  trigger does not fire the trigger again.
- Cascaded deletes (`ON DELETE CASCADE`) fire `AFTER DELETE` triggers, so deleting a term
  tombstones its flashcard and progress rows for free.
- While a pull is being applied, `sync_state.applying = 1` inside the same transaction, so remote
  rows keep their remote `updated_at` and do not get re-marked as local changes.
- **Backup restore must set `applying = 1` too**, or restoring a backup tombstones every row and
  re-inserts it as new. After a restore, the device should do a full push (every row, with its
  `updated_at`) so the server merges what the backup brought.
- The tombstone key for a term needs the language code; store `language_code` on the tombstone
  via a join in the trigger (`SELECT name FROM languages WHERE id = OLD.language_id`).

Dirty rows for a push are simply `WHERE updated_at > sync_state.last_push_at`, plus the tombstones
newer than that. No outbox table is needed.

### 2.4 The protocol

Two endpoints, JSON, gzip, both idempotent:

```
POST /sync/push      { deviceId, changes: [ { kind, key, updatedAt, deleted, data: {…} } ] }  →  { seq }
GET  /sync/pull?since=<seq>&limit=1000                                                      →  { changes: [...], seq, more }
```

- `kind` is the table name on the wire (`term`, `book`, `page`, …); `data` is the row's columns
  minus ids, with foreign keys expressed as sync keys.
- The server stores one row per `(user, kind, key)` and assigns a per-user, monotonically
  increasing `seq` to each accepted change. `seq` is the pull cursor; it never depends on any
  device's clock.
- A sync is: push dirty rows → pull since `last_seq` → apply, in batches, advancing `last_seq`
  only after each batch is committed locally. An interrupted sync resumes from the last committed
  cursor; re-pushing a row is harmless because the merge is idempotent.
- The pull echoes the device's own pushes too (simplest server); the client skips changes whose
  `deviceId` is its own, or applies them (a no-op by timestamp).
- Payload size cap (say 5 MB per push) and a `more` flag on pull keep requests small on mobile.

Why plain HTTP and manual triggering: the brief asks for manual sync, offline-first and no
permanent connection. Background or automatic sync (on app start, after N changes, pull-to-refresh)
is a client scheduling decision on top of the same two calls. WebSockets, push notifications and
realtime are not needed.

### 2.5 Merge rules

The default is **last writer wins per row**, by `updatedAt`, including tombstones (a delete is a
change with a time), ties going to the greater device id. A few kinds keep more than the winner
(the table below). **The server applies exactly the same merge as the devices**, from the shared
`sync-protocol` module (`SyncMerge.merge`): on push it reads the stored record `FOR UPDATE`,
merges the incoming one into it, and writes the result with a new `seq`. A plain
upsert-if-newer would lose the field-wise rules: the account's copy of a word would drop a status
one device set before another device's later translation edit.

The merge is a join: the same result whatever order and grouping versions meet in, and merging a
version in twice changes nothing (`SyncMergeTest` checks this over random version sets, ties and
deletions included). That property is what brings every device to the same library once all have
synced. Two details keep it true: a word's status and a page's dates are merged on their own
timeline, and a deletion carries them along rather than dropping them; and a card or practice
progress is ranked by one order (last use, a deletion counting as used when it was made, then the
change time), never by two orders mixed. Rules per table:

| Table | Rule |
|---|---|
| `terms` | Field-wise: `status` by `status_changed_at` (already there), `translation`/`sentence`/`romanization` by `updated_at`. A term deleted on one device but given a new status later on another comes back (the later change wins). |
| `flashcards` | The version with the later `last_review` wins whole (scheduling fields belong together). |
| `flashcard_reviews`, `words_read` | Append-only: union, never conflict. |
| `practice_progress` | Later `last_practised` wins. |
| `pages` | `read_date` and `start_date`: the earliest non-null wins (a page read is read); `text`: LWW. |
| `books` | `current_page_id` → carried as `current_page_order`, LWW; `archived` LWW. |
| `vocabulary_levels` | LWW. |
| settings | Per key, LWW, only for the keys chosen to sync (§2.7). |

Scenarios from the brief:

- *Same record edited on two devices offline*: the later edit wins per field (terms) or per row.
  Neither device loses anything it did later than the other.
- *Deleted on one, modified on the other*: the later of the two wins; a modification after the
  deletion resurrects the row, a deletion after the modification removes it. Children of a
  resurrected term (flashcard, progress) come back only if they were also modified later.
- *A device that has not synced for months*: it pushes its months of changes with their original
  timestamps, pulls everything since its old `seq`; the same rules apply. Nothing special, except
  that the pull may run to several batches.
- *Network failure mid-sync*: the cursor advances per committed batch; a failed push is retried
  whole; merging a version in twice changes nothing, so retries are safe.
- *Two devices that both had data before the account existed* (the common first-sync case): each
  does a full push; terms with the same word merge by key; books created independently with the
  same title stay separate (two rows), except pack books and tutorials, which are matched by
  `source_uri`. That is acceptable and should be said in the UI ("Your libraries were combined").

Clock skew: LWW by device clocks is the standard trade-off; a device with a clock a day ahead
wins arguments it should not. Two mitigations cost little: the server rejects `updatedAt` more than
a few minutes in the future (clamping it to server time), and the client learns the server time
offset from the push response and applies it when stamping later changes. A hybrid logical clock
is the full fix and can be added later without changing the protocol.

### 2.6 Content rows and reading progress

Books from course packs and book packs are content: the other device installs the same pack.
Their user data is the progress on them: `pages.read_date`, `books.current_page_id`,
`page_bookmarks`, `book_stats`, `words_read`. Sync these rows keyed by the book's `source_uri`
(`bookpack:<pack>/<book>` or the lesson id) instead of a UUID, and apply them only when the pack is
installed; keep unapplied ones in the local `records` mirror until it is. Hand-made books and
imported texts sync whole, text included (a few MB once).

### 2.7 Settings

Most of `UserSettings` is device-specific (theme, font, speech engine, model paths, hotkeys,
window layout) and must not sync. Sync a small named subset, each a `setting` record keyed by
name: `nativeLanguage`, `myLanguages` (by language codes, not ids), `currentLanguageId` (as a
code), vocabulary-related keys, flashcard daily limits, practice setup. Add the rest one at a time
if asked for.

## 3. Backend stack

### 3.1 Evaluation

| | Java + Spring Boot + JHipster | Kotlin + Spring Boot | Kotlin + Ktor | Supabase (hosted Postgres + Auth) | PocketBase |
|---|---|---|---|---|---|
| Development speed for *this* backend (6–8 endpoints) | Slow to start: generator produces an Angular/React admin, Keycloak-oriented OAuth, hundreds of files to own; then you delete most of it | Fast: Spring Security, Data JDBC, Flyway, Actuator auto-configured; you write the sync and Stripe code | Medium: you assemble auth, DB access, migrations, metrics yourself | Fastest for auth and storage; the sync logic moves entirely client-side | Fast, but hooks are JS/Go |
| Simplicity | Low | Medium | High | High (as long as RLS policies stay simple) | High |
| Performance | Same JVM | Same JVM; virtual threads on Java 21 make MVC handle thousands of concurrent syncs on one core | Lighter, slightly faster startup | Postgres-bound | Good |
| Memory / hosting | 400–600 MB, two containers if Keycloak | 250–400 MB; one container on a €4–6/month VPS | 100–200 MB | Free tier, then $25/month | ~30 MB |
| Email/password + Google | Generated, but tied to its user model and UI | Spring Security + bcrypt + Google ID-token verification: a few hundred lines | ktor-auth-jwt + your own user store | Built in, including Google | Built in, including Google |
| Subscriptions / Stripe | Not generated; same work as Spring | Stripe Java SDK + one webhook controller | Same SDK, same work | Stripe webhook → Edge Function sets a flag; RLS reads it | Hook on webhook |
| Database | Liquibase + JPA generated | Postgres + Flyway + Spring Data JDBC | Postgres + Exposed/jOOQ + Flyway by hand | Postgres | SQLite |
| Scalability | Fine | Fine to tens of thousands of users on one box; stateless, so horizontal later | Same | Vendor-managed | Single node |
| Solo maintainability | Poor: generator upgrades, large surface | Good: you know it, boring, well-documented | Good if you enjoy assembling; fewer guard rails | Good until you need something RLS cannot express | Fine |
| Ecosystem maturity | Mature | Mature | Mature core, thinner for ops concerns | Mature | Young |

Answers to the specific questions:

- **Is Java + Spring Boot + JHipster a good choice?** No. JHipster earns its weight when the
  product *is* a CRUD web application with an admin UI and many entities. This backend has one
  entity of consequence (`records`) and no web UI. You would generate 90 % scaffolding you then
  maintain, and its OAuth path assumes Keycloak or Okta, which is a second service to run. It
  would slow you down, not speed you up.
- **Would Kotlin + Spring Boot provide meaningful benefits over Java?** Yes, two concrete ones
  beyond nicer syntax: (1) a `sync-protocol` KMP module holding the wire DTOs and the merge rules
  compiles into both the apps and the backend, so the two sides cannot drift and the conflict
  rules are tested once; (2) no mental switch between client and server work. Spring Boot 3 +
  Kotlin is first-class (Kotlin DSLs, kotlinx-serialization message converter, coroutine support
  if ever wanted).
- **Is Ktor a better fit?** It is the leaner tool and the natural choice if memory or startup
  time mattered or you wanted a single stack with no Spring. For a solo developer who already
  knows Spring Security's defaults (password hashing, CSRF/CORS, JWT resource server,
  Actuator health/metrics), Spring saves more hours than Ktor's leanness saves euros. The hosting
  difference (~€2/month) is not a reason.
- **Would JHipster accelerate development?** Only the first day. Everything after is slower.
- **Advantage of Kotlin on both sides?** The shared `sync-protocol` module. It is the one place
  where "same language" is a real engineering win rather than a comfort.
- **Alternatives worth naming**: Supabase is the least-code route (Auth with Google, Postgres with
  row-level security, a `records` table, Stripe webhook in an Edge Function flipping an
  `active_until` column that the RLS policy checks; `supabase-kt` supports KMP). It removes the
  backend almost entirely at the cost of lock-in and merge logic living only on clients. If you
  did not want to run a server at all, choose this. PocketBase is the self-hosted equivalent in
  Go. Neither uses your Spring experience.

### 3.2 Recommendation

**Kotlin + Spring Boot 3 (Web MVC on Java 21 virtual threads) + Spring Security + Spring Data JDBC
+ Flyway + PostgreSQL**, as one container, plus a `sync-protocol` KMP module shared with the apps,
all in this repository as a `backend` Gradle module that the app builds and the release workflow
leave out. Hosting (decided): a platform, Fly.io or Railway, in an EU region, with a managed
Postgres (Neon's free tier, or the platform's); the platform runs backups, TLS and restarts, and
the owner never administers a database. Under €10/month until well past the first thousand
subscribers.

Skip: WebFlux (no benefit here), JPA (Data JDBC is simpler for one wide table), Keycloak,
Kubernetes, message queues, Redis.

## 4. Backend data model

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY, email TEXT UNIQUE NOT NULL, email_verified BOOL NOT NULL DEFAULT FALSE,
  password_hash TEXT, google_sub TEXT UNIQUE, apple_sub TEXT UNIQUE, created_at TIMESTAMPTZ NOT NULL,
  subscription_source TEXT,            -- 'paddle', 'apple' or 'google'
  paddle_customer_id TEXT UNIQUE, paddle_subscription_id TEXT,
  apple_original_transaction_id TEXT UNIQUE,
  google_purchase_token TEXT UNIQUE,
  subscription_status TEXT, subscription_active_until TIMESTAMPTZ
);
-- One row per signed-in device; the Account screen lists them and revoking one signs it out.
CREATE TABLE refresh_tokens (id UUID PRIMARY KEY, user_id UUID REFERENCES users, device_id TEXT, device_name TEXT, platform TEXT, last_used_at TIMESTAMPTZ, expires_at TIMESTAMPTZ, revoked BOOL);
CREATE TABLE records (
  user_id UUID NOT NULL REFERENCES users, kind TEXT NOT NULL, key TEXT NOT NULL,
  seq BIGINT NOT NULL, updated_at BIGINT NOT NULL, deleted BOOL NOT NULL DEFAULT FALSE,
  device_id TEXT, data JSONB,
  PRIMARY KEY (user_id, kind, key)
);
CREATE INDEX records_user_seq ON records (user_id, seq);
CREATE TABLE user_seq (user_id UUID PRIMARY KEY REFERENCES users, last_seq BIGINT NOT NULL DEFAULT 0);
CREATE TABLE backups (user_id UUID, created_at TIMESTAMPTZ, object_key TEXT, size_bytes BIGINT);   -- cloud backup files live in object storage
```

One generic `records` table is deliberate: the server never needs to know what a term or a book
is, so adding a synced table or column on the client needs no server deployment. The server's only
jobs are isolation (every query filtered by the user from the JWT), ordering (`seq`), and the
shared merge. `data` as JSONB keeps the option of server-side queries later. Push assigns
`seq` inside one transaction with `SELECT … FOR UPDATE` on `user_seq`, so a user's changes are
totally ordered even when two devices push at once.

Size: 35k terms ≈ 15 MB in Postgres; 10,000 heavy users ≈ 150 GB, which is when page texts move
to object storage (`data` keeps a pointer). Not a day-one concern.

Cloud backup files (§2.1) go to S3-compatible object storage (Hetzner Object Storage, Cloudflare
R2, Backblaze B2), one or a few per user, the newest three kept.

## 5. Authentication, OAuth and subscriptions

### 5.1 Tokens

The backend issues its own **access JWT (15 min)** and **refresh token (90 days, rotated on use,
stored hashed, revocable per device)**. Every app stores them in its secure store (`SecureStore`
exists already: Keychain on iOS, EncryptedSharedPreferences on Android, the OS keyring on desktop;
on the web, memory plus an httpOnly cookie for the refresh token). Spring Security's OAuth2
resource server validates the access token on every request; the subscription check is a filter
or a method guard on the `/sync/*` and `/backup/*` routes reading `subscription_active_until`
from the user row (cached a minute).

### 5.2 Email and password

Spring Security `PasswordEncoder` (bcrypt or argon2), email verification and password reset through
one transactional email provider (Resend, Postmark, or SES; all have free tiers), rate limiting on
login and reset (Bucket4j or a simple table), and account deletion that removes `records`, backups
and the user. This is the part JHipster would generate; it is roughly 400 lines by hand.

### 5.3 Google

Do not run the OAuth authorization-code flow on the server for native apps. Each app obtains a
**Google ID token** natively and posts it to `/auth/google`:

- Android: Credential Manager with `GetGoogleIdOption`.
- iOS: GoogleSignIn SDK (the app is sideloaded today; the SDK still works with a bundle id
  registered in Google Cloud).
- Desktop: open the system browser to Google with PKCE and a `http://127.0.0.1:<port>` loopback
  redirect (the pattern every desktop app uses), exchange the code in the app, send the ID token.
- Web: Google Identity Services button.

The server verifies the token's signature against Google's JWKS and its `aud` (one Google client id
per platform, all allowed), then finds or creates the user by `google_sub` (linking to an existing
email account when the email matches and is verified). Spring Security's `JwtDecoder` pointed at
`https://www.googleapis.com/oauth2/v3/certs` does the verification in ten lines. Apple Sign In is
required by Apple if the app ever ships on the App Store with Google sign-in; it works the same
way (verify Apple's ID token) and can wait until then.

### 5.4 Subscriptions

The account has one entitlement, `subscription_active_until`, set by two sources. The apps only
read it (`/me`); `/sync/*` and `/backup/*` return 402 when it has passed. An expired subscription
keeps every local row; the apps show "Renew to sync"; the server keeps the records for 12 months,
so renewing resumes where it stopped.

**Web and desktop: Paddle.** Paddle is a merchant of record: it sells to the customer in its own
name, collects VAT and sales tax worldwide, and pays the owner out. That matters twice here: it
onboards a Georgian individual entrepreneur (Stripe does not, as far as known), and it spares a
non-EU seller the EU VAT registration that digital sales to EU consumers otherwise require.

1. The desktop app and the web app open Paddle's hosted checkout in the browser with the user's
   account id as `custom_data`, for the monthly or the yearly price.
2. Paddle calls `/billing/paddle` for `subscription.created`, `.updated`, `.canceled`,
   `.past_due` and `transaction.completed`; the handler verifies the signature and writes
   `subscription_status`, `paddle_subscription_id` and
   `subscription_active_until = current_billing_period.ends_at + grace (3 days)`.
3. Managing or cancelling goes through Paddle's customer portal link, returned by the backend.

**iOS: StoreKit.** Apple's multiplatform rule (App Review 3.1.3(b)) lets an app unlock a
subscription bought elsewhere only if the same subscription is also offered as an in-app purchase.
So the iOS app sells the same two prices through StoreKit 2 (the small-business rate is 15 %):

1. The app buys with StoreKit and posts the signed transaction (JWS) to `/billing/apple`.
2. The backend verifies it with Apple's `app-store-server-library-java`, stores the
   `originalTransactionId` on the user, and sets `active_until` from the transaction's expiry.
3. Apple's App Store Server Notifications v2 call `/billing/apple/notifications` for renewals,
   expirations and refunds; the same handler updates `active_until`.
4. Apple requires Sign in with Apple in any app that offers Google sign-in, so the iOS app ships
   with both; the backend verifies Apple's ID token the same way it verifies Google's (§5.3).

Buying on the web while a StoreKit subscription is active, or the reverse, is prevented by `/me`
telling the apps which source the entitlement comes from and hiding the other purchase path while
it is active.

**Android: Google Play Billing.** Under the company's merchant account the Android app sells the
same two prices through the Play Billing Library:

1. The app buys; the backend receives the purchase token at `/billing/google` and verifies it
   with the Google Play Developer API (`purchases.subscriptionsv2.get`), stores the token, and sets
   `active_until` from the subscription's expiry.
2. Real-time developer notifications (a Pub/Sub topic) call `/billing/google/notifications` for
   renewals, cancellations, grace periods and refunds; the handler re-queries the purchase and
   updates `active_until`.

Should the company be delayed, the Android app can ship earlier as sign-in only: Play policy allows
an app to give access to a subscription bought outside the app provided it neither sells it nor
directs the user to where it is sold (no price, no link, no mention of the website). That is a
fallback, not the plan.

**One entitlement, three handlers.** `/me` reports the source (`paddle`, `apple`, `google`) so an
app hides its own purchase path while another source is active, and so "Manage subscription"
sends the user to the right place (Paddle's portal, the App Store, or Google Play). RevenueCat
would wrap all three behind one SDK and one webhook and is worth checking for Paddle support once
the accounts exist; otherwise three handlers of a few hundred lines each are fine and keep the
backend free of a vendor.

## 6. Client architecture (KMP)

New modules:

- `sync-protocol` (commonMain + jvm): the DTOs (`SyncChange`, `PushRequest`, `PullResponse`,
  `AuthTokens`, …) with kotlinx-serialization, the wire-key functions (term key, book key), and the
  pure merge functions (`mergeTerm(local, remote)`, …). Used by `core/data` and by the backend.
- `core/sync` (commonMain): `AuthSession` (tokens in `SecureStore`, refresh), `TayraApi` (Ktor
  client, already configured per platform), `SyncEngine` (collect dirty rows → push → pull → apply
  in a transaction with `applying = 1` → advance cursor), `CloudBackup` (upload/download the
  existing backup file), and a `SyncStatus` flow for the UI.
- `feature/account`: Account screen (sign in with email or Google, subscription status, "Sync
  now", last sync time, "Sign out of this device"), plus pull-to-refresh hooks on the phone's
  Books and Vocabulary lists, and the Settings → Account entry.

Schema changes in `core/data` (one migration): `updated_at` on synced tables, `sync_id` on books
and playlists (filled with UUIDs for existing rows), `sync_tombstones`, `sync_state`, the
triggers; `BackupRepositoryImpl.restore` sets `applying`; `ClearDataService` and
`LanguageRemovalService` run with `applying = 0` so removals produce tombstones (removing a
language on one device removes its words everywhere, which is what the user asked for) — or with
`applying = 1` if removal should stay local; decide and say so in the dialog.

The engine does not touch repositories or screens; the triggers see every write they make. SQL
queries for "dirty rows since" and "apply row by key" live in a new `Sync.sq`.

## 7. Technical challenges, in order of how much they will hurt

1. **Integer ids and foreign keys.** Solved by wire keys (§2.2); the cost is a translation layer
   in `SyncEngine` and one `sync_id` column on two tables. Do not change primary keys.
2. **No change tracking today.** Solved by triggers (§2.3). The trap is any bulk operation that
   must not count as user changes: backup restore, catalog sync, pack installs. Each needs
   `applying = 1` or an explicit decision.
3. **Terms' natural key across languages whose ids differ per device.** Always carry the language
   code; resolve to the local id on apply; if the language does not exist locally (not in this
   build's catalog), keep the record unapplied and apply it later.
4. **Pack-derived rows.** Progress on pack books and lessons keyed by `source_uri`, applied when
   the pack is present (§2.6). Easy to get wrong in the first version; test it with the tutorial
   books, which every device has.
5. **Clock skew.** LWW mitigations in §2.5. Say in the UI which device's change won when it
   matters? No: nobody reads that. Keep it silent, keep cloud backups.
6. **First sync of two libraries that both already have data.** Duplicate hand-made books with
   the same title; the vocabulary merges cleanly. Tell the user before the first sync what will
   happen ("Combine this device's library with your account's").
7. **Large first upload on mobile data.** Batch pushes of ~1,000 records, gzip, show progress,
   resume by cursor. 15 MB once is fine on Wi-Fi; warn on cellular.
8. **Schema evolution with devices on different app versions.** Records carry a `v` field; the
   client ignores unknown fields and fills defaults for missing ones; the server stores whatever it
   gets. Never remove a field's meaning, only add fields. An old app pulling a record from a newer
   app must still apply what it understands.
9. **The web app.** Same schema on wasm SQLite (OPFS); tokens cannot use the keychain, so the
   refresh token goes in an httpOnly cookie and CORS is configured for the app's origin only.
10. **Account deletion and export.** GDPR requires both; export is the existing backup file,
    deletion is one endpoint; do them in the first release, they are cheap now and awkward later.

## 8. Roadmap

The paid release waits for the company, which sets the order: everything that does not need a
billing account is built first, against a backend whose entitlement is granted by hand for test
accounts. Billing is the last thing wired in, not the first.

Done so far: Phases 0, 1 and 2. Sync runs from the account page's "Sync now" and from `tayra
sync`; pull-to-refresh moved to Phase 5 with automatic sync. What Phase 2 settled on the way:

- Words left unknown (status 0) are records too: a saved word set back to unknown is a change, and
  a word's parents may be unknown words.
- A language made from the catalog has `updated_at = 0`: its defaults are no one's change, so a
  device installed later does not overwrite settings chosen on another one. A device that lacks a
  language a pulled record needs makes it from the catalog first.
- Signing in to another account (or another service) starts over: the next sync sends everything
  and takes everything.
- Applying a language removal deletes that language's words and level older than the removal;
  records made after it stay.

**Phase 0: client groundwork (about a week, no backend).**
Migration with `updated_at`, `sync_id`, tombstones, `sync_state`, triggers; `applying` in
restore/clear/removal; `sync-protocol` module with keys and merge functions and their unit tests
(`PracticeTest`-style, pure). Nothing visible to users. Can start now.

**Phase 1: accounts, backend skeleton and cloud backup (3 weeks).**
`backend` module: register/login/refresh/logout with verification and reset mail, Google and
Apple ID-token login, `/me` with the entitlement (set by hand in the database for now), the device
list and remote sign-out, `/backup` upload/list/download to object storage, account deletion and
export. Apps: Account screen without purchase buttons, "Back up to cloud" / "Restore from cloud".
Deployed to the platform in the EU region so the owner's own devices use it from here on.

**Phase 2: record sync for vocabulary (2–3 weeks).**
`/sync/push` and `/sync/pull`; `SyncEngine`; terms, term_parents, flashcards, flashcard_reviews,
practice_progress, practice_examples, level_terms, vocabulary_levels, language settings. This is
the data users care most about and the smallest in bytes. "Sync now" button and pull-to-refresh on
the vocabulary.

**Phase 3: texts and progress (2 weeks).**
books (hand-made and imported, with pages, under the size cap), pack-book progress by
`source_uri`, bookmarks, stats, words_read, playlists, the settings subset. First-sync "combine
libraries" dialog. By the end of this phase the product is complete except for taking money, and
the owner has been living on it across their own devices for weeks.

**Phase 4: billing, once the company exists (2 weeks).**
Paddle account, Apple and Google developer and merchant accounts under the company; the three
handlers of §5.4; purchase buttons in each app; "Manage subscription" per source; the 402 gating
switched on. Store listings follow. This is the first paid release.

**Phase 5: polish and operations (ongoing).**
Automatic sync on app start/resume and after a burst of changes; web app sync (cookie refresh
tokens, CORS); conflict telemetry (count of LWW losses) to see whether field-level rules need
extending; rate limiting; Actuator + a tiny status page; a load test with 100 simulated devices;
hybrid logical clock if skew ever shows up in telemetry.

Phases 0–3 need no company, no Paddle and no store accounts; they are the bulk of the work and
can run in parallel with incorporation.

## 9. Portability: moving between hosting providers

The owner may move from the platform to a self-managed box (Hetzner-style) or back at any time.
The backend is kept portable by rule, not by luck:

- **The apps, Paddle, Apple and Google all address the API by a hostname on the owner's domain**,
  never by a platform hostname. Moving is one DNS change.
- **All configuration is environment variables**: database URL, bucket endpoint and credentials,
  JWT secret, provider keys. No platform secrets manager or config API.
- **Plain Postgres 15+** through Spring Data JDBC and Flyway; no provider-only features.
- **S3-compatible object storage** through the AWS SDK with a configurable endpoint: R2, B2,
  Hetzner Object Storage or MinIO on the same box are interchangeable.
- **No platform services**: Spring's scheduler for jobs, stdout for logs, Actuator for health.
- **`docker-compose.yml` in the repository** runs the app, Postgres and MinIO; it is the
  development setup and, unchanged, the self-hosting setup. Self-hosting adds a reverse proxy for
  TLS (Caddy) and a nightly `pg_dump` to the bucket.

A move is `pg_dump`, `pg_restore`, copy the bucket, change DNS. Users do not self-host; the API
base URL is a build-time constant per release.

## 10. Cost and operations summary

- Fly.io or Railway machine (512 MB–1 GB, ~$5–10/month) + Neon Postgres (free tier, then ~$19)
  in an EU region; cloud backup files on Cloudflare R2 or Backblaze B2 (~€1/month per 100 GB).
- Paddle: about 5 % + a fixed fee per transaction, VAT handled; no monthly cost.
- Apple: 15 % of iOS subscription revenue (small-business programme), $99/year developer account.
- Google Play: 15 % of Android subscription revenue (under the first $1M), $25 once for the
  developer account; the merchant account is tied to the company.
- The company itself: incorporation, a local bank account and yearly accounting in Uruguay or
  Paraguay, plus whatever Georgia charges on income earned through it. Not an app cost, but the
  largest fixed cost in this plan; get an accountant's number before incorporating.
- Email: free tiers cover thousands of messages per month.
- Google OAuth and Sign in with Apple: free.
- Monitoring: Actuator health endpoint + an uptime pinger (free tier). Logs on the box.

A single developer can run this for a few thousand subscribers without touching the
infrastructure again; the first scaling step (a bigger box, then a managed Postgres) is a size
change, not an architecture change, because the service is stateless and the data model is one
table.
