package com.tayra.languages.core.data.db

import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.js.Promise

/** Loads sql.js, whose loader the web app serves beside the worker scripts. */
private fun loadSqlJs(): Promise<JsAny?> = js(
    """(window.initSqlJs ? Promise.resolve() : new Promise(function (resolve, reject) {
            var script = document.createElement('script');
            script.src = 'sql-wasm.js';
            script.onload = resolve;
            script.onerror = function () { reject(new Error('Could not load sql-wasm.js')); };
            document.head.appendChild(script);
        })).then(function () { return window.initSqlJs({ locateFile: function (file) { return file; } }); })""",
)

/** A new database in memory. */
private fun openSqlite(sqlJs: JsAny?): JsAny = js("new sqlJs.Database()")

private fun runSqlite(db: JsAny, sql: String): Unit = js("db.run(sql)")

/** The database's file as base64. */
private fun exportSqlite(db: JsAny): String = js(
    """(function () {
        var bytes = db.export();
        var binary = '';
        for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
        return btoa(binary);
    })()""",
)

private fun closeSqlite(db: JsAny): Unit = js("db.close()")

/** Runs the queries on the database in the base64 [file], answering their rows as JSON. */
private fun querySqlite(sqlJs: JsAny?, file: String, queriesJson: String): String = js(
    """(function () {
        var binary = atob(file);
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        var db = new sqlJs.Database(bytes);
        try {
            return JSON.stringify(JSON.parse(queriesJson).map(function (sql) {
                var result = db.exec(sql);
                return result.length ? result[0].values : [];
            }));
        } finally {
            db.close();
        }
    })()""",
)

internal actual suspend fun newSqliteFile(write: suspend (execute: suspend (String) -> Unit) -> Unit): ByteArray {
    val sqlJs = loadSqlJs().await<JsAny?>()
    val db = openSqlite(sqlJs)
    try {
        write { sql -> runSqlite(db, sql) }
        return Base64.decode(exportSqlite(db))
    } finally {
        closeSqlite(db)
    }
}

internal actual suspend fun readSqliteFile(file: ByteArray, queries: List<SqliteQuery>): List<List<List<String?>>> {
    val sqlJs = loadSqlJs().await<JsAny?>()
    val results = Json.parseToJsonElement(querySqlite(sqlJs, Base64.encode(file), Json.encodeToString(queries.map { it.sql }))).jsonArray
    return results.mapIndexed { i, rows ->
        rows.jsonArray.map { row -> List(queries[i].columns) { column -> row.jsonArray.getOrNull(column)?.jsonPrimitive?.contentOrNull } }
    }
}
