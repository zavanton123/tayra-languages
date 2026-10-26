// SQLDelight web worker for the library: the protocol of @cashapp/sqldelight-sqljs-worker, with the
// database kept in IndexedDB between visits. It opens the file saved last, and saves the whole file
// again a moment after the last write, once no transaction is open, and when the page asks to
// ({action: "flush"}, as it is hidden). Only the tab holding the "tayra-library" lock saves, so two
// tabs never write over each other's library; another tab works on its copy, which sync carries.
importScripts("sql-wasm.js");

const DATABASE = "tayra-library";
const STORE = "files";
const KEY = "tayra.db";
const SAVE_AFTER_MS = 1500;
// Writes may never pause for long while the app works in the background: they wait no longer than this.
const SAVE_WITHIN_MS = 10000;

let db = null;
let owner = false;
let inTransaction = false;
let dirty = false;
let timer = null;
let dirtySince = 0;

function idb() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DATABASE, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function readSaved() {
  const store = await idb();
  return new Promise((resolve, reject) => {
    const request = store.transaction(STORE).objectStore(STORE).get(KEY);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function writeSaved(bytes) {
  const store = await idb();
  return new Promise((resolve, reject) => {
    const transaction = store.transaction(STORE, "readwrite");
    transaction.objectStore(STORE).put(bytes, KEY);
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error);
  });
}

/** Whether this tab holds the library; the lock is kept until the tab closes. */
function claim() {
  if (!self.navigator || !navigator.locks) return Promise.resolve(true);
  return new Promise((resolve) => {
    navigator.locks.request(DATABASE, { ifAvailable: true }, (lock) => {
      resolve(lock !== null);
      return lock ? new Promise(() => {}) : undefined;
    });
  });
}

async function open() {
  const SQL = await initSqlJs({ locateFile: () => "sql-wasm.wasm" });
  owner = await claim();
  const saved = await readSaved().catch((err) => { console.error("Could not read the saved library", err); return undefined; });
  db = new SQL.Database(saved ? new Uint8Array(saved) : undefined);
}

function changed() {
  if (!dirty || !dirtySince) dirtySince = Date.now();
  dirty = true;
  if (!owner) return;
  clearTimeout(timer);
  timer = setTimeout(save, Math.max(0, Math.min(SAVE_AFTER_MS, dirtySince + SAVE_WITHIN_MS - Date.now())));
}

async function save() {
  if (!owner || !dirty) return;
  if (inTransaction) return changed();
  dirty = false;
  dirtySince = 0;
  // Exporting closes and reopens the database, which sets its pragmas back.
  const bytes = db.export();
  db.exec("PRAGMA foreign_keys = ON;");
  await writeSaved(bytes).catch((err) => { dirty = true; console.error("Could not save the library", err); });
}

const READS = /^\s*(SELECT|WITH|EXPLAIN|PRAGMA\s+\w+\s*;?\s*$)/i;

function handle(data) {
  switch (data && data.action) {
    case "exec": {
      if (!data.sql) throw new Error("exec: Missing query string");
      const results = db.exec(data.sql, data.params)[0] ?? { values: [] };
      if (!READS.test(data.sql)) {
        if (inTransaction) dirty = true;
        else changed();
      }
      return postMessage({ id: data.id, results });
    }
    case "begin_transaction":
      inTransaction = true;
      return postMessage({ id: data.id, results: db.exec("BEGIN TRANSACTION;") });
    case "end_transaction": {
      const results = db.exec("END TRANSACTION;");
      inTransaction = false;
      if (dirty) changed();
      return postMessage({ id: data.id, results });
    }
    case "rollback_transaction": {
      const results = db.exec("ROLLBACK TRANSACTION;");
      inTransaction = false;
      return postMessage({ id: data.id, results });
    }
    case "flush":
      clearTimeout(timer);
      return save();
    default:
      throw new Error(`Unsupported action: ${data && data.action}`);
  }
}

const opened = open();
opened.catch((err) => console.error("Could not open the library", err));

self.onmessage = (event) => {
  const data = event.data;
  opened.then(() => handle(data)).catch((err) => {
    if (data && data.id !== undefined) postMessage({ id: data.id, error: String(err) });
    else console.error(err);
  });
};
