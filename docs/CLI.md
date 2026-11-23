# The `idiomiko` command

The desktop app comes with a command-line tool, `idiomiko`, that works on the same library as the
app: the books, courses, words and their statuses, backups and settings. It is meant for people
who like a terminal and for AI agents, which can read and change the library through it.

## Installing

`idiomiko` is inside the installed app, and the app puts it on the `PATH` by itself the first
time it runs, in the background and without asking. Terminal windows opened after that find it.

- **macOS and Linux:** a small script goes into `~/.local/bin`, and when the shell does not
  search that folder yet, one line in its profile (`~/.zshrc`, `~/.bash_profile` or
  `~/.bashrc`, or a file in `~/.config/fish/conf.d`) adds it to the `PATH`.
- **Windows:** an `idiomiko.cmd` that runs the launcher is put in `%LOCALAPPDATA%\Idiomiko\bin`, and that folder is added to the user's `PATH`.

It happens once per computer: when a terminal already finds an `idiomiko`, the app leaves it
alone, and a failed attempt is only noted in the app's log. To take the command off again, delete
`~/.local/bin/idiomiko` and the profile line marked `Added by Idiomiko` (on Windows,
`%LOCALAPPDATA%\Idiomiko\bin\idiomiko.cmd` and that folder's `PATH` entry); the app does not put
it back. Either way, the launcher can be run by its full path:

| System | Where the launcher is |
| --- | --- |
| macOS | `/Applications/Idiomiko.app/Contents/MacOS/idiomiko-cli` |
| Windows | `%LOCALAPPDATA%\Idiomiko\idiomiko-cli.exe` |
| Linux (DEB/RPM) | `/opt/idiomiko/bin/idiomiko-cli` |
| Linux (tarball) | `idiomiko/bin/idiomiko-cli` where it was unpacked |

A run of the app from the sources has no launcher to put on the `PATH`, so it sets nothing up.
There, run the command with `./gradlew :cli:run --args="books list"`, or `./gradlew :cli:installDist`
and then `cli/build/install/idiomiko/bin/idiomiko`.

## Using it

```bash
idiomiko init                                   # on a new computer: languages, tutorials, courses
idiomiko languages use pt                       # the language to work in (also changes the app's)
idiomiko books list
cat story.txt | idiomiko books add --title "Uma história" --tags a1 --file -
idiomiko read page 42                           # how much of the page is new, and which words
idiomiko terms set-status known eu você
idiomiko terms add casa --translation house --status 2
idiomiko courses create --title "No mercado" --level A2
idiomiko lessons add <course-id> --title "As frutas" --file frutas.txt
idiomiko backups create
idiomiko --help                                 # every command; `idiomiko <command> --help` for one
```

`-l/--language` picks another language for one command, by name or ISO code; without it the
command works in the language being learned in the app.

| Group | Commands |
| --- | --- |
| `init` | set up the library as the app's first start does |
| `languages` | `list [--available]`, `add`, `use` |
| `books` | `list`, `show`, `text`, `add`, `edit`, `archive`, `unarchive`, `delete` |
| `courses` | `list`, `show`, `create`, `edit`, `delete` |
| `lessons` | `add`, `edit`, `move`, `delete`, `open` (gives the book id to read) |
| `packs` | `list`, `download`, `remove` (the ready-made course packs) |
| `dictionaries` | `convert` (a Yomitan, MDict or StarDict dictionary into the app's pack file, outside the library) |
| `terms` | `list`, `show`, `add`, `set-status`, `delete` |
| `read` | `page` (each word with its status), `mark-read` |
| `export` | `anki`, `terms --format csv\|json` |
| `backups` | `list`, `create`, `restore`, `import`, `export`, `delete` |
| `account` | `register`, `login`, `logout`, `status`, `send-code`, `confirm`, `devices`, `delete` |
| `cloud` | `list`, `backup`, `restore`, `delete` (library backups kept in the account) |
| `sync` | sends the words, texts, reading and learning setup changed here and takes those changed elsewhere; the first sync into an account holding another library needs `--yes` |
| `stats` | reading and vocabulary statistics |
| `settings` | `get [name]`, `set <name> <value>` |
| `level` | `get`, `set <rank>` (the vocabulary level) |
| `commands` | describes every command and option |

Statuses are written `0`/`unknown`, `1` to `4` (learning), `5`/`known` and `i`/`ignored`.

## For scripts and AI agents

- `--json` makes every command print JSON on stdout. Field names stay as they are; new fields
  may be added.
- `idiomiko --json commands` describes every command, argument and option, for an agent to read
  before it starts.
- Messages and errors go to stderr. Exit codes: `0` done, `1` bad arguments or an unexpected
  error, `2` not found, `3` input refused (such as an empty title, or a missing `--yes`), `4`
  conflicts with the open app.
- Nothing asks questions. Deleting, removing and restoring need `--yes`; without it the command
  says what it would do and exits with `3`.
- Text comes from `--text`, or `--file`, where `-` reads stdin.
- Commands that create something print its id.
- `--data-dir <folder>`, or the `IDIOMIKO_DATA_DIR` variable, points the command at another data
  folder with its own settings, such as a copy of the library to try things on.

## With the app open

The command and the app can be used at the same time. The app shows what the command changed
once it is closed and opened again; commands that change something say so on stderr while the
app is open. `backups restore` refuses to run while the app is open, because the app would write
what it still holds over the restored library; close the app first, or pass `--force` and close
the app right after without changing anything.

Each run starts a Java runtime, so a command takes a second or two.
