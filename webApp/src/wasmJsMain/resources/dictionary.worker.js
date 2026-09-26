// SQLDelight web worker for downloaded dictionary packs: same message protocol as
// @cashapp/sqldelight-sqljs-worker, but the database is loaded from a prebuilt SQLite file
// announced by a first {action: "load", url} message instead of created empty. The file was
// stored in the Cache API by the app; it is gzip-compressed and inflated with DecompressionStream.
importScripts("sql-wasm.js");

let loading = null;
let db = null;

async function load(url) {
  const SQL = await initSqlJs({ locateFile: () => "sql-wasm.wasm" });
  const cache = await caches.open("tayra-dictionaries");
  const response = (await cache.match(url)) ?? (await fetch(url));
  if (!response.ok) throw new Error(`Could not fetch dictionary ${url}: ${response.status}`);
  const inflated = new Response(response.body.pipeThrough(new DecompressionStream("gzip")));
  db = new SQL.Database(new Uint8Array(await inflated.arrayBuffer()));
}

function handle(data) {
  switch (data && data.action) {
    case "exec":
      if (!data.sql) throw new Error("exec: Missing query string");
      return postMessage({ id: data.id, results: db.exec(data.sql, data.params)[0] ?? { values: [] } });
    case "begin_transaction":
      return postMessage({ id: data.id, results: db.exec("BEGIN TRANSACTION;") });
    case "end_transaction":
      return postMessage({ id: data.id, results: db.exec("END TRANSACTION;") });
    case "rollback_transaction":
      return postMessage({ id: data.id, results: db.exec("ROLLBACK TRANSACTION;") });
    default:
      throw new Error(`Unsupported action: ${data && data.action}`);
  }
}

self.onmessage = (event) => {
  const data = event.data;
  if (data && data.action === "load") {
    loading = load(data.url);
    loading.catch((err) => console.error(err));
    return;
  }
  if (!loading) {
    return postMessage({ id: data.id, error: "Dictionary worker received a query before load" });
  }
  loading.then(() => handle(data)).catch((err) => postMessage({ id: data.id, error: String(err) }));
};
